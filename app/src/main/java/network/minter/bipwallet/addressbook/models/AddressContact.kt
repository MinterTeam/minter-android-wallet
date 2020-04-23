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
package network.minter.bipwallet.addressbook.models

import androidx.annotation.NonNull
import androidx.room.*
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.views.widgets.RemoteImageView
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.profile.MinterProfileApi
import org.parceler.Parcel

@Parcel
@Entity(
        tableName = "minter_contacts",
        indices = [
            Index(value = ["name", "address"], unique = true)
        ]
)
class AddressContact : Comparable<AddressContact>, AddressBookItem {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @JvmField
    @NonNull var name: String? = null

    @JvmField
    @NonNull var address: String? = null

    @JvmField
    @NonNull
    @TypeConverters(AddressTypeConverter::class)
    var type: AddressType = AddressType.Address

    enum class AddressType(var type: String) {
        Address("address"),
        ValidatorPubKey("validator");

        companion object {
            @JvmStatic
            fun find(type: String): AddressType {
                for (t in values()) {
                    if (t.type == type) {
                        return t
                    }
                }
                throw IllegalArgumentException("Unknown address type $type")
            }
        }
    }

    constructor() {}

    constructor(nameOrAddress: String) {
        name = nameOrAddress
        address = nameOrAddress

        if (address == null) {
            throw IllegalStateException("Address can't be null")
        }

        if (address!!.matches(MinterAddress.ADDRESS_PATTERN.toRegex())) {
            type = AddressType.Address
        } else if (address!!.matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())) {
            type = AddressType.ValidatorPubKey
        }
    }

    constructor(name: String, address: MinterAddress) {
        this.name = name
        this.address = address.toString()
        type = AddressType.Address
    }

    constructor(name: String, publicKey: MinterPublicKey) {
        this.name = name
        address = publicKey.toString()
        type = AddressType.ValidatorPubKey
    }

    fun applyAddressIcon(remoteImageView: RemoteImageView) {
        if (type == AddressType.Address) {
            remoteImageView.setImageUrlFallback(avatar, R.drawable.img_avatar_default)
        } else {
            remoteImageView.setImageResource(R.drawable.img_avatar_delegate)
        }
    }

    val avatar: String
        get() = MinterProfileApi.getUserAvatarUrlByAddress(address)

    val minterAddress: MinterAddress
        get() = MinterAddress(address)

    val minterPublicKey: MinterPublicKey
        get() = MinterPublicKey(address)

    val shortAddress: String
        get() {
            return when (type) {
                AddressType.Address -> minterAddress.toShortString()
                AddressType.ValidatorPubKey -> minterPublicKey.toShortString()
            }
        }

    override fun getViewType(): Int {
        return AddressBookItem.TYPE_ITEM
    }

    override fun isSameOf(item: AddressBookItem): Boolean {
        return item.viewType == viewType && id == (item as AddressContact).id
    }

    override fun toString(): String {
        return "AddressContact{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", type=" + type +
                '}'
    }

    override fun compareTo(other: AddressContact): Int {
        return name!!.compareTo(other.name!!)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddressContact) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }

    object AddressTypeConverter {
        @JvmStatic
        @TypeConverter
        fun toType(type: String): AddressType {
            return AddressType.find(type)
        }

        @JvmStatic
        @TypeConverter
        fun fromType(type: AddressType): String {
            return type.type
        }
    }
}