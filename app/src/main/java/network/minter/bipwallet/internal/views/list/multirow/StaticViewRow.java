package network.minter.bipwallet.internal.views.list.multirow;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class StaticViewRow implements MultiRowContract.Row<StaticViewRow.ViewHolder> {
    private int mLayoutId;
    private boolean mVisible = true;

    public StaticViewRow(@LayoutRes int layoutId) {
        mLayoutId = layoutId;
    }

    public StaticViewRow(@LayoutRes int layoutId, boolean visible) {
        this(layoutId);
        mVisible = visible;
    }

    @Override
    public int getItemView() {
        return mLayoutId;
    }

    @Override
    public int getRowPosition() {
        return 0;
    }

    @Override
    public boolean isVisible() {
        return mVisible;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder) {
    }

    @Override
    public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {

    }

    @NonNull
    @Override
    public Class<ViewHolder> getViewHolderClass() {
        return ViewHolder.class;
    }

    public static class ViewHolder extends MultiRowAdapter.RowViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
