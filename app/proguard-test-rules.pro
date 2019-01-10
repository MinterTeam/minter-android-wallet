# Proguard rules that are applied to your test apk/code.
-ignorewarnings

-keepattributes *Annotation*

-keep class org.hamcrest.** {*;}
-keep class junit.framework.** {*;}
-keep class junit.runner.** {*;}
-keep class org.junit.** { *; }
-keep class org.mockito.** {*;}
-keep class android.test.** {*;}
-keep class android.support.test.** {*;}
-keep class com.squareup.javawriter.JavaWriter {*;}
-keep class io.reactivex.plugins.RxJavaPlugins { *; }
-keep class io.reactivex.disposables.CompositeDisposable { *; }
-keep class network.minter.bipwallet.* { *; }
-keep class android.support.test.** { *; }
