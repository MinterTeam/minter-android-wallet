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

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Maybe;
import io.reactivex.Single;
import network.minter.bipwallet.addressbook.models.AddressContact;

@Dao
public interface AddressBookDao {

    @Query("SELECT * FROM minter_contacts ORDER BY name ASC")
    Maybe<List<AddressContact>> findAll();

    @Query("SELECT * FROM minter_contacts WHERE id = :id")
    Maybe<AddressContact> findById(int id);

    @Query("SELECT * FROM minter_contacts WHERE address = :address")
    Maybe<AddressContact> findByAddress(String address);

    /**
     * Get the count of contacts like passed name or address
     * @param nameOrAddress should be ENCLOSED with "%"
     * @return
     */
    @Query("SELECT COUNT(*) FROM minter_contacts WHERE address LIKE :nameOrAddress OR name LIKE :nameOrAddress")
    Single<Integer> countByNameOrAddress(String nameOrAddress);

    /**
     * Get the count of contacts like passed name or address
     * @param nameOrAddress should be ENCLOSED with "%"
     * @return
     */
    @Query("SELECT COUNT(*) FROM minter_contacts WHERE (name != :excludeName OR address != :excludeName) AND (address LIKE :nameOrAddress OR name LIKE :nameOrAddress)")
    Single<Integer> countByNameOrAddress(String nameOrAddress, String excludeName);

    /**
     * Get the count of contacts like passed name
     * @param name should be ENCLOSED with "%"
     * @return
     */
    @Query("SELECT COUNT(*) FROM minter_contacts WHERE (name != :excludeName) AND (name = :name)")
    Single<Integer> countByName(String name, String excludeName);

    /**
     * Get the count of contacts like passed name
     * @param name should be ENCLOSED with "%"
     * @return
     */
    @Query("SELECT COUNT(*) FROM minter_contacts WHERE (name = :name)")
    Single<Integer> countByName(String name);

    /**
     * Get the count of contacts like passed address
     * @param address should be ENCLOSED with "%"
     * @return
     */
    @Query("SELECT COUNT(*) FROM minter_contacts WHERE (address != :excludeAddress) AND (address = :address)")
    Single<Integer> countByAddress(String address, String excludeAddress);

    @Query("SELECT COUNT(*) FROM minter_contacts WHERE (address = :address)")
    Single<Integer> countByAddress(String address);

    @Query("SELECT * FROM minter_contacts WHERE address = :nameOrAddress OR name = :nameOrAddress")
    Maybe<AddressContact> getByNameOrAddress(String nameOrAddress);

    @Query("SELECT * FROM minter_contacts WHERE address LIKE :nameOrAddress OR name LIKE :nameOrAddress OR name = :nameOrAddress or address = :nameOrAddress GROUP BY id")
    Maybe<List<AddressContact>> findByNameOrAddress(String nameOrAddress);

    @Query("SELECT COUNT(*) FROM minter_contacts WHERE id=:id")
    Single<Integer> exists(int id);

    @Insert
    void insert(AddressContact contact);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insert(List<AddressContact> contactList);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(AddressContact contact);

    @Query("DELETE FROM minter_contacts WHERE id = :id")
    void deleteById(int id);

    @Delete
    void delete(AddressContact contact);

    @Query("DELETE FROM minter_contacts")
    void truncate();

}
