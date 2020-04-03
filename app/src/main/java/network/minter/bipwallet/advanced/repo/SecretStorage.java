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

package network.minter.bipwallet.advanced.repo;

import com.annimon.stream.Stream;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.core.bip39.HDKey;
import network.minter.core.bip39.MnemonicResult;
import network.minter.core.bip39.NativeBip39;
import network.minter.core.bip39.NativeHDKeyEncoder;
import network.minter.core.crypto.BytesData;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.PrivateKey;
import network.minter.core.crypto.PublicKey;

import static network.minter.bipwallet.internal.common.Preconditions.checkArgument;
import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SecretStorage {

    private final static String KEY_SECRETS = BuildConfig.MINTER_STORAGE_VERS + "secret_storage_mnemonic_secret_list";
    private final static String KEY_ADDRESSES = BuildConfig.MINTER_STORAGE_VERS + "secret_storage_addresses_list";
    private final static String KEY_ENCRYPTION_PASS = BuildConfig.MINTER_STORAGE_VERS + "secret_storage_encryption_key";
    private final static String KEY_PIN_CODE = BuildConfig.MINTER_STORAGE_VERS + "secret_app_pin_code";
    private final static String KEY_MAIN_WALLET = BuildConfig.MINTER_STORAGE_VERS + "secret_main_wallet";
    private final KVStorage mStorage;

    public SecretStorage(KVStorage storage) {
        mStorage = storage;
    }

    public static SecretData generateAddress() {
        final SecureRandom random = new SecureRandom();
        final MnemonicResult mnemonicResult = NativeBip39.encodeBytes(random.generateSeed(16));
        final BytesData seed = new BytesData(mnemonicResult.toSeed());
        final HDKey rootKey = NativeHDKeyEncoder.makeBip32RootKey(seed.getBytes());
        final HDKey extKey = NativeHDKeyEncoder.makeExtenderKey(rootKey);
        final PrivateKey privateKey = extKey.getPrivateKey();
        final PublicKey publicKey = privateKey.getPublicKey(false);

        return new SecretData(mnemonicResult.getMnemonic(), seed, privateKey, publicKey);
    }

    public boolean hasPinCode() {
        return mStorage.contains(KEY_PIN_CODE);
    }

    public String getPinCode() {
        return mStorage.get(KEY_PIN_CODE);
    }

    public void setPinCode(String pinCode) {
        mStorage.put(KEY_PIN_CODE, pinCode);
    }

    public void removePinCode() {
        mStorage.delete(KEY_PIN_CODE);
    }

    public void setMain(MinterAddress mainWallet) {
        mStorage.put(KEY_MAIN_WALLET, mainWallet);
    }

    public MinterAddress add(@NonNull MnemonicResult mnemonicResult) {
        return add(mnemonicResult, null);
    }

    public MinterAddress add(@NonNull MnemonicResult mnemonicResult, String title) {
        if (!mnemonicResult.isOk()) {
            throw new IllegalArgumentException("Mnemonic result is not in valid state");
        }

        final BytesData seed = new BytesData(mnemonicResult.toSeed());
        checkNotNull(seed);
        checkArgument(seed.size() > 0, "Seed can't be empty");

        final HDKey rootKey = NativeHDKeyEncoder.makeBip32RootKey(seed.getBytes());
        final HDKey extKey = NativeHDKeyEncoder.makeExtenderKey(rootKey);
        final PrivateKey privateKey = extKey.getPrivateKey();
        final PublicKey publicKey = privateKey.getPublicKey(false);

        final SecretData data = new SecretData(mnemonicResult.getMnemonic(), seed, privateKey, publicKey, title);
        return add(data);
    }

    public MinterAddress add(@NonNull final String mnemonicPhrase) {
        return add(new MnemonicResult(mnemonicPhrase), null);
    }

    public MinterAddress add(@NonNull final String mnemonicPhrase, @Nullable String title) {
        return add(new MnemonicResult(mnemonicPhrase), title);
    }

    public Map<String, SecretData> getSecrets() {
        Map<String, SecretData> secrets = mStorage.get(KEY_SECRETS);
        if (secrets == null) {
            secrets = new HashMap<>();
        }

        return secrets;
    }

    public List<SecretData> getSecretsListSafe() {
        final Map<String, SecretData> src = getSecretsSafe();
        final SecretData main = getSecret(getMainWallet());
        final List<SecretData> out = Stream.of(src.values()).toList();
        Collections.sort(out, new Comparator<SecretData>() {
            @Override
            public int compare(SecretData as, SecretData bs) {
                final Date a = as.getDate();
                final Date b = bs.getDate();

                if (a.equals(b)) // update to make it stable
                    return 0;
                if (a.equals(main.getDate()))
                    return -1;
                if (b.equals(main.getDate()))
                    return 1;

                return a.compareTo(b);
            }
        });

        return out;
    }

    public Map<String, SecretData> getSecretsSafe() {
        Map<String, SecretData> secrets = mStorage.get(KEY_SECRETS);
        if (secrets == null) {
            secrets = new HashMap<>();
        }

        for (Map.Entry<String, SecretData> entry : secrets.entrySet()) {
            entry.getValue().cleanup();
        }

        return secrets;
    }

    public Stream<Map.Entry<String, SecretData>> getSecretsStream() {
        return Stream.of(getSecrets().entrySet());
    }

    public List<MinterAddress> getAddresses() {
        List<MinterAddress> addresses = mStorage.get(KEY_ADDRESSES);
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        return addresses;
    }

    public void destroy() {
        mStorage.deleteAll();
    }

    /**
     * DON'T FORGET to cleanup SecretData by yourself
     * @param secretData
     * @return
     */
    public MinterAddress add(@NonNull final SecretData secretData) {
        Map<String, SecretData> secrets = getSecrets();
        List<MinterAddress> addresses = getAddresses();
        boolean isMain = secrets.size() == 0;

        final MinterAddress address = secretData.getMinterAddress();
        secrets.put(address.toString(), secretData);
        mStorage.put(KEY_SECRETS, secrets);

        if (!addresses.contains(address)) {
            addresses.add(address);
            mStorage.put(KEY_ADDRESSES, addresses);
        }

        if (isMain) {
            setMain(address);
        }

        return address;
    }

    public MinterAddress getMainWallet() {
        return mStorage.get(KEY_MAIN_WALLET, getAddresses().get(0));
    }

    public void update(SecretData data) {
        Map<String, SecretData> secrets = getSecrets();
        secrets.put(data.getMinterAddress().toString(), data);
        mStorage.put(KEY_SECRETS, secrets);
    }

    public SecretData getSecret(MinterAddress address) {
        return mStorage.<Map<String, SecretData>>get(KEY_SECRETS).get(address.toString());
    }

    public SecretData getSecretSafe(MinterAddress address) {
        final SecretData sd = getSecret(address);
        sd.cleanup();
        return sd;
    }

    @NonNull
    public boolean isMainWallet(@NonNull MinterAddress minterAddress) {
        checkNotNull(minterAddress, "Address can't be null while getting main wallet");
        return getMainWallet().equals(minterAddress);
    }

    public boolean delete(@NonNull MinterAddress address) {
        checkNotNull(address, "Address required");

        Map<String, SecretData> secrets = getSecrets();
        if (secrets.size() == 1) {
            throw new IllegalStateException("Can't delete last wallet");
        }

        if (!secrets.containsKey(address.toString())) {
            return false;
        }
        secrets.remove(address.toString());

        if (secrets.size() == 1) {
            Map.Entry<String, SecretData> it = secrets.entrySet().iterator().next();
            setMain(new MinterAddress(it.getKey()));
        }

        return mStorage.put(KEY_SECRETS, secrets);
    }
}
