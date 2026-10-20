/*
   THE FORGE  |  Copyright (c) 2026 Yannie D. Forest. All rights reserved.
   NO LICENCE IS GRANTED. No copying, adaptation, redistribution, reverse
   engineering or commercial use without the author's written permission.
   See LICENCE.txt. Every distributed copy carries a build marker.
*/
package com.yannie.carpediem;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.graphics.Color;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends Activity {
    private WebView web;
    // The file chooser bridge. Android WebViews do NOT open <input type=file>
    // on their own: without onShowFileChooser the Import buttons silently do
    // nothing, which is exactly what happened on the tablet.
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 4711;

    // The SAVE bridge. A blob download in a WebView is opened as a page, which
    // showed the raw backup text and lost the app. Export instead calls
    // window.CarpeNative.saveFile, which opens the system Save dialog here and
    // writes the chosen file itself. The Save dialog needs no storage permission.
    private static final int SAVE_FILE_REQUEST = 4712;
    // 642: the saves waiting their turn ({name, mime, base64}); the first is the one whose dialog is open.
    // A single pending file was overwritten when the page saved several at once (Fabrica's five install
    // packages, the Editor's six), so every dialog wrote the last file's bytes under its own name.
    private final java.util.ArrayDeque<String[]> saves = new java.util.ArrayDeque<>();
    private boolean saveOpen;

    // THE MICROPHONE, ASKED FOR PROPERLY (build 1044). Granting the page's
    // request is not enough on Android 6 and later: the app itself must hold
    // the microphone permission, and Android grants that only when the person
    // says yes to its own question. Until 1044 the app never asked, so on a
    // tablet that had not been set by hand, Scriba's microphone stayed silent.
    // Now the page's request waits while Android asks, and is granted or
    // refused by the answer.
    private static final int MIC_REQUEST = 4713;
    private PermissionRequest pendingMic;

    // 9s58: the longest the page waits for the clipboard (see CarpeBridge.readClipboard).
    private static final long CLIPBOARD_WAIT_MS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        // A floating window is an iframe pointing at this same file, so the
        // WebView must let a file:// page load a file:// subframe. Without
        // these the floating windows stay blank on some WebView versions.
        try {
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
        } catch (Throwable ignored) {
        }
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setBackgroundColor(Color.parseColor("#1B6B93"));
        // The app loads only its own HTML from assets, so the only page that can
        // reach this interface is the app's own.
        web.addJavascriptInterface(new CarpeBridge(), "CarpeNative");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        boolean wantsMic = false;
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) wantsMic = true;
                        }
                        if (wantsMic && Build.VERSION.SDK_INT >= 23
                                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            if (pendingMic != null) { try { pendingMic.deny(); } catch (Exception e) {} }
                            pendingMic = request;
                            requestPermissions(new String[]{ Manifest.permission.RECORD_AUDIO }, MIC_REQUEST);
                            return;
                        }
                        request.grant(request.getResources());
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });
        // The app is served over https from its own assets rather than opened
        // as a file. A file:// page CANNOT put another file:// document in an
        // iframe, which is why the floating windows were empty on the tablet and
        // had to fall back to carrying a studio alone. Served this way the origin
        // is ordinary, the iframe is allowed, and a floating window carries the
        // WHOLE application exactly as the desktop one does.
        // The handler only ever reads app/src/main/assets. The INTERNET
        // permission is there for what can use a connection, such as Scriba's
        // first fetch of Whisper; the app itself always comes from inside.
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });
        web.loadUrl("https://appassets.androidplatform.net/assets/the_forge.html");
        setContentView(web);
    }

    // Exposed to the page as window.CarpeNative. The page calls saveFile when the
    // person exports a backup; it hands over the bytes as base64 and this opens the
    // system Save dialog. The file is written in onActivityResult once a place is
    // chosen. Returns true when the dialog is being opened, so the page knows the
    // native path was taken; false lets the page fall back to Copy.
    public class CarpeBridge {
        @JavascriptInterface
        public boolean saveFile(final String name, final String mime, final String b64) {
            if (b64 == null) return false;
            final String type = (mime == null || mime.length() == 0) ? "application/octet-stream" : mime;
            final String fname = (name == null || name.length() == 0) ? "the-forge-backup.json" : name;
            synchronized (saves) { saves.add(new String[]{ fname, type, b64 }); }
            runOnUiThread(new Runnable() { @Override public void run() { nextSave(); } });
            return true;
        }

        // 9s58: THE CLIPBOARD, for the page's own menu (Paste, Paste as plain
        // text, Copy, Cut), so a link copied in any other app pastes from it:
        //   window.CarpeNative.readClipboard()            -> '{"text": ..., "html": ...}'
        //   window.CarpeNative.writeClipboard(text, html) -> true or false
        // Android runs these on its own JavaBridge thread while the page waits
        // for the answer (a synchronous call). The clipboard itself is used on
        // the UI thread, where Android's own text fields use it, and this thread
        // waits for that on a latch for at most CLIPBOARD_WAIT_MS, so the page is
        // never held longer. The answer is null (read) or false (write) when it
        // could not be done in that time or Android refused: the page then tries
        // its next way.
        @JavascriptInterface
        public String readClipboard() {
            final String[] answer = new String[1];
            if (!onUiThread(new Runnable() {
                @Override public void run() { answer[0] = readClipboardNow(); }
            })) return null;
            return answer[0];
        }

        @JavascriptInterface
        public boolean writeClipboard(final String text, final String html) {
            final boolean[] answer = new boolean[1];
            if (!onUiThread(new Runnable() {
                @Override public void run() { answer[0] = writeClipboardNow(text, html); }
            })) return false;
            return answer[0];
        }

        // Android finds a page's call by its name AND its number of arguments,
        // so writeClipboard(text) alone needs this one.
        @JavascriptInterface
        public boolean writeClipboard(final String text) {
            return writeClipboard(text, null);
        }
    }

    // 9s58: the clipboard's work, on the UI thread (see CarpeBridge.readClipboard).
    // Runs work there and waits for it, at most CLIPBOARD_WAIT_MS. True when it
    // finished in time; a fault inside it also counts as finished, and leaves
    // its answer at null or false.
    private boolean onUiThread(final Runnable work) {
        final CountDownLatch done = new CountDownLatch(1);
        runOnUiThread(new Runnable() {
            @Override public void run() {
                try { work.run(); } catch (Throwable ignored) { } finally { done.countDown(); }
            }
        });
        try {
            return done.await(CLIPBOARD_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // The JSON the page reads, or null when the clipboard may not be read now.
    // From Android 10 only the app in focus (or the keyboard) may read it;
    // for any other app getPrimaryClip() answers null, as if it were empty.
    // The app has the focus while she uses its menu. If it has not (another
    // app in front in split screen, say), null tells the page to try its next
    // way, rather than say the clipboard is empty.
    private String readClipboardNow() {
        if (Build.VERSION.SDK_INT >= 29 && !hasWindowFocus()) return null;
        ClipboardManager board = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (board == null) return null;
        StringBuilder text = new StringBuilder();
        String html = "";
        ClipData clip = board.getPrimaryClip();
        if (clip != null) {
            int count = clip.getItemCount();
            for (int i = 0; i < count; i++) {
                ClipData.Item item = clip.getItemAt(i);
                // coerceToText: the text, or a link's address, or what a
                // content link points to, read as text
                CharSequence t = item.coerceToText(this);
                if (t != null && t.length() > 0) {
                    if (text.length() > 0) text.append('\n');
                    text.append(t);
                }
                if (count == 1 && item.getHtmlText() != null) html = item.getHtmlText();
            }
        }
        try {
            JSONObject answer = new JSONObject();
            answer.put("text", text.toString());
            answer.put("html", html);
            return answer.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    // A formatted copy (ClipData.newHtmlText) when html is given, plain text
    // otherwise. Android's Java bridge hands a JavaScript undefined over as the
    // text "undefined", so that word alone counts as "no html".
    private boolean writeClipboardNow(String text, String html) {
        ClipboardManager board = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (board == null) return false;
        String t = text == null ? "" : text;
        boolean rich = html != null && html.length() > 0 && !"undefined".equals(html);
        board.setPrimaryClip(rich ? ClipData.newHtmlText("the-forge", t, html)
                                  : ClipData.newPlainText("the-forge", t));
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
            return;
        }
        if (requestCode == SAVE_FILE_REQUEST) {
            OutputStream os = null;
            try {
                String[] s;
                synchronized (saves) { s = saves.pollFirst(); saveOpen = false; }
                String b64 = s == null ? null : s[2];
                if (resultCode == RESULT_OK && data != null && data.getData() != null && b64 != null) {
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                    os = getContentResolver().openOutputStream(data.getData());
                    if (os != null) { os.write(bytes); os.flush(); }
                }
            } catch (Exception e) {
                // best effort: a failed write leaves the app untouched, and the
                // person can still use Copy to move the backup instead.
            } finally {
                try { if (os != null) os.close(); } catch (Exception e) {}
                nextSave();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 642: on the UI thread, the Save dialog for the first file waiting, when none is open
    private void nextSave() {
        String[] s;
        synchronized (saves) {
            if (saveOpen || saves.isEmpty()) return;
            saveOpen = true;
            s = saves.peekFirst();
        }
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(s[1]);
            intent.putExtra(Intent.EXTRA_TITLE, s[0]);
            startActivityForResult(intent, SAVE_FILE_REQUEST);
        } catch (Exception e) {
            synchronized (saves) { saves.pollFirst(); saveOpen = false; }
            nextSave();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MIC_REQUEST) {
            PermissionRequest req = pendingMic;
            pendingMic = null;
            if (req == null) return;
            boolean ok = grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            try { if (ok) req.grant(req.getResources()); else req.deny(); } catch (Exception e) {}
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
