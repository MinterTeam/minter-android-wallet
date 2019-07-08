package network.minter.bipwallet.internal.views.list;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;

/**
 * Minter. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class SimpleRecyclerViewHolder extends MultiRowAdapter.RowViewHolder {
    public RecyclerView list;

    public SimpleRecyclerViewHolder(View itemView) {
        super(itemView);
        list = ((RecyclerView) itemView);

    }
}
