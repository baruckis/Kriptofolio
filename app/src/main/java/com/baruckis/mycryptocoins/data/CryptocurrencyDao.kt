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

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

/**
 * The Data Access Object for the [Cryptocurrency] class.
 */
@Dao
interface CryptocurrencyDao {

    @Query("SELECT * FROM cryptocurrencies WHERE amount > 0")
    fun getMyCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>>

    @Query("SELECT * FROM cryptocurrencies")
    fun getAllCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>>

    @Query("SELECT * FROM cryptocurrencies WHERE symbol = :specificCryptoCode")
    fun getSpecificCryptocurrencyLiveData(specificCryptoCode: String): LiveData<Cryptocurrency>

    @Insert
    fun insertDataToAllCryptocurrencyList(data: List<Cryptocurrency>)
}