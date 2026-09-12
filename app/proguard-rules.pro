# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For Room
-keep class androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.TypeConverter
-keep interface androidx.room.RoomDatabase$*

# For Retrofit/Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.shaaztechno.videoplayer.data.remote.** { *; }
