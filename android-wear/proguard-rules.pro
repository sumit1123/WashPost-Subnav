# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/<user>/android/sdk/tools/proguard/proguard-android.txt
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

##### General #####
-dontobfuscate
-optimizations !code/simplification/cast,!code/simplification/advanced,!field/*,!class/merging/*,!method/removal/parameter,!method/propagation/parameter,!code/allocation/variable,!code/simplification/arithmetic
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-verbose

##### Libraries specific #####
# -- okhttp --
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

# -- okio --
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# -- Retrofit --
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

# -- Android support --
-keep class android.support.v7.widget.SearchView { *; }

# -- Debug specific --
# -----------------------------------
# -- Android Annotations --
-dontwarn org.springframework.**
# -- RxAndroid --
-dontwarn rx.internal.util.unsafe.**
# -----------------------------------

# -- Release specific --
# -------------------------------------------------------------------
# -- RxAndroid --
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
# -------------------------------------------------------------------



##### Project models/classes #####
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-repackageclasses ''
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# -- Classic specific --
-keep class com.amazon.** { *; }
-keep class com.google.vending.licensing.ILicensingService
-keep class com.android.vending.licensing.ILicensingService
-keep class com.android.vending.billing.** { *; }
-keep class com.google.ads.interactivemedia.v3.** { *; }
-keep class com.washingtonpost.android.androidlive.cache.model.** {*;}
-keep class com.washingtonpost.android.androidlive.liveblog.model.** {*;}
-keep class com.washingtonpost.android.paywall.newdata.** {*;}
-keep class com.washingtonpost.android.paywall.features.ccpa.models.** { *; }
-keep class com.wapo.flagship.json.** {*;}
-keep class com.wapo.flagship.config.** {*;}
-keep class com.wapo.flagship.data.** {*;}
-keep class com.wapo.flagship.features.sections.model.** {*;}
-keep class com.wapo.flagship.features.search.** {*;}
-keep class com.wapo.flagship.model.** {*;}
-keep class com.wapo.flagship.content.notifications.** {*;}
-keep class com.washingtonpost.android.comics.model.ComicStrip{*;}
-keep class com.wapo.flagship.features.articles.models.** { *; }
-keep class com.wapo.flagship.features.audio.service.library.** { *; }

-dontwarn com.comscore.**
-dontwarn com.google.android.maps.**
-dontwarn com.google.android.gms.measurement.**
-dontwarn com.adobe.**
-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.** { *; }

# -- Wear specific --
-keep class com.wapo.flagship.WearFlagshipApplication { *; }
-keep class com.google.android.gms.wearable.** {*;}

# -- AppsFlyer integration --
-dontwarn com.android.installreferrer
-dontwarn com.appsflyer.**
-keep public class com.google.firebase.messaging.FirebaseMessagingService {
  public *;
}

# -- Chartbeat specific --
-dontwarn com.fasterxml.jackson.**

# Grid
-keep class com.wapo.flagship.features.grid.** { *; }

# -- https://github.com/Kotlin/kotlinx.coroutines/issues/1270 --
-dontwarn kotlinx.coroutines.flow.**inlined**