# SpotiFade (Android)

> *Trop de pub tue la pub.*

🇬🇧 [Read in English](README.en.md)

Coupe automatiquement le son des pubs Spotify sur Android — pas de root.

## Comment ça marche

Un `NotificationListenerService` se branche sur le `MediaSessionManager`
système et surveille les metadata de la session media de Spotify.
Lorsqu'une pub est détectée (via le flag `METADATA_KEY_ADVERTISEMENT`,
un `mediaId` contenant `spotify:ad`, ou un titre localisé — `Annonce`,
`Advertisement`, `Werbung`, …), la session Spotify est mutée via
`MediaController.setVolumeTo(0)`. Dès que la piste suivante démarre, le
volume précédent est rétabli.

Si la voie par session est indisponible (Android plus ancien, appareil
restreint), un fallback bascule sur `AudioManager.adjustStreamVolume(
ADJUST_MUTE)` sur `STREAM_MUSIC`. Les deux chemins fonctionnent sans
permission "Ne pas déranger".

## Build (CLI)

Nécessite JDK 17 et le SDK Android avec la plateforme 35 + build-tools 35.

```bash
./gradlew assembleDebug    # APK debug : app/build/outputs/apk/debug/
./gradlew assembleRelease  # APK release : app/build/outputs/apk/release/
```

### Signer ta release

Le build release cherche un `keystore.properties` à la racine du projet
(gitignoré). En son absence, il bascule sur la clé de debug — OK pour
sideload local, pas pour de la distribution.

Pour mettre en place ta propre clé de signature :

```bash
keytool -genkey -v \
  -keystore release.jks \
  -alias spotifade \
  -keyalg RSA -keysize 2048 -validity 10000
```

Puis crée `keystore.properties` :

```properties
storeFile=release.jks
storePassword=…
keyAlias=spotifade
keyPassword=…
```

## Installation sur le téléphone

1. Installe l'APK : `adb install -r app-release.apk`.
2. Lance l'app.
3. Appuie sur **Accorder l'accès aux notifications** et active
   *SpotiFade* dans la liste système. Cette permission est ce qui
   débloque `MediaSessionManager.getActiveSessions` — aucune
   notification n'est lue ni stockée.
4. La carte passe au vert ("Actif"). Tu peux fermer l'app — le listener
   continue de tourner en arrière-plan, écran éteint inclus.

Sur les surcouches agressives (Xiaomi/MIUI, Huawei, OnePlus…), il peut
falloir désactiver l'optimisation de batterie pour *SpotiFade* afin que
le système ne tue pas le listener en veille.

## Debug

```bash
adb logcat -s SpotifyAdMute:V
```

Les logs verbeux par metadata sont retirés de l'APK release.

## Licence

MIT — voir `LICENSE`.
