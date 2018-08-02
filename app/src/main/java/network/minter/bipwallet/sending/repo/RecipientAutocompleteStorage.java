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

package network.minter.bipwallet.sending.repo;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.sending.models.RecipientItem;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class RecipientAutocompleteStorage {
    private final static String KEY_RECIPIENTS = "recipient_autocomplete_items";
    private final static Object sItemsLock = new Object();
    private final KVStorage mStorage;

    public RecipientAutocompleteStorage(KVStorage storage) {
        mStorage = storage;
    }

    public RecipientAutocompleteStorage add(final RecipientItem item) {
        return add(item, null);
    }

    public RecipientAutocompleteStorage add(final RecipientItem item, Action onNext) {
        getItems()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(items -> {
                    synchronized (sItemsLock) {
                        if (items.contains(item)) {
                            items.get(items.indexOf(item)).update();
                        } else {
                            items.add(item.update());
                        }
                    }
                    mStorage.put(KEY_RECIPIENTS, items);
                    if (onNext != null) {
                        onNext.run();
                    }
                });

        return this;
    }

    public void removeAll() {
        Observable.just(mStorage)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(res -> {
                    res.delete(KEY_RECIPIENTS);
                });
    }

    public Observable<List<RecipientItem>> getItems() {
        return Observable.create(emitter -> {
            emitter.onNext(mStorage.get(KEY_RECIPIENTS, new ArrayList<>()));
            emitter.onComplete();
        });
    }
}
