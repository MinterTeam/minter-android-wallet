/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.internal.data;

import android.support.annotation.Nullable;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;
import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * MinterWallet. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 *
 * Usage: app()->cache()->get(MyRepository.class)
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class CacheManager {
    private final static Object sUpdatePrepareLock = new Object();
    private final static Object sUpdateInternalLock = new Object();
    private final ReplaySubject<Boolean> mCompleteNotifier = ReplaySubject.createWithSize(1);
    private final ConcurrentHashMap<Class<? extends CachedEntity>, CachedRepository> mItems = new ConcurrentHashMap<>();
    private volatile LinkedBlockingQueue<CachedRepository> deferred = new LinkedBlockingQueue<>();

    public <Res, T extends CachedEntity<Res>> CacheManager(List<CachedRepository<Res, T>> items) {
        if (items == null) return;
        Stream.of(items).forEach(this::add);
    }

    public <Res, T extends CachedEntity<Res>> CacheManager(Set<CachedRepository<Res, T>> items) {
        if (items == null) return;
        Stream.of(items).forEach(this::add);
    }

    public CacheManager() {
    }

    public ReplaySubject<Boolean> observeComplete() {
        return mCompleteNotifier;
    }

    public <Res, T extends CachedEntity<Res>> CacheManager add(CachedRepository<Res, T> entityRepo) {
        if (mItems.containsKey(entityRepo.getEntityClass())) {
            return this;
        }

        mItems.put(entityRepo.getEntityClass(), entityRepo);

        return this;
    }

    public <T extends CachedEntity> boolean has(Class<T> clazz) {
        return mItems.containsKey(clazz);
    }

    public <T extends CachedEntity> CacheManager remove(Class<T> clazz) {
        if (!has(clazz)) {
            return this;
        }

        mItems.remove(clazz);
        return this;
    }

    public <T extends CachedEntity> void defer(T entity) {
        defer(entity.getClass());
    }

    public <T extends CachedEntity> void defer(Class<T> clazz) {
        checkNotNull(get(clazz), String.format("Entity %s does not exists", clazz.getName())).expire();
    }

    public void updateDeferred() {
        ArrayList<CachedRepository> list = new ArrayList<>(deferred.size());

        while (!deferred.isEmpty()) {
            final CachedRepository entry;
            try {
                entry = deferred.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Timber.w(e, "Deferred interruption");
                break;
            }

            if (entry == null) {
                break; // на случай если конкурентно забрали последний элемент после проверки isEmpty()
            }

            entry.invalidateTime();
            list.add(entry);
        }

        if (list.isEmpty()) {
            return;
        }

        updateInternalItems(list, null, null, null);
    }


    @SuppressWarnings("unchecked")
    public <Res, T extends CachedEntity<Res>> CachedRepository<Res, T> get(Class<T> entityClass) {
        if (!has(entityClass)) {
            return null;
        }

        return (CachedRepository<Res, T>) mItems.get(entityClass);
    }


    public void update() {
        updateInternal(false, null, null, null);
    }


    public void update(boolean force) {
        updateInternal(force, null, null, null);
    }


    public void update(boolean force, Consumer<Object> onNext) {
        updateInternal(force, onNext, null, null);
    }


    public void update(boolean force, Consumer<Object> onNext, Consumer<Throwable> onError) {
        updateInternal(force, onNext, onError, null);
    }


    public void update(boolean force, Consumer<Object> onNext, Consumer<Throwable> onError,
                       Action onComplete) {
        updateInternal(force, onNext, onError, onComplete);
    }


    public void update(Consumer<Object> onNext) {
        updateInternal(false, onNext, null, null);
    }


    public void update(Consumer<Object> onNext, Consumer<Throwable> onError) {
        updateInternal(false, onNext, onError, null);
    }


    public void update(Consumer<Object> onNext, Consumer<Throwable> onError, Action onComplete) {
        updateInternal(false, onNext, onError, onComplete);
    }


    public void updateDelayed(int time, TimeUnit unit) {
        updateDelayedInternal(time, unit, false, null, null, null);
    }


    public void updateDelayed(int time, TimeUnit unit, boolean force) {
        updateDelayedInternal(time, unit, force, null, null, null);
    }


    public void updateDelayed(int time, TimeUnit unit, boolean force, Consumer<Object> onNext) {
        updateDelayedInternal(time, unit, force, onNext, null, null);
    }


    public void updateDelayed(int time, TimeUnit unit, boolean force, Consumer<Object> onNext,
                              Consumer<Throwable> onError) {
        updateDelayedInternal(time, unit, force, onNext, onError, null);
    }


    public void updateDelayed(int time, TimeUnit unit, boolean force, Consumer<Object> onNext,
                              Consumer<Throwable> onError, Action onComplete) {
        updateDelayedInternal(time, unit, force, onNext, onError, onComplete);
    }


    public void updateDelayed(int time, TimeUnit unit, Consumer<Object> onNext) {
        updateDelayedInternal(time, unit, false, onNext, null, null);
    }


    public void updateDelayed(int time, TimeUnit unit, Consumer<Object> onNext,
                              Consumer<Throwable> onError) {
        updateDelayedInternal(time, unit, false, onNext, onError, null);
    }


    public void updateDelayed(int time, TimeUnit unit, Consumer<Object> onNext,
                              Consumer<Throwable> onError, Action onComplete) {
        updateDelayedInternal(time, unit, false, onNext, onError, onComplete);
    }

    @SuppressWarnings("unchecked")
    public synchronized Observable<Object> toObservable(boolean force) {
        List<CachedRepository> data;

        synchronized (sUpdatePrepareLock) {
            // если нужно форсировать, то просто все инвалидируем
            if (force) {
                Stream.of(mItems.values())
                        .forEach(CachedRepository::expire);
            }

            // таким образом получаем в фильтре нужные элементы
            data = Stream.of(mItems.values())
                    .filter(CachedRepository::isExpired)
                    .toList();

            Stream.of(data).forEach(CachedRepository::invalidateTime);
        }

        if (data.size() == 0) {
            return Observable.just(Collections.emptyList());
        }

        return Observable.fromIterable(data)
                .switchMap(
                        cacheDataRepository -> cacheDataRepository.getUpdateObservable().subscribeOn(
                                Schedulers.io()))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public long countExpired() {
        return Stream.of(mItems.values())
                .filter(CachedRepository::isExpired)
                .count();
    }

    public void expireAll() {
        Stream.of(mItems.values())
                .forEach(CachedRepository::expire);
    }

    public void clear() {
        Stream.of(mItems.values())
                .forEach(CachedRepository::clear);
    }

    private void updateDelayedInternal(
            int delay, TimeUnit unit,
            boolean force,
            @Nullable Consumer<Object> onNext,
            @Nullable Consumer<Throwable> onError,
            @Nullable Action onComplete) {

        Observable.timer(delay, unit)
                .subscribeOn(Schedulers.io())
                .subscribe(est -> updateInternal(force, onNext, onError, onComplete));
    }

    @SuppressWarnings("unchecked")
    private void updateInternalItems(
            List<CachedRepository> data,
            @Nullable Consumer<Object> onNext,
            @Nullable Consumer<Throwable> onError,
            @Nullable Action onComplete) {

        if (data.size() == 0) {
            return;
        }

        Stream.of(data).forEach(CachedRepository::invalidateTime);

        Observable.fromIterable(data)
                .flatMap(cacheDataRepository -> cacheDataRepository.getUpdateObservable()
                        .subscribeOn(Schedulers.io()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        n -> {
                            try {
                                if (onNext != null) {
                                    onNext.accept(n);
                                }
                            } catch (Throwable e) {
                                //noinspection RedundantCast - какая-то лажа судя по всему у компилятора
                                throw Exceptions.propagate((Throwable) e);
                            }
                        },
                        t -> {
                            if (onError != null) {
                                //noinspection RedundantCast
                                onError.accept((Throwable) t);
                            }
                        },
                        () -> {
                            if (onComplete != null) {
                                onComplete.run();
                            }
                            mCompleteNotifier.onNext(true);
                            Timber.d("Global Update OnComplete (updated %d items)", data.size());
                        }
                );
    }

    private void updateInternal(boolean force,
                                @Nullable Consumer<Object> onNext,
                                @Nullable Consumer<Throwable> onError,
                                @Nullable Action onComplete) {


        List<CachedRepository> items;

        synchronized (sUpdateInternalLock) {
            // если нужно форсировать, то просто все инвалидируем
            if (force) {
                Stream.of(mItems.values())
                        .forEach(CachedRepository::expire);
            }

            // таким образом получаем в фильтре нужные элементы
            items = Stream.of(mItems.values())
                    .filter(CachedRepository::isExpired)
                    .map(item -> {
                        Timber.d("Item to update: %s", item.getClass().getSimpleName());
                        return item;
                    })
                    .toList();
        }


        if (items.size() == 0) {
            return;
        }

        updateInternalItems(items, onNext, onError, onComplete);
    }
}
