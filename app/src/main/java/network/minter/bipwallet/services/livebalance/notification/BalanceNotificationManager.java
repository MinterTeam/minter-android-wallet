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

package network.minter.bipwallet.services.livebalance.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.google.gson.GsonBuilder;

import androidx.core.app.NotificationCompat;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.external.ui.ExternalActivity;
import network.minter.bipwallet.internal.common.Lazy;
import network.minter.bipwallet.internal.helpers.NetworkHelper;
import network.minter.bipwallet.internal.notifications.BaseNotificationManager;
import network.minter.bipwallet.internal.settings.SettingsManager;
import network.minter.bipwallet.services.livebalance.models.RTMBalance;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import network.minter.core.crypto.MinterAddress;
import timber.log.Timber;

import static android.os.Build.VERSION.SDK_INT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class BalanceNotificationManager extends BaseNotificationManager {

    public final static String WALLET_BALANCE_UPDATE_CHANNEL = "bipwallet_balance_update";
    private final static int WALLET_BALANCE_ID = 1;
    private final NotificationManager mNotificationManager;
    private final GsonBuilder mGsonBuilder;
    private final NetworkHelper mNetwork;
    private final Context mContext;
    private final Lazy<Boolean> mEnabledNotifications;

    public BalanceNotificationManager(Context context, GsonBuilder gsonBuilder, NetworkHelper network, SettingsManager prefs) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mGsonBuilder = gsonBuilder;
        mNetwork = network;
        mEnabledNotifications = () -> prefs.getBool(SettingsManager.EnableLiveNotifications);
    }

    public void showBalanceUpdate(String json, MinterAddress address) {
        if (!mEnabledNotifications.get()) {
            return;
        }

        Timber.d("Balance update message: %s", json);

        showBalanceUpdated(address);

//        List<RTMBalance> message;
//        try {
//            message = mGsonBuilder.create().fromJson(json, new TypeToken<List<RTMBalance>>(){}.getType());
//        } catch (Throwable t) {
//            Timber.e(t, "Unable to decode live balance message: %s", json);
//            return;
//        }
//
//        for(RTMBalance msg: message) {
//            showBalanceForCoin(msg, address);
//        }


    }

    private void showBalanceUpdated(MinterAddress address) {
        if (SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final String channelName = mContext.getResources().getString(R.string.notification_balance_update_name);
            final String channelDescription = mContext.getResources().getString(R.string.notification_balance_update_description);
            NotificationChannel channel = new NotificationChannel(WALLET_BALANCE_UPDATE_CHANNEL, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(channelDescription);
            channel.setShowBadge(true);
            mNotificationManager.createNotificationChannel(channel);
        }

        Intent contentIntent = ExternalActivity.createAction(mContext, ExternalActivity.ACTION_OPEN_HOME, null);

        final String listTxLabel = mContext.getResources().getString(R.string.notification_balance_list_tx_label);
        final Intent listTxIntent = ExternalActivity.createAction(mContext, ExternalActivity.ACTION_OPEN_TRANSACTION_LIST, null);

        NotificationCompat.Action listTxAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_notify_list_tx,
                        listTxLabel,
                        createActionIntent(mContext, listTxIntent)
                ).build();

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        style.addLine("Status: updated").setSummaryText(address.toShortString());

        final Notification messageNotification = new NotificationCompat.Builder(mContext, WALLET_BALANCE_UPDATE_CHANNEL)
                .setContentTitle("Balance")
                .setContentText("Status: updated")
                .setSmallIcon(R.drawable.ic_notify_coin)
                .setColor(mContext.getResources().getColor(R.color.colorPrimaryDark))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setStyle(style)
                .setGroup(address.toString())
                .setContentIntent(createActionIntent(mContext, contentIntent))
                .addAction(listTxAction)
                .setNumber(1)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .build();

        mNotificationManager.notify(WALLET_BALANCE_ID, messageNotification);
    }

    private void showBalanceForCoin(RTMBalance message, MinterAddress address) {
        if (SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final String channelName = mContext.getResources().getString(R.string.notification_balance_update_name);
            final String channelDescription = mContext.getResources().getString(R.string.notification_balance_update_description);
            NotificationChannel channel = new NotificationChannel(WALLET_BALANCE_UPDATE_CHANNEL, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(channelDescription);
            channel.setShowBadge(true);
            mNotificationManager.createNotificationChannel(channel);
        }

        Intent contentIntent = ExternalActivity.createAction(mContext, ExternalActivity.ACTION_OPEN_HOME, null);

        final String listTxLabel = mContext.getResources().getString(R.string.notification_balance_list_tx_label);
        final Intent listTxIntent = new Intent(mContext, TransactionListActivity.class);
        NotificationCompat.Action listTxAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_notify_list_tx,
                        listTxLabel,
                        createActionIntent(mContext, listTxIntent)
                ).build();

        mNetwork.downloadImageCircle(message.getCoinAvatar())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {

                    final String text = String.format("%s: %s", message.getCoin(), bdHuman(message.getAmount()));

                    NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                    style.addLine(text).setSummaryText(address.toShortString());

                    final Notification messageNotification = new NotificationCompat.Builder(mContext, WALLET_BALANCE_UPDATE_CHANNEL)
                            .setContentTitle("Balance updated")
                            .setContentText(text)
                            .setSmallIcon(R.drawable.ic_notify_coin)
                            .setLargeIcon(result)
                            .setColor(mContext.getResources().getColor(R.color.colorPrimaryDark))
                            .setAutoCancel(true)
                            .setCategory(NotificationCompat.CATEGORY_STATUS)
                            .setStyle(style)
                            .setGroup(address.toString() + "_" + message.getCoin())
                            .setContentIntent(createActionIntent(mContext, contentIntent))
                            .addAction(listTxAction)
                            .setNumber(1)
                            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                            .build();

                    mNotificationManager.notify(WALLET_BALANCE_ID, messageNotification);
                });
    }
}
