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

package network.minter.bipwallet.settings.views.rows;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract;

public class TitleRow implements MultiRowContract.Row<TitleRow.ViewHolder> {
    private CharSequence mTitle;

    public TitleRow(CharSequence title) {
        mTitle = title;
    }

    @Override
    public int getItemView() {
        return R.layout.item_list_settings_header;
    }

    @Override
    public int getRowPosition() {
        return 0;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder) {
        viewHolder.title.setText(mTitle);
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
        @BindView(R.id.title) TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
