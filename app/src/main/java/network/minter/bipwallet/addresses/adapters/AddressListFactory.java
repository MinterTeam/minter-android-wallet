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

import android.arch.paging.DataSource;

import network.minter.bipwallet.addresses.models.AddressItem;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.explorerapi.repo.ExplorerAddressRepository;
import network.minter.my.repo.MyAddressRepository;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AddressListFactory extends DataSource.Factory<Integer, AddressItem> {
    private final ExplorerAddressRepository mExplorerAddressRepository;
    private MyAddressRepository mMyAddressRepository;
    private SecretStorage mSecretStorage;

    public AddressListFactory(MyAddressRepository addressRepository, ExplorerAddressRepository explorerAddressRepository) {
        this(explorerAddressRepository);
        mMyAddressRepository = addressRepository;
    }

    public AddressListFactory(SecretStorage secretStorage, ExplorerAddressRepository explorerAddressRepository) {
        this(explorerAddressRepository);
        mSecretStorage = secretStorage;
    }

    private AddressListFactory(ExplorerAddressRepository explorerAddressRepository) {
        mExplorerAddressRepository = explorerAddressRepository;
    }

    @Override
    public DataSource<Integer, AddressItem> create() {
        if (mMyAddressRepository != null) {
            return new AddressListRemoteDataSource(mMyAddressRepository, mExplorerAddressRepository);
        }

        if (mSecretStorage != null) {
            return new AddressListLocalDataSource(mSecretStorage, mExplorerAddressRepository);
        }

        throw new IllegalStateException("Factory must be instatiated with one of repo: MyMinter address repo or local SecretStorage");
    }
}
