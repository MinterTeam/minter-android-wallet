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

package network.minter.bipwallet.internal.views.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.airbnb.paris.Paris;
import com.airbnb.paris.annotations.Attr;
import com.airbnb.paris.annotations.Styleable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.helpers.ViewHelper;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Styleable("ToolbarProgress")
public class ToolbarProgress extends Toolbar {
    @BindView(R.id.tpAction) View action;
    @BindView(R.id.tpProgress) ProgressBar progress;
    @BindView(R.id.tpTitle) TextView title;
    private boolean mEnableAction = true;

    public ToolbarProgress(Context context) {
        super(context);
    }

    public ToolbarProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ToolbarProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        inflate(getContext(), R.layout.view_toolbar_with_progress, this);
        ButterKnife.bind(this);
        Paris.style(this).apply(attrs);
    }

    @Attr(R.styleable.ToolbarProgress_navigationIcon)
    public void setNavigationIcon(@DrawableRes int navigationIcon) {
        super.setNavigationIcon(navigationIcon);
    }

    @Attr(R.styleable.ToolbarProgress_navigationIcon)
    public void setNavigationIcon(Drawable navigationIcon) {
        super.setNavigationIcon(navigationIcon);
    }

    @Attr(R.styleable.ToolbarProgress_enableAction)
    public void setEnableAction(boolean enableAction) {
        mEnableAction = enableAction;
        ViewHelper.visible(action, mEnableAction);
    }

    @Attr(R.styleable.ToolbarProgress_progressColor)
    public void setProgressColor(int color) {
        progress.getIndeterminateDrawable().setColorFilter(
                color,
                android.graphics.PorterDuff.Mode.SRC_IN);
    }

    @Attr(R.styleable.ToolbarProgress_title)
    public void setTitleText(String titleText) {
        title.setText(titleText);
    }

    @Attr(R.styleable.ToolbarProgress_titleTextColor)
    public void setTitleTextColor(int color) {
        super.setTitleTextColor(color);
    }

    public void showProgress() {
        post(() -> {
            if (mEnableAction) {
                action.setVisibility(GONE);
                action.setEnabled(false);
            }
            progress.setVisibility(VISIBLE);
        });
    }

    public void hideProgress() {
        post(() -> {
            progress.setVisibility(GONE);
            if (mEnableAction) {
                action.setVisibility(VISIBLE);
                action.setEnabled(true);
            }
        });
    }

    public void setOnActionClickListener(OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void setTitle(int resId) {
        if (title != null) {
            title.setText(getContext().getString(resId));
        } else {
            super.setTitle(resId);
        }

    }

    @Override
    public void setTitle(CharSequence title) {
        if (this.title != null) {
            this.title.setText(title);
        } else {
            super.setTitle(title);
        }
    }

    @Override
    public void setTitleTextAppearance(Context context, int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && title != null) {
            title.setTextAppearance(resId);
        }
    }

    public View getAction() {
        return action;
    }
}
