package network.minter.bipwallet.internal.helpers;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.common.annotations.Dp;
import timber.log.Timber;

import static network.minter.bipwallet.internal.Wallet.app;

/**
 * Dogsy. 2018
 *
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
}
