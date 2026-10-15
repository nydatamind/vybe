# ProGuard & R8 Optimization Rules for VYBE

# AndroidX Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room Database
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil Image Loading
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# App Models & Entities
-keep class com.example.vybe.core.model.** { *; }
-keep class com.example.vybe.core.database.** { *; }
-keep class com.example.vybe.data.provider.** { *; }

# Compose Material Icons (Keep only referenced icons)
-keepclassmembers class androidx.compose.material.icons.** {
    public static ** get*(...);
}
