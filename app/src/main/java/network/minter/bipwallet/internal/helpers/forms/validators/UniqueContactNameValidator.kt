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

package network.minter.bipwallet.internal.helpers.forms.validators

import com.edwardstock.inputfield.form.validators.BaseValidator
import io.reactivex.Single
import network.minter.bipwallet.internal.Wallet

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class UniqueContactNameValidator(
        private val exclude: String? = null,
        private val findBy: FindBy,
        errorMessage: CharSequence = "Contact with this title already exists",
        required: Boolean = true
) : BaseValidator(errorMessage, required) {

    enum class FindBy {
        Name,
        Address
    }

    override fun validate(value: CharSequence?): Single<Boolean> {
        if (!isRequired && (value == null || value.isEmpty())) {
            return Single.just(true)
        }

        if (value == null || value.isEmpty()) return Single.just(false)

        return when (findBy) {
            FindBy.Name -> {
                Wallet.app().addressBookRepo().countByName(value.toString(), exclude).map {
                    it == 0
                }
            }
            FindBy.Address -> {
                Wallet.app().addressBookRepo().countByAddress(value.toString(), exclude).map {
                    it == 0
                }
            }
        }
    }
}