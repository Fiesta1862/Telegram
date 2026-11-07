# TeleSave

A standalone Android link-to-video downloader with a liquid-glass inspired interface.

## Features
- Receive links shared from Telegram or other apps.
- Paste a copied link with the in-app clipboard button.
- Download direct video URLs and media files exposed by publicly accessible web pages.
- Android DownloadManager handles background transfers and completion notifications; retry/resume behavior depends on the source server and Android.
- Original vector launcher icon and animated Compose interface.
- GitHub Actions builds a debug APK on pushes, pull requests, and manual runs.

## Important limitations
TeleSave is not an official Telegram client and does not sign in to Telegram. It cannot access private channels, invite-only posts, protected content, or media that requires a Telegram session. Public Telegram post pages do not always expose a downloadable video URL; in those cases the app explains that the media could not be resolved. It does not bypass access controls, DRM, or server restrictions.

Download speed depends on the source server, network, and throttling. Android DownloadManager can manage transfers in the background, but cannot guarantee speeds faster than Telegram.

## Build with GitHub
1. Open **Actions → Build Android APK**.
2. Wait for the workflow to finish successfully.
3. Open the workflow run and download the `telesave-debug-apk` artifact.
4. Extract the ZIP and install `app-debug.apk` on Android.

The workflow produces a debug APK, not a Play Store release-signed APK.

## Build locally
Requirements: JDK 17 and Gradle 8.9.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy
Links are processed on-device. For webpage links, the app requests the public page to look for openly exposed video URLs. It does not upload a Telegram account, password, or user data to a developer server.
