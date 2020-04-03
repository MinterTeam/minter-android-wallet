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

package network.minter.bipwallet.addressbook.models;

import org.parceler.Parcel;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.views.widgets.RemoteImageView;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.profile.MinterProfileApi;

@Parcel
@Entity(tableName = "minter_contacts", indices = {
        @Index(value = {"name", "address"}, unique = true)
})
public class AddressContact implements Comparable<AddressContact>, AddressBookItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    @NonNull
    public String address;
    @NonNull
    @TypeConverters({AddressTypeConverter.class})
    public AddressType type;

    public enum AddressType {
        Address("address"),
        ValidatorPubKey("validator");
        String mName;

        AddressType(String name) {
            mName = name;
        }

        public static AddressType find(String name) {
            for (AddressType t : AddressType.values()) {
                if (t.mName.equals(name)) {
                    return t;
                }
            }
            return null;
        }
    }

    public AddressContact(String nameOrAddress) {
        this.name = nameOrAddress;
        this.address = nameOrAddress;
        if (this.address.matches(MinterAddress.ADDRESS_PATTERN)) {
            type = AddressType.Address;
        } else if (this.address.matches(MinterPublicKey.PUB_KEY_PATTERN)) {
            type = AddressType.ValidatorPubKey;
        }
    }

    public AddressContact(String name, MinterAddress address) {
        this.name = name;
        this.address = address.toString();
        this.type = AddressType.Address;
    }

    public AddressContact(String name, MinterPublicKey publicKey) {
        this.name = name;
        this.address = publicKey.toString();
        this.type = AddressType.ValidatorPubKey;
    }

    public AddressContact() {
    }

    public void applyAddressIcon(RemoteImageView remoteImageView) {
        if (type == AddressType.Address) {
            remoteImageView.setImageUrlFallback(getAvatar(), R.drawable.img_avatar_default);
        } else {
            remoteImageView.setImageResource(R.drawable.img_avatar_delegate);
        }
    }

    public String getAvatar() {
        return MinterProfileApi.getUserAvatarUrlByAddress(address);
    }

    public MinterAddress getMinterAddress() {
        return new MinterAddress(address);
    }

    public MinterPublicKey getMinterPublicKey() {
        return new MinterPublicKey(address);
    }

    @Override
    public int getViewType() {
        return TYPE_ITEM;
    }

    @Override
    public boolean isSameOf(AddressBookItem item) {
        return item.getViewType() == getViewType() && id == ((AddressContact) item).id;
    }

    @Override
    public boolean equals(Object o) {
//        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressContact that = (AddressContact) o;
        return id == that.id &&
                name.equals(that.name) &&
                address.equals(that.address) &&
                type == that.type;
    }

    @Override
    public String toString() {
        return "AddressContact{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, address, type);
    }

    @Override
    public int compareTo(AddressContact o) {
        return name.compareTo(o.name);
    }

    public static class AddressTypeConverter {
        @TypeConverter
        public static AddressType toType(String type) {
            return AddressType.find(type);
        }

        @TypeConverter
        public static String fromType(AddressType type) {
            return type.mName;
        }
    }
}
