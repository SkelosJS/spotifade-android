# SpotiFade (Android)

> *Trop de pub tue la pub.*

🇫🇷 [Lire en français](README.md)

Mute Spotify ads automatically on Android. No root required.

## How it works

A `NotificationListenerService` taps into the system `MediaSessionManager`
and watches Spotify's media session metadata. When an ad is detected —
via the `METADATA_KEY_ADVERTISEMENT` flag, a `mediaId` containing
`spotify:ad`, or a localized ad title (`Annonce`, `Advertisement`,
`Werbung`, `광고`, …) — the Spotify session is muted via
`MediaController.setVolumeTo(0)`. As soon as the next track starts,
the previous volume is restored.

If the per-session route is unavailable (older Android, restricted
device), it falls back to `AudioManager.adjustStreamVolume(ADJUST_MUTE)`
on `STREAM_MUSIC`. Both paths work without the Do Not Disturb permission.

## Requirements

- JDK 17
- Android SDK with platform 35 and build-tools 35
- A device running Android 8.0 (API 26) or newer

## Build

```bash
git clone https://github.com/SkelosJS/spotifade-android.git
cd spotifade-android
./gradlew assembleDebug    # debug APK: app/build/outputs/apk/debug/
./gradlew assembleRelease  # release APK: app/build/outputs/apk/release/
```

### Signing the release

The release build looks for a `keystore.properties` file at the project
root (gitignored). If absent, it falls back to the debug signing key —
fine for local sideloading, not for public distribution.

To generate a dedicated signing key:

```bash
keytool -genkey -v \
  -keystore release.jks \
  -alias spotifade \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then create `keystore.properties` at the project root:

```properties
storeFile=release.jks
storePassword=…
keyAlias=spotifade
keyPassword=…
```

> ⚠️ Never commit `release.jks` or `keystore.properties` — both are
> already covered by the project's `.gitignore`.

## Install on a device

1. Install the APK: `adb install -r app-release.apk` (or copy the file
   to the device and open it).
2. Launch SpotiFade.
3. Tap **Accorder l'accès aux notifications** and enable *SpotiFade* in
   the system list. This permission is what unlocks
   `MediaSessionManager.getActiveSessions` — no notifications are read
   or stored.
4. The status card turns green ("Actif"). The app can be closed: the
   listener keeps running in the background, screen off included.

On aggressive battery managers (Xiaomi/MIUI, Huawei, OnePlus…), battery
optimization for *SpotiFade* may need to be disabled so the system
doesn't kill the listener in standby.

## Debug

```bash
adb logcat -s SpotifyAdMute:V
```

Verbose per-metadata logs are stripped from the release APK.

## License

MIT — see `LICENSE`.
