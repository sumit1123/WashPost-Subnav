# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/<user>/android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
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
# The custom Retrofit adapters inspect APIResult<T> at runtime. In R8 full mode,
# keeping the wrapper class preserves the generic return-type signature while
# still allowing its name to be obfuscated.
-keep,allowobfuscation,allowoptimization class **.APIResult

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
-keepattributes SourceFile,LineNumberTable  # Keep file names and line numbers.
-keepattributes *Annotation*
# -- https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=android
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

# Firebase discovers ComponentRegistrars from class names in the merged manifest
# and creates them reflectively. Keep their zero-argument constructors and
# implementations when R8 performs full optimization.
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }

# Airship loads optional module factories and push providers from class names at
# runtime. Keep their constructors and implementations for R8 full mode.
-keep class com.urbanairship.**ModuleFactoryImpl { *; }
-keep class * implements com.urbanairship.push.PushProvider { *; }

# -- Classic specific --
-keep class com.amazon.** { *; }
-keep class com.google.vending.licensing.ILicensingService
-keep class com.android.vending.licensing.ILicensingService
-keep class com.android.vending.billing.** { *; }
-keep class com.google.ads.interactivemedia.v3.** { *; }
-keep class com.wapo.flagship.json.** {*;}
-keep class com.wapo.flagship.features.sections.model.** {*;}
-keep class com.wapo.flagship.features.search.** {*;}
-keep class com.wapo.flagship.model.** {*;}
-keep class com.wapo.flagship.content.notifications.** {*;}
-keep class com.washingtonpost.android.comics.model.ComicStrip{*;}
-keep class com.wapo.flagship.features.articles.models.** { *; }
-keep class com.wapo.flagship.features.audio.service2.media.library.** { *; }

# -- https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md --
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# IAP subscription data is persisted as Gson JSON. Retain the model fields and
# their generic signatures so Gson restores nested Map and List values to their
# concrete types when R8 full mode is enabled.
-keep class com.washingtonpost.android.paywall.newdata.model.IAPSubItems { <fields>; }
-keep class com.washingtonpost.android.paywall.newdata.model.IAPSubItem { <fields>; }
-keep class com.washingtonpost.android.paywall.newdata.model.IAPOfferItem { <fields>; }

-dontwarn com.comscore.**
-dontwarn com.google.android.maps.**
-dontwarn com.google.android.gms.measurement.**
-dontwarn com.adobe.**
-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.** { *; }

# -- Amazon specific --
-libraryjars ../android-push/android-push-amazon/libs/amazon-device-messaging-1.1.0.jar
-dontwarn com.amazon.device.messaging.**
-keep class com.amazon.device.messaging.** {*;}
-keep public class * extends com.amazon.device.messaging.ADMMessageReceiver
-keep public class * extends com.amazon.device.messaging.ADMMessageHandlerBase
-keep public class * extends com.amazon.device.messaging.ADMMessageHandlerJobBase
-keep class com.wapo.android.push.ADMMessageHandler {*;}
-keep class com.wapo.android.push.ADMPushNotificationReceiver { *; }

-dontwarn com.amazon.**

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

# Radea PDF library
-keep class com.radaee.** { *; }
# -- https://github.com/Kotlin/kotlinx.coroutines/issues/1270 --
-dontwarn kotlinx.coroutines.flow.**inlined**
# -- https://stackoverflow.com/questions/52677638/module-with-main-dispatcher-is-missing
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory {*;}

# -- jwt --
-keepnames class com.fasterxml.jackson.databind.** { *; }
-keepattributes InnerClasses
-keep class org.bouncycastle.** { *; }
-keepnames class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }
-dontwarn javax.xml.bind.DatatypeConverter
-dontwarn io.jsonwebtoken.impl.Base64Codec
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames interface com.fasterxml.jackson.** { *; }


# Permutive - Uber
# Taken from rxdogtag.pro(https://mvnrepository.com/artifact/com.uber.rxdogtag/rxdogtag/0.2.0)
# We keep these in order for DogTagObservers to properly work and resolve entries in each of these packages
-keepnames class com.uber.rxdogtag.**
-keepnames class io.reactivex.**

# Iterable
-keep class org.json.** { *; }

#Suggested by R8
-dontwarn com.google.protobuf.java_com_google_android_gmscore_sdk_target_granule__proguard_group_gtm_N1281923064GeneratedExtensionRegistryLite$Loader
-dontwarn com.google.zxing.BarcodeFormat
-dontwarn com.google.zxing.EncodeHintType
-dontwarn com.google.zxing.MultiFormatWriter
-dontwarn com.google.zxing.common.BitMatrix
-dontwarn com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

#Search models
-keep class com.wapo.flagship.features.search2.model.** { *; }
-keep class com.google.android.icing.** { *; }

# https://stackoverflow.com/questions/76128717/missing-class-org-slf4j-impl-staticloggerbinder-referenced-from-void-org-slf4j
-dontwarn org.slf4j.impl.StaticLoggerBinder

## -- https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
###---------------Begin: proguard configuration for Gson  ----------
## Gson uses generic type information stored in a class file when working with fields. Proguard
## removes such information by default, so configure it to keep all of it.
#-keepattributes Signature
#
## For using GSON @Expose annotation
#-keepattributes *Annotation*
#
## Gson specific classes
#-dontwarn sun.misc.**
##-keep class com.google.gson.stream.** { *; }
#
## Application classes that will be serialized/deserialized over Gson
#-keep class com.google.gson.examples.android.model.** { <fields>; }
#
## Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
## JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
#-keep class * extends com.google.gson.TypeAdapter
#-keep class * implements com.google.gson.TypeAdapterFactory
#-keep class * implements com.google.gson.JsonSerializer
#-keep class * implements com.google.gson.JsonDeserializer
#
## Prevent R8 from leaving Data object members always null
#-keepclassmembers,allowobfuscation class * {
#  @com.google.gson.annotations.SerializedName <fields>;
#}
#
## Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
#-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
#-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
#
###---------------End: proguard configuration for Gson  ----------
