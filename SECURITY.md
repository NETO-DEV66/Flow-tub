# Security Policy

## Supported versions

Security fixes land on the latest release line only. Older versions do not
receive backported patches.

| Version | Supported |
| ------- | --------- |
| 1.0.x   | ✅ |
| < 1.0.0 | ❌ |

Always update to the newest release before reporting a security issue.

## Reporting a vulnerability

**Do not report security vulnerabilities through public GitHub issues.**

Report privately through
[GitHub Security Advisories](https://github.com/NETO-DEV66/Flow-tub/security/advisories/new).

Please include:

- Type of issue (for example: credential exposure, path traversal, injection).
- Full paths of the source files involved.
- The affected tag, branch, or commit.
- Any configuration required to reproduce the issue.
- Step-by-step reproduction instructions.
- Proof-of-concept or exploit code, if available.
- Impact, including how an attacker might exploit it.

You will receive an acknowledgement within 48 hours and a timeline for a fix.
Please do not disclose the issue publicly until a fix has shipped.

## Verifying release APKs

Every release publishes a `checksums.txt` next to the APK. Compare it with the
file you downloaded:

```bash
sha256sum -c checksums.txt
```

NeuroTube ships one universal APK per release. Any file that is not named
`NeuroTube-<version>-universal.apk`, or that fails the checksum, is not an
official build. Official distribution is the
[GitHub Releases page](https://github.com/NETO-DEV66/Flow-tub/releases).
