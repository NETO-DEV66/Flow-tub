# NeuroTube v1.3.0

**Release date:** 2026-10-20

The package name changed from `com.flowtube.app` to `com.neurotube.app`. Android treats a package
name as the app's identity, so this is a one-time break: v1.3.0 installs as a **new app** next to
the old one and does not inherit its data.

**Before you update, export a backup.** Open the old app, go to Settings → Backup, and save a
master backup. Then uninstall the old app, install v1.3.0, and restore that backup.

## Changed

- **New package name.** `com.neurotube.app` replaces `com.flowtube.app`. The launcher label, the
  Home header, the notification, the widgets and the About screen already read NeuroTube; now the
  internal identity matches the name.
- **Downloads in the old folders are still found.** `Download/NeuroTube` is the current location,
  and the app keeps scanning the `Flow Tube` folders from earlier versions, so existing files stay
  listed and are still recovered after a reinstall.
- **Metadata in existing downloads is still read.** Files downloaded by an earlier version carry
  `com.flowtube.app.*` tags; those are still parsed, so titles, artists and video ids survive.

## Unchanged

- Signing key: v1.3.0 is signed with the same certificate as every previous release.
- Media files already in `Download/NeuroTube` are not touched — they live in shared storage, not in
  app-private data.

## Known limitation

- The old app's private data (watch history, playlists, settings, the recommendation engine) cannot
  be carried over automatically. Android does not let a new package read another package's private
  storage, and the in-app updater refuses an APK whose package name differs. The backup export and
  restore path is the way across.
