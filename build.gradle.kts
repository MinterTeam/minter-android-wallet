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

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
        classpath("com.google.gms:google-services:4.3.10")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.1")
        classpath("com.google.firebase:firebase-appdistribution-gradle:3.0.1")
        classpath("gradle.plugin.firebase.test.lab:plugin:1.1.2")
    }
}

allprojects {

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven ( url = "https://clojars.org/repo/" )
        maven ( url = "https://s01.oss.sonatype.org/content/repositories/releases/")
        maven ( url = "https://oss.sonatype.org/content/repositories/snapshots/")
        maven ( url = "https://jitpack.io" )
        maven ( url = "https://repo1.maven.org/maven2/com/google/zxing/" )
        maven ( url = "https://maven.fabric.io/public" )
        maven ( url = "https://oss.jfrog.org/libs-snapshot/" )
        maven ( url = "https://oss.jfrog.org/artifactory/oss-snapshot-local/" )
        maven ( url = "https://minter.jfrog.io/artifactory/android/" )
    }
}
