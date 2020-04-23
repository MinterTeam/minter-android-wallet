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

package network.minter.bipwallet.addressbook.db;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.db.WalletDatabase;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;

public class AddressBookRepository {
    private WalletDatabase mDb;

    public AddressBookRepository(WalletDatabase db) {
        mDb = db;
    }

    public Maybe<List<AddressContact>> findAll() {
        return mDb.addressBook().findAll();
    }

    public Maybe<AddressContact> findById(int id) {
        return mDb.addressBook().findById(id);
    }

    public Completable insert(AddressContact contact) {
        return Completable.create(emitter -> {
            try {
                mDb.addressBook().insert(contact);
                emitter.onComplete();
            } catch (Throwable t) {
                emitter.onError(t);
            }
        });
    }

    public Completable update(AddressContact contact) {
        return Completable.create(emitter -> {
            try {
                mDb.addressBook().update(contact);
                emitter.onComplete();
            } catch (Throwable t) {
                emitter.onError(t);
            }
        });
    }

    public Completable delete(AddressContact contact) {
        return Completable.create(emitter -> {
            try {
                mDb.addressBook().delete(contact);
                emitter.onComplete();
            } catch (Throwable t) {
                emitter.onError(t);
            }
        });
    }

    public Completable delete(int id) {
        return Completable.create(emitter -> {
            try {
                mDb.addressBook().deleteById(id);
                emitter.onComplete();
            } catch (Throwable t) {
                emitter.onError(t);
            }
        });
    }

    public Single<Integer> countByNameOrAddress(String nameOrAddress) {
        final String likeArg = String.format("%%%s%%", nameOrAddress);
        return mDb.addressBook().countByNameOrAddress(likeArg);
    }

    public Single<AddressContact> findByNameOrAddress(String nameOrAddress) {
        /*
        если мы указали адрес и не нашли контакт, возвращаем адрес как имя контакта
        если мы указали валидатора - аналогично
        если мы указали черт знает то отправить ему мы не можем, соответственно пишем ошибку
         */
        AddressContact def = new AddressContact();
        Single<AddressContact> defResult = Single.error(new RuntimeException("Not found in address book"));
        def.name = nameOrAddress;
        def.address = nameOrAddress;

        if (nameOrAddress.matches(MinterAddress.ADDRESS_PATTERN)) {
            def.type = AddressContact.AddressType.Address;
            defResult = Single.just(def);
        } else if (nameOrAddress.matches(MinterPublicKey.PUB_KEY_PATTERN)) {
            def.type = AddressContact.AddressType.ValidatorPubKey;
            defResult = Single.just(def);
        }


        return mDb.addressBook().getByNameOrAddress(nameOrAddress)
                .switchIfEmpty(defResult)
                .subscribeOn(Schedulers.io());
    }

    public Maybe<List<AddressContact>> findSuggestionsByNameOrAddress(String nameOrAddress) {
        final String likeArg = String.format("%%%s%%", nameOrAddress);
        return mDb.addressBook().findByNameOrAddress(likeArg)
                .subscribeOn(Schedulers.io());
    }
}
