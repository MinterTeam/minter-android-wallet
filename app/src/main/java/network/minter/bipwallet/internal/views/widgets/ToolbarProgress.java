/*
 * Copyright (C) by MinterTeam. 2018
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
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.sweers.barber.Barber;
import io.sweers.barber.Kind;
import io.sweers.barber.StyledAttr;
import network.minter.bipwallet.R;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ToolbarProgress extends Toolbar {

    @StyledAttr(R.styleable.ToolbarProgress_title)
    String titleText;
    @StyledAttr(value = R.styleable.ToolbarProgress_titleTextColor, kind = Kind.COLOR)
    int titleTextColor;
    @StyledAttr(value = R.styleable.ToolbarProgress_navigationIcon, kind = Kind.RES_ID)
    int navigationIcon;
    @StyledAttr(R.styleable.ToolbarProgress_enableAction)
    boolean enableAction = true;
    @StyledAttr(value = R.styleable.ToolbarProgress_progressColor, kind = Kind.COLOR)
    int progressColor;

    @BindView(R.id.tpAction) View action;
    @BindView(R.id.tpProgress) ProgressBar progress;
    @BindView(R.id.tpTitle) TextView title;

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
        Barber.style(this, attrs, R.styleable.ToolbarProgress, defStyleAttr);

        title.setText(titleText);

        if (!enableAction) {
            action.setVisibility(GONE);
        }

        if (navigationIcon != 0) {
            setNavigationIcon(navigationIcon);
        }

        progress.getIndeterminateDrawable().setColorFilter(
                progressColor,
                android.graphics.PorterDuff.Mode.SRC_IN);

        setTitleTextColor(titleTextColor);

    }

    public void showProgress() {
        post(() -> {
            if (enableAction) {
                action.setVisibility(GONE);
                action.setEnabled(false);
            }
            progress.setVisibility(VISIBLE);
        });
    }

    public void hideProgress() {
        post(() -> {
            progress.setVisibility(GONE);
            if (enableAction) {
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
