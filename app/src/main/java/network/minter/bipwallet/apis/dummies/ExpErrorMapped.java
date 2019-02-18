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

package network.minter.bipwallet.apis.dummies;

import network.minter.core.internal.exceptions.NetworkException;
import network.minter.explorer.models.ExpResult;
import retrofit2.HttpException;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class ExpErrorMapped<Result> extends ExpResult<Result> implements ResultErrorMapper {
    public int statusCode;
    public String errorMessage;

    @Override
    public boolean mapError(Throwable throwable) {
        if (throwable instanceof HttpException) {
            // don't handle, we need real error data, not just status info
            return false;
        }

        if (!NetworkException.isNetworkError(throwable)) {
            return false;
        }

        NetworkException e = (NetworkException) NetworkException.convertIfNetworking(throwable);
        result = null;
        statusCode = e.getStatusCode();
        errorMessage = e.getUserMessage();

        return true;
    }
}
