# NotificationListenerService is instantiated by the system via the manifest.
-keep class dev.skelos.spotifyadmute.SpotifyAdMuteListener { *; }

# Strip verbose logs from the release APK.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
