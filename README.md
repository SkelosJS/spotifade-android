# SpotiFade (Android)

> *Trop de pub tue la pub.*

🇬🇧 [Read in English](README.en.md)

Coupe automatiquement le son des pubs Spotify sur Android. Aucun root
requis.

## Comment ça marche

Un `NotificationListenerService` se branche sur le `MediaSessionManager`
système et surveille les metadata de la session media de Spotify.
Lorsqu'une pub est détectée — via le flag `METADATA_KEY_ADVERTISEMENT`,
un `mediaId` contenant `spotify:ad`, ou un titre localisé (`Annonce`,
`Advertisement`, `Werbung`, `광고`, …) — la session Spotify est mutée
via `MediaController.setVolumeTo(0)`. Dès que la piste suivante démarre,
le volume précédent est rétabli.

Si la voie par session est indisponible (Android plus ancien, appareil
restreint), un fallback bascule sur `AudioManager.adjustStreamVolume(
ADJUST_MUTE)` sur `STREAM_MUSIC`. Les deux chemins fonctionnent sans la
permission "Ne pas déranger".

## Prérequis

- JDK 17
- Android SDK avec la plateforme 35 et build-tools 35
- Un appareil sous Android 8.0 (API 26) ou plus récent

## Build

```bash
git clone https://github.com/SkelosJS/spotifade-android.git
cd spotifade-android
./gradlew assembleDebug    # APK debug : app/build/outputs/apk/debug/
./gradlew assembleRelease  # APK release : app/build/outputs/apk/release/
```

### Signer la release

Le build release cherche un fichier `keystore.properties` à la racine du
projet (gitignoré). En son absence, il bascule automatiquement sur la
clé de debug — suffisant pour du sideload local, pas pour distribuer
publiquement.

Pour générer une clé de signature dédiée :

```bash
keytool -genkey -v \
  -keystore release.jks \
  -alias spotifade \
  -keyalg RSA -keysize 2048 -validity 10000
```

Puis créer `keystore.properties` à la racine :

```properties
storeFile=release.jks
storePassword=…
keyAlias=spotifade
keyPassword=…
```

> ⚠️ Ne jamais committer `release.jks` ni `keystore.properties` — ils
> sont déjà couverts par le `.gitignore` du projet.

## Installation sur l'appareil

1. Installer l'APK : `adb install -r app-release.apk` (ou copier le
   fichier sur le téléphone et l'ouvrir).
2. Lancer SpotiFade.
3. Appuyer sur **Accorder l'accès aux notifications** et activer
   *SpotiFade* dans la liste système. Cette permission est ce qui
   débloque `MediaSessionManager.getActiveSessions` — aucune
   notification n'est lue ni stockée.
4. La carte de statut passe au vert ("Actif"). L'app peut être fermée :
   le listener continue de tourner en arrière-plan, écran éteint inclus.

Sur les surcouches agressives en gestion de batterie (Xiaomi/MIUI,
Huawei, OnePlus…), il peut être nécessaire de désactiver l'optimisation
de batterie pour *SpotiFade* afin que le système ne tue pas le listener
en veille.

## Debug

```bash
adb logcat -s SpotifyAdMute:V
```

Les logs verbeux par metadata sont retirés de l'APK release.

## Licence

MIT — voir `LICENSE`.
