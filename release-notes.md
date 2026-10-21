# NeuroTube v1.5.1

**Release date:** 2026-10-21

A bug-fix release for the account library import added in v1.5.0.

## Fixes

- **Subscriptions now arrive.** After signing in and importing, only liked songs showed up. The
  account's subscription pages — the guide and the "All subscriptions" list — were being requested
  while signed out, so YouTube returned an empty document and the import found nothing to add. They
  are now sent as the signed-in viewer, so the channels your account follows come in as before.
- **The feed is personalised straight away.** An import now teaches the recommendation engine the
  channels it brought in and clears the cached lanes, so the next load is ranked for the account
  you just signed in with rather than the previous identity.

## Notes

- Importing only reads. Nothing is written back to the account and nothing is deleted.
- Re-importing is safe: anything already present is left as it is, and each part is tried on its
  own, so one failing does not lose the others.
- The session is a cookie, stored encrypted on the device only and sent only to YouTube.
- One universal APK, and the same signing key as every earlier release.
