/*
 * Copyright (C) by MinterTeam. 2022
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

/*
 * Copyright (C) by MinterTeam. 2021
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

plugins {
    id("com.android.application")
    id("com.google.firebase.crashlytics")
    id("kotlin-android")
    id("kotlin-kapt")
    kotlin("plugin.parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")
}

//gradleLint {
//    rules=['unused-dependency']
//}


val localProps = gradleLocalProperties(project.rootProject.rootDir)


android {
    compileSdk = deps.versions.maxSdk.get().toInt()
    defaultConfig {
        applicationId = "network.minter.bipwallet"
        minSdk = deps.versions.minSdk.get().toInt()
        targetSdk = deps.versions.maxSdk.get().toInt()
        versionCode = deps.versions.appVersionCode.get().toInt()
        versionName = deps.versions.appVersion.get()
        testInstrumentationRunner = "network.minter.bipwallet.tests.internal.WalletTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                        // here go the options for Moxy compiler
                        "defaultMoxyStrategy" to "moxy.viewstate.strategy.AddToEndStrategy",
                        "room.schemaLocation" to "$projectDir/schemas".toString(),
                )
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    useLibrary("android.test.runner")
    useLibrary("android.test.base")
    useLibrary("android.test.mock")


    testOptions {
        animationsDisabled = true
    }

    signingConfigs {
        if (localProps.containsKey("minter_key_alias")) {
            create("config") {
                keyAlias = localProps["minter_key_alias"] as String
                keyPassword = localProps["minter_key_password"] as String
                storeFile = file(localProps["minter_key_store_file"] as String)
                storePassword = localProps["minter_key_store_password"] as String
            }
        }
    }


    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("config")
            isMinifyEnabled = false
            isShrinkResources = false
            isTestCoverageEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro", "proguard-test-rules.pro", "proguard-release-rules.pro")
        }

        debug {
            signingConfig = signingConfigs.findByName("config")
            isMinifyEnabled = false
            isShrinkResources = false
            isTestCoverageEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro", "proguard-test-rules.pro", "proguard-release-rules.pro")
        }
    }
    packagingOptions {
        resources {
            excludes += listOf(
                    "META-INF/spring.tooling",
                    "META-INF/DEPENDENCIES.txt",
                    "META-INF/LICENSE.txt",
                    "META-INF/NOTICE.txt",
                    "META-INF/NOTICE",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE-notice.md",
                    "META-INF/DEPENDENCIES",
                    "META-INF/notice.txt",
                    "META-INF/license.txt",
                    "META-INF/dependencies.txt",
                    "META-INF/LGPL2.1"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // For Kotlin projects
    kotlinOptions {
        jvmTarget = "1.8"
    }

    flavorDimensions += "env"

    productFlavors {
        create("netMain") {
            dimension = "env"
            applicationId = "network.minter.bipwallet.mainnet"
            buildConfigField("String", "LIVE_BALANCE_URL", "\"wss://explorer-rtm.apps.minter.network/connection/websocket\"")
            buildConfigField("String", "EXPLORER_API_URL", "null")
            buildConfigField("String", "EXPLORER_FRONT_URL", "null")
            buildConfigField("String", "EXPLORER_STATIC_URL", "\"https://explorer-static.minter.network\"")
            buildConfigField("String", "GATE_API_URL", "null")
            buildConfigField("String", "COIN_AVATAR_BASE_URL", "\"https://my.apps.minter.network/api/v1/avatar/by/coin/\"")
            buildConfigField("String", "ADDRESS_AVATAR_BASE_URL", "\"https://my.apps.minter.network/api/v1/avatar/by/address/\"")
        }

        create("netMainLocal"){ initWith(getByName("netMain")) }
//
        create("netTest") {
            dimension = "env"
            applicationId = "network.minter.bipwallet"
            buildConfigField("String", "LIVE_BALANCE_URL", "\"wss://explorer-rtm.testnet.minter.network/connection/websocket\"")
            buildConfigField("String", "EXPLORER_API_URL", "\"https://explorer-api.testnet.minter.network/api/v2/\"")
            buildConfigField("String", "EXPLORER_FRONT_URL", "\"https://explorer.testnet.minter.network/api/v2\"")
            buildConfigField("String", "EXPLORER_STATIC_URL", "\"https://explorer-static.testnet.minter.network\"")
            buildConfigField("String", "GATE_API_URL", "\"https://gate-api.testnet.minter.network/api/v2/\"")
            buildConfigField("String", "COIN_AVATAR_BASE_URL", "\"https://my.beta.minter.network/api/v1/avatar/by/coin/\"")
            buildConfigField("String", "ADDRESS_AVATAR_BASE_URL", "\"https://my.beta.minter.network/api/v1/avatar/by/address/\"")
        }

        create("netTestLocal"){ initWith(getByName("netTest")) }

        create("toronet") {
            dimension = "env"
            applicationId = "network.minter.bipwallet"

            buildConfigField("String", "LIVE_BALANCE_URL", "\"wss://explorer-rtm.toronet.minter.network/connection/websocket\"")
            buildConfigField("String", "EXPLORER_API_URL", "\"https://explorer-api.toronet.minter.network/api/v2/\"")
            buildConfigField("String", "EXPLORER_FRONT_URL", "\"https://explorer.toronet.minter.network/api/v2\"")
            buildConfigField("String", "EXPLORER_STATIC_URL", "\"https://explorer-static.toronet.minter.network\"")
            buildConfigField("String", "GATE_API_URL", "\"https://gate-api.toronet.minter.network/api/v2/\"")
            buildConfigField("String", "COIN_AVATAR_BASE_URL", "\"https://my.beta.minter.network/api/v1/avatar/by/coin/\"")
            buildConfigField("String", "ADDRESS_AVATAR_BASE_URL", "\"https://my.beta.minter.network/api/v1/avatar/by/address/\"")
        }

        create("toronetLocal") { initWith(getByName("toronet")) }
    }

    productFlavors.forEach { flavor ->
        if(localProps.containsKey("minter_bot_secret")) {
            flavor.buildConfigField("String", "MINTER_BOT_SECRET", "\"${localProps.getProperty("minter_bot_secret")}\"")
        } else {
            flavor.buildConfigField("String", "MINTER_BOT_SECRET", "null")
        }

        flavor.buildConfigField("String", "MINTER_STORAGE_VERS", "\"v2_\"")
        flavor.buildConfigField("String", "MINTER_CACHE_VERS", "\"v4_\"")
        flavor.buildConfigField("Boolean", "ENABLE_LEDGER_UI", "Boolean.parseBoolean(\"${localProps.getProperty("ledger_ui", "false")}\")")
        flavor.buildConfigField("Boolean", "ENABLE_STORIES", "Boolean.parseBoolean(\"${localProps.getProperty("enable_stories", "true")}\")")
        flavor.buildConfigField("String", "CHAINIK_API_URL", "\"https://yf.chainik.io/api/v1/\"")
        flavor.buildConfigField("String", "STORIES_API_URL", "\"https://stories-api.bip.to/api/v1/\"")
    }

    androidResources {
        additionalParameters += "--no-version-vectors"
    }
    lint {
        abortOnError = false
        disable += listOf("CheckResult", "DefaultLocale")
    }
}

fun resolveMinterDeps(): Map<String, List<Provider<MinimalExternalModuleDependency>>> {
    val testnetDeps = listOf(
            deps.minter.sdk.core.testnet,
            deps.minter.sdk.blockchain.testnet,
            deps.minter.sdk.explorer.testnet,
    )
    val mainnetDeps = listOf(
            deps.minter.sdk.core.mainnet,
            deps.minter.sdk.blockchain.mainnet,
            deps.minter.sdk.explorer.mainnet,
    )

    val configurations = listOf(
            "netMainImplementation",
            "netMainLocalImplementation",
            "netTestImplementation",
            "netTestLocalImplementation",
            "toronetImplementation",
            "toronetLocalImplementation",
            )

    val depsMap: MutableMap<String, List<Provider<MinimalExternalModuleDependency>>> = HashMap()

    configurations.forEach {
        val tmp = it.toLowerCase()

        if(tmp.toLowerCase().contains("main")) {
            depsMap[it] = mainnetDeps
        } else {
            depsMap[it] = testnetDeps
        }
    }

    return depsMap
}

dependencies {
    // minter sdk deps
    resolveMinterDeps().forEach { kv ->
        kv.value.forEach { dep ->
            add(kv.key, dep)
        }
    }

    // android
    implementation(deps.base.androidx.appcompat)
    implementation(deps.base.androidx.core.ktx)
    implementation(deps.base.androidx.annotations)
    implementation(deps.base.androidx.biometric)
    implementation(deps.base.androidx.material)
    implementation(deps.base.androidx.vectordrawable)
    implementation(deps.base.androidx.exitinterface)
    implementation(deps.base.androidx.view.recyclerview)
    implementation(deps.base.androidx.view.viewpager2)
    implementation(deps.base.androidx.view.cardview)
    implementation(deps.base.androidx.view.constraintlayout)
    implementation(deps.base.androidx.view.swiperefreshlayout)
    implementation(deps.base.androidx.multidex)
    // room db
    implementation (deps.storage.androidx.room.runtime)
    implementation (deps.storage.androidx.room.rxjava2)
    kapt (deps.storage.androidx.room.compiler)
    // android arch
    implementation(deps.base.androidx.paging.runtime)
    implementation(deps.base.androidx.paging.rxjava2)

    //kotlin
    implementation (deps.base.kotlin.stdlib)
    implementation (deps.base.kotlin.coroutines.core)
    implementation (deps.base.kotlin.coroutines.coreJvm)
    implementation (deps.base.kotlin.coroutines.reactive)
    implementation (deps.base.kotlin.coroutines.android)


    implementation(platform(deps.analytics.firebase.bom))
    implementation(deps.analytics.firebase.analytics)
    implementation(deps.analytics.firebase.crashlytics)


    // yandex
    implementation(deps.analytics.yandex.mobmetrica)
    implementation(deps.analytics.android.installreferer)

    // common
    implementation(deps.qrscanner)
    implementation(deps.zxing.core)
    implementation(deps.zxing.androidCore)
    implementation(deps.zxing.androidIntegration)
    implementation(deps.log.timber)
    implementation(deps.parceler.api)
    kapt(deps.parceler.compiler)

    implementation(deps.storage.kv.hawk)

    implementation("net.danlew:android.joda:2.10.9.1")
    implementation("com.fatboyindustrial.gson-jodatime-serialisers:gson-jodatime-serialisers:1.8.0") {
        exclude(group = "joda-time")
        exclude(group = "com.google.code.gson")
    }
    implementation("com.github.permissions-dispatcher:permissionsdispatcher:4.9.1")
    kapt( "com.github.permissions-dispatcher:permissionsdispatcher-processor:4.9.1")
    implementation("me.saket:better-link-movement-method:2.2.0")
    implementation("io.github.centrifugal:centrifuge-java:0.0.5")
    implementation("com.github.zerobranch:SwipeLayout:1.3.1")

    // Annotations and DI
    kapt("com.google.dagger:dagger-compiler:2.41")
    kapt("com.google.dagger:dagger-android-processor:2.41")
    implementation("com.google.dagger:dagger:2.41")
    implementation("com.google.dagger:dagger-android-support:2.41")
    compileOnly ("javax.annotation:jsr250-api:1.0")

    // MVP
    implementation("com.github.moxy-community:moxy:2.2.2")
    implementation("com.github.moxy-community:moxy-androidx:2.2.2")
    implementation ("com.github.moxy-community:moxy-ktx:2.2.2")
    kapt("com.github.moxy-community:moxy-compiler:2.2.2")

    // Reactive
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // networking
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.4")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.4")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")

    //ui
    implementation("com.jakewharton:butterknife:10.2.3")
    kapt ("com.jakewharton:butterknife-compiler:10.2.1")
    implementation("io.coil-kt:coil:1.1.1")
    implementation("com.squareup.picasso:picasso:2.71828")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.edwardstock:advanced-input-field:0.1.0")
    implementation("com.edwardstock:autocomplete:1.1.2")

    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.airbnb.android:paris:1.7.3")
    kapt ("com.airbnb.android:paris-processor:1.7.3")

    implementation("com.airbnb:deeplinkdispatch:5.2.0")
    kapt ("com.airbnb:deeplinkdispatch-processor:5.2.0")


    //tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("org.mockito:mockito-core:4.3.1")
    testImplementation ("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation ("joda-time:joda-time:2.10.10")

    androidTestImplementation("org.mockito:mockito-android:4.3.1")
    androidTestImplementation("com.squareup.rx.idler:rx2-idler:0.11.0")

    // Core library
    androidTestImplementation("androidx.test:core:1.4.0")

    // AndroidJUnitRunner and JUnit Rules
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")

    // Assertions
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.ext:truth:1.4.0")
    androidTestImplementation("com.google.truth:truth:1.1.3")
    // Espresso dependencies
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.4.0")
    androidTestImplementation("androidx.test.espresso.idling:idling-concurrent:3.4.0")
    kaptAndroidTest("com.google.dagger:dagger-compiler:2.41")
    kaptAndroidTest ("com.google.dagger:dagger-android-processor:2.41")
}
//
//apply plugin: 'com.google.gms.google-services'
