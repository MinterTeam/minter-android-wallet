# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions, InnerClasses

# Retain generated class which implement Unbinder.
-keep public class * implements butterknife.Unbinder { public <init>(**, android.view.View); }

# APP
-keep class network.minter.bipwallet.internal.system.WalletFileProvider { }
-keep class * extends network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter$RowViewHolder { *; }
-keep class * extends android.support.v7.util.DiffUtil$Callback { *; }
-keep class * extends android.support.v7.widget.RecyclerView$ViewHolder { *; }

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

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**
-dontwarn org.apache.**
-dontwarn sun.misc.**
-dontwarn org.bouncycastle.**
-dontwarn io.netty.**

-dontwarn java.lang.management.**
