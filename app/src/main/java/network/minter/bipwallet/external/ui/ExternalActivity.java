/*******************************************************************************
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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
 ******************************************************************************/

package network.minter.bipwallet.external.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.gson.GsonBuilder;

import javax.inject.Inject;

import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseInjectActivity;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ExternalActivity extends BaseInjectActivity {
    public final static String ACTION_OPEN_HOME = "OPEN_HOME";
    public final static String ACTION_OPEN_TRANSACTION_LIST = "OPEN_TRANSACTION_LIST";
    private final static String EXTRA_ACTION = "EXTRA_ACTION";
    private final static String EXTRA_PAYLOAD = "EXTRA_PAYLOAD";
    @Inject AuthSession session;
    @Inject GsonBuilder gsonBuilder;

    public static Intent createAction(String action, String payload) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_PAYLOAD, payload);
        intent.putExtra(EXTRA_ACTION, action);
        return intent;
    }

    public static Intent createAction(Context context, String action, String payload) {
        Intent intent = new Intent(context, ExternalActivity.class);
        intent.putExtra(EXTRA_PAYLOAD, payload);
        intent.putExtra(EXTRA_ACTION, action);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session.restore();
        if (!session.isLoggedIn(true)) {
            setTheme(R.style.Wallet_External);
            Timber.d("Session not verified. Logging in");
            startLogin();
            return;
        }

        setTheme(android.R.style.Theme_Translucent);
        startAction();
    }

    private void startAction() {
        if (getIntent() == null || getIntent().getExtras() == null) {
            startSplash();
            return;
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle.size() == 0 || (getIntent().getAction() == null && !getIntent().hasExtra(EXTRA_ACTION))) {
            startSplash();
            return;
        }

        final String action;
        if (getIntent().hasExtra(EXTRA_ACTION)) {
            action = getIntent().getStringExtra(EXTRA_ACTION);
        } else {
            action = getIntent().getAction();
        }

        switch (action) {
            case ACTION_OPEN_HOME:
                new HomeActivity.Builder(this).start();
                finish();
                break;
            case ACTION_OPEN_TRANSACTION_LIST:
                new TransactionListActivity.Builder(this).start();
                finish();
                break;
            default:
                startSplash();
                break;
        }

        Timber.d(bundle.toString());
    }

    private void startSplash() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    private void startLogin() {
        startSplash();
//        startActivity(new Intent(this, LoginActivity.class));
//        finish();
    }
}
