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
package network.minter.bipwallet.internal.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.db.WalletDatabase

@Module
class DbModule {
    @Provides
    @WalletApp
    fun provideWalletDB(context: Context? /*, @DbMigration Set<Migration> migrationSet*/): WalletDatabase {
        /* Enable when migrations will be required
        final Migration[] migrations = new Migration[migrationSet.size()];
        int i = 0;
        for (Migration m : migrationSet) {
            migrations[i] = m;
        }
         */
        return Room.databaseBuilder(context!!, WalletDatabase::class.java, DB_NAME) //                .addMigrations(migrations)
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    @WalletApp
    fun provideAddressBookRepository(db: WalletDatabase, validatorsRepo: RepoValidators): AddressBookRepository {
        return AddressBookRepository(db, validatorsRepo)
    }

    companion object {
        const val DB_NAME = "minter.db"
        const val DB_VERSION = 1
    }
}