# NeuroTube v1.2.0

**Release date:** 2026-10-19

NeuroTube is the new name. The app used to be called Flow Tube; this release renames it everywhere
the name is shown, and keeps every download and setting you already have.

## Changed

- **New name.** The launcher label, the Home header, the notification, the widgets, the About screen
  and the update screen all read NeuroTube now. The name is translated in all 30 bundled languages.
- **New download folders.** New downloads go to `Download/NeuroTube`, `Movies/NeuroTube` and
  `Music/NeuroTube` instead of the old `Flow Tube` folders.
- **Old folders are still read.** The app keeps scanning the previous `Flow Tube` folders, so a file
  saved by an earlier version is still found, listed in Downloads and recovered after a reinstall.
- **Project renamed too.** The README, the project website, the release asset names
  (`NeuroTube-<version>-universal.apk`) and the Gradle project name all use the new name.

## Unchanged

- The application id stays `com.flowtube.app`, so this installs as a normal update over v1.1.0 and
  your library, downloads, playlists and the recommendation profile are kept.
- Same signing key as v1.1.0.
- One universal APK for every supported device, as before.
