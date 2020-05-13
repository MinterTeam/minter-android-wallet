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

package network.minter.bipwallet.tx.adapters;

import org.parceler.Parcel;

import java.math.BigDecimal;
import java.util.List;

import androidx.annotation.Nullable;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterHash;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.models.ValidatorMeta;

import static com.google.common.base.MoreObjects.firstNonNull;
import static network.minter.profile.MinterProfileApi.getUserAvatarUrl;

@Parcel
public class TransactionFacade {
    public HistoryTransaction tx;

    public ValidatorMeta validatorMeta = null;
    public UserMeta userMeta = null;

    public TransactionFacade(HistoryTransaction tx) {
        this.tx = tx;
    }

    TransactionFacade() {
    }

    public HistoryTransaction get() {
        return tx;
    }

    public String getAvatar() {
        if (userMeta != null) {
            return firstNonNull(userMeta.avatarUrl, getUserAvatarUrl(1));
        } else if (validatorMeta != null) {
            return validatorMeta.iconUrl;
        } else {
            return getUserAvatarUrl(1);
        }
    }

    public String getName() {
        if (userMeta != null) {
            return userMeta.username;
        } else if (validatorMeta != null) {
            return validatorMeta.name;
        }

        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof TransactionFacade) {
            return ((TransactionFacade) obj).tx.equals(tx);
        } else if (obj instanceof HistoryTransaction) {
            return obj.equals(tx);
        }

        return super.equals(obj);
    }

    public HistoryTransaction.Type getType() {
        return tx.getType();
    }

    public boolean isIncoming(List<MinterAddress> myAddresses) {
        return tx.isIncoming(myAddresses);
    }

    public MinterAddress getFrom() {
        return tx.getFrom();
    }

    public <TxData> TxData getData() {
        return tx.getData();
    }

    public void setUserMeta(String username, String avatarUrl) {
        userMeta = new UserMeta(username, avatarUrl);
    }

    public MinterHash getHash() {
        return tx.getHash();
    }

    public BigDecimal getFee() {
        return tx.getFee();
    }

    public void setValidatorMeta(ValidatorMeta validatorMeta) {
        this.validatorMeta = validatorMeta;
    }

    @Parcel
    public static class UserMeta {
        public String username;
        public String avatarUrl;

        public UserMeta(String username, String avatarUrl) {
            this.username = username;
            this.avatarUrl = avatarUrl;
        }

        UserMeta() {
        }
    }
}
