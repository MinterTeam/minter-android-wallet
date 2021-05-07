/*
 * Copyright (C) by MinterTeam. 2021
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
package network.minter.bipwallet.sending.ui

import com.edwardstock.inputfield.form.validators.BaseValidator
import io.reactivex.Single
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import java.util.*

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
internal class RecipientValidator(
        errorMessage: CharSequence = tr(R.string.input_validator_invalid_recipient),
        required: Boolean = true) : BaseValidator(errorMessage, required) {

    private val defMessage: CharSequence = errorMessage

    override fun validate(value: CharSequence?): Single<Boolean> {
        if (value.isNullOrEmpty()) {
            return Single.just(isRequired)
        }

        errorMessage = defMessage

        val v = value.toString()
        if (v.length >= 2) {
            val pref = v.substring(0, 2)
            if (pref.lowercase(Locale.getDefault()) == "mx") {
                return Single.just(v.matches(MinterAddress.ADDRESS_PATTERN.toRegex()))
            } else if (pref.lowercase(Locale.getDefault()) == "mp" && v.lowercase(Locale.getDefault()).matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())) {
                errorMessage = HtmlCompat.fromHtml(String.format(tr(R.string.input_validator_delegation_is_not_allowed), v))
                return Single.just(false)
            }
        }

        return Wallet.app().addressBookRepo().countExactByNameOrAddress(v)
                .map { cnt -> cnt > 0 }
    }
}