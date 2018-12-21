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

package com.baruckis.mycryptocoins.db

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
    abstract fun getSpecificCryptocurrencyLiveDataByCryptoCode(specificCryptoCode: String): LiveData<Cryptocurrency>

    @Query("SELECT * FROM cryptocurrencies WHERE id = :id LIMIT 1")
    abstract fun getSpecificCryptocurrencyLiveDataById(id: Int): Cryptocurrency

    // Update specific item in the database.
    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateCryptocurrency(cryptocurrency: Cryptocurrency): Int


    /**
     * Insert an array of objects in the database.
     *
     * @param itemList the objects list to be inserted.
     * @return The SQLite row ids.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertCryptocurrencyList(itemList: List<Cryptocurrency>): List<Long>


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
            for (updateItem in updateList) {

                val currentItem = getSpecificCryptocurrencyLiveDataById(updateItem.id)

                currentItem.name = updateItem.name
                currentItem.rank = updateItem.rank
                currentItem.symbol = updateItem.symbol
                currentItem.currencyFiat = updateItem.currencyFiat
                currentItem.priceFiat = updateItem.priceFiat
                currentItem.pricePercentChange1h = updateItem.pricePercentChange1h
                currentItem.pricePercentChange7d = updateItem.pricePercentChange7d
                currentItem.pricePercentChange24h = updateItem.pricePercentChange24h

                if (currentItem.amount != null) {
                    currentItem.amountFiat = currentItem.amount!! * updateItem.priceFiat
                    currentItem.amountFiatChange24h = currentItem.amountFiat!! * (updateItem.pricePercentChange24h / (100))
                }

                updateCryptocurrency(currentItem)
            }
        }
    }

}