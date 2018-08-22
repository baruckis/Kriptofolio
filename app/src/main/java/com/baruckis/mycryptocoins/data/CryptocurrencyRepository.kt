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

/**
 * The class for managing multiple data sources.
 */
class CryptocurrencyRepository private constructor(
        private val cryptocurrencyDao: CryptocurrencyDao
) {

    fun getMyCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>> {
        return cryptocurrencyDao.getMyCryptocurrencyLiveDataList()
    }

    fun getAllCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>> {
        return cryptocurrencyDao.getAllCryptocurrencyLiveDataList()
    }

    fun getSpecificCryptocurrencyLiveData(specificCryptoCode: String): LiveData<Cryptocurrency> {
        return cryptocurrencyDao.getSpecificCryptocurrencyLiveData(specificCryptoCode)
    }


    companion object {

        // Marks the JVM backing field of the annotated property as volatile, meaning that writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: CryptocurrencyRepository? = null

        // For Singleton instantiation.
        fun getInstance(cryptocurrencyDao: CryptocurrencyDao) =
                instance ?: synchronized(this) {
                    instance
                            ?: CryptocurrencyRepository(cryptocurrencyDao).also { instance = it }
                }
    }
}