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

package network.minter.bipwallet.advanced.repo;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.advanced.models.AddressBalanceTotal;
import network.minter.bipwallet.advanced.models.AddressListBalancesTotal;
import network.minter.bipwallet.internal.data.CachedEntity;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.AddressBalance;
import network.minter.explorer.models.BCExplorerResult;
import network.minter.explorer.models.DelegationInfo;
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorerGate.rxExpGate;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AccountStorage implements CachedEntity<AddressListBalancesTotal> {
    private final static String KEY_BALANCE = BuildConfig.MINTER_STORAGE_VERS + "account_storage_balance";
    private final ExplorerAddressRepository mExpAddressRepo;
    private final SecretStorage mSecretStorage;
    private final KVStorage mStorage;

    public AccountStorage(KVStorage storage, SecretStorage secretStorage, ExplorerAddressRepository expAddressRepo) {
        mStorage = storage;
        mExpAddressRepo = expAddressRepo;
        mSecretStorage = secretStorage;
    }

    @Override
    public AddressListBalancesTotal initialData() {
        return mStorage.get(KEY_BALANCE, new AddressListBalancesTotal(mSecretStorage.getAddresses()));
    }

    public AddressBalanceTotal getMainWallet() {
        return initialData().getBalance(mSecretStorage.getMainWallet());
    }

    public AddressListBalancesTotal getWallets() {
        return initialData();
    }

    @Override
    public void onAfterUpdate(AddressListBalancesTotal result) {
        mStorage.put(KEY_BALANCE, result);
    }

    @Override
    public void onClear() {
        mStorage.delete(KEY_BALANCE);
    }

    @Override
    public Observable<AddressListBalancesTotal> getUpdatableData() {
        return Observable
                .fromIterable(mSecretStorage.getAddresses())
                .flatMap(mapBalances())
                .toList()
                .map(aggregate())
                .subscribeOn(Schedulers.io())
                .toObservable();
    }

    private Function<MinterAddress, ObservableSource<BCExplorerResult<AddressBalanceTotal>>> mapBalances() {
        return address -> rxExpGate(mExpAddressRepo.getAddressData(address, true))
                .switchMap(mapToDelegations());
    }

    private Function<BCExplorerResult<AddressBalance>, ObservableSource<BCExplorerResult<AddressBalanceTotal>>> mapToDelegations() {
        return res -> rxExpGate(mExpAddressRepo.getDelegations(res.result.address, 0))
                .map(mapDelegationsToBalances(res));
    }

    private Function<ExpResult<List<DelegationInfo>>, BCExplorerResult<AddressBalanceTotal>> mapDelegationsToBalances(final BCExplorerResult<AddressBalance> res) {
        return item -> {
            BCExplorerResult<AddressBalanceTotal> out = new BCExplorerResult<>();
            out.error = item.error;
            out.result = new AddressBalanceTotal(res.result, item.meta.additional.delegatedAmount);
            return out;
        };
    }

    private Function<List<BCExplorerResult<AddressBalanceTotal>>, AddressListBalancesTotal> aggregate() {
        return bcExplorerResults -> {
            AddressListBalancesTotal out = new AddressListBalancesTotal();
            for (BCExplorerResult<AddressBalanceTotal> balance : bcExplorerResults) {
                if (balance.isOk()) {
                    out.balances.add(balance.result);
                } else {
                    Timber.w(balance.error.getMessage());
                }
            }

            return out;
        };
    }

}
