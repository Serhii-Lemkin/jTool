# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/serhii/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
# http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom rules here that might be needed for your specific libraries.
# Most modern Android libraries (Compose, Firebase, etc.) provide their own
# ProGuard rules automatically.

# Keep the data classes we annotated with @Keep just in case
-keepattributes *Annotation*
-keep @androidx.annotation.Keep class * {*;}
-keep class com.jtool.android.core.models.** { *; }

# Keep Google Play Services Auth classes
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.android.gms.internal.auth.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.firebase.auth.** { *; }
# Keep OAuth credentials & Jackson/Gson serialization if used
-dontwarn com.google.android.gms.**