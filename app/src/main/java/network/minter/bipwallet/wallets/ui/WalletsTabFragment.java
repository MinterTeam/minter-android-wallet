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

package network.minter.bipwallet.wallets.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.delegation.ui.DelegationListActivity;
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialog;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.SoundManager;
import network.minter.bipwallet.internal.views.utils.SingleCallHandler;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.bipwallet.sending.ui.SendTabFragment;
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import network.minter.bipwallet.wallets.contract.WalletsTabView;
import network.minter.bipwallet.wallets.dialogs.ui.AddWalletDialog;
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog;
import network.minter.bipwallet.wallets.dialogs.ui.EditWalletDialog;
import network.minter.bipwallet.wallets.selector.WalletItem;
import network.minter.bipwallet.wallets.selector.WalletListAdapter;
import network.minter.bipwallet.wallets.selector.WalletSelector;
import network.minter.bipwallet.wallets.views.WalletsTabPresenter;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@RuntimePermissions
public class WalletsTabFragment extends HomeTabFragment implements WalletsTabView {
    public final static int REQUEST_CODE_QR_SCAN_TX = 2002;

    @Inject Provider<WalletsTabPresenter> presenterProvider;
    @Inject SoundManager soundManager;
    @Inject SecretStorage secretStorage;
    @InjectPresenter WalletsTabPresenter presenter;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.balance_int) TextView balanceInt;
    @BindView(R.id.balance_fractions) TextView balanceFract;
    @BindView(R.id.balance_coin_name) TextView balanceCoinName;
    @BindView(R.id.balance_today) TextView balanceRewards;
    //    @BindView(R.id.container_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.delegated_balance) TextView delegationAmount;
    //    @BindView(R.id.balance_title) TextView balanceTitle;
    @BindView(R.id.wallet_selector) WalletSelector walletSelector;
    @BindView(R.id.tabs) TabLayout tabs;
    @BindView(R.id.tabsPager) ViewPager pager;
    @BindView(R.id.collapsing) CollapsingToolbarLayout collapsing;
    @BindView(R.id.appbar) AppBarLayout appbar;
    @BindView(R.id.delegated_layout) View delegatedLayout;
    @BindView(R.id.collapsing_content) View collapsingContent;
    @BindView(R.id.overlay) View overlay;
    //    @BindView(R.id.last_update_time) TextView lastUpdateText;

    private Unbinder mUnbinder;
    private SwipeRefreshHacker mSwipeRefreshHacker = new SwipeRefreshHacker();
    private WalletDialog mCurrentDialog = null;
    private BaseBottomSheetDialog mBottomDialog = null;
    @SuppressLint("ClickableViewAccessibility")

    private WalletsTopRecolorHelper mRecolorHelper;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
//        swipeRefreshLayout.setOnRefreshListener(listener);
//        mSwipeRefreshHacker.setOnRefreshStartListener(this::onStartRefresh);
//        mSwipeRefreshHacker.hack(swipeRefreshLayout);
    }

    @Override
    public void showRefreshProgress() {
//        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void hideRefreshProgress() {
//        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void startExplorer(String hash) {
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + hash)));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        presenter.onRestoreInstanceState(savedInstanceState);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_wallets, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        presenter.onRestoreInstanceState(savedInstanceState);
        mRecolorHelper = new WalletsTopRecolorHelper(this);

