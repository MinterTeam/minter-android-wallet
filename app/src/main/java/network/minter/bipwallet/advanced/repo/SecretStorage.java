/*
 * Copyright (C) by MinterTeam. 2018
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import androidx.annotation.NonNull;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.core.bip39.HDKey;
import network.minter.core.bip39.MnemonicResult;
import network.minter.core.bip39.NativeBip39;
import network.minter.core.bip39.NativeHDKeyEncoder;
import network.minter.core.crypto.BytesData;
import network.minter.core.crypto.HashUtil;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.PrivateKey;
import network.minter.core.crypto.PublicKey;
import network.minter.core.internal.helpers.StringHelper;

import static network.minter.bipwallet.internal.common.Preconditions.checkArgument;
import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SecretStorage {

    private final static String KEY_SECRETS = "secret_storage_mnemonic_secret_list";
    private final static String KEY_SECRETS_MIGRATION = "secret_storage_mnemonic_secret_list_migration";
    private final static String KEY_ADDRESSES = "secret_storage_addresses_list";
    private final static String KEY_ENCRYPTION_PASS = "secret_storage_encryption_key";
    private final static String KEY_PIN_CODE = "secret_app_pin_code";
    private final KVStorage mStorage;

    public SecretStorage(KVStorage storage) {
        mStorage = storage;
    }

    public static SecretData generateAddress() {
        final SecureRandom random = new SecureRandom();
        final MnemonicResult mnemonicResult = NativeBip39.encodeBytes(random.generateSeed(16));
        final BytesData seed = new BytesData(mnemonicResult.toSeed());
        final HDKey rootKey = NativeHDKeyEncoder.makeBip32RootKey(seed.getData());
        final HDKey extKey = NativeHDKeyEncoder.makeExtenderKey(rootKey);
        final PrivateKey privateKey = extKey.getPrivateKey();
        final PublicKey publicKey = privateKey.getPublicKey(false);

        return new SecretData(mnemonicResult.getMnemonic(), seed, privateKey, publicKey);
    }

    public void setEncryptionKey(byte[] sha256EncryptedKey) {
        mStorage.put(KEY_ENCRYPTION_PASS, StringHelper.bytesToHexString(sha256EncryptedKey));
    }

    public boolean hasEncryptionKey() {
        return mStorage.contains(KEY_ENCRYPTION_PASS);
    }

    public String getEncryptionKey() {
        return mStorage.get(KEY_ENCRYPTION_PASS);
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

    public void setEncryptionKey(String rawEncryptionKey) {
        mStorage.put(KEY_ENCRYPTION_PASS, HashUtil.sha256Hex(rawEncryptionKey));
    }

    public MinterAddress add(@NonNull MnemonicResult mnemonicResult) {
        if (!mnemonicResult.isOk()) {
            throw new IllegalArgumentException("Mnemonic result is not in valid state");
        }

        final BytesData seed = new BytesData(mnemonicResult.toSeed());
        checkNotNull(seed);
        checkArgument(seed.size() > 0, "Seed can't be empty");

        final HDKey rootKey = NativeHDKeyEncoder.makeBip32RootKey(seed.getData());
        final HDKey extKey = NativeHDKeyEncoder.makeExtenderKey(rootKey);
        final PrivateKey privateKey = extKey.getPrivateKey();
        final PublicKey publicKey = privateKey.getPublicKey(false);

        final SecretData data = new SecretData(mnemonicResult.getMnemonic(), seed, privateKey, publicKey);
        return add(data);
    }

    public MinterAddress add(@NonNull final String mnemonicPhrase) {
        return add(new MnemonicResult(mnemonicPhrase));
    }

    public void remove(MinterAddress address) {
        Map<MinterAddress, SecretData> secrets = mStorage.get(KEY_SECRETS);
        if (secrets == null || secrets.isEmpty()) {
            return;
        }

        secrets.remove(address);
    }

    public Map<String, SecretData> getSecrets() {
        Map<String, SecretData> secrets = mStorage.get(KEY_SECRETS);
        if (secrets == null) {
            secrets = new HashMap<>();
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
     *
     * @param secretData
     * @return
     */
    public MinterAddress add(@NonNull final SecretData secretData) {
        Map<String, SecretData> secrets = getSecrets();
        List<MinterAddress> addresses = getAddresses();

        final MinterAddress address = secretData.getMinterAddress();
        secrets.put(address.toString(), secretData);
        mStorage.put(KEY_SECRETS, secrets);

        if (!addresses.contains(address)) {
            addresses.add(address);
            mStorage.put(KEY_ADDRESSES, addresses);
        }

        return address;
    }

    public SecretData getSecret(MinterAddress address) {
        return mStorage.<Map<String, SecretData>>get(KEY_SECRETS).get(address.toString());
    }

    public void removeMigrationQueue() {
        mStorage.delete(KEY_SECRETS_MIGRATION);
    }

    public void updateMigrationQueue(Queue<SecretData> secretData) {
        mStorage.delete(KEY_SECRETS_MIGRATION);
        mStorage.putQueue(KEY_SECRETS_MIGRATION, secretData);
    }

    public Queue<SecretData> getOrCreateMigrationQueue() {
        if (mStorage.contains(KEY_SECRETS_MIGRATION)) {
            return mStorage.getQueue(KEY_SECRETS_MIGRATION);
        }

        Queue<SecretData> secretQueue = new LinkedList<>(getSecrets().values());
        mStorage.putQueue(KEY_SECRETS_MIGRATION, secretQueue);

        return secretQueue;
    }
}
