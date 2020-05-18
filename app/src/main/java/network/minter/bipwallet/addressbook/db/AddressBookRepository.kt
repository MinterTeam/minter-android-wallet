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
package network.minter.bipwallet.addressbook.db

import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.db.WalletDatabase
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey

class AddressBookRepository(
        private val db: WalletDatabase,
        private val validatorsRepo: RepoValidators
) {
    fun findAll(): Observable<List<AddressContact>> {
        return db.addressBook().findAll()
                .toObservable()
                .flatMap { contacts ->
                    validatorsRepo.fetch()
                            .map { validators ->
                                contacts
                                        .filter { it.type == AddressContact.AddressType.ValidatorPubKey }
                                        .forEach { contact ->
                                            validators.forEach { validator ->
                                                if (contact.address.equals(validator.pubKey.toString())) {
                                                    contact.extAvatar = validator.meta.iconUrl
                                                }
                                            }
                                        }

                                contacts
                            }
                }
    }

    fun findById(id: Int): Maybe<AddressContact> {
        return db.addressBook().findById(id)
    }

    fun insert(contact: AddressContact?): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            try {
                db.addressBook().insert(contact)
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    fun update(contact: AddressContact?): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            try {
                db.addressBook().update(contact)
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    fun delete(contact: AddressContact?): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            try {
                db.addressBook().delete(contact)
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    fun delete(id: Int): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            try {
                db.addressBook().deleteById(id)
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    @JvmOverloads
    fun countByName(name: String, exclude: String? = null): Single<Int> {
        if (exclude != null) {
            return db.addressBook().countByName(name, exclude)
        }

        return db.addressBook().countByName(name)
    }

    @JvmOverloads
    fun countByAddress(address: String, exclude: String? = null): Single<Int> {
        if (exclude != null) {
            return db.addressBook().countByAddress(address, exclude)
        }

        return db.addressBook().countByAddress(address)
    }

    @JvmOverloads
    fun countLikeByNameOrAddress(nameOrAddress: String, exclude: String? = null): Single<Int> {
        val likeArg = String.format("%%%s%%", nameOrAddress)

        if (exclude != null) {
            return db.addressBook().countByNameOrAddress(likeArg, exclude)
        }

        return db.addressBook().countByNameOrAddress(likeArg)
    }

    fun findByNameOrAddress(nameOrAddress: String): Single<AddressContact> {
        /*
        если мы указали адрес и не нашли контакт, возвращаем адрес как имя контакта
        если мы указали валидатора - аналогично
        если мы указали черт знает то отправить ему мы не можем, соответственно пишем ошибку
         */
        val def = AddressContact()
        var defResult = Single.error<AddressContact?>(RuntimeException("Not found in address book"))
        def.name = nameOrAddress
        def.address = nameOrAddress
        if (nameOrAddress.matches(MinterAddress.ADDRESS_PATTERN.toRegex())) {
            def.type = AddressContact.AddressType.Address
            defResult = Single.just(def)
        } else if (nameOrAddress.matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())) {
            def.type = AddressContact.AddressType.ValidatorPubKey
            defResult = Single.just(def)
        }
        return db.addressBook().getByNameOrAddress(nameOrAddress)
                .switchIfEmpty(defResult)
                .subscribeOn(Schedulers.io())
    }

    fun findSuggestionsByNameOrAddress(nameOrAddress: String?): Maybe<List<AddressContact>> {
        val likeArg = String.format("%%%s%%", nameOrAddress)
        return db.addressBook().findByNameOrAddress(likeArg)
                .subscribeOn(Schedulers.io())
    }

    fun exist(contact: AddressContact): Single<Boolean> {
        if (contact.id == 0) {
            return Single.just(false)
        }

        return db.addressBook().exists(contact.id)
                .map { it == 1 }
    }

}