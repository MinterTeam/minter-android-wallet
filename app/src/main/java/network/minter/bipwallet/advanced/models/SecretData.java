/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.advanced.models;

import java.io.Serializable;
import java.util.UUID;

import network.minter.mintercore.crypto.BytesData;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.mintercore.crypto.PrivateKey;
import network.minter.mintercore.crypto.PublicKey;
import network.minter.my.models.MyAddressData;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class SecretData implements Serializable {
    private String id;
    private String mSeedPhrase;
    private BytesData mSeed;
    private PrivateKey mPrivateKey;
    private PublicKey mPublicKey;
    private MinterAddress mMinterAddress;

    public SecretData(String seedPhrase, BytesData seed, PrivateKey privateKey, PublicKey publicKey) {
        id = UUID.randomUUID().toString();
        mSeedPhrase = seedPhrase;
        mSeed = seed;
        mPrivateKey = privateKey;
        mPublicKey = publicKey;
        mMinterAddress = publicKey.toMinter();
    }

    public String getSeedPhrase() {
        return mSeedPhrase;
    }

    public BytesData getSeed() {
        return mSeed;
    }

    public PrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    public PublicKey getPublicKey() {
        return mPublicKey;
    }

    public MinterAddress getMinterAddress() {
        return mMinterAddress;
    }

    public MyAddressData toAddressData(boolean isMain, boolean isServerSecured, String encKey) {
        return new MyAddressData(mMinterAddress, isMain, mSeedPhrase, isServerSecured, encKey);
    }

    public void cleanup() {
        mSeedPhrase = "";
        mSeed.cleanup();
        mPrivateKey.cleanup();
    }

    public String getId() {
        return id;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Can't clone secret data");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cleanup();
    }
}
