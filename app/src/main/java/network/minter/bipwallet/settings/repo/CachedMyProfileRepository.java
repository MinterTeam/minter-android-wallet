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

package network.minter.bipwallet.settings.repo;

import android.support.annotation.NonNull;

import io.reactivex.Observable;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CachedEntity;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.core.internal.api.ApiService;
import network.minter.profile.models.User;
import network.minter.profile.repo.ProfileRepository;

import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.rxProfile;
import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.toProfileError;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class CachedMyProfileRepository extends ProfileRepository implements CachedEntity<User.Data> {
    private final static String KEY_USER_DATA = "cached_my_profile_repository_user_data";
    private final KVStorage mStorage;
    private final AuthSession mSession;

    public CachedMyProfileRepository(@NonNull ApiService.Builder apiBuilder, KVStorage storage, AuthSession session) {
        super(apiBuilder);
        mStorage = storage;
        mSession = session;
    }

    @Override
    public User.Data initialData() {
        return mStorage.get(KEY_USER_DATA, createEmpty());
    }

    @Override
    public Observable<User.Data> getUpdatableData() {
        return rxProfile(getProfile())
                .onErrorResumeNext(toProfileError())
                .map(item -> {
                    if (item.isSuccess()) {
                        return item.data;
                    }

                    return createEmpty();
                });
    }

    @Override
    public void onAfterUpdate(User.Data result) {
        mStorage.put(KEY_USER_DATA, result);
        mSession.setUser(result);
    }

    @Override
    public void onClear() {
        mStorage.delete(KEY_USER_DATA);
    }

    private User.Data createEmpty() {
        return new User.Data();
    }
}
