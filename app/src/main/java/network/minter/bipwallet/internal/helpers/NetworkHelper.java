package network.minter.bipwallet.internal.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class NetworkHelper {

    private Context mContext;

    public NetworkHelper(final Context context) {
        mContext = context;
    }

    public boolean hasNetworkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        @SuppressLint("MissingPermission") NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public Observable<Boolean> getInternetChecker(final String url) {
        return downloadFile(url)
                .map(item -> item != null && (item.contentLength() == -1 || item.contentLength() > 0))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    public boolean checkCarrierName(final String name) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }

        //@TODO проверять разрешение
        @SuppressLint("MissingPermission") NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        TelephonyManager manager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (manager == null) {
            return false;
        }
        String carrierName = manager.getNetworkOperatorName();
        boolean isWiFi = false;
        if (activeNetwork != null) {
            isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }

        if (carrierName.equalsIgnoreCase(name)) {
            return !isConnected || isWiFi;
        }

        return false;
    }


    public Observable<Bitmap> downloadImage(String url) {
        return downloadFile(url)
                .switchMap(result -> Observable.just(result.bytes()))
                .switchMap(bytes -> Observable.just(BitmapFactory.decodeByteArray(bytes, 0, bytes.length)));
    }

    public Observable<Bitmap> downloadImageCircle(String url) {
        return downloadImage(url)
                .switchMap(bm -> Observable.just(ImageHelper.makeBitmapCircle(bm)));
    }

    /**
     * Загрзука файла
     */
    public Observable<ResponseBody> downloadFile(String url) {
        return Observable.create(subscriber -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    subscriber.onNext(response.body());
                    subscriber.onComplete();
                }
            } catch (IOException e) {
                subscriber.onError(e);
            }
        });
    }
}
