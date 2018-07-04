package network.minter.bipwallet.internal.views.list.multirow;

import android.support.annotation.NonNull;

/**
 * Stars. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SortableRow<V extends MultiRowAdapter.RowViewHolder, T extends MultiRowContract.Row<V>> implements
        MultiRowContract.Row<V> {

    private T row;
    private int position = 0;
    private int previousPosition = -1;

    public SortableRow(final T row, int position) {
        this.row = row;
        this.position = position;
    }

    public void setPosition(int position) {
        this.previousPosition = this.position;
        this.position = position;
    }

    public boolean isInserted() {
        return previousPosition == -1;
    }

    public T getRow() {
        return row;
    }

    @Override
    public int getItemView() {
        return row.getItemView();
    }

    @Override
    public int getRowPosition() {
        return position;
    }

    @Override
    public boolean isVisible() {
        return row.isVisible();
    }

    @Override
    public void onBindViewHolder(@NonNull V viewHolder) {
        row.onBindViewHolder(viewHolder);
    }

    @Override
    public void onUnbindViewHolder(@NonNull V viewHolder) {

    }

    @NonNull
    @Override
    public Class<V> getViewHolderClass() {
        return row.getViewHolderClass();
    }

    public int getPreviousPosition() {
        return previousPosition;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MultiRowContract.Row) {
            return ((MultiRowContract.Row) obj).getRowPosition() == getRowPosition()
                    && ((MultiRowContract.Row) obj).getItemView() == getItemView()
                    && ((MultiRowContract.Row) obj).isVisible() == isVisible()
                    && ((MultiRowContract.Row) obj).getViewHolderClass() == getViewHolderClass();
        }

        return row.equals(obj);
    }
}


