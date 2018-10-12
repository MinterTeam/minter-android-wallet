/*
 * Copyright (C) by MinterTeam. 2018
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

package network.minter.bipwallet.settings.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addresses.ui.AddressListActivity;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.views.SnackbarBuilder;
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator;
import network.minter.bipwallet.internal.views.list.NonScrollableLinearLayoutManager;
import network.minter.bipwallet.settings.SettingsTabModule;
import network.minter.bipwallet.settings.views.SettingsTabPresenter;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SettingsTabFragment extends HomeTabFragment implements SettingsTabModule.SettingsTabView {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.list_main) RecyclerView listMain;
    @BindView(R.id.list_additional) RecyclerView listAdditional;
    @BindView(R.id.free_get) Button freeGet;

    @Inject Provider<SettingsTabPresenter> presenterProvider;
    @InjectPresenter SettingsTabPresenter presenter;

    private WalletDialog mDialog;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (presenter != null) {
            presenter.onLowMemory();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_settings, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);

        getActivity().getMenuInflater().inflate(R.menu.menu_tab_settings, toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        return view;
    }

    @Override
    public void setMainAdapter(RecyclerView.Adapter<?> mainAdapter) {
        listMain.setLayoutManager(new NonScrollableLinearLayoutManager(getActivity()));
        listMain.addItemDecoration(new BorderedItemSeparator(getActivity(), R.drawable.shape_bottom_separator, true, true));
        listMain.setAdapter(mainAdapter);
    }

    @Override
    public void setAdditionalAdapter(RecyclerView.Adapter<?> additionalAdapter) {
        listAdditional.setLayoutManager(new NonScrollableLinearLayoutManager(getActivity()));
        listAdditional.addItemDecoration(new BorderedItemSeparator(getActivity(), R.drawable.shape_bottom_separator, true, true));
        listAdditional.setAdapter(additionalAdapter);
    }

    @Override
    public void startAddressList() {
        getActivity().startActivity(new Intent(getActivity(), AddressListActivity.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WalletDialog.dismissInstance(mDialog);
    }

    @Override
    public void startAvatarChooser() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .setAutoZoomEnabled(true)
                .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                .setOutputCompressQuality(100)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setActivityMenuIconColor(getResources().getColor(R.color.white))
                .start(getActivity());
    }

    @Override
    public void startPasswordChange() {
        startActivity(new Intent(getActivity(), PasswordChangeMigrationActivity.class));
    }

    @Override
    public void showMessage(CharSequence message) {
        new SnackbarBuilder(this)
                .setMessage(message)
                .setDurationShort()
                .show();
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mDialog = WalletDialog.switchDialogWithExecutor(this, mDialog, executor);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            presenter.onLogout();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setOnFreeCoinsClickListener(View.OnClickListener listener) {
        freeGet.setOnClickListener(listener);
    }

    @Override
    public void showFreeCoinsButton(boolean show) {
        freeGet.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void startLogin() {
        Wallet.app().cache().clear();
        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Wallet.app().session().logout();
        Wallet.app().secretStorage().destroy();
        Wallet.app().storage().deleteAll();

        getActivity().startActivity(intent);
        getActivity().finish();
    }

    @ProvidePresenter
    SettingsTabPresenter providePresenter() {
        return presenterProvider.get();
    }
}
