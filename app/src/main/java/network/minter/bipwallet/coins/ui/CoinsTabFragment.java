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

package network.minter.bipwallet.coins.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.coins.contract.CoinsTabView;
import network.minter.bipwallet.coins.views.CoinsTabPresenter;
import network.minter.bipwallet.delegation.ui.DelegationListActivity;
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.SoundManager;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import network.minter.blockchain.models.operational.ExternalTransaction;
import network.minter.explorer.BuildConfig;
import network.minter.explorer.MinterExplorerApi;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@RuntimePermissions
public class CoinsTabFragment extends HomeTabFragment implements CoinsTabView {
    public final static int REQUEST_CODE_QR_SCAN_TX = 2002;

    @Inject Provider<CoinsTabPresenter> presenterProvider;
    @Inject SoundManager soundManager;
    @InjectPresenter CoinsTabPresenter presenter;
    @BindView(R.id.bip_logo) View logo;
    //    @BindView(R.id.user_avatar) BipCircleImageView avatar;
//    @BindView(R.id.username) TextView username;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.balance_int) TextView balanceInt;
    @BindView(R.id.balance_fractions) TextView balanceFract;
    @BindView(R.id.balance_coin_name) TextView balanceCoinName;
    @BindView(R.id.list) RecyclerView list;
    @BindView(R.id.container_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.delegation_view) View delegationView;
    @BindView(R.id.delegation_amount) TextView delegationAmount;
    @BindView(R.id.balance_container) View balanceContainer;
    @BindView(R.id.balance_title) TextView balanceTitle;

    private Unbinder mUnbinder;
    private SwipeRefreshHacker mSwipeRefreshHacker = new SwipeRefreshHacker();
    private GestureDetector mGestureDetector;
    private WalletDialog mCurrentDialog = null;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
        swipeRefreshLayout.setOnRefreshListener(listener);
        mSwipeRefreshHacker.setOnRefreshStartListener(this::onStartRefresh);
        mSwipeRefreshHacker.hack(swipeRefreshLayout);
    }

    @Override
    public void showRefreshProgress() {
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void hideRefreshProgress() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void startExplorer(String hash) {
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MinterExplorerApi.FRONT_URL + "/transactions/" + hash)));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        presenter.onRestoreInstanceState(savedInstanceState);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void scrollTop() {
        list.post(() -> {
            if (list != null) list.scrollToPosition(0);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("StringBufferReplaceableByString")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_coins, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        presenter.onRestoreInstanceState(savedInstanceState);

        if (network.minter.bipwallet.BuildConfig.DEBUG) {
            logo.setOnLongClickListener(v -> {
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

        setHasOptionsMenu(true);
        getActivity().getMenuInflater().inflate(R.menu.menu_tab_scan_tx, toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_scan_tx) {
            startScanQRWithPermissions(REQUEST_CODE_QR_SCAN_TX);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void startExternalTransaction(ExternalTransaction tx) {
        new ExternalTransactionActivity.Builder(getActivity(), tx)
                .start();
    }

    @Override
    public void setOnClickScanQR(View.OnClickListener listener) {

    }

    @NeedsPermission(Manifest.permission.CAMERA)
    @Override
    public void startScanQR(int requestCode) {
        Intent i = new Intent(getActivity(), QRCodeScannerActivity.class);
        getActivity().startActivityForResult(i, requestCode);
    }

    @Override
    public void startScanQRWithPermissions(int requestCode) {
        CoinsTabFragmentPermissionsDispatcher.startScanQRWithPermissionCheck(this, requestCode);
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
        delegationView.setOnClickListener(v -> startDelegationList());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        presenter.onTrimMemory(level);
    }

    @Override
    public void setAvatar(String url) {
        if (url == null) {
            return;
        }

//        avatar.setImageUrl(url);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        presenter.onLowMemory();
//        avatar.setImageDrawable(null);
        Timber.d("OnLowMemory");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        Timber.d("Destroy");
    }

    @Override
    public void setUsername(CharSequence name) {
//        username.setText(name);
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
        delegationView.setVisibility(View.VISIBLE);
        delegationAmount.setText(amount);
    }

    @Override
    public void setBalanceClickListener(View.OnClickListener listener) {
        balanceContainer.setOnClickListener(listener);
    }

    @Override
    public void setBalanceTitle(int title) {
        balanceTitle.setText(title);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        presenter.onSaveInstanceState(outState);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);
    }

    @Override
    public void setOnAvatarClick(View.OnClickListener listener) {
//        avatar.setOnClickListener(listener);
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
    public void hideAvatar() {
//        if (avatar != null) avatar.setVisibility(View.GONE);
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

    @ProvidePresenter
    CoinsTabPresenter providePresenter() {
        return presenterProvider.get();
    }

    private void onStartRefresh() {
        Wallet.app().sounds().play(R.raw.refresh_pop_down);
    }

    private void onSwipeRefreshDown() {

    }
}
