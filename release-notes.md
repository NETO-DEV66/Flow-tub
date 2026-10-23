# NeuroTube v1.5.4

**Release date:** 2026-10-22

A second bug-fix release for the account library import, and the first to bring in the account's
youtube.com data as well as its YouTube Music data.

## Fixes

- **Saved playlists and artists are imported again.** A signed-in music library now answers with
  `lockupViewModel` rows in place of the `musicTwoRowItemRenderer` rows the import knew, and the
  reader walked past the new shape. The import therefore finished with the playlists, artists and
  podcasts lists still empty — the same class of bug that hid the subscriptions. Both shapes are
  read now, on the first page and on every continuation.

## Features

- **Your video playlists come across.** The playlists you keep on youtube.com are imported as
  playlists of their own, so they sit beside the ones saved in YouTube Music.
- **Watch Later and watch history come across.** Your Watch Later list and your watch history are
  imported as well, so a new install starts with the lists you already had.
- **The import reports everything it brought in.** The finished screen now counts video playlists,
  Watch Later and watch history along with the subscriptions, artists, liked songs, playlists and
  albums it already reported.

## Notes

- Importing only reads. Nothing is written back to the account and nothing is deleted.
- Re-importing is safe: anything already present is left as it is, and each part is tried on its
  own, so one failing does not lose the others.
- One universal APK, signed with the same key as every earlier release.

# NeuroTube v1.5.3

**Release date:** 2026-10-22

A bug-fix release for the account library import added in v1.5.0.

## Fixes

- **Subscriptions are imported again.** The import read the channels a signed-in account follows
  from the guide and from the `FEchannels` page, but YouTube now renders those channels in a newer
  `lockupViewModel` row that the reader did not know. The row was walked past, so the import
  finished with the subscriptions list still empty. Both shapes are now read.
- **A long subscription list imports in full.** The token that continues `FEchannels` also arrives
  in a newer shape, so an account with more channels than fit on one page stopped after the first.
  Both the older and the newer continuation rows are followed now.
- **The music library no longer stops after one page.** A library page hangs its next-page token on
  the section list rather than on the grid inside it, and a grid can also append the token as a
  final row. Neither was read, so a large library rendered only its first page. Both are read now,
  and a section-list token is only taken when the page carries a single section, so it is never
  attributed to the wrong one.
- **A continuation row no longer aborts the import.** A grid ends with a bare
  `continuationItemRenderer` row, and two fields on that row were required by the model. The row
  failed to parse, and with it the whole response — so the import brought back nothing at all.
  Those fields are optional now, and the token the row carries is used.

## Notes

- Importing only reads. Nothing is written back to the account and nothing is deleted.
- Re-importing is safe: anything already present is left as it is, and each part is tried on its
  own, so one failing does not lose the others.
- The session is a cookie, stored encrypted on the device only and sent only to YouTube.
- One universal APK, and the same signing key as every earlier release.
