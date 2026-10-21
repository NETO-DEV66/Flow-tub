# NeuroTube v1.5.0

**Release date:** 2026-10-21

A feature release. Signing in now brings the account's own library in: the channels it follows, the
artists it keeps, the songs it liked and the playlists and albums it saved, all written into the
stores the app already reads from.

## New

- **Import your library from your account.** On **Settings → Account**, with a session signed in,
  the **Import library** row reads your account and stores what it finds. No file, no export, no
  copy-paste — it is one tap.
- **Subscriptions.** The channels your account follows are read from YouTube's own list, a page at
  a time, and merged with the guide's short list so the most-used channels arrive even if the full
  page is unavailable. They show up in the Subscriptions feed like any you added by hand.
- **Artists.** The artists your account follows on YouTube Music are added as music subscriptions.
- **Liked songs.** The songs your account liked land in Liked music, newest first.
- **Playlists and albums.** The playlists and albums your account saved are stored with their
  songs, so they open and play offline from the library.
- **The page reports what happened.** While it runs it names the part it is on, and when it
  finishes it lists how many of each kind were added.

## Notes

- Importing is a one-way read: nothing is written back to the account, and nothing is deleted.
  Re-importing is safe — a channel or song already present is left as it is.
- Each part is attempted on its own, so one failing does not lose the others.
- The session is still a cookie, stored encrypted on the device only. It never leaves the phone
  except in requests to YouTube itself.
- One universal APK (arm64-v8a + armeabi-v7a), no ABI splits.
- Same signing key as every earlier release.
- Downloads, backups, sync and the recommendation engine are untouched.

## A note on the account

Google's terms do not permit third-party clients, and an account used this way can be rate-limited
or blocked. If you want to sign in, a separate account is the safer choice. Signing in remains
optional: everything works signed out, exactly as before.
