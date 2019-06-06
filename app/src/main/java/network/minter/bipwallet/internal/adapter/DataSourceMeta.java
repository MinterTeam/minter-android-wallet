package network.minter.bipwallet.internal.adapter;

import java.util.ArrayList;
import java.util.List;

import network.minter.explorer.models.ExpResult;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
public class DataSourceMeta<T> {
    private List<T> items = new ArrayList<>();
    private ExpResult.Meta meta;

    public ExpResult.Meta getMeta() {
        return meta;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public void setMeta(ExpResult.Meta meta) {
        this.meta = meta;
    }
}
