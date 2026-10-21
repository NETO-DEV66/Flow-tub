# NeuroTube v1.4.1

**Release date:** 2026-10-21

A fix release for the YouTube sign-in added in v1.4.0. Sign-in did not complete: the page opened,
but finishing the flow was impossible. This release makes it work, and keeps the session usable
across restarts.

## Fixed

- **Sign-in now completes.** v1.4.0 loaded the YouTube website and waited for the user to press
  YouTube's own "Sign in" button. That button hands the account chooser to a popup window, which
  the sign-in screen was silently dropping, so nothing happened. The app now opens Google's sign-in
  page directly, with YouTube as the return target.
- **Sign-in no longer stalls part-way.** A navigation filter was refusing Google's own hand-off
  pages, which could strand the flow with no error message. Navigation is now unrestricted.
- **The session is captured whole.** Two values the session was minted with were never read, so
  requests made after signing in were still treated as signed out. Both are read from the page now,
  saved with the session, and re-applied on launch.
- **Signing in or out refreshes the feed immediately**, instead of leaving the previous account's
  videos on screen until the next manual refresh.

## Unchanged

- The session is a Google cookie, validated against YouTube before it is saved, written encrypted
  (AES/GCM, key in the Android keystore) to app-private storage excluded from cloud backup.
- It never leaves the device except in requests to YouTube itself. There is no NeuroTube server and
  no telemetry. Signing out clears it from memory and deletes the saved copy.
- Signing key: v1.4.1 is signed with the same certificate as every previous release.
- One universal APK (arm64-v8a + armeabi-v7a), no ABI splits.
- Downloads, backups, sync and the recommendation engine are untouched.

## A note on the account

Google's terms do not permit third-party clients, and an account used this way can be rate-limited
or blocked. If you want to sign in, a separate account is the safer choice. Signing in remains
optional: everything works signed out, exactly as before.
