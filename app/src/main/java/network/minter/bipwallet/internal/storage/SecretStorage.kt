/*
 * Copyright (C) by MinterTeam. 2020
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
package network.minter.bipwallet.internal.storage

import com.annimon.stream.Stream
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.common.Preconditions
import network.minter.bipwallet.internal.common.Preconditions.checkNotNull
import network.minter.bipwallet.internal.storage.models.SecretData
import network.minter.core.bip39.MnemonicResult
import network.minter.core.bip39.NativeBip39
import network.minter.core.bip39.NativeHDKeyEncoder
import network.minter.core.crypto.BytesData
import network.minter.core.crypto.MinterAddress
import java.security.SecureRandom
import java.util.*
import kotlin.collections.ArrayList

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
typealias OnMainWalletChangeListener = (MinterAddress) -> Unit

class SecretStorage(private val mStorage: KVStorage) {
    companion object {
        private const val KEY_SECRETS = BuildConfig.MINTER_STORAGE_VERS + "secret_storage_mnemonic_secret_list"
        private const val KEY_ADDRESSES = BuildConfig.MINTER_STORAGE_VERS + "secret_storage_addresses_list"
        private const val KEY_ENCRYPTION_PASS = BuildConfig.MINTER_STORAGE_VERS + "secret_storage_encryption_key"
        private const val KEY_PIN_CODE = BuildConfig.MINTER_STORAGE_VERS + "secret_app_pin_code"
        private const val KEY_MAIN_WALLET = BuildConfig.MINTER_STORAGE_VERS + "secret_main_wallet"

        @JvmStatic
        fun generateAddress(): SecretData {
            val random = SecureRandom()
            val mnemonicResult = NativeBip39.encodeBytes(random.generateSeed(16))
            val seed = BytesData(mnemonicResult.toSeed())
            val rootKey = NativeHDKeyEncoder.makeBip32RootKey(seed.bytes)
            val extKey = NativeHDKeyEncoder.makeExtenderKey(rootKey)
            val privateKey = extKey.privateKey
            val publicKey = privateKey.getPublicKey(false)
            return SecretData(mnemonicResult.mnemonic, seed, privateKey, publicKey)
        }
    }

    private val mainWalletChangedListeners: MutableList<OnMainWalletChangeListener> = ArrayList()

    fun hasPinCode(): Boolean {
        return mStorage.contains(KEY_PIN_CODE)
    }

    fun addOnMainWalletChangeListener(listener: OnMainWalletChangeListener) {
        mainWalletChangedListeners.add(listener)
    }

    fun removeOnMainWalletChangeListener(listener: OnMainWalletChangeListener) {
        mainWalletChangedListeners.remove(listener)
    }

    var pinCode: String?
        get() = mStorage.get(KEY_PIN_CODE)
        set(pinCode) {
            mStorage.put(KEY_PIN_CODE, pinCode)
        }

    fun removePinCode() {
        mStorage.delete(KEY_PIN_CODE)
    }

    fun setMain(mainWallet: MinterAddress) {
        mStorage.put(KEY_MAIN_WALLET, mainWallet)
        mainWalletChangedListeners.forEach { it(mainWallet) }
    }

    @JvmOverloads
    fun add(mnemonicResult: MnemonicResult, title: String? = null): MinterAddress {
        require(mnemonicResult.isOk) { "Mnemonic result is not in valid state" }
        val seed = BytesData(mnemonicResult.toSeed())
        checkNotNull(seed)
        Preconditions.checkArgument(seed.size() > 0, "Seed can't be empty")
        val rootKey = NativeHDKeyEncoder.makeBip32RootKey(seed.bytes)
        val extKey = NativeHDKeyEncoder.makeExtenderKey(rootKey)
        val privateKey = extKey.privateKey
        val publicKey = privateKey.getPublicKey(false)
        val data = SecretData(mnemonicResult.mnemonic, seed, privateKey, publicKey, title)
        return add(data)
    }

    @JvmOverloads
    fun add(mnemonicPhrase: String, title: String? = null): MinterAddress {
        return add(MnemonicResult(mnemonicPhrase), title)
    }

    val secrets: MutableMap<String, SecretData>
        get() {
            var secrets = mStorage.get<MutableMap<String, SecretData>?>(KEY_SECRETS)
            if (secrets == null) {
                secrets = HashMap()
            }
            return secrets
        }

    // update to make it stable
    val secretsListSafe: List<SecretData>
        get() {
            val src = secretsSafe
            val main = getSecret(mainWallet)
            val out = Stream.of(src.values).toList()
            Collections.sort(out, Comparator { `as`, bs ->
                val a = `as`.date
                val b = bs.date
                if (a == b) // update to make it stable
                    return@Comparator 0
                if (a == main.date) return@Comparator -1
                if (b == main.date) 1 else a.compareTo(b)
            })
            return out
        }

    val secretsSafe: Map<String, SecretData>
        get() {
            var secrets = mStorage.get<Map<String, SecretData>>(KEY_SECRETS)
            if (secrets == null) {
                secrets = HashMap()
            }
            secrets.forEach { it.value.cleanup() }
            return secrets
        }

    val addresses: MutableList<MinterAddress>
        get() {
            var addresses = mStorage.get<MutableList<MinterAddress>>(KEY_ADDRESSES)
            if (addresses == null) {
                addresses = ArrayList()
            }
            return addresses
        }

    fun destroy() {
        mStorage.deleteAll()
    }

    /**
     * DON'T FORGET to cleanup SecretData by yourself
     * @param secretData
     * @return
     */
    fun add(secretData: SecretData): MinterAddress {
        val secrets = secrets
        val addresses = addresses
        val isMain = secrets.isEmpty()
        val address = secretData.minterAddress
        secrets[address.toString()] = secretData
        mStorage.put(KEY_SECRETS, secrets)
        if (!addresses.contains(address)) {
            addresses.add(address)
            mStorage.put<List<MinterAddress>>(KEY_ADDRESSES, addresses)
        }
        if (isMain) {
            setMain(address)
        }
        return address
    }

    val mainWallet: MinterAddress
        get() = mStorage[KEY_MAIN_WALLET, addresses[0]]

    fun update(data: SecretData) {
        val secrets = secrets
        secrets[data.minterAddress.toString()] = data
        mStorage.put(KEY_SECRETS, secrets)
    }

    val hasSecrets: Boolean
        get() = mStorage.contains(KEY_SECRETS)

    val mainSecret: SecretData
        get() = getSecret(mainWallet)

    fun getSecret(address: MinterAddress): SecretData {
        return mStorage.get<MutableMap<String, SecretData>>(KEY_SECRETS)?.get(address.toString())!!
    }

    fun getSecretSafe(address: MinterAddress): SecretData {
        val sd = getSecret(address)
        sd.cleanup()
        return sd
    }

    fun isMainWallet(minterAddress: MinterAddress): Boolean {
        checkNotNull(minterAddress, "Address can't be null while getting main wallet")
        return mainWallet == minterAddress
    }

    fun delete(address: MinterAddress): Boolean {
        checkNotNull(address, "Address required")
        val secrets = secrets
        check(secrets.size != 1) { "Can't delete last wallet" }
        if (!secrets.containsKey(address.toString())) {
            return false
        }
        secrets.remove(address.toString())
        if (secrets.size == 1) {
            val it: Map.Entry<String?, SecretData?> = secrets.entries.iterator().next()
            setMain(MinterAddress(it.key))
        }
        var ret = mStorage.put(KEY_SECRETS, secrets)

        val newAddressList = addresses.filterNot { it == address }.toMutableList()
        ret = ret && mStorage.put(KEY_ADDRESSES, newAddressList)

        return ret
    }


}