# NeuroTube v1.4.0

**Release date:** 2026-10-20

NeuroTube can now sign in to YouTube. The account is optional: without it the app behaves exactly
as it did in v1.3.0. With it, browse and search use your account, so subscriptions, liked videos
and playlists come back personalised instead of anonymous.

## Added

- **YouTube sign-in.** Settings -> Account opens a Google sign-in page inside the app. The same
  control appears as a card in the onboarding import step, and both can be skipped.
- **A personalised home.** Once a session exists, browse and search ask YouTube for login, so the
  signed-in library is what comes back. Signing out restores anonymous requests immediately.
- **The account name on the settings row**, so you can see who is signed in without opening the page.

## How the session is kept

- The session is a Google cookie captured from the sign-in page and validated against YouTube
  before it is saved. A cookie that YouTube rejects is discarded, not stored.
- It is written encrypted (AES/GCM, key in the Android keystore) to app-private storage that is
  excluded from cloud backup.
- It never leaves the device except in requests to YouTube itself. There is no NeuroTube server
  and no telemetry.
- Signing out clears the session from memory and deletes the saved copy.

## Unchanged

- Signing key: v1.4.0 is signed with the same certificate as every previous release.
- One universal APK (arm64-v8a + armeabi-v7a), no ABI splits.
- Downloads, backups, sync and the recommendation engine are untouched.

## A note on the account

Google's terms do not permit third-party clients, and an account used this way can be rate-limited
or blocked. If you want to sign in, a separate account is the safer choice. Signing out at any time
removes the session from the device.
