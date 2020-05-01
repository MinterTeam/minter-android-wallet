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
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.paris.annotations.Attr;
import com.airbnb.paris.annotations.Styleable;

import java.math.BigDecimal;
import java.util.List;

import androidx.annotation.ColorInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.Paris;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.common.DeferredCall;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLT;

@Styleable("WalletSelector")
public class WalletSelector extends FrameLayout {
    @BindView(R.id.icon) TextView weight;
    @BindView(R.id.title) TextView name;
    @BindView(R.id.dropdown) ImageView dropdown;

    private WalletListAdapter mAdapter;
    private WalletsPopupWindow mPopup;
    private WalletListAdapter.OnClickWalletListener mOnClickWallet;
    private WalletListAdapter.OnClickAddWalletListener mOnClickAddWallet;
    private WalletListAdapter.OnClickEditWalletListener mOnClickEditWallet;
    private DeferredCall<WalletsPopupWindow> mPopupDefer = DeferredCall.create();

    public enum WalletWeight {
        Shrimp("\uD83E\uDD90"),
        Shell("\uD83D\uDC1A"),
        Crab("\uD83E\uDD80"),
        TropicalFish("\uD83D\uDC20"),
        Shark("\uD83E\uDD88"),
        Whale("\uD83D\uDC0B"),
        Dolphin("\uD83D\uDC2C");
        private String mEmoji;

        WalletWeight(String emoji) {
            mEmoji = emoji;
        }

        public static WalletWeight detect(BigDecimal balance) {
            if (bdLT(balance, new BigDecimal("1000"))) {
                return Shrimp;
            } else if (bdLT(balance, new BigDecimal("10000"))) {
                return Shell;
            } else if (bdLT(balance, new BigDecimal("100000"))) {
                return Crab;
            } else if (bdLT(balance, new BigDecimal("1000000"))) {
                return TropicalFish;
            } else if (bdLT(balance, new BigDecimal("10000000"))) {
                return Shark;
            } else if (bdLT(balance, new BigDecimal("100000000"))) {
                return Whale;
            } else {
                return Dolphin;
            }
        }

        public String getEmoji() {
            return mEmoji;
        }
    }

    public WalletSelector(Context context) {
        super(context);
    }

    public WalletSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public WalletSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    public WalletSelector(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mAdapter = new WalletListAdapter();
        mAdapter.setOnClickWalletListener(this::onClickWallet);
        mAdapter.setOnClickEditWalletListener(this::onClickEditWallet);
        mAdapter.setOnClickAddWalletListener(this::onClickAddWallet);

        View view = inflate(getContext(), R.layout.view_wallet_selector, this);
        ButterKnife.bind(this, view);

        view.setOnClickListener(this::openPopup);
        Paris.style(this).apply(attrs);
    }

    public void setOnClickWalletListener(WalletListAdapter.OnClickWalletListener listener) {
        mOnClickWallet = listener;
    }

    public void setOnClickAddWalletListener(WalletListAdapter.OnClickAddWalletListener listener) {
        mOnClickAddWallet = listener;
    }

    public void setOnClickEditWalletListener(WalletListAdapter.OnClickEditWalletListener listener) {
        mOnClickEditWallet = listener;
    }

    public void setMainWallet(WalletItem wallet) {
        name.setText(firstNonNull(wallet.getTitle(), wallet.getAddressShort()));
        weight.setText(wallet.getWeight().getEmoji());
    }

    @Override
    public void clearFocus() {
        super.clearFocus();
    }

    @Attr(value = R.styleable.WalletSelector_ws_name_color)
    public void setNameColor(@ColorInt int color) {
        name.setTextColor(color);
    }

    @Attr(value = R.styleable.WalletSelector_ws_dropdown_tint)
    public void setDropdownTint(@ColorInt int color) {
        dropdown.setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
    }

    public void showPopupProgress(boolean show) {
        mPopupDefer.call(t -> {
            if (show) {
                t.getProgressBar().setVisibility(VISIBLE);
                t.getList().setClickable(false);
                t.getList().setAlpha(0.3f);
            } else {
                t.getProgressBar().setVisibility(GONE);
                t.getList().setAlpha(1.0f);
                t.getList().setClickable(true);
            }

        });
    }

    public void openPopup(View view) {
        mPopup = WalletsPopupWindow.create(this, mAdapter);
        mPopupDefer.attach(mPopup);
    }

    public void setWallets(List<WalletItem> addresses) {
        post(() -> {
            mAdapter.setWallets(addresses);
            mAdapter.notifyDataSetChanged();
        });
    }

    private void dismissPopup() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopupDefer.detach();
            mPopup.dismiss();
            mPopup = null;
        }
    }

    private void onClickAddWallet() {
        dismissPopup();
        if (mOnClickAddWallet != null) {
            mOnClickAddWallet.onClickAddWallet();
        }
    }

    private void onClickEditWallet(WalletItem addressBalance) {
        dismissPopup();
        if (mOnClickEditWallet != null) {
            mOnClickEditWallet.onClickEdit(addressBalance);
        }
    }

    private void onClickWallet(WalletItem addressBalance) {
        dismissPopup();
        setMainWallet(addressBalance);
        if (mOnClickWallet != null) {
            mOnClickWallet.onClickWallet(addressBalance);
        }
    }
}
