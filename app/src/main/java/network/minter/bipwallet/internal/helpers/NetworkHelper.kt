/*
 * Copyright (C) by MinterTeam. 2021
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
package network.minter.bipwallet.internal.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.BehaviorSubject
import network.minter.bipwallet.internal.helpers.ImageHelper.Companion.makeBitmapCircle
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.IOException

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class NetworkHelper(private val context: Context) {
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private var networkCallback: ConnectivityManager.NetworkCallback
    private var hasNetwork: Boolean = false
    private var hasInternet: Boolean = false
    private val networkSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    init {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                hasNetwork = true
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                networkSubject.onNext(hasNetwork && hasInternet)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                hasNetwork = false
                hasNetwork = false
                networkSubject.onNext(false)
            }
        }

        connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                networkCallback)
    }

    fun downloadImage(url: String?): Observable<Bitmap?> {
        return downloadFile(url)
                .switchMap { result: ResponseBody -> Observable.just(result.bytes()) }
                .switchMap { bytes: ByteArray -> Observable.just(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) }
    }

    fun downloadImageCircle(url: String?): Observable<Bitmap> {
        return downloadImage(url)
                .switchMap { bm: Bitmap? -> Observable.just(makeBitmapCircle(bm!!)) }
    }

    fun downloadFile(url: String?): Observable<ResponseBody?> {
        return Observable.create { subscriber: ObservableEmitter<ResponseBody?> ->
            val client = OkHttpClient()
            val request = Request.Builder().url(url!!).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    subscriber.onNext(response.body!!)
                    subscriber.onComplete()
                }
            } catch (e: IOException) {
                subscriber.onError(e)
            }
        }
    }

    fun hasNetworkConnectionSubject(): BehaviorSubject<Boolean> {
        return networkSubject
    }

}