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

package network.minter.bipwallet.wallets.ui;

import android.view.View;

import com.google.android.material.appbar.AppBarLayout;

import java.lang.ref.WeakReference;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.helpers.MathHelper;

import static network.minter.bipwallet.internal.helpers.ViewHelper.setStatusBarColor;
import static network.minter.bipwallet.internal.helpers.ViewHelper.setStatusBarColorAnimate;
import static network.minter.bipwallet.internal.helpers.ViewHelper.setSystemBarsLightness;

public class WalletsTopRecolorHelper extends AppBarOffsetChangedListener {
    private final int mCollapsedStatusColor = 0xFFFFFFFF;
    private final int mCollapsedTextColor = 0xFF000000;
    private final int mCollapsedDropdownColor;
    private final float mElevation;
    private final int mExpandedToolbarIconsColor = 0xFF_FFFFFF;
    private final int mCollapsedToolbarIconsColor;
    private WeakReference<WalletsTabFragment> mRef;
    private int mStatusColor = -1;
    private boolean mEnableRecolor = true;
    private int mExpandedStatusColor;
    private boolean mLightStatus = false;
    private int mToolbarIconsColor = mCollapsedStatusColor;

    WalletsTopRecolorHelper(WalletsTabFragment fragment) {
        mRef = new WeakReference<>(fragment);

        setSystemBarsLightness(fragment, false);
        mExpandedStatusColor = fragment.getResources().getColor(R.color.colorPrimary);
        mCollapsedToolbarIconsColor = fragment.getResources().getColor(R.color.colorPrimaryLight);
        mStatusColor = mExpandedStatusColor;
        mCollapsedDropdownColor = fragment.getResources().getColor(R.color.grey);
        mElevation = fragment.getResources().getDimension(R.dimen.card_elevation);
    }

    @Override
    public void onStateChanged(AppBarLayout appbar, State state, int verticalOffset, float percent) {
        if (!mEnableRecolor || mRef.get() == null) {
            return;
        }

        final int textColor = MathHelper.blendColors(mCollapsedStatusColor, mCollapsedTextColor, 1.0f - percent);
        final int dropdownColor = MathHelper.blendColors(mCollapsedStatusColor, mCollapsedDropdownColor, 1.0f - percent);
        mStatusColor = MathHelper.blendColors(mCollapsedStatusColor, mExpandedStatusColor, percent);
        mLightStatus = percent < 0.5f;
        setSystemBarsLightness(mRef.get().getActivity(), mLightStatus);
        setStatusBarColor(mRef.get().getActivity(), mStatusColor);
        mRef.get().walletSelector.setNameColor(textColor);
        mRef.get().walletSelector.setDropdownTint(dropdownColor);

        if (mLightStatus) {
            if (mToolbarIconsColor != mExpandedStatusColor) {
                mToolbarIconsColor = mCollapsedToolbarIconsColor;
                for (int i = 0; i < mRef.get().toolbar.getMenu().size(); i++) {
                    mRef.get().toolbar.getMenu().getItem(i).getIcon().setColorFilter(mToolbarIconsColor, android.graphics.PorterDuff.Mode.MULTIPLY);
                }
            }
        } else {
            if (mToolbarIconsColor != mCollapsedStatusColor) {
                mToolbarIconsColor = mExpandedToolbarIconsColor;
                for (int i = 0; i < mRef.get().toolbar.getMenu().size(); i++) {
                    mRef.get().toolbar.getMenu().getItem(i).getIcon().setColorFilter(mToolbarIconsColor, android.graphics.PorterDuff.Mode.MULTIPLY);
                }
            }
        }

        if (percent < 1.0f && percent > 0.0f) {
            mRef.get().overlay.setVisibility(View.VISIBLE);
            final float overlayAlpha;
            if (percent <= 0.5f) {
                overlayAlpha = MathHelper.clamp((1.0f - percent) * 1.1f, 0.f, 1.f);
            } else {
                overlayAlpha = (1.0f - percent);
            }
            mRef.get().overlay.setAlpha(overlayAlpha);
        } else if (percent == 0) {
            mRef.get().overlay.setVisibility(View.VISIBLE);
        } else {
            mRef.get().overlay.setVisibility(View.GONE);
        }

        appbar.setElevation(mElevation * (1 - percent));
    }

    public void setEnableRecolor(boolean enable) {
        mEnableRecolor = enable;
    }

    public void setTabSelected() {
        if (mRef.get() == null) return;
        setSystemBarsLightness(mRef.get(), mLightStatus);
        setStatusBarColorAnimate(mRef.get(), mStatusColor);
    }
}
