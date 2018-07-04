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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import butterknife.BindView;
import network.minter.bipwallet.R;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class WalletDialog extends Dialog {

    public @BindView(R.id.title) TextView title;

    protected WalletDialog(@NonNull Context context) {
        super(context, R.style.Wallet_Dialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getWindow() != null) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            getWindow().setAttributes(params);

            getWindow().setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public static <T extends WalletDialog> T switchDialogWithExecutor(Fragment fragment, T dialog, DialogExecutor executor) {
        return switchDialogWithExecutor(fragment.getActivity(), dialog, executor);
    }

    public static <T extends WalletDialog> T switchDialogWithExecutor(Activity activity, T dialog, DialogExecutor executor) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = (T) executor.run(activity);
        dialog.show();
        return dialog;
    }

    public interface DialogExecutor {
        WalletDialog run(Context ctx);
    }

    public interface WithPositiveAction<T> {
        T setPositiveAction(CharSequence title, Dialog.OnClickListener listener);
    }
}
