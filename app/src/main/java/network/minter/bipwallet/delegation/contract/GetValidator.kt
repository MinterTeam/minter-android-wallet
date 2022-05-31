/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.delegation.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import network.minter.bipwallet.delegation.ui.ValidatorSelectorActivity
import network.minter.explorer.models.ValidatorItem
import org.parceler.Parcels

data class GetValidatorOptions(
        val filter: ValidatorSelectorActivity.Filter?
)

class GetValidator : ActivityResultContract<GetValidatorOptions?, ValidatorItem?>() {
    override fun createIntent(context: Context, input: GetValidatorOptions?): Intent {
        val intent = Intent(context, ValidatorSelectorActivity::class.java)
        input?.let {
            input.filter?.let {
                intent.putExtra(EXTRA_FILTER, it)
            }
        }

        return intent

    }

    override fun parseResult(resultCode: Int, intent: Intent?): ValidatorItem? {
        return when {
            resultCode != Activity.RESULT_OK -> null
            intent?.hasExtra(RESULT) != true -> null
            else -> Parcels.unwrap<ValidatorItem>(
                    intent.getParcelableExtra(RESULT)
            )
        }
    }

    companion object {
        const val RESULT = "RESULT_VALIDATOR"
        const val EXTRA_FILTER = "EXTRA_FILTER"
    }

}