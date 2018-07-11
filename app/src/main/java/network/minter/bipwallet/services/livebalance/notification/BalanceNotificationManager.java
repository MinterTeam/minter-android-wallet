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

package network.minter.bipwallet.services.livebalance.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.gson.GsonBuilder;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.external.ui.ExternalActivity;
import network.minter.bipwallet.internal.helpers.NetworkHelper;
import network.minter.bipwallet.internal.notifications.BaseNotificationManager;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import timber.log.Timber;

import static android.os.Build.VERSION.SDK_INT;

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

    public BalanceNotificationManager(Context context, GsonBuilder gsonBuilder, NetworkHelper network) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mGsonBuilder = gsonBuilder;
        mNetwork = network;
    }

    public void showBalanceUpdate(String json) {
        LiveBalanceMessage message;
        try {
            message = mGsonBuilder.create().fromJson(json, LiveBalanceMessage.class);
        } catch (Throwable t) {
            Timber.e(t, "Unable to decode live balance message");
            return;
        }

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

                    final String text = String.format("%s: %s", message.getCoin(), message.getAmount().toPlainString());

                    NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                    style.addLine(text).setSummaryText(message.getAddress().toShortString());

                    final Notification messageNotification = new NotificationCompat.Builder(mContext, WALLET_BALANCE_UPDATE_CHANNEL)
                            .setContentTitle("Balance updated")
                            .setContentText(text)
                            .setSmallIcon(R.drawable.ic_notify_coin)
                            .setLargeIcon(result)
                            .setColor(mContext.getResources().getColor(R.color.colorPrimaryDark))
                            .setAutoCancel(true)
                            .setCategory(NotificationCompat.CATEGORY_STATUS)
                            .setStyle(style)
                            .setGroup(message.getAddress().toString() + "_" + message.getCoin())
                            .setContentIntent(createActionIntent(mContext, contentIntent))
                            .addAction(listTxAction)
                            .setNumber(1)
                            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                            .build();

                    mNotificationManager.notify(WALLET_BALANCE_ID, messageNotification);
                });
        /*
        final Notification paymentNotification = new NotificationCompat.Builder(getApplication(), DOGSY_PAYMENT_CHANNEL)
                .setContentTitle("Dogsy")
                .setSmallIcon(R.drawable.ic_notif_payment)
                .setColor(0xFF15BBDE)
                .setContentTitle("Оплата комиссии")
                .setContentText(message.text)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle("Оплата комиссии").bigText(message.text))
                .setGroup(message.getGroup())
                .setContentIntent(createActionIntent(contentIntent))
                .setVibrate(new long[]{0, 250, 250, 250})
                .setLights(0xFF15BBDE, 3000, 3000)
                .build();
                */
    }
}
