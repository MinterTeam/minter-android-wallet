# Android BIP Wallet
[![Download](https://play.google.com/intl/en_us/badges/images/badge_new.png)](https://play.google.com/store/apps/details?id=network.minter.bipwallet)


BIP Wallet TESTNET is an application for buying, selling, and exchanging MNT—digital coin used in the Minter blockchain’s test network.

Featured on Telegram’s official blog, Minter has put a lot of effort into designing a fast, robust, and user-friendly mobile wallet.

Tired of having to memorize those long strings of symbols they call blockchain addresses? Quit that! With BIP wallet, you can transfer funds to an e-mail address and even a username. What’s more, it takes only five seconds for a transaction to be completed.

## Download

Latest version always available at [Google Play](https://play.google.com/store/apps/details?id=network.minter.bipwallet),

or from latest tag:

[![**release tag**](https://img.shields.io/github/release/MinterTeam/minter-android-wallet.svg)](https://github.com/MinterTeam/minter-android-wallet/releases/latest)


## Documentation

Javadoc available in code comments

## Build

### 1. Clone repository
```bash
git clone https://github.com/MinterTeam/minter-android-wallet.git

// see latest stable release tag
git checkout ${latest.tag}
```

### 2. Install [Android SDK](https://developer.android.com/) (if not installed yet)

### 3. Build via gradle

#### 3.1 Create keystore for [app signing](https://developer.android.com/studio/publish/app-signing)

First, locate your [**JDK**](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) path and add `$JAVA_HOME/bin` directory to your $PATH variable

```bash
export PATH=$PATH:/path/to/jdk/
```

Default JDK locations:
 - Windows: `C:\Program Files\Java\jre7\bin`
 - macOS: `/Library/Java/JavaVirtualMachines/jdk-$VERS.jdk`

Second. Create keystore file via [**keytool**](https://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html)

Example:
```
keytool -genkey -v -keystore wallet.keystore -alias wallet_alias -keyalg RSA -keysize 2048 -validity 10000
```

Or via Android Studio. See google examples: [link](https://developer.android.com/studio/publish/app-signing)

Third. Put keystore values to $HOME/.gradle/gradle.properties
```groovy
minter_key_alias=$KEY_ALIAS_YOU_WERE_SET
minter_key_password=$KEY_PASSWORD_YOU_WERE_SET
minter_key_store_file=$KEY_STORE_FILE_PATH_YOU_WERE_SET
minter_key_store_password=$KEY_STORE_FILE_PASSWORD_YOU_WERE_SET
```

#### 3.2 Assembling
```bash
cd /path/to/minter-android-wallet

// Testnet flavor
./gradlew :clean :app:assembleNetTestRelease

// Mainnet flavor
./gradlew :clean :app:assembleNetMainRelease
```

Built apk file will be stored at:
 - For testnet: **app/build/outputs/apk/netTest/release**
 - For mainnet: **app/build/outputs/apk/netMain/release**


### 4. Testing
Unit tests:
```bash
./gradlew :app:testNetTestDebugUnitTest
```

UI test (emulator or physical device required)
```bash
./gradlew :app:connectedNetTestDebugAndroidTest
```

## Changelog

See [RELEASE_NOTES.md](RELEASE_NOTES.md)


## License

This software is released under the [MIT](LICENSE.txt) License.

© 2018 MinterTeam <edward.vstock@gmail.com>, All rights reserved.