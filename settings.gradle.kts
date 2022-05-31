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

enableFeaturePreview("VERSION_CATALOGS")

include(":app")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
        maven(url = uri("https://repo1.maven.org/maven2/"))
        maven(url = uri("https://clojars.org/repo/"))
        maven(url = uri("https://oss.sonatype.org/content/repositories/snapshots/"))
        maven(url = uri("https://jitpack.io"))
        maven(url = uri("https://oss.jfrog.org/libs-snapshot/"))
        maven(url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/"))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven(url = uri("https://repo1.maven.org/maven2/"))
        maven(url = uri("https://clojars.org/repo/"))
        maven(url = uri("https://oss.sonatype.org/content/repositories/snapshots/"))
        maven(url = uri("https://jitpack.io"))
        maven(url = uri("https://oss.jfrog.org/libs-snapshot/"))
        maven(url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/"))
    }
    versionCatalogs {
        create("deps") {
            from(files("gradle/deps.versions.toml"))
        }
    }

}
