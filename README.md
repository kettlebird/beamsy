# Beamsy

Mirror your iPhone to your Android TV. No ads. No subscription. No time limit.

Open Beamsy on the TV, then pick the TV in Screen Mirroring on the iPhone. The app speaks the AirPlay protocol, so nothing is installed on the iPhone.

- **Free:** mirroring up to 720p with sound, one iPhone at a time, no ads, no time limit.
- **Full HD:** 1080p comes from a separate paid app, "Beamsy Full HD Unlock" (`com.beamsy.unlock`), **$6.99, paid once** (a one-time purchase, not a subscription). The main app checks that the unlock app is installed and signed with the same certificate.
- Works while the app is open on the TV, or all the time with Settings › "Start Beamsy when the TV turns on". iPhone only for now; Android phones are planned.

## Two Gradle modules

| Module | applicationId | What it is |
| --- | --- | --- |
| `:app` | `com.beamsy.app` | The receiver (free, 720p) |
| `:unlock` | `com.beamsy.unlock` | The one-time "Full HD Unlock" key app (no native code) |

## Based on

This is a GPL-3.0 fork of [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server), which is built on [UxPlay](https://github.com/FDH2/UxPlay) (also GPL-3.0). The upstream `LICENSE` is kept, and this project is released under the same licence. Source for this app: https://github.com/yugio999/beamsy.

Also used, as in upstream: [FFmpeg](https://ffmpeg.org) (lossless audio decoder) and [Next Player](https://github.com/anilbeesetti/nextplayer) (video player).

Not affiliated with Apple Inc.

## Building

The native part (OpenSSL and FFmpeg, via CMake under [`app/src/main/cpp`](app/src/main/cpp)) needs a Linux toolchain. It cannot be built on a plain Windows host.

Initialise the submodules first:

```bash
git submodule update --init --recursive
```

The build applies patches to the UxPlay submodule, so it shows as modified afterwards. Never commit or reset it.

### Docker (local build on Windows/macOS/Linux)

The [`docker/`](docker) folder holds a Linux build image (JDK 21, Android SDK/NDK/CMake). This is the local route for a key-less build. Verified on Windows 11 with Docker Desktop 29.8.1 (2026-10-18): the debug build of both apps takes about 5 minutes the first time, and `:app:assembleRelease :unlock:assembleRelease` (unsigned, R8) about 3. From the repo root (run in Git Bash on Windows):

```bash
docker build -t screenmirroringtv-build docker && MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD":/src -v gradle-cache:/root/.gradle -v android-keys:/root/.android screenmirroringtv-build :app:assembleDebug :unlock:assembleDebug
```

Gradle arguments after the image name are passed to `./gradlew` (default `:app:assembleDebug`). APKs are copied back to `app/build/outputs` and `unlock/build/outputs`. The container never sees your `local.properties`, so this route gives debug-signed builds (`app-debug.apk`, `unlock-debug.apk`). Always build both in the same run: the main app checks that the unlock app has the same signing certificate, and the `android-keys` volume keeps the debug keystore the same across runs. Debug APKs built on different machines, or without that volume, will not unlock. Signed release builds come from CI (below) or from a local build once the upload key exists (see Releasing).

### GitHub Actions

[`.github/workflows/apk.yml`](.github/workflows/apk.yml) builds signed release APKs and AABs for both modules and attaches them to a GitHub release. It runs on `release/**` branches, `v*.*.*` tags, or manually. It needs two repository secrets (upstream's pattern): `STORE` (base64 of the upload keystore) and `LOCAL` (base64 of a `local.properties` with the signing settings).

Outputs:

- `app/build/outputs/apk/release/app-release.apk` and `app/build/outputs/bundle/release/app-release.aab`
- `unlock/build/outputs/apk/release/unlock-release.apk` and `unlock/build/outputs/bundle/release/unlock-release.aab`

### Unit tests

```bash
./gradlew :app:testDebugUnitTest
```

## Releasing

1. **Confirm the final app name and both applicationIds before the first upload.** An applicationId can never change once published.
2. **Create one upload key and use it for both apps.** Put its settings in `local.properties` (never commit it; `*.jks` and `local.properties` are git-ignored):

   ```properties
   storeFile=../upload.jks
   storePassword=...
   keyAlias=...
   keyPassword=...
   ```

   `storeFile` is resolved from each module's folder, so `../upload.jks` works for both `:app` and `:unlock`.
3. **Play Console:** enable Play App Signing for the unlock app and choose "use the same app signing key as another app" (the main app). Otherwise Google signs the two apps with different keys and the unlock check always fails.
4. **Verify both APKs carry the same certificate:**

   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   apksigner verify --print-certs unlock/build/outputs/apk/release/unlock-release.apk
   ```

   The SHA-256 digests must match. (A release build made without signing settings is unsigned and has no certificate to compare.)
5. Keep https://github.com/yugio999/beamsy public and up to date with each release: GPL-3.0 requires the source to be available to everyone who gets the app. It is linked from this README and both apps' `full_description.txt`.
6. Publish [`docs/privacy-policy.md`](docs/privacy-policy.md) at a public URL and enter that URL in Play Console for both apps. Data safety form: no data collected, no data shared.
7. Store listings live in `fastlane/metadata/android/en-US` (Beamsy) and `unlock/fastlane/metadata/android/en-US` (unlock): title, short and full description, changelog, 512 px icon, 1024×500 feature graphic, 1280×720 TV banner and 1920×1080 TV screenshots. Icons, banners and graphics are generated by [`tools/make_brand_assets.py`](tools/make_brand_assets.py) (the Figma "App icon & TV banner" board in code); the TV screenshots come from the real app. Android TV apps also need the Android TV form factor turned on (Play Console › Release › Advanced settings › Form factors) and pass a separate TV quality review.
8. Developer name in Play Console: **Kettlebird**. Price: Beamsy free, Beamsy Full HD Unlock $6.99 (paid app, one-time).
9. In CI, the `LOCAL` secret's `storeFile` must be `../upload.jks` (the workflow writes the keystore to the repo root as `upload.jks`).
10. Run the real-device checks in [`docs/on-device-checklist.md`](docs/on-device-checklist.md).
