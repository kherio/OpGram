# Continuous integration

`.github/workflows/build.yml` builds OpGram on every push to `main`, on pull
requests, on demand (workflow_dispatch) and when you publish a release.

## Signing (optional but recommended)

Add these repository secrets (Settings → Secrets and variables → Actions) so
CI produces a signed APK and attaches it to releases:

- `KEYSTORE_B64` — your keystore, base64-encoded: `base64 -w0 keystore.jks`
- `KEYSTORE_PASS` — keystore password
- `KEY_PASS` — key password
- `KEY_ALIAS` — key alias

Without these secrets the job still builds and uploads the unsigned APK as an
artifact; it just skips the signing step.

## Publishing a release

Create a GitHub release (tag e.g. `v1.0`); CI builds, signs and attaches
`OpGram-release.apk` automatically.

## Automatic releases

A push to `main` that **changes `versionName` in `app/build.gradle`** builds, signs and publishes the
release `v<versionName>` on its own, using the matching `## <versionName>` section of `CHANGELOG.md`
as the release notes. If the tag already exists nothing is published, so ordinary pushes only build.
Pushing a `v*` tag by hand still works the same way.

To release: bump `versionCode`/`versionName` (and `Branding.VERSION`, `app_name` in `strings.xml`),
add the section to `CHANGELOG.md`, and push to `main`.
