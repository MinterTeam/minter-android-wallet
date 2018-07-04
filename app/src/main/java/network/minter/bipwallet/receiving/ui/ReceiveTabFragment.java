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

package network.minter.bipwallet.receiving.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import network.minter.bipwallet.R;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.receiving.ReceiveTabModule.ReceiveTabView;
import network.minter.bipwallet.receiving.views.ReceiveTabPresenter;
import network.minter.mintercore.crypto.MinterAddress;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ReceiveTabFragment extends HomeTabFragment implements ReceiveTabView {
    @Inject Provider<ReceiveTabPresenter> presenterProvider;
    @InjectPresenter ReceiveTabPresenter presenter;
    @BindView(R.id.address) TextView address;
    @BindView(R.id.qr) ImageView qrImage;
    @BindView(R.id.action_share) Button actionShare;
    private Unbinder mUnbinder;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_receive, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setQRCode(Bitmap bmp) {
        qrImage.setImageBitmap(bmp);
    }

    @Override
    public void setAddress(MinterAddress address) {
        this.address.setText(address.toString());
    }

    @Override
    public void setOnActionShareQR(View.OnClickListener listener) {
        actionShare.setOnClickListener(listener);
    }

    @Override
    public void setOnActionQR(View.OnClickListener listener) {
        qrImage.setOnClickListener(listener);
    }

    @Override
    public void startQRPreview(View shared, String filePath) {
        new QRPreviewActivity.Builder(getActivity(), filePath)
                .addSharedView(shared)
                .start();
    }

    @Override
    public void showQRProgress() {

    }

    @Override
    public void hideQRProgress() {

    }

    @Override
    public void startShare(Intent intent) {
        getActivity().startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @ProvidePresenter
    ReceiveTabPresenter providePresenter() {
        return presenterProvider.get();
    }
}
