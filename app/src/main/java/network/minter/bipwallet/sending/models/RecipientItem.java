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

package network.minter.bipwallet.sending.models;

import org.parceler.Parcel;

import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public class RecipientItem implements Comparable<RecipientItem> {
    String mAddress;
    String mName;
    Date mDate;
    int mCountUpdates = 0;

    public RecipientItem(CharSequence address) {
        mAddress = address.toString();
    }

    public RecipientItem(CharSequence address, CharSequence name) {
        mAddress = address.toString();
        if (name != null && name.length() > 0) {
            mName = name.toString();
        }
    }

    RecipientItem() {
    }

    public Date getDate() {
        return mDate;
    }

    public String getName() {
        return mName;
    }

    public String getAddress() {
        return mAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipientItem that = (RecipientItem) o;
        return Objects.equals(mAddress, that.mAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAddress);
    }

    @Override
    public int compareTo(@NonNull RecipientItem o) {
        if (mDate == null) {
            mDate = new Date();
        }
        if (o.mDate == null) {
            o.mDate = new Date();
        }

        int cmp = o.mDate.compareTo(mDate);

        if (cmp != 0) {
            return cmp;
        }

        return Integer.compare(o.mCountUpdates, mCountUpdates);
    }

    public RecipientItem update() {
        mDate = new Date();
        mCountUpdates++;
        return this;
    }
}
