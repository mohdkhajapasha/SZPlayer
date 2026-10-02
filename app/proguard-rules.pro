# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# General obfuscation and optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Room
-keep class androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.TypeConverter
-keep interface androidx.room.RoomDatabase$*

# Retrofit / OkHttp / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.shaaztechno.videoplayer.data.remote.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleDeclarations

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Google Play Services Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Compose
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.runtime.** { *; }
