/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.coins.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.coins.CoinsTabModule;
import network.minter.bipwallet.coins.views.CoinsTabPresenter;
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.bipwallet.tx.ui.TransactionListActivity;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class CoinsTabFragment extends HomeTabFragment implements CoinsTabModule.CoinsTabView {

    @Inject Provider<CoinsTabPresenter> presenterProvider;
    @InjectPresenter CoinsTabPresenter presenter;
    @BindView(R.id.userAvatar) BipCircleImageView avatar;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.balanceInt) TextView balanceInt;
    @BindView(R.id.balanceFract) TextView balanceFract;
    @BindView(R.id.balanceCoinName) TextView balanceCoinName;
    @BindView(R.id.list) RecyclerView list;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_coins, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setAvatar(String url) {
        if (url == null) {
            return;
        }

        avatar.setImageUrl(url);
    }

    @Override
    public void setUsername(CharSequence name) {
        username.setText(name);
    }

    @Override
    public void setBalance(long intPart, long fractionalPart, CharSequence coinName) {
        balanceInt.setText(String.valueOf(intPart));
        balanceFract.setText("." + String.valueOf(fractionalPart));
        balanceCoinName.setText(coinName);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);
//        list.setNestedScrollingEnabled(false);
    }

    @Override
    public void setOnAvatarClick(View.OnClickListener listener) {
        avatar.setOnClickListener(listener);
    }

    @Override
    public void startTransactionList() {
        startActivity(new Intent(getActivity(), TransactionListActivity.class));
    }

    @Override
    public void hideAvatar() {
        avatar.setVisibility(View.GONE);
    }

    @Override
    public void startConvertCoins() {
        getActivity().startActivity(new Intent(getActivity(), ConvertCoinActivity.class));
    }

    @ProvidePresenter
    CoinsTabPresenter providePresenter() {
        return presenterProvider.get();
    }
}
