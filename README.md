# SpotiFade (Android)

> *Trop de pub tue la pub.*

Mute Spotify ads automatically on Android — no root required.

## How it works

A `NotificationListenerService` taps into the system `MediaSessionManager` and
watches Spotify's media session metadata. When an ad is detected (via the
`METADATA_KEY_ADVERTISEMENT` flag, a `mediaId` containing `spotify:ad`, or a
localized ad title — `Annonce`, `Advertisement`, `Werbung`, …), the Spotify
session is muted via `MediaController.setVolumeTo(0)`. As soon as the next
track starts, the previous volume is restored.

If the per-session route is unavailable (older Android, restricted device),
it falls back to `AudioManager.adjustStreamVolume(ADJUST_MUTE)` on
`STREAM_MUSIC`. Both paths work without `Do Not Disturb` access.

## Build (CLI)

Requires JDK 17 and the Android SDK with platform 35 + build-tools 35.

```bash
./gradlew assembleDebug    # debug APK at app/build/outputs/apk/debug/
./gradlew assembleRelease  # release APK at app/build/outputs/apk/release/
```

### Signing your release

The release build looks for `keystore.properties` at the project root
(gitignored). If absent, it falls back to the debug signing key — fine for
local sideloading but not for distribution.

To set up your own signing key:

```bash
keytool -genkey -v \
  -keystore release.jks \
  -alias spotify-ad-mute \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then create `keystore.properties`:

```properties
storeFile=release.jks
storePassword=…
keyAlias=spotify-ad-mute
keyPassword=…
```

## Setup on the device

1. Install the APK: `adb install -r app-release.apk`.
2. Launch the app.
3. Tap **Accorder l'accès aux notifications** and enable *SpotiFade*
   in the system list. This permission is what unlocks
   `MediaSessionManager.getActiveSessions` — no notifications are read or
   stored.
4. The card turns green ("Actif"). You can close the app — the listener
   keeps running in the background, screen off included.

On aggressive battery managers (Xiaomi/MIUI, Huawei, OnePlus…) you may need
to disable battery optimization for *SpotiFade* so the system doesn't
kill the listener in standby.

## Debugging

```bash
adb logcat -s SpotifyAdMute:V
```

Per-metadata debug logs are stripped from the release APK.

## License

MIT — see `LICENSE`.
