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

package network.minter.bipwallet.internal.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.common.annotations.Dp;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import timber.log.Timber;

import static android.content.Context.CLIPBOARD_SERVICE;
import static network.minter.bipwallet.internal.Wallet.app;

/**
 * Dogsy. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public final class ViewHelper {


    public static void visible(@Nullable View view, boolean cond) {
        if (view == null) {
            return;
        }
        view.setVisibility(cond ? View.VISIBLE : View.GONE);
    }

    public static void visible(@Nullable TextView tv, CharSequence text) {
        if (tv == null) {
            return;
        }

        if (text == null || text.equals("")) {
            tv.setVisibility(View.GONE);
            tv.setText(null);
        } else {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        }
    }

    public static void switchView(View firstTrue, View secondFalse, boolean cond) {
        if (cond) {
            firstTrue.setVisibility(View.VISIBLE);
            secondFalse.setVisibility(View.GONE);
        } else {
            firstTrue.setVisibility(View.GONE);
            secondFalse.setVisibility(View.VISIBLE);
        }
    }

    public static void setTypeface(TextView textView, @StringRes int resId) {
        final Context context = textView.getContext();
        final Typeface tf = Typeface.create(context.getString(resId), Typeface.NORMAL);
        textView.setTypeface(tf);
    }

    public static void setMarginTop(TextView textView, @DimenRes int dimenRes) {
        final Context context = textView.getContext();
        ViewGroup.MarginLayoutParams lp = ((ViewGroup.MarginLayoutParams) textView.getLayoutParams());
        lp.topMargin = ((int) context.getResources().getDimension(dimenRes));
        textView.setLayoutParams(lp);
    }

    public static void setMarginTop(TextView textView, @Dp float dps) {
        ViewGroup.MarginLayoutParams lp = ((ViewGroup.MarginLayoutParams) textView.getLayoutParams());
        if (dps == 0) {
            lp.topMargin = 0;
        } else {
            lp.topMargin = app().display().dpToPx(dps);
        }

        textView.setLayoutParams(lp);
    }

    public static void setButtonBackground(Button button, @ColorRes int colorRes) {
        int color = app().res().getColor(colorRes);
        if ((color >> 24) == 0) {
            button.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        button.getBackground()
                .setColorFilter(
                        new LightingColorFilter(color, 0x00000000)
                );
        button.invalidate();
    }

    public static void setButtonBackgroundColor(Button button, int color) {
        button.getBackground()
                .setColorFilter(
                        new LightingColorFilter(color, 0x00000000)
                );
        button.invalidate();
    }

    public static void setButtonStyle(Button button, boolean enabled, @StyleRes int enabledStyle) {
        if (enabled) {
            setButtonStyle(button, enabledStyle);
        } else {
//            setButtonStyle(button, R.style.Dogsy_Button_Disabled);
        }
    }

    public static void setButtonStyle(Button button, @StyleRes int styleRes) {
        Context context = button.getContext();

        int attrColorButton = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? android.R.attr.colorButtonNormal : R.attr.colorButtonNormal;
        TypedArray attrs = context.obtainStyledAttributes(styleRes, new int[]{
                android.R.attr.textColor,
                attrColorButton,
        });

        int textColor = attrs.getColor(0, context.getResources().getColor(R.color.textColorPrimary));

        button.setTextColor(textColor);
        if (attrs.hasValue(1)) {
            setButtonBackgroundColor(button, attrs.getColor(1, Color.GRAY));
        }


        attrs.recycle();
    }

    public static void setHeight(View view, @Dp float dps) {
        setHeight(view, app().display().dpToPx(dps));
    }

    public static void setHeight(View view, @Px int px) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = px;
        view.setLayoutParams(lp);
    }

    public static void setViewMarginsDP(View v, int left, int top, int right, int bottom) {
        DisplayMetrics metrics = v.getContext().getResources().getDisplayMetrics();
        int density = (int) metrics.density;
        density += 0.5F;

        setViewMargins(v, left * density, top * density, right * density, bottom * density);
    }

    public static void setViewMargins(View v, int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams params;

        if (v.getParent() instanceof LinearLayout) {
            params = (LinearLayout.LayoutParams) v.getLayoutParams();
        } else if (v.getParent() instanceof RelativeLayout) {
            params = (RelativeLayout.LayoutParams) v.getLayoutParams();
        } else {
            params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        }

        if (params == null) {
            Timber.w("Cannot set layout margins. Layout params is null.");
            return;
        }

        params.setMargins(left, top, right, bottom); //substitute parameters for left, top, right, bottom
        v.setLayoutParams(params);
        v.requestLayout();
    }

    public static View[] getChildren(@Nullable final ViewGroup viewGroup) {
        if (viewGroup == null || viewGroup.getChildCount() == 0) {
            return new View[0];
        }

        final int cnt = viewGroup.getChildCount();
        final View[] children = new View[viewGroup.getChildCount()];
        for (int i = 0; i < cnt; i++) {
            children[i] = viewGroup.getChildAt(i);
        }

        return children;
    }

    public static void setButtonEnabledElevation(View button, boolean enabled) {
        float elevation = Wallet.app().display().dpToPx(4f);
        ViewCompat.setElevation(button, enabled ? elevation : 0f);
    }

    public static void setSelectableItemBackground(View view) {
        TypedValue outValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
    }

    public static int systemBarsLightness(boolean light) {
        int flags = 0;

        if (light && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }

        return flags;
    }

    public static void setStatusBarColorAnimate(Fragment fragment, @ColorInt int color) {
        if (fragment == null || fragment.getActivity() == null) {
            return;
        }

        final int fromColor = fragment.getActivity().getWindow().getStatusBarColor();
        if (fromColor == color) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int c = MathHelper.blendColors(fromColor, color, (float) animation.getAnimatedValue());
            fragment.getActivity().getWindow().setStatusBarColor(c);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                fragment.getActivity().getWindow().setStatusBarColor(color);
            }
        });
        animator.start();
    }


    public static void setStatusBarColor(Fragment fragment, @ColorInt int color) {
        if (fragment == null || fragment.getActivity() == null) return;
        setStatusBarColor(fragment.getActivity(), color);
    }

    public static void setStatusBarColor(Activity activity, @ColorInt int color) {
        if (activity == null) return;
        if (activity.getWindow().getStatusBarColor() == color) return;
        activity.getWindow().setStatusBarColor(color);
    }

    public static void setSystemBarsLightness(Fragment fragment, boolean light) {
        if (fragment == null || fragment.getActivity() == null) return;
        setSystemBarsLightness(fragment.getActivity(), light);
    }

    public static void setSystemBarsLightness(Activity activity, boolean light) {
        if (activity == null) return;
        activity.getWindow().getDecorView().setSystemUiVisibility(systemBarsLightness(light));
    }

    public static void setSystemBarsLightness(Dialog dialog, boolean light) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().getDecorView().setSystemUiVisibility(systemBarsLightness(light));
    }

    public static void setOnImeActionListener(EditText input, int action, View.OnClickListener listener) {
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == action && listener != null) {
                    listener.onClick(v);
                    return true;
                }
                return false;
            }
        });
    }

    public static void tryToPasteMinterAddressFromCB(View view, EditText input) {
        if (view.getContext() == null) return;

        Pattern pattern = Pattern.compile(String.format("%s|%s", MinterAddress.ADDRESS_PATTERN, MinterPublicKey.PUB_KEY_PATTERN), Pattern.CASE_INSENSITIVE);

        ClipboardManager cm = (ClipboardManager) view.getContext().getSystemService(CLIPBOARD_SERVICE);
        if (cm == null) {
            return;
        }

        ClipData cd = cm.getPrimaryClip();
        if (cd == null) return;

        for (int i = 0; i < cd.getItemCount(); i++) {
            ClipData.Item item = cd.getItemAt(i);
            CharSequence raw = item.getText();
            Matcher matcher = pattern.matcher(raw.toString());
            if (matcher.find()) {
                String val = matcher.group();
                input.setText(val);
                input.setSelection(input.length());
                break;
            }

        }
    }
}
