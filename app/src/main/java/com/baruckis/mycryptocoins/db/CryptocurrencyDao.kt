/*
 * Copyright 2018-2019 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baruckis.mycryptocoins.db

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * The Data Access Object for the [Cryptocurrency] class.
 */
@Dao
abstract class CryptocurrencyDao {


    @Query("SELECT * FROM all_cryptocurrencies ORDER BY rank ASC")
    abstract fun getAllCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>>

    /**
     * Delete all of objects from the database.
     */
    @Query("DELETE FROM all_cryptocurrencies")
    abstract fun deleteAll()

    /**
     * Insert an array of objects in the database.
     *
     * @param itemList the objects list to be inserted.
     * @return The SQLite row ids.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertCryptocurrencyList(itemList: List<Cryptocurrency>): List<Long>


    @Transaction
    open fun reloadCryptocurrencyList(itemList: List<Cryptocurrency>) {
        deleteAll()
        insertCryptocurrencyList(itemList)
    }


    // The LIKE operator does a pattern matching comparison. The operand to the right contains the
    // pattern, the left hand operand contains the string to match against the pattern. A percent
    // symbol ("%") in the pattern matches any sequence of zero or more characters in the string.
    // An underscore ("_") in the pattern matches any single character in the string.
    @Query("SELECT * FROM all_cryptocurrencies WHERE name LIKE :searchText OR symbol LIKE :searchText ORDER BY rank ASC")
    abstract fun getCryptocurrencyLiveDataListBySearch(searchText: String): LiveData<List<Cryptocurrency>>


    // LIMIT clause is used to constrain the number of rows returned by the query.
    // We need just one as we search for exact specific cryptocurrency by code.
    @Query("SELECT * FROM all_cryptocurrencies WHERE symbol = :specificCryptoCode LIMIT 1")
    abstract fun getSpecificCryptocurrencyLiveDataByCryptoCode(specificCryptoCode: String): LiveData<Cryptocurrency>

}