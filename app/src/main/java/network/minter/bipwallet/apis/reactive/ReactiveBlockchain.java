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

package network.minter.bipwallet.apis.reactive;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.blockchain.models.NodeResult;
import network.minter.core.internal.exceptions.NetworkException;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class ReactiveBlockchain {

    public static NodeResult copyError(NodeResult another) {
        return copyError(another);
    }

    public static Function<? super Throwable, ? extends ObservableSource<? extends NodeResult>> toNodeError() {
        return (Function<Throwable, ObservableSource<? extends NodeResult>>) throwable -> {
            return Observable.just(createNodeError(NetworkException.convertIfNetworking(throwable)));
        };
    }

    public static NodeResult createNodeError(Throwable t) {
        Throwable e = NetworkException.convertIfNetworking(t);
        if (e instanceof NetworkException) {
            return createNodeError(((NetworkException) e).getStatusCode(), ((NetworkException) e).getUserMessage());
        }

        return createNodeError(-1, e.getMessage());
    }

    public static NodeResult createNodeError(int code, String message) {
        NodeResult out = new NodeResult();
        out.error = new NodeResult.Error();
        out.error.code = code;
        out.error.message = message;
        return out;
    }
}
