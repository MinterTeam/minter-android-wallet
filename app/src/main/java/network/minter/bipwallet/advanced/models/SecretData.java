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

package network.minter.bipwallet.advanced.models;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import network.minter.core.crypto.BytesData;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.PrivateKey;
import network.minter.core.crypto.PublicKey;
import network.minter.profile.models.ProfileAddressData;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class SecretData implements Serializable {
    private String id;
    private String mTitle;
    private String mSeedPhrase;
    private BytesData mSeed;
    private PrivateKey mPrivateKey;
    private PublicKey mPublicKey;
    private MinterAddress mMinterAddress;
    private Date mAdded = new Date();
    private boolean mCanSign = true;

    public SecretData(MinterAddress address) {
        id = UUID.randomUUID().toString();
        mSeedPhrase = null;
        mSeed = new BytesData(0);
        mPrivateKey = null;
        mPublicKey = null;
        mMinterAddress = address;
    }

    public SecretData(String seedPhrase, BytesData seed, PrivateKey privateKey, PublicKey publicKey, String title) {
        id = UUID.randomUUID().toString();
        mSeedPhrase = seedPhrase;
        mSeed = seed;
        mPrivateKey = privateKey;
        mPublicKey = publicKey;
        mMinterAddress = publicKey.toMinter();
        mTitle = firstNonNull(title, mMinterAddress.toShortString());
    }

    public SecretData(String seedPhrase, BytesData seed, PrivateKey privateKey, PublicKey publicKey) {
        this(seedPhrase, seed, privateKey, publicKey, null);
    }

    public String getTitle() {
        return firstNonNull(mTitle, mMinterAddress.toShortString());
    }

    public void setTitle(String title) {
        mTitle = title;
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

    public ProfileAddressData toAddressData(boolean isMain, boolean isServerSecured, String encKey) {
        return new ProfileAddressData(mMinterAddress, isMain, mSeedPhrase, isServerSecured, encKey);
    }

    public boolean canSign() {
        return mCanSign;
    }

    public void cleanup() {
        mSeedPhrase = "";
        mSeed.cleanup();
        if (mPrivateKey != null) {
            mPrivateKey.cleanup();
        }
        mCanSign = false;
    }

    public String getId() {
        return id;
    }

    public Date getDate() {
        return mAdded;
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
