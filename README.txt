THE FORGE  |  Copyright (c) 2026 Yannie D. Forest. All rights reserved.
NO LICENCE IS GRANTED. No copying, adaptation, redistribution, reverse
engineering or commercial use without the author's written permission.
See LICENCE.txt. Every distributed copy carries a build marker.
--------------------------------------------------------------------------

THE FORGE ON THE FIRE TABLET
============================

Build 651 (9s83), 2026-10-28. Made by The Forge, 28 October 2026.

This package holds everything needed to put The Forge on a Fire tablet
(or any Android tablet or phone), and to rebuild it yourself later on
GitHub, with no help and no tools installed on your computer.

WHAT IS IN THIS PACKAGE
  READ ME FIRST.txt               This guide.
  the_forge.html                  The whole app as one file (Way A below).
  Fire tablet project/            The Android project. GitHub turns it into
                                  the installable app, the-forge.apk (Way B).
  GITHUB_WORKFLOW_paste_this.txt  The build recipe, as a visible file you can
                                  copy from (see Part 2, Step 3).
  LICENCE.txt


TWO WAYS TO HAVE THE FORGE ON THE TABLET
----------------------------------------

WAY A. THE FILE IN THE BROWSER (two minutes, nothing to build)
  1. Copy the_forge.html to the tablet's Download folder (by USB cable,
     by email to yourself, or from cloud storage).
  2. On the tablet open the Files (or Docs) app, go to Download, and tap
     the_forge.html. If asked, open it with Silk Browser.
     If tapping does nothing, open Silk and type this in the address bar:
         file:///sdcard/Download/the_forge.html
  3. Optional: in Silk's menu choose "Add to Home Screen" for a shortcut.
  It runs offline and keeps your work in the browser on this tablet.

WAY B. THE REAL APP (about fifteen minutes the first time)
  the-forge.apk is a real Android app: the seal on the home screen, full
  screen, no browser around it, and it runs offline. GitHub builds it for
  you (Part 2), and you install it (Part 1). Part 2 comes first in time;
  Part 1 is first here because it is what you will repeat most.


PART 1. INSTALL THE APP ON THE TABLET
-------------------------------------

Step 1. Allow the install (once only).
  On the tablet open Settings, then Security & Privacy, then
  "Apps from Unknown Sources". Turn it on for the app you will use to
  open the APK: Files, Docs or Silk Browser. The wording differs a
  little between Fire OS versions.

Step 2. Put the-forge.apk on the tablet. Choose ONE way.

  Straight from GitHub, on the tablet:
    In Silk, sign in at github.com, open your repository, tap
    "Releases" (on the right of the main page, or add /releases to the
    address), then "The Forge for Fire tablet", then the-forge.apk.
    You must be signed in because the repository is private.

  By USB cable from a Windows PC:
    Download the-forge.apk from the Releases page on the PC. Connect the
    tablet and unlock it; it appears in File Explorer under "This PC".
    Copy the-forge.apk into its Download folder.

  By USB cable from a Mac:
    A Mac cannot see a Fire tablet's storage without a helper app,
    because the tablet uses a connection type (MTP) that macOS does not
    read by itself. OpenMTP is one free helper; please check its website
    for the macOS versions it supports. Connect the tablet, unlock it,
    choose "File transfer" if asked, and copy the-forge.apk into Download.

  Without a cable:
    Email the-forge.apk to yourself and open the email on the tablet.

Step 3. Install.
  On the tablet open Files (or Docs), go to Download, tap the-forge.apk,
  then tap Install. The Forge appears with your apps.

UPDATING LATER
  Every APK made from your repository is signed with the same permanent
  key, so a newer APK installs over the old one and keeps everything you
  have written. Repeat Steps 2 and 3 with the new file.

IF THE TABLET SAYS "App not installed"
  This happens once if an older copy of The Forge is on the tablet
  that was signed with a different key (for example one built before
  this package). Android will not replace it. Do this, in this order:
    1. Open the old copy and export a backup of your work.
       Save it somewhere you can find it, such as Download.
    2. Uninstall the old copy (press and hold its icon).
    3. Install the-forge.apk as in Step 3.
    4. Open The Forge and import the backup.
  After this, every future update installs over the top.


PART 2. BUILD THE APK YOURSELF ON GITHUB
----------------------------------------

GitHub builds the APK on its own computers. You install nothing. You
upload the project once; every time you upload a change, GitHub builds a
fresh the-forge.apk and puts it on your repository's Releases page.