//        checkLastUpdate();
//        BroadcastReceiverManager bbm = new BroadcastReceiverManager(getActivity());
//        bbm.add(new RTMBlockReceiver(this::checkLastUpdate));
//        bbm.register();

        if (network.minter.bipwallet.BuildConfig.DEBUG) {
            toolbar.setOnLongClickListener(v -> {
                final StringBuilder sb = new StringBuilder();
                sb.append("    Env: ").append(BuildConfig.FLAVOR).append("\n");
                sb.append("  Build: ").append(BuildConfig.VERSION_CODE).append("\n");
                sb.append("Version: ").append(BuildConfig.VERSION_NAME).append("\n");
                sb.append("  URole: ").append(Wallet.app().session().getRole().name()).append("\n");

                new WalletConfirmDialog.Builder(getActivity(), "About")
                        .setText(sb.toString())
                        .setTextTypeface(Typeface.MONOSPACE)
                        .setTextIsSelectable(true)
                        .setPositiveAction("OK")
                        .create()
                        .show();
                return false;
            });
        }

        appbar.addOnOffsetChangedListener(mRecolorHelper);

        setHasOptionsMenu(true);
        getActivity().getMenuInflater().inflate(R.menu.menu_wallets_toolbar, toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        setupTabAdapter();

        return view;
    }


    @Override
    public void onTabUnselected() {
        super.onTabUnselected();
        mRecolorHelper.setEnableRecolor(false);
    }

    @Override
    public void onTabSelected() {
        super.onTabSelected();
        mRecolorHelper.setEnableRecolor(true);
        if (getActivity() == null) return;

        mRecolorHelper.setTabSelected();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_scan_tx) {
            SingleCallHandler.call(item, () -> startScanQRWithPermissions(REQUEST_CODE_QR_SCAN_TX));
        } else if (item.getItemId() == R.id.menu_share) {

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void startExternalTransaction(String rawData) {
        new ExternalTransactionActivity.Builder(getActivity(), rawData)
                .start();
    }

    @Override
    public void setOnClickScanQR(View.OnClickListener listener) {

    }

    @NeedsPermission(Manifest.permission.CAMERA)
    @Override
    public void startScanQR(int requestCode) {
        Intent i = new Intent(getActivity(), QRCodeScannerActivity.class);
        if (getActivity() == null) {
            return;
        }
        getActivity().startActivityForResult(i, requestCode);
    }

    @Override
    public void startWalletEdit(WalletItem walletItem, BaseBottomSheetDialog.OnSubmitListener onSubmitListener) {
        if (mBottomDialog != null) {
            mBottomDialog.dismiss();
            mBottomDialog = null;
        }

        if (getFragmentManager() == null) {
            Timber.w("Fragment manager is NULL");
            return;
        }

        mBottomDialog = EditWalletDialog.newInstance(walletItem);
        mBottomDialog.setOnSubmitListener(onSubmitListener);
        mBottomDialog.show(getFragmentManager(), "wallet_edit");
    }

    @Override
    public void startWalletAdd(BaseBottomSheetDialog.OnSubmitListener onSubmit, BaseBottomSheetDialog.OnDismissListener onDismiss) {
        if (mBottomDialog != null) {
            mBottomDialog.dismiss();
            mBottomDialog = null;
        }

        if (getFragmentManager() == null) {
            Timber.w("Fragment manager is NULL");
            return;
        }

        AddWalletDialog addWalletDialog = AddWalletDialog.newInstance();
        addWalletDialog.setOnSubmitListener(onSubmit);
        addWalletDialog.setOnDismissListener(onDismiss);

        addWalletDialog.setOnGenerateNewWalletListener((submitListener, dismissListener, title) -> {
            mBottomDialog.dismiss();
            mBottomDialog = new CreateWalletDialog.Builder()
                    .setEnableDescription(true)
                    .setEnableTitleInput(true)
                    .setWalletTitle(title)
                    .setOnSubmitListener(submitListener)
                    .setOnDismissListener(dismissListener)
                    .setEnableStartHomeOnSubmit(false)
                    .build();

            mBottomDialog.show(getFragmentManager(), "wallet_generate");
        });

        mBottomDialog = addWalletDialog;
        mBottomDialog.show(getFragmentManager(), "wallet_add");
    }

    @Override
    public void showSendAndSetAddress(String address) {
        runOnUiThread(() -> {
            try {
                ((HomeActivity) getActivity()).setCurrentPage(1);
                ((SendTabFragment) ((HomeActivity) getActivity()).getCurrentTabFragment()).setRecipient(new AddressContact(address));
            } catch (Throwable t) {
                Timber.w("Unable to scan address directly to send tab");
            }

        });
    }

    @Override
    public void setMainWallet(WalletItem mainWallet) {
        walletSelector.setMainWallet(mainWallet);
    }

    @Override
    public void setWallets(List<WalletItem> addresses) {
        walletSelector.setWallets(addresses);
    }

    @Override
    public void setOnClickWalletListener(WalletListAdapter.OnClickWalletListener listener) {
        walletSelector.setOnClickWalletListener(listener);
    }

    @Override
    public void setOnClickAddWalletListener(WalletListAdapter.OnClickAddWalletListener listener) {
        walletSelector.setOnClickAddWalletListener(listener);
    }

    @Override
    public void setOnClickEditWalletListener(WalletListAdapter.OnClickEditWalletListener listener) {
        walletSelector.setOnClickEditWalletListener(listener);
    }

    @Override
    public void setOnClickDelegated(View.OnClickListener listener) {
        delegatedLayout.setOnClickListener(listener);
    }

    @Override
    public void startScanQRWithPermissions(int requestCode) {
        WalletsTabFragmentPermissionsDispatcher.startScanQRWithPermissionCheck(this, requestCode);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        presenter.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @javax.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        delegationView.setOnClickListener(v -> startDelegationList());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        presenter.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        presenter.onLowMemory();
        Timber.d("OnLowMemory");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        Timber.d("Destroy");
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void setBalance(String intPart, String fractionalPart, CharSequence coinName) {
        runOnUiThread(() -> {
            if (balanceInt != null) {
                balanceInt.setText(String.valueOf(intPart));
                balanceFract.setText("." + fractionalPart);
                balanceCoinName.setText(coinName);
            }
        });
    }

    @Override
    public void setDelegationAmount(String amount) {
//        delegationView.setVisibility(View.VISIBLE);
        delegationAmount.setText(amount);
    }

    @Override
    public void setBalanceClickListener(View.OnClickListener listener) {
//        balanceContainer.setOnClickListener(listener);
    }

    @Override
    public void setBalanceTitle(int title) {
//        balanceTitle.setText(title);
    }

    @Override
    public void setBalanceRewards(String rewards) {
        balanceRewards.setText(rewards);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        presenter.onSaveInstanceState(outState);
    }

    @Override
    public void startTransactionList() {
        startActivity(new Intent(getActivity(), TransactionListActivity.class));
    }

    @Override
    public void startDelegationList() {
        startActivity(new Intent(getActivity(), DelegationListActivity.class));
    }

    @Override
    public void startConvertCoins() {
        getActivity().startActivity(new Intent(getActivity(), ConvertCoinActivity.class));
    }

    @Override
    public void startTab(@IdRes int tab) {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).setCurrentPageById(tab);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        WalletsTabFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @ProvidePresenter
    WalletsTabPresenter providePresenter() {
        return presenterProvider.get();
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void showRationaleForCamera(final PermissionRequest request) {
        new WalletConfirmDialog.Builder(getActivity(), "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Sure", (d, w) -> {
                    request.proceed();
                    d.dismiss();
                })
                .setNegativeAction("No, I've change my mind", (d, w) -> {
                    request.cancel();
                    d.dismiss();
                }).create()
                .show();
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void showOpenPermissionsForCamera() {
        new WalletConfirmDialog.Builder(getActivity(), "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Open settings", (d, w) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", network.minter.bipwallet.BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    startActivity(intent);
                    d.dismiss();
                })
                .setNegativeAction("Cancel", (d, w) -> {
                    d.dismiss();
                })
                .create()
                .show();
    }

    private void setupTabAdapter() {
        pager.setAdapter(new FragmentStatePagerAdapter(getActivity().getSupportFragmentManager()) {

            @Override
            public int getCount() {
                return 2;
            }

            @NonNull
            @Override
            public Fragment getItem(int position) {
                if (position == 0) {
                    return new CoinsTabPageFragment();
                } else if (position == 1) {
                    return new TxsTabPageFragment();
                }
                return null;
            }
        });
        pager.setOffscreenPageLimit(2);
        tabs.setupWithViewPager(pager);
        tabs.getTabAt(0).setText(R.string.tab_page_coins);
        tabs.getTabAt(1).setText(R.string.tab_page_txs);

    }

    private void checkLastUpdate() {
//        if (!Wallet.app().storage().contains(PrefKeys.LAST_BLOCK_TIME)) {
//            lastUpdateText.setText(HtmlCompat.fromHtml(getString(R.string.balance_last_updated_never)));
//            return;
//        }
//        DateTime lastBlockTime = new DateTime((long) Wallet.app().storage().get(PrefKeys.LAST_BLOCK_TIME, 0L));
//        Seconds diff = Seconds.secondsBetween(lastBlockTime, new DateTime());
//        int res = diff.getSeconds();
//        Timber.d("Diff: now=%s, ts=%s", new DateTime().toString(), lastBlockTime.toString());
//        lastUpdateText.setText(HtmlCompat.fromHtml(getString(R.string.balance_last_updated, Plurals.timeValue((long) res), Plurals.time((long) res))));
    }

    private void onStartRefresh() {
        Wallet.app().sounds().play(R.raw.refresh_pop_down);
    }
}
