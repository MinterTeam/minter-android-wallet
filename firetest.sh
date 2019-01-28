#!/usr/bin/env bash

./gradlew :app:assembleNetTestNoCrashlyticsDebug :app:assembleNetTestNoCrashlyticsDebugAndroidTest
gcloud firebase test android run \
    --type instrumentation \
    --app app/build/outputs/apk/netTestNoCrashlytics/debug/app-netTestNoCrashlytics-debug.apk \
    --test app/build/outputs/apk/androidTest/netTestNoCrashlytics/debug/app-netTestNoCrashlytics-debug-androidTest.apk \
    --device model=hwALE-H,version=21,locale=en,orientation=portrait