# Unofficial Android 7 compatibility build

This branch packages an Android 7 / API 24 compatibility build of Fieldwatch. It exists to keep
useful older hardware in service and does not replace or represent the official project.

## Credit and license

Fieldwatch was created by **OffGridPete / Off Grid Pete LLC**. The original source, documentation,
design, signature catalog, and sensor data come from
[OffGridPete/Fieldwatch](https://github.com/OffGridPete/Fieldwatch). Please send appreciation,
feature requests, and general project support to the original project.

This fork preserves the upstream Git history, [`LICENSE`](LICENSE), and [`NOTICE`](NOTICE). It is
distributed under the same MIT License. OffGridPete has not reviewed, endorsed, or agreed to
support this compatibility build.

## Compatibility target

- Minimum Android version: Android 7.0 / API 24
- Tested device: NVIDIA Shield Tablet K1 (`shieldtablet`), stock NVIDIA Android 7
- CPU tested: ARM 32-bit / `armeabi-v7a`
- Upstream source base: Fieldwatch v1.1.37 source at commit `4f74cec`
- Application ID remains `app.fieldwatch`

Other API 24–28 devices should be compatible with the guarded code paths, but the Shield Tablet K1
is the device that has received live Wi-Fi, BLE, GPS, logging, report, and sensor-layer testing.

## Compatibility changes

- Lowers `minSdk` from API 29 to API 24.
- Uses core-library desugaring for `java.time` on Android 7.
- Adds legacy location-listener and guarded fallbacks for APIs introduced after Android 7.
- Retains the official catalog, UI, reports, exports, and radio-scanning behavior.
- Includes the official `dist/fieldwatch-sensors.geojson.gz` dataset as an offline fallback when an
  Android 7 certificate store cannot validate GitHub's current TLS chain. The ordinary verified
  HTTPS download remains the first choice; certificate verification is never disabled.
- Uses conservative Gradle memory settings so the project can build on modest hardware.

The bundled sensor file is copied byte-for-byte from the upstream source tree. Its SHA-256 is:

```text
bb1e3439089d194cadd78cc7ba56b721d1a7bdef721ec1b7135ce5f52a6df474
```

It contains 40,112 mapped records: 33,707 cameras, 3,961 gunshot detectors, 1,765 speed cameras,
and 679 red-light cameras.

## Known Android 7 limitations

- Android 7 does not expose the faster Wi-Fi scan APIs available on Android 11 and later.
- Some modern notification, background-restriction, GNSS, vibration, and radio-detail fields are
  unavailable; the app uses guarded fallbacks.
- Device vendors can impose additional scan quotas and background limits.
- Detection and signature matches remain hypotheses, exactly as described in the upstream safety
  guidance. This is not a direction finder or a complete RF capture.

## Build and verification

Use JDK 17 and Android SDK / Build Tools 35:

```sh
./gradlew --no-daemon testDebugUnitTest assembleDebug
```

The compatibility APK is minified, not debuggable, and includes `armeabi-v7a`. Each GitHub release
publishes the APK checksum and signing-certificate fingerprint. The current compatibility signing
certificate SHA-256 fingerprint is:

```text
b19ac372b8efc499405596e374a3ceedbd751da467e1774e2c8e11bd72c75342
```

An APK signed by a different certificate cannot update this build in place. Back up Fieldwatch
settings and reports before switching between independently signed builds.

## Upstream contribution

Compatibility work should be offered upstream only if it can remain maintainable without weakening
the official app's security or newer-Android behavior. Until accepted upstream, releases from this
fork are unofficial and supported only by the fork maintainer.
