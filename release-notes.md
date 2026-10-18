# Flow Tube v1.0.1

**Release date:** 2026-10-18

A maintenance release. It fixes the in-app updater, which never worked in v1.0.0, and adds the
attribution and disclaimer notices the project was missing.

## Fixed

- **The in-app updater now works.** v1.0.0 asked GitHub for releases at an old repository address
  that no longer exists, so every update check returned HTTP 404 and the app could never see or
  install a new version. Update checks now point at this project, and the About screen links go to
  the right place. If you are on v1.0.0, install this build once by hand; every release after it
  can be installed from inside the app.

## Added

- **Not affiliated with Google or YouTube.** The app reads from YouTube but has no relationship with
  Google, and now says so plainly on the About screen, on the website and in the store listing.
- **Credit to the upstream project.** Flow Tube is built on the free software project Flow, and the
  bundled notices now carry its licence and copyright.

## Notes

- No changes to playback, the library or your local data.
- Same signing key as v1.0.0, so this installs as a normal update and your data is kept.
- One universal APK for every supported device, as before.
