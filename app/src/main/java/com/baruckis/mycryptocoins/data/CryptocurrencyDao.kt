/*
 * Copyright 2018 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com
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

package com.baruckis.mycryptocoins.data

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * The Data Access Object for the [Cryptocurrency] class.
 */
@Dao
abstract class CryptocurrencyDao {

    @Query("SELECT * FROM cryptocurrencies WHERE amount IS NOT NULL")
    abstract fun getMyCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>>

    // The GROUP_CONCAT(X,Y) function returns a string which is the concatenation of all non-NULL
    // values of X. If parameter Y is present then it is used as the separator between instances
    // of X. A comma (",") is used as the separator if Y is omitted. The order of the concatenated
    // elements is arbitrary.
    @Query("SELECT GROUP_CONCAT(id) FROM cryptocurrencies WHERE amount IS NOT NULL")
    abstract fun getMyCryptocurrencyIds(): String

    @Query("SELECT * FROM cryptocurrencies ORDER BY rank ASC")
    abstract fun getAllCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>>

    // LIMIT clause is used to constrain the number of rows returned by the query.
    // We need just one as we search for exact specific cryptocurrency by code.
    @Query("SELECT * FROM cryptocurrencies WHERE symbol = :specificCryptoCode LIMIT 1")
    abstract fun getSpecificCryptocurrencyLiveData(specificCryptoCode: String): LiveData<Cryptocurrency>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateCryptocurrency(cryptocurrency: Cryptocurrency)


    /**
     * Insert an array of objects in the database.
     *
     * @param itemList the objects list to be inserted.
     * @return The SQLite row ids.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertCryptocurrencyList(itemList: List<Cryptocurrency>): List<Long>

    /**
     * Update individual fields for specific item in the database.
     *
     * @param id the item unique primary key.
     */
    @Query("UPDATE cryptocurrencies SET name = :name, rank = :rank, symbol = :symbol, currencyFiat = :currencyFiat, priceFiat = :priceFiat, pricePercentChange1h = :pricePercentChange1h, pricePercentChange7d = :pricePercentChange7d, pricePercentChange24h = :pricePercentChange24h WHERE id = :id")
    abstract fun updateCryptocurrencyFields(id: Int,
                                            name: String,
                                            rank: Short,
                                            symbol: String,
                                            currencyFiat: String,
                                            priceFiat: Double,
                                            pricePercentChange1h: Double,
                                            pricePercentChange7d: Double,
                                            pricePercentChange24h: Double)

    // Update database table or insert if rows do not exist.
    @Transaction
    open fun upsert(itemList: List<Cryptocurrency>) {
        // First try to insert all items. Here you get the id for each item as return value from
        // insert operation with IGNORE as a OnConflictStrategy if it was successful.
        val insertResult = insertCryptocurrencyList(itemList)
        val updateList = ArrayList<Cryptocurrency>()

        for (i in insertResult.indices) {
            // If it equals to -1 then it means row wasn't inserted and needs to be updated.
            if (insertResult[i] == -1L) {
                // So we add such item to separate update list.
                updateList.add(itemList[i])
            }
        }

        // Than we check if we there is anything to update.
        if (!updateList.isEmpty()) {
            // Finally we update items one by one.
            for (item in updateList) {
                // We update individual fields for each item.
                updateCryptocurrencyFields(item.id, item.name, item.rank, item.symbol, item.currencyFiat, item.priceFiat, item.pricePercentChange1h, item.pricePercentChange7d, item.pricePercentChange24h)
            }
        }
    }

}