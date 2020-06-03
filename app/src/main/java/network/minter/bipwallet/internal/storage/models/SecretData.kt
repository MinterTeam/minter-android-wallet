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
package network.minter.bipwallet.internal.storage.models

import com.google.common.base.MoreObjects
import network.minter.core.crypto.BytesData
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.PrivateKey
import network.minter.core.crypto.PublicKey
import java.io.Serializable
import java.util.*

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class SecretData : Serializable, Cloneable {
    var id: String
        private set
    var seedPhrase: String?
        private set
    var seed: BytesData
        private set
    var privateKey: PrivateKey = PrivateKey("F000000000000000000000000000000000000000000000000000000000000000")
        private set
    var publicKey: PublicKey?
        private set
    var minterAddress: MinterAddress
        private set
    var date = Date()

    private var mTitle: String? = null

    var canSign: Boolean = true
        private set

    constructor(address: MinterAddress) {
        id = UUID.randomUUID().toString()
        seedPhrase = null
        seed = BytesData(0)
        publicKey = null
        minterAddress = address
    }

    @JvmOverloads
    constructor(seedPhrase: String, seed: BytesData, privateKey: PrivateKey, publicKey: PublicKey, title: String? = null) {
        id = UUID.randomUUID().toString()
        this.seedPhrase = seedPhrase
        this.seed = seed
        this.privateKey = privateKey
        this.publicKey = publicKey
        minterAddress = publicKey.toMinter()
        mTitle = MoreObjects.firstNonNull(title, minterAddress.toShortString())
    }

    val hasTitle: Boolean
        get() = mTitle != null && mTitle != minterAddress.toShortString()

    var title: String?
        get() = mTitle ?: minterAddress.toShortString()
        set(title) {
            mTitle = title
        }

    fun cleanup() {
        seedPhrase = ""
        seed.cleanup()
        privateKey.cleanup()
        canSign = false
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        throw CloneNotSupportedException("Can't clone secret data")
    }
}