# NeuroTube v1.5.2

**Release date:** 2026-10-22

A bug-fix release for the account library import added in v1.5.0.

## Fixes

- **Saved albums are imported.** The import asked the account for its songs, artists and playlists
  but never for the albums it saved, so they were missing from the library afterwards. Albums are
  now requested like every other section and written into the app's own stores.
- **The import no longer stops after one page.** Each library section hands out the rest of its
  contents as a continuation token, and only one of the two token types the endpoint can send was
  being read. A section with more items than fit on one page therefore ended early — with a large
  library, most of it never arrived. Both token types are now followed to the end.
- **Album and artist tiles are no longer dropped.** A saved album carries its playlist id in a
  slightly different place than a normal playlist, and a missing optional field on an artist tile
  aborted the whole import. Both shapes now parse, and a single unexpected item no longer takes the
  rest of the page down with it.

## Notes

- Importing only reads. Nothing is written back to the account and nothing is deleted.
- Re-importing is safe: anything already present is left as it is, and each part is tried on its
  own, so one failing does not lose the others.
- The session is a cookie, stored encrypted on the device only and sent only to YouTube.
- One universal APK, and the same signing key as every earlier release.
