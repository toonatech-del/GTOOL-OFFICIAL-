# ==============================================================================
# GTOOL X - Production ProGuard & R8 Configuration
# Optimized for Jetpack Compose, Room, Coroutines, Moshi, and AndroidX
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. General Kotlin & Standard Library
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlin.**
-dontwarn kotlinx.serialization.**

# ------------------------------------------------------------------------------
# 2. Jetpack Compose
# ------------------------------------------------------------------------------
-keep class androidx.compose.runtime.ParcelableSnapshotValue { *; }
-keep class androidx.compose.runtime.snapshots.SnapshotKt { *; }
-keep @androidx.compose.runtime.Stable class *
-keep @androidx.compose.runtime.Immutable class *

-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
    @androidx.compose.runtime.ReadOnlyComposable <methods>;
}

# ------------------------------------------------------------------------------
# 3. Room Database & SQLite
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(...); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-dontwarn androidx.room.paging.**

# Keep model and data entities to prevent Room / Moshi mapping errors
-keep class com.example.model.** { *; }
-keep class com.example.data.local.** { *; }

# ------------------------------------------------------------------------------
# 4. Kotlin Coroutines
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------------------
# 5. AndroidX Lifecycle & ViewModel
# ------------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ------------------------------------------------------------------------------
# 6. Moshi (JSON Serialization)
# ------------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class *
-keep class **JsonAdapter { *; }
-keep class **$JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# ------------------------------------------------------------------------------
# 7. Retrofit & OkHttp
# ------------------------------------------------------------------------------
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep @retrofit2.http.* interface * { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------------------------
# 8. ML Kit & Google Play Services
# ------------------------------------------------------------------------------
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# 9. Security: Strip Log Statements
# ------------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ------------------------------------------------------------------------------
# 10. Coil (Image Loading)
# ------------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**
