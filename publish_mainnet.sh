#!/usr/bin/env bash

export FIREBASE_TOKEN=$MINTER_FIREBASE_TOKEN
./gradlew --stop
./gradlew assembleNetMainDebug appDistributionUploadNetMainDebug