/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.internal.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class WalletDialogBuilder<D extends WalletDialog, B extends WalletDialogBuilder> {
    protected final Context mContext;
    protected CharSequence mTitle;

    protected CharSequence mPositiveTitle, mNegativeTitle, mNeutralTitle;
    protected Dialog.OnClickListener mPositiveListener, mNegativeListener, mNeutralListener;

    public WalletDialogBuilder(Context context) {
        mContext = context;
    }

    public WalletDialogBuilder(Context context, CharSequence title) {
        mContext = context;
        setTitle(title);
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public WalletDialogBuilder setTitle(CharSequence title) {
        mTitle = title;
        return this;
    }

    public abstract D create();

    public CharSequence getPositiveTitle() {
        return mPositiveTitle;
    }

    public CharSequence getNegativeTitle() {
        return mNegativeTitle;
    }

    public CharSequence getNeutralTitle() {
        return mNeutralTitle;
    }

    public Dialog.OnClickListener getPositiveListener() {
        return mPositiveListener;
    }

    public boolean hasPositiveListener() {
        return mPositiveListener != null;
    }

    public boolean hasNegativeListener() {
        return mNegativeListener != null;
    }

    public boolean hasNeutralListener() {
        return mNeutralListener != null;
    }

    public Dialog.OnClickListener getNegativeListener() {
        return mNegativeListener;
    }

    public Dialog.OnClickListener getNeutralListener() {
        return mNeutralListener;
    }

    protected B setAction(int whichBtn, CharSequence title, Dialog.OnClickListener listener) {
        switch (whichBtn) {
            case DialogInterface.BUTTON_POSITIVE:
                mPositiveTitle = title;
                mPositiveListener = listener;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                mNegativeTitle = title;
                mNegativeListener = listener;
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                mNeutralTitle = title;
                mNeutralListener = listener;
                break;
        }

        return (B) this;
    }

    protected CharSequence getActionTitle(int whichBtn) {
        switch (whichBtn) {
            case DialogInterface.BUTTON_POSITIVE:
                return mPositiveTitle;
            case DialogInterface.BUTTON_NEGATIVE:
                return mNegativeTitle;
            case DialogInterface.BUTTON_NEUTRAL:
                return mNeutralTitle;
        }

        return null;
    }

    protected Dialog.OnClickListener getActionListener(int whichBtn) {
        switch (whichBtn) {
            case DialogInterface.BUTTON_POSITIVE:
                return mPositiveListener;
            case DialogInterface.BUTTON_NEGATIVE:
                return mNegativeListener;
            case DialogInterface.BUTTON_NEUTRAL:
                return mNeutralListener;
        }

        return null;
    }

}
