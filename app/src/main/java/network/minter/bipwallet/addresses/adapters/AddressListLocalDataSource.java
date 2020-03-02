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

package network.minter.bipwallet.addresses.adapters;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import network.minter.bipwallet.addresses.models.AddressItem;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.paging.ListDataSource;
import network.minter.bipwallet.wallets.repos.ExplorerBalanceFetcher;
import network.minter.core.MinterSDK;
import network.minter.explorer.repo.ExplorerAddressRepository;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AddressListLocalDataSource extends ListDataSource<AddressItem> {
    private final SecretStorage mRepo;
	private final ExplorerAddressRepository mExplorerAddressRepo;
    private List<AddressItem> mItems;

    AddressListLocalDataSource(SecretStorage secretStorage, ExplorerAddressRepository explorerAddressRepository) {
	    mExplorerAddressRepo = explorerAddressRepository;
        mRepo = secretStorage;
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    @Override
    protected Observable<List<AddressItem>> getData() {
	    return Observable.create(emitter -> {
		    mItems = new ArrayList<>();
		    for (Map.Entry<String, SecretData> entry : mRepo.getSecrets().entrySet()) {
			    AddressItem item = new AddressItem(entry.getValue().getId(), entry.getValue().getMinterAddress());
			    item.balance.setFetcher(ExplorerBalanceFetcher.createSingleCoinBalance(mExplorerAddressRepo, item.address, MinterSDK.DEFAULT_COIN));
			    mItems.add(item);
		    }

		    emitter.onNext(mItems);
		    emitter.onComplete();
        });
    }
}
