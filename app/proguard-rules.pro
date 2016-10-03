# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Develop\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn android.support.**
-keep class android.support.** { *; }
-dontwarn butterknife.**
-keep class butterknife.** { *; }
-dontwarn de.measite.minidns.**
-keep class de.measite.minidns.** { *; }
-dontwarn org.**
-keep class org.** { *; }
-dontwarn timber.**
-keep class timber.** { *; }
-dontwarn com.lcjian.wechatsimulation.entity.**
-keep class com.lcjian.wechatsimulation.entity.** { *; }
-dontwarn rx.**
-keep class rx.** { *; }