Step 1. Make a repository.
  Sign in at github.com. Click the "+" at the top right, then
  "New repository". Name it, for example the-forge-fire.
  Choose Private (your app's files are yours alone).
  Do NOT tick "Add a README file". Click "Create repository".
  (If you already have a repository for The Forge on the Fire tablet,
  you may use it instead. See "Using a repository you already have".)

Step 2. Upload the project.
  On the new repository's page click "uploading an existing file".
  On your computer, open the folder "Fire tablet project" from this
  package, select EVERYTHING INSIDE IT, and drag it onto the page.
  You should see build.gradle, settings.gradle and an "app" folder in
  the list. Wait until every file has finished uploading (the app file
  inside "app" is large, so this can take a minute), then click
  "Commit changes".

Step 3. Add the build recipe. THIS STEP MATTERS.
  The recipe lives in a folder called .github. A Mac hides every name
  that begins with a full stop, so this folder is often left behind
  when you drag files. Without it
  GitHub never builds anything. Add it by hand; it takes one minute:
    a. On the repository page click "Add file", then "Create new file".
    b. In the name box type exactly:
           .github/workflows/build-apk.yml
       (GitHub turns each "/" into a folder as you type.)
    c. Open GITHUB_WORKFLOW_paste_this.txt from this package, select
       all of it, copy it, and paste it into the large box on GitHub.
    d. Click "Commit changes", then "Commit changes" again.
  If GitHub says the file already exists, the folder did upload in
  Step 2. That is fine; skip this step.

Step 4. Wait for the build.
  Click the "Actions" tab. A run called "Build The Forge APK" appears.
  A yellow dot means it is working. It takes about two to five
  minutes. A green tick means it worked.
  If nothing has started after a minute: click "Build The Forge APK"
  on the left, then "Run workflow" on the right, then the green
  "Run workflow" button.

Step 5. Get the APK.
  On the repository's main page, on the right, click "Releases", then
  "The Forge for Fire tablet". Click the-forge.apk to download it.
  The same APK is also on the run's own page under "Artifacts", as a zip.
  Now follow Part 1.

THE SIGNING KEY (kept in the repository's secrets)
  Android installs a newer APK over the old one only when both carry the
  same key, so every build is signed with one key. The key and its
  password live only in this repository's secrets, never in its files:
  on github.com open Settings, Secrets and variables, Actions, and add
    - ANDROID_KEYSTORE_B64: the key file (.p12 or .keystore), as base64;
    - ANDROID_KEYSTORE_PASSWORD: its password;
    - ANDROID_KEY_ALIAS: only if the key's name is not "app".
  Ingenium's Fabrica makes a key on your own device and saves it as a
  .p12 file (Save this key). Keep the file and its password safe, away
  from the computer. The key that earlier builds kept in the folder
  "signing" has been readable by anyone who could read this repository,
  so it is being replaced; until the new one is in the secrets, the
  build still uses it, and says so in its log.

USING A REPOSITORY YOU ALREADY HAVE
-----------------------------------
  You may upload this project over an existing repository for this app.
  Uploading replaces files with the same names and keeps the rest,
  including any "signing" folder and any older key that the build
  already recognises and keeps using.
  Then add or replace the recipe as in Part 2, Step 3 (if the file
  .github/workflows/build-apk.yml already exists, open it, click the
  pencil, select all, paste the new recipe over it, and commit).


IF THE BUILD FAILS
------------------

A red cross within a few seconds, with no steps listed:
  GitHub refused to start the build. The project has not even been
  looked at. This is about the GitHub account, not the app. Open the
  failed run: GitHub shows the reason in a notice near the top. The
  usual reasons are that the account has used its free build minutes
  for this month (private repositories get a monthly allowance), or a
  spending limit or payment problem in the billing settings
  (github.com/settings/billing). Fix what the notice names, then open
  the run and click "Re-run all jobs". GitHub's billing rules change from
  time to time, so please check its own help pages for current figures.

A red cross after a few minutes, with steps listed:
  Click the step with the red cross. The step "If the build failed, show
  why" prints the last 60 lines of the build log; they name the cause.

"No settings.gradle found":
  The project files were not uploaded, or only some were. Upload the
  CONTENTS of the "Fire tablet project" folder again (Part 2, Step 2).

A yellow warning that the signing key could not be saved:
  Download "signing-key-keep-this" from the run's Artifacts, unzip it,
  and upload its files into a folder named "signing" in the project
  (Add file, Upload files, then type signing/ before the file names).


ABOUT THE APP
-------------
  - It runs entirely from the copy of the_forge.html inside it, and works
    with no connection. It is also allowed to use the internet, so that
    anything inside it that can use a connection may do so. Only secure
    (https) addresses load.
  - The first time something in it asks for the microphone, the tablet
    asks "Allow The Forge to record audio?". Choose Allow. If it was
    refused: Settings, Apps & Notifications, The Forge, Permissions.
  - The app's own version is 641; Android usually shows it on the
    app's information page.
  - Its Android identity is com.yannieforest.theforge.
    Keep it the same for every version: a different identity installs
    as a separate, empty app.
