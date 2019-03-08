/*
 * Copyright 2018-2019 Andrius Baruckis www.baruckis.com | kriptofolio.app
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

package com.baruckis.kriptofolio.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.baruckis.kriptofolio.utilities.getAmountFiatChange24hCounted
import com.baruckis.kriptofolio.utilities.getAmountFiatCounted


/**
 * The Data Access Object for the [MyCryptocurrency] class.
 */
@Dao
abstract class MyCryptocurrencyDao {

    @Query("SELECT * FROM my_cryptocurrencies WHERE amount IS NOT NULL ORDER BY amount_fiat DESC, rank ASC")
    abstract fun getMyCryptocurrencyLiveDataList(): LiveData<List<MyCryptocurrency>>

    // The GROUP_CONCAT(X,Y) function returns a string which is the concatenation of all non-NULL
    // values of X. If parameter Y is present then it is used as the separator between instances
    // of X. A comma (",") is used as the separator if Y is omitted. The order of the concatenated
    // elements is arbitrary.
    @Query("SELECT GROUP_CONCAT(id) FROM my_cryptocurrencies WHERE amount IS NOT NULL")
    abstract fun getMyCryptocurrencyIds(): String


    @Query("SELECT * FROM my_cryptocurrencies")
    abstract fun getMyCryptocurrencyList(): List<MyCryptocurrency>


    @Query("SELECT * FROM my_cryptocurrencies WHERE id = :id LIMIT 1")
    abstract fun getSpecificCryptocurrencyById(id: Int): MyCryptocurrency


    // Update specific item in the database.
    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateCryptocurrency(myCryptocurrency: MyCryptocurrency): Int


    // Update specific items in the database.
    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateCryptocurrencyList(myCryptocurrencyList: List<MyCryptocurrency>): Int


    /**
     * Insert an array of objects in the database.
     *
     * @param itemList the objects list to be inserted.
     * @return The SQLite row ids.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertCryptocurrencyList(itemList: List<MyCryptocurrency>): List<Long>


    // Update database table or insert if rows do not exist.
    @Transaction
    open fun upsert(itemList: List<MyCryptocurrency>, updateAmount: Boolean = false) {
        // First try to insert all items. Here you get the id for each item as return value from
        // insert operation with IGNORE as a OnConflictStrategy if it was successful.
        val insertResult = insertCryptocurrencyList(itemList)
        val updateList = ArrayList<MyCryptocurrency>()

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

                val currentItem = getSpecificCryptocurrencyById(updateItem.myId)

                currentItem.cryptoData.name = updateItem.cryptoData.name
                currentItem.cryptoData.rank = updateItem.cryptoData.rank
                currentItem.cryptoData.symbol = updateItem.cryptoData.symbol
                currentItem.cryptoData.currencyFiat = updateItem.cryptoData.currencyFiat
                currentItem.cryptoData.priceFiat = updateItem.cryptoData.priceFiat
                currentItem.cryptoData.pricePercentChange1h = updateItem.cryptoData.pricePercentChange1h
                currentItem.cryptoData.pricePercentChange7d = updateItem.cryptoData.pricePercentChange7d
                currentItem.cryptoData.pricePercentChange24h = updateItem.cryptoData.pricePercentChange24h
                currentItem.cryptoData.lastFetchedDate = updateItem.cryptoData.lastFetchedDate

                if (updateAmount) {
                    currentItem.amount = updateItem.amount
                }

                if (currentItem.amount != null) {
                    currentItem.amountFiat =
                            getAmountFiatCounted(currentItem.amount, currentItem.cryptoData.priceFiat)
                    currentItem.amountFiatChange24h =
                            getAmountFiatChange24hCounted(currentItem.amountFiat, currentItem.cryptoData.pricePercentChange24h)
                }

                updateCryptocurrency(currentItem)
            }
        }
    }


    /**
     * Delete an array of objects from the database.
     *
     * @param itemList the objects list to be deleted.
     */
    @Delete
    abstract fun deleteCryptocurrencyList(itemList: List<MyCryptocurrency>)


    /**
     * Update my crypto currency list from database with provided data where they match by id.
     *
     * @param cryptocurrencyList the objects list to be used for update matches.
     */
    @Transaction
    open fun reloadMyCryptocurrencyList(cryptocurrencyList: List<Cryptocurrency>) {

        val myCryptocurrencyList = getMyCryptocurrencyList()

        myCryptocurrencyList.forEach { myCryptocurrency ->
            val cryptocurrency = cryptocurrencyList.find { cryptocurrency -> cryptocurrency.id == myCryptocurrency.myId }
            cryptocurrency?.let {
                myCryptocurrency.cryptoData = it
                myCryptocurrency.amountFiat =
                        getAmountFiatCounted(myCryptocurrency.amount, myCryptocurrency.cryptoData.priceFiat)
                myCryptocurrency.amountFiatChange24h =
                        getAmountFiatChange24hCounted(myCryptocurrency.amountFiat, myCryptocurrency.cryptoData.pricePercentChange24h)
            }
        }

        if (!myCryptocurrencyList.isEmpty()) updateCryptocurrencyList(myCryptocurrencyList)
    }

}