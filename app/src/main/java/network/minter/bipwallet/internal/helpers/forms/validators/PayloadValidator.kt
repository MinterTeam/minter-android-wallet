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
package network.minter.bipwallet.internal.helpers.forms.validators

import com.edwardstock.inputfield.form.validators.BaseValidator
import io.reactivex.Single
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.blockchain.models.operational.Transaction
import java.nio.charset.StandardCharsets

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 30-May-19
 */
class PayloadValidator
@JvmOverloads constructor(
        errorMessage: CharSequence = tr(R.string.input_validator_max_payload_length),
        required: Boolean = false
) : BaseValidator(errorMessage, required) {

    override fun validate(value: CharSequence?): Single<Boolean> {
        if (!isRequired && (value.isNullOrEmpty())) {
            return Single.just(true)
        }

        return Single.just(
                value.toString().toByteArray(StandardCharsets.UTF_8).size <= MAX_PAYLOAD_LENGTH
        )
    }

    companion object {
        const val MAX_PAYLOAD_LENGTH = Transaction.MAX_PAYLOAD_LENGTH
    }
}
