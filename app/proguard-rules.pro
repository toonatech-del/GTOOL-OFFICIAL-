# ==============================================================================
# GTOOL X - Production ProGuard & R8 Configuration
# Optimized for Jetpack Compose, Room, Coroutines, and AndroidX
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
# Keep Compose compiler metrics and stable markers
-keep class androidx.compose.runtime.ParcelableSnapshotValue { *; }
-keep class androidx.compose.runtime.snapshots.SnapshotKt { *; }
-keep @androidx.compose.runtime.Stable class *
-keep @androidx.compose.runtime.Immutable class *

# Prevent layout failure by keeping Composable metadata
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
    @androidx.compose.runtime.ReadOnlyComposable <methods>;
}

# ------------------------------------------------------------------------------
# 3. Room Database & SQLite
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }

# Keep Room's generated code (KSP/APT)
-keep class androidx.room.RoomDatabase { _prefix*; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep everything in model and local data packages to prevent Room/Moshi mapping errors
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
# Keep ViewModels and their parameterless constructors for ViewModelProvider.Factory
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ------------------------------------------------------------------------------
# 6. Moshi (JSON Serialization)
# ------------------------------------------------------------------------------
# Keep classes with @JsonClass and their generated JsonAdapters
-keep @com.squareup.moshi.JsonClass class *
-keep class **JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json(name = ...) <fields>;
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
# Removes all android.util.Log calls in release builds for maximum security and reduced size
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    # Note: We keep w() and e() for critical diagnostic reporting
}

# ------------------------------------------------------------------------------
# 10. Coil (Image Loading)
# ------------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**
