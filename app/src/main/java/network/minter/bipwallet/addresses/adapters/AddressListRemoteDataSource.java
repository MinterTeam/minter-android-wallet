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

package network.minter.bipwallet.addresses.adapters;

import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import java.util.List;

import io.reactivex.functions.Function;
import network.minter.bipwallet.addresses.models.AddressItem;
import network.minter.bipwallet.coins.repos.ExplorerBalanceFetcher;
import network.minter.explorerapi.repo.ExplorerAddressRepository;
import network.minter.my.models.MyAddressData;
import network.minter.my.models.MyResult;
import network.minter.my.repo.MyAddressRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AddressListRemoteDataSource extends PageKeyedDataSource<Integer, AddressItem> {
    private final MyAddressRepository mRepo;
    private final ExplorerAddressRepository mExplorerAddressRepo;

    AddressListRemoteDataSource(MyAddressRepository myAddressRepository, ExplorerAddressRepository explorerAddressRepository) {
        mRepo = myAddressRepository;
        mExplorerAddressRepo = explorerAddressRepository;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Integer, AddressItem> callback) {
        rxCallMy(mRepo.getAddresses(1))
                .map(mapMyItems())
                .subscribe(res -> {
                    callback.onResult(res.data, 0, res.getMeta().total, null, 2);
                }, t -> {
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, AddressItem> callback) {
        rxCallMy(mRepo.getAddresses(params.key))
                .map(mapMyItems())
                .subscribe(res -> {
                    callback.onResult(res.data, params.key == 1 ? null : params.key - 1);
                }, t -> {
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, AddressItem> callback) {
        rxCallMy(mRepo.getAddresses(params.key))
                .map(mapMyItems())
                .subscribe(res -> {
                    callback.onResult(res.data, params.key > res.getMeta().lastPage ? null : params.key + 1);
                }, t -> {
                });
    }

    private Function<MyResult<List<MyAddressData>>, MyResult<List<AddressItem>>> mapMyItems() {
        return listMyResult -> {
            final List<AddressItem> items = Stream.of(listMyResult.data)
                    .map(AddressItem::new)
                    .map(item -> {
                        item.balance.setFetcher(ExplorerBalanceFetcher.createSingle(mExplorerAddressRepo, item.address));
                        return item;
                    })
                    .toList();
            MyResult<List<AddressItem>> result = new MyResult<>();
            result.meta = listMyResult.meta;
            result.error = listMyResult.error;
            result.data = items;
            return result;
        };
    }

}
