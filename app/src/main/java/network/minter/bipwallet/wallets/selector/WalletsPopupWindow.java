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

package network.minter.bipwallet.wallets.selector;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import network.minter.bipwallet.R;
import network.minter.bipwallet.databinding.PopupWalletSelectorBinding;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.views.widgets.ColoredProgressBar;

public class WalletsPopupWindow extends PopupWindow {

    public WalletsPopupWindow(Context context) {
        super(context);
    }

    public WalletsPopupWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WalletsPopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WalletsPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public WalletsPopupWindow() {
    }

    public WalletsPopupWindow(View contentView) {
        super(contentView);
    }

    public WalletsPopupWindow(int width, int height) {
        super(width, height);
    }

    public WalletsPopupWindow(View contentView, int width, int height) {
        super(contentView, width, height);
    }

    public WalletsPopupWindow(View contentView, int width, int height, boolean focusable) {
        super(contentView, width, height, focusable);
    }

    private PopupWalletSelectorBinding binding;

    public static WalletsPopupWindow create(ViewGroup parent, WalletListAdapter adapter) {
        WalletsPopupWindow win = new WalletsPopupWindow(parent.getContext());
        win.binding = PopupWalletSelectorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        win.getList().setLayoutManager(new LinearLayoutManager(parent.getContext()));
        win.getList().setAdapter(adapter);


        // Creating the PopupWindow
        win.setContentView(win.binding.getRoot());
        win.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        win.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        win.setFocusable(true);
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        win.setElevation(Wallet.app().display().getDimen(R.dimen.card_elevation));
        win.setAnimationStyle(R.style.Wallet_PopupAnimation);

        // Some offset to align the popup a bit to the left, and a bit down, relative to button's position.

        int OFFSET_X = (int) Wallet.app().display().getDimen(R.dimen.margin_edges_half);
//        int OFFSET_X = 0;
        int OFFSET_Y = 0;

        // Clear the default translucent background
//        win.setBackgroundDrawable(parent.getContext().getResources().getDrawable(R.drawable.bg_white_rounded));

        // Displaying the popup at the specified location, + offsets.
//        win.showAtLocation(layout, Gravity.NO_GRAVITY, p.x + OFFSET_X, p.y + OFFSET_Y);
        win.showAtLocation(win.binding.getRoot(), Gravity.NO_GRAVITY, OFFSET_X, OFFSET_Y);

        return win;
    }

    public ColoredProgressBar getProgressBar() {
        return binding.progress;
    }

    public RecyclerView getList() {
        return binding.list;
    }
}
