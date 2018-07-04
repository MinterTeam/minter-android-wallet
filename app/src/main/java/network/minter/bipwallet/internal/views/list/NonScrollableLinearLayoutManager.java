package network.minter.bipwallet.internal.views.list;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;

/**
 * Minter. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class NonScrollableLinearLayoutManager extends LinearLayoutManager {
    public NonScrollableLinearLayoutManager(Context context) {
        super(context);
    }

    public NonScrollableLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public NonScrollableLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }
}
