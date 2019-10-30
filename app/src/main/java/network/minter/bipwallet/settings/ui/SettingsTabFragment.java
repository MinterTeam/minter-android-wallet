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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addresses.ui.AddressListActivity;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.views.SnackbarBuilder;
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator;
import network.minter.bipwallet.internal.views.list.NonScrollableLinearLayoutManager;
import network.minter.bipwallet.internal.views.utils.SingleCallHandler;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.ui.PinEnterActivity;
import network.minter.bipwallet.settings.contract.SettingsTabView;
import network.minter.bipwallet.settings.views.SettingsTabPresenter;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SettingsTabFragment extends HomeTabFragment implements SettingsTabView {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.list_main) RecyclerView listMain;
    @BindView(R.id.list_additional) RecyclerView listAdditional;
    @BindView(R.id.list_security) RecyclerView listSecurity;
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
    public void setSecurityAdapter(RecyclerView.Adapter<?> securityAdapter) {
        listSecurity.setLayoutManager(new NonScrollableLinearLayoutManager(getActivity()));
        listSecurity.addItemDecoration(new BorderedItemSeparator(getActivity(), R.drawable.shape_bottom_separator, true, true));
        listSecurity.setAdapter(securityAdapter);
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
    public void startPinCodeManager(int requestCode, SecurityModule.PinMode mode) {
        SingleCallHandler.call("pin-manager",
                () -> new PinEnterActivity.Builder(getActivity(), mode)
                        .start(requestCode)
        );
    }

    @Override
    public void startBiometricPrompt(BiometricPrompt.AuthenticationCallback callback) {
        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.pin_fp_title_enable))
                .setDescription("")
                .setSubtitle("")
                .setNegativeButtonText(getString(R.string.btn_cancel))
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        final BiometricPrompt prompt = new BiometricPrompt(getActivity(), executor, callback);

        prompt.authenticate(info);
    }

    @Override
    public void startFingerprintEnrollment() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            intent = new Intent(Settings.ACTION_FINGERPRINT_ENROLL);
        } else {
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException wtf) {
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            startActivity(intent);
        } catch (SecurityException wtf2) {
            try {
                intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                startActivity(intent);
            } catch (Throwable wtf3) {
                startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, R.string.fingerprint_dialog_enroll_title)
                        .setText(R.string.fingerprint_dialog_enroll_text_fix)
                        .setPositiveAction(R.string.btn_ok, (d, w) -> {
                            d.dismiss();
                        })
                        .create());
            }
        }
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

        getActivity().startActivity(intent);
        getActivity().finish();
    }

    @ProvidePresenter
    SettingsTabPresenter providePresenter() {
        return presenterProvider.get();
    }
}
