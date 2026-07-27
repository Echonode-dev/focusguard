# ProGuard rules for FocusGuard

# Kotlin
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepnames interface retrofit2.*
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# GSON
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment

# App-specific
-keep class com.focusguard.app.** { *; }
-keepclassmembers class com.focusguard.app.** {
    public <init>(...);
}

# Keep all model classes
-keep class com.focusguard.app.models.** { *; }
