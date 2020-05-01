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
package network.minter.bipwallet.internal.dialogs

import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import com.airbnb.paris.extensions.style
import network.minter.bipwallet.apis.reactive.ReactiveMyMinter
import network.minter.bipwallet.databinding.DialogConfirmBinding
import network.minter.bipwallet.internal.common.Preconditions.checkArgument
import network.minter.bipwallet.internal.helpers.ExceptionHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.ViewHelper.setSelectableItemBackground
import network.minter.core.internal.exceptions.NetworkException
import network.minter.profile.models.ProfileResult
import retrofit2.HttpException
import java.io.IOException

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class ConfirmDialog(context: Context, private val builder: Builder) : WalletDialog(context) {

    private lateinit var binding: DialogConfirmBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.title.text = builder.title
        binding.dialogDescription.visible = builder.mDescription != null
        binding.dialogDescription.text = builder.mDescription

        binding.dialogText.text = builder.mText
        binding.dialogText.textAlignment = builder.mTextAlignment
        binding.dialogText.setTextIsSelectable(builder.mTextIsSelectable)

        if (builder.mDescriptionTypeface > 0) {
            binding.dialogDescription.typeface = ResourcesCompat.getFont(context, builder.mDescriptionTypeface)
        }
        if (builder.mButtonStyleRes > 0) {
            binding.actionConfirm.style(builder.mButtonStyleRes)
        }

        if (builder.mOnTextClickListener != null) {
            binding.dialogText.isClickable = true
            binding.dialogText.isFocusable = true
            setSelectableItemBackground(binding.dialogText)
            binding.dialogText.setOnClickListener(builder.mOnTextClickListener)
        }
        if (builder.mTypeface != null) {
            binding.dialogText.typeface = builder.mTypeface
        }


        builder.bindAction(this, binding.actionConfirm, DialogInterface.BUTTON_POSITIVE)
        builder.bindAction(this, binding.actionDecline, DialogInterface.BUTTON_NEGATIVE)
    }

    class Builder : WalletDialogBuilder<ConfirmDialog, Builder> {
        var mText: CharSequence? = null
        var mDescription: CharSequence? = null
        var mTextIsSelectable = false
        var mTypeface: Typeface? = null
        var mDescriptionTypeface: Int = -1
        var mOnTextClickListener: View.OnClickListener? = null
        var mTextAlignment = View.TEXT_ALIGNMENT_INHERIT

        @get:StyleRes
        var mButtonStyleRes = -1

        constructor(context: Context, @StringRes title: Int) : super(context, title)
        constructor(context: Context, title: CharSequence?) : super(context, title)

        override fun create(): ConfirmDialog {
            checkArgument(hasActionTitle(DialogInterface.BUTTON_POSITIVE), "At least, positive action title should be set")
            return ConfirmDialog(context, this)
        }

        fun setPositiveActionStyle(@StyleRes resId: Int): Builder {
            mButtonStyleRes = resId
            return this
        }

        fun setDescriptionTypeface(@FontRes resId: Int): Builder {
            mDescriptionTypeface = resId
            return this
        }

        fun setText(text: CharSequence?): Builder {
            mText = text
            return this
        }

        fun setText(@StringRes resId: Int): Builder {
            return setText(context.getString(resId))
        }

        fun setText(text: String, vararg args: Any?): Builder {
            mText = String.format(text, *args)
            return this
        }

        fun setDescription(description: CharSequence?): Builder {
            mDescription = description
            return this
        }

        fun setDescription(@StringRes resId: Int): Builder {
            return setDescription(context.getString(resId))
        }

        fun setDescription(@StringRes resId: Int, vararg args: Any): Builder {
            mDescription = context.getString(resId, *args)
            return this
        }

        fun setOnTextClickListener(listener: View.OnClickListener?): Builder {
            mOnTextClickListener = listener
            return this
        }

        fun setTextIsSelectable(isSelectable: Boolean): Builder {
            mTextIsSelectable = isSelectable
            return this
        }

        fun setText(t: Throwable): Builder {
            if (t is HttpException) {
                if (t.code() in 500..999) {
                    setTitle(title.toString() + " (server error " + t.code() + ")")
                } else if (t.code() in 1..499) {
                    setTitle(title.toString() + " (client error " + t.code() + ")")
                } else {
                    setTitle(title.toString() + " (network error " + t.code() + ")")
                }
                try {
                    var out = """
                        ${t.response()!!.errorBody()!!.string()}

                        """.trimIndent()
                    val errorResult: ProfileResult<*> = ReactiveMyMinter.createProfileError<Any>(t)
                    out += """
                        ${errorResult.getError().message}
                        ${errorResult.getError().message}
                        ${ExceptionHelper.getStackTrace(t)}
                        """.trimIndent()
                    mText = out
                } catch (e: IOException) {
                    e.printStackTrace()
                    mText = """
                        ${t.message()}
                        ${ExceptionHelper.getStackTrace(t)}
                        """.trimIndent()
                }
            } else if (t is NetworkException) {
                val statusCode = t.statusCode
                if (statusCode in 500..999) {
                    setTitle(title.toString() + " (server error " + statusCode + ")")
                } else if (statusCode in 1..499) {
                    setTitle(title.toString() + " (client error " + statusCode + ")")
                } else {
                    setTitle(title.toString() + " (network error " + statusCode + ")")
                }
                mText = t.message
            } else {
                mText = """
                    ${t.message}
                    ${ExceptionHelper.getStackTrace(t)}
                    """.trimIndent()
            }
            return this
        }

        fun setTextTypeface(typeface: Typeface?): Builder {
            mTypeface = typeface
            return this
        }

        fun setTextAlignment(textAlignment: Int): Builder {
            mTextAlignment = textAlignment
            return this
        }
    }

}