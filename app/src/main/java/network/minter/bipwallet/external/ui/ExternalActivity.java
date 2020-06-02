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

package network.minter.bipwallet.external.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.airbnb.deeplinkdispatch.DeepLinkEntry;
import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.airbnb.deeplinkdispatch.DeepLinkUri;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Method;
import java.util.Map;

import javax.inject.Inject;

import androidx.core.app.TaskStackBuilder;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.external.DeepLinkModule;
import network.minter.bipwallet.external.DeepLinkModuleRegistry;
import network.minter.bipwallet.external.DeepLinkModuleRegistryHelper;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseInjectActivity;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.security.PauseTimer;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.ui.PinEnterActivity;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@DeepLinkHandler({DeepLinkModule.class})
public class ExternalActivity extends BaseInjectActivity {
    public final static String ACTION_OPEN_HOME = "OPEN_HOME";
    public final static String ACTION_OPEN_TRANSACTION_LIST = "OPEN_TRANSACTION_LIST";
    private final static String EXTRA_ACTION = "EXTRA_ACTION";
    private final static String EXTRA_PAYLOAD = "EXTRA_PAYLOAD";
    @Inject AuthSession session;
    @Inject SecretStorage secretStorage;
    @Inject GsonBuilder gsonBuilder;
    private DeepLinkModuleRegistry mLinkLoader;
    private DeepLinkModuleRegistryHelper<DeepLinkModuleRegistry> mLinkReg;

    /*
    public DeepLinkEntry parseUri(String uri) {
    for (DeepLinkEntry entry : REGISTRY) {
      if (entry.matches(uri)) {
        return entry;
      }
    }
    return null;
  }

  public boolean matches(String inputUri) {
    DeepLinkUri deepLinkUri = DeepLinkUri.parse(inputUri);
    return deepLinkUri != null && regex.matcher(schemeHostAndPath(deepLinkUri)).find();
  }
     */


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
        setTheme(R.style.Wallet_Splash);
        session.restore();
        if (!session.isLoggedIn(true)) {
            Timber.d("Session not verified. Logging in");
            startLogin();
            return;
        }

        startAction();
    }

    private void startDeepLink() {
        mLinkLoader = new DeepLinkModuleRegistry();
        mLinkReg = new DeepLinkModuleRegistryHelper<>(mLinkLoader);
        DeepLinkDelegate delegate = new DeepLinkDelegate(mLinkLoader);
        String old = getIntent().getData().toString();
        if (old.substring(old.length() - 1).equals("/")) {
            old = old.substring(0, old.length() - 1);
        }
        Uri uri = Uri.parse(old);
        getIntent().setData(uri);

        if (!secretStorage.hasPinCode() || PauseTimer.isLoggedIn()) {
            delegate.dispatchFrom(this);
            finish();
        } else {
            Intent target = dispatchDeeplink(this, getIntent());
            new PinEnterActivity.Builder(this, SecurityModule.PinMode.Validation)
                    .setSuccessIntent(target)
                    .startClearTop();
            finish();
        }
    }

    private DeepLinkEntry findEntry(String uri) {
        return mLinkReg.parseUri(uri);
    }

    private Intent dispatchDeeplink(Activity activity, Intent sourceIntent) {
        if (activity == null) {
            throw new NullPointerException("activity == null");
        }
        if (sourceIntent == null) {
            throw new NullPointerException("sourceIntent == null");
        }
        Uri uri = sourceIntent.getData();
        if (uri == null) {
            return null;
        }
        String uriString = uri.toString();
        DeepLinkEntry entry = findEntry(uriString);
        if (entry != null) {
            DeepLinkUri deepLinkUri = DeepLinkUri.parse(uriString);
            Map<String, String> parameterMap = entry.getParameters(DeepLinkUri.parse(uriString));
            for (String queryParameter : deepLinkUri.queryParameterNames()) {
                for (String queryParameterValue : deepLinkUri.queryParameterValues(queryParameter)) {
                    if (parameterMap.containsKey(queryParameter)) {
                        Timber.w("Duplicate parameter name in path and query param: %s", queryParameter);
                    }
                    parameterMap.put(queryParameter, queryParameterValue);
                }
            }
            parameterMap.put(DeepLink.URI, uri.toString());
            Bundle parameters;
            if (sourceIntent.getExtras() != null) {
                parameters = new Bundle(sourceIntent.getExtras());
            } else {
                parameters = new Bundle();
            }
            for (Map.Entry<String, String> parameterEntry : parameterMap.entrySet()) {
                parameters.putString(parameterEntry.getKey(), parameterEntry.getValue());
            }
            try {
                Class<?> c = entry.getActivityClass();
                Intent newIntent;
                TaskStackBuilder taskStackBuilder = null;
                if (entry.getType() == DeepLinkEntry.Type.CLASS) {
                    newIntent = new Intent(activity, c);
                } else {
                    Method method;
                    try {
                        method = c.getMethod(entry.getMethod(), Context.class);
                        if (method.getReturnType().equals(TaskStackBuilder.class)) {
                            taskStackBuilder = (TaskStackBuilder) method.invoke(c, activity);
                            if (taskStackBuilder.getIntentCount() == 0) {
                                return null;
                            }
                            newIntent = taskStackBuilder.editIntentAt(taskStackBuilder.getIntentCount() - 1);
                        } else {
                            newIntent = (Intent) method.invoke(c, activity);
                        }
                    } catch (NoSuchMethodException exception) {
                        method = c.getMethod(entry.getMethod(), Context.class, Bundle.class);
                        if (method.getReturnType().equals(TaskStackBuilder.class)) {
                            taskStackBuilder = (TaskStackBuilder) method.invoke(c, activity, parameters);
                            if (taskStackBuilder.getIntentCount() == 0) {
                                return null;
                            }
                            newIntent = taskStackBuilder.editIntentAt(taskStackBuilder.getIntentCount() - 1);
                        } else {
                            newIntent = (Intent) method.invoke(c, activity, parameters);
                        }
                    }
                }
                if (newIntent.getAction() == null) {
                    newIntent.setAction(sourceIntent.getAction());
                }
                if (newIntent.getData() == null) {
                    newIntent.setData(sourceIntent.getData());
                }
                newIntent.putExtras(parameters);
                newIntent.putExtra(DeepLink.IS_DEEP_LINK, true);
                newIntent.putExtra(DeepLink.REFERRER_URI, uri);
                if (activity.getCallingActivity() != null) {
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                }

                return newIntent;
            } catch (Throwable t) {
                Timber.e(t, "Unable to handle deeplink");
            }
        }

        return null;
    }

    private void startAction() {
        if (getIntent() != null && getIntent().getExtras() == null) {
            startDeepLink();
            return;
        }

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
                if (secretStorage.hasPinCode()) {
                    new PinEnterActivity.Builder(this, SecurityModule.PinMode.Validation)
                            .startHomeOnSuccess()
                            .start();
                } else {
                    new HomeActivity.Builder(this).start();
                }

                finish();
                break;
            case ACTION_OPEN_TRANSACTION_LIST:
                if (secretStorage.hasPinCode()) {
                    Intent intent = new Intent(this, TransactionListActivity.class);
                    new PinEnterActivity.Builder(this, SecurityModule.PinMode.Validation)
                            .setSuccessIntent(intent)
                            .start();
                } else {
                    new TransactionListActivity.Builder(this).start();
                }

                finish();
                break;
            default:
                startDeepLink();
//                startSplash();
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
