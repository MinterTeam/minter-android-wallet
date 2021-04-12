/*
 * Copyright (C) by MinterTeam. 2021
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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.db.WalletDatabase
import network.minter.bipwallet.internal.di.annotations.DbCache
import network.minter.bipwallet.internal.storage.KVStorage

@Module
class DbModule {
    @Provides
    @WalletApp
    fun provideWalletDB(context: Context?): WalletDatabase {

        val migration_v3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val add_mintable = "ALTER TABLE `minter_coins` ADD COLUMN `mintable` INTEGER DEFAULT 0"
                val add_burnable = "ALTER TABLE `minter_coins` ADD COLUMN `burnable` INTEGER DEFAULT 0"
                val add_type = "ALTER TABLE `minter_coins` ADD COLUMN `type` INTEGER DEFAULT 0"

                db.execSQL(add_mintable)
                db.execSQL(add_burnable)
                db.execSQL(add_type)
            }

        }

        val migration_v2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val createTable = """
                    CREATE TABLE IF NOT EXISTS `minter_coins` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `coinId` TEXT,
                    `symbol` TEXT,
                    `crr` INTEGER,
                    `reserveBalance` TEXT,
                    `maxSupply` TEXT,
                    `owner` TEXT
                    )
                """.trimIndent()
                val createIndex = """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_minter_coins_coinId_symbol` ON `minter_coins` (`coinId`, `symbol`)
                """.trimIndent()

                db.execSQL(createTable)
                db.execSQL(createIndex)
            }

        }

        return Room.databaseBuilder(context!!, WalletDatabase::class.java, DB_NAME)
                .addMigrations(migration_v2)
                .addMigrations(migration_v3)
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    @WalletApp
    fun provideAddressBookRepository(db: WalletDatabase, validatorsRepo: RepoValidators, @DbCache storage: KVStorage): AddressBookRepository {
        return AddressBookRepository(db, validatorsRepo, storage)
    }

    companion object {
        const val DB_NAME = "minter.db"
        const val DB_VERSION = 3
    }
}
