package network.minter.bipwallet.internal.views.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import io.sweers.barber.Barber;
import io.sweers.barber.Kind;
import io.sweers.barber.StyledAttr;
import network.minter.bipwallet.R;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ColoredProgressBar extends ProgressBar {

    @StyledAttr(value = R.styleable.ColoredProgressBar_color, kind = Kind.COLOR)
    int color;

    public ColoredProgressBar(Context context) {
        super(context);
    }

    public ColoredProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);

    }

    public ColoredProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public ColoredProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        Barber.style(this, attrs, R.styleable.ColoredProgressBar, defStyleAttr, defStyleRes);

        getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}
