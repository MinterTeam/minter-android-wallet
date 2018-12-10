-dontobfuscate

# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions, InnerClasses
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }

# Retain generated class which implement Unbinder.
-keep public class * implements butterknife.Unbinder { public <init>(**, android.view.View); }

# APP

-keep class network.minter.bipwallet.internal.system.WalletFileProvider { }
-keep class * extends network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter$RowViewHolder {
    public <init>(android.view.View);
 }
-keep class * extends android.support.v7.util.DiffUtil$Callback { *; }
-keep class * extends android.support.v7.widget.RecyclerView$ViewHolder { *; }

-keep class * extends network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter$RowViewHolder { *; }
-keep class * implements network.minter.bipwallet.internal.views.list.multirow.MultiRowContract$Row { *; }
-keep class * extends android.support.v7.util.DiffUtil$Callback { *; }
-keep class * extends android.support.v7.widget.RecyclerView$ViewHolder { *; }
-keep class com.esafirm.imagepicker.helper.ImagePickerFileProvider { *; }
-keep class com.google.firebase.iid.FirebaseInstanceId
-keep class com.google.firebase.provider.FirebaseInitProvider
-keep class org.parceler.IdentityCollection

-keep class android.support.design.internal.BottomNavigationMenuView {*;}
#-keep class android.arch.lifecycle.**
#-keep class org.apache.http.** { *; }
#-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.**

# JodaTime android
-dontwarn org.joda.convert.**
-dontwarn org.joda.time.**
-keep class org.joda.time.** { *; }
-keep interface org.joda.time.** { *; }
-keep class net.danlew.** { *; }

# Barber
-keep class **$$Barbershop { *; }
-keep class io.sweers.barber.** { *; }
-keepclasseswithmembers class * {
    @io.sweers.barber.* <fields>;
}
-keepclasseswithmembers class * {
    @io.sweers.barber.* <methods>;
}

#EventBus
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Prevent obfuscation of types which use ButterKnife annotations since the simple name
# is used to reflectively look up the generated ViewBinding.
-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }

# Parceler library
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }

-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.* { *; }
-dontwarn javax.annotation.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn okhttp3.internal.platform.*

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**
-dontwarn org.apache.**
-dontwarn sun.misc.**
-dontwarn org.bouncycastle.**
-dontwarn io.netty.**

-dontwarn java.lang.management.**

# Skip external usage notes (for obfuscation)
-dontnote com.theartofdev.edmodo.**
-dontnote com.prolificinteractive.materialcalendarview.**
-dontnote com.lapism.searchview.SearchView
-dontnote com.google.android.gms.**
-dontnote com.github.chrisbanes.photoview.**
-dontnote com.facebook.share.**
-dontnote okhttp3.internal.platform.**
-dontnote io.sweers.barber.**
-dontnote com.warkiz.widget.**

# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
# DexGuard
#-keepresourcexmlelements manifest/application/meta-data@name=io.fabric.ApiKey
-printmapping mapping.txt
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

-keepattributes *Annotation*

# Centrifuge
-keep class centrifuge.** { *; }
-keep class go.** { *; }

