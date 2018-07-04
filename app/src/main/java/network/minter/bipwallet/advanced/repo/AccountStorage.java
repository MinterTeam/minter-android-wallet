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

package network.minter.bipwallet.advanced.repo;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.coins.repos.ExplorerBalanceFetcher;
import network.minter.bipwallet.internal.data.CachedEntity;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.explorerapi.repo.ExplorerAddressRepository;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AccountStorage implements CachedEntity<UserAccount> {
    private final static String KEY_BALANCE = "account_storage_balance";
    private final ExplorerAddressRepository mExpAddressRepo;
    private final SecretStorage mSecretStorage;
    private final KVStorage mStorage;


    public AccountStorage(KVStorage storage, SecretStorage secretStorage, ExplorerAddressRepository expAddressRepo) {
        mStorage = storage;
        mExpAddressRepo = expAddressRepo;
        mSecretStorage = secretStorage;
    }

    @Override
    public UserAccount initialData() {
        if (mStorage.contains(KEY_BALANCE)) {
            return mStorage.get(KEY_BALANCE);
        }

        return new UserAccount(Collections.emptyList());
    }

    /**
     * Group AccountItems's (inside UserAccount) by coin, but reduces MinterAddress (it will be null after grouping)
     * Map does not changes original data
     *
     * @return RxJava2 function
     */
    public static Function<UserAccount, UserAccount> groupAccountByCoin() {
        return items -> {
            List<AccountItem> in = new ArrayList<>(items.size());
            Stream.of(items.getAccounts()).forEach(item -> in.add(new AccountItem(item)));
            List<AccountItem> out = new ArrayList<>();
            final Map<String, AccountItem> tmp = new HashMap<>();
            for (AccountItem item : in) {
                if (!tmp.containsKey(item.coin)) {
//                    item.address = null;
                    tmp.put(item.coin, item);
                } else {
                    tmp.get(item.coin).balance = tmp.get(item.coin).balance.add(item.balance);
                }
            }

            Stream.of(tmp.values())
                    .forEach(out::add);

            return new UserAccount(out);
        };
    }

    /**
     * Group AccountItems's by coin, but reduces MinterAddress (it will be null after grouping)
     *
     * @return RxJava2 function
     */
    public static Function<List<AccountItem>, List<AccountItem>> groupAccountItemsByCoin() {
        return items -> {
            List<AccountItem> in = new ArrayList<>(items.size());
            Stream.of(items).forEach(item -> in.add(new AccountItem(item)));
            List<AccountItem> out = new ArrayList<>();
            final Map<String, AccountItem> tmp = new HashMap<>();
            for (AccountItem item : items) {
                if (!tmp.containsKey(item.coin)) {
//                    item.address = null;
                    tmp.put(item.coin, item);
                } else {
                    tmp.get(item.coin).balance = tmp.get(item.coin).balance.add(item.balance);
                }
            }

            Stream.of(tmp.values())
                    .forEach(out::add);

            return out;
        };
    }

    @Override
    public void onAfterUpdate(UserAccount result) {
        mStorage.put(KEY_BALANCE, result);
    }

    @Override
    public Observable<UserAccount> getUpdatableData() {
        return ExplorerBalanceFetcher
                .create(mExpAddressRepo, mSecretStorage.getAddresses())
                .map(UserAccount::new);
    }

}
