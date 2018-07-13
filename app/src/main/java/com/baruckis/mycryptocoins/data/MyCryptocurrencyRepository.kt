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
class MyCryptocurrencyRepository private constructor(
        private val myCryptocurrencyDao: MyCryptocurrencyDao
) {

    fun getMyCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>> {
        return myCryptocurrencyDao.getMyCryptocurrencyLiveDataList()
    }

    companion object {

        // Marks the JVM backing field of the annotated property as volatile, meaning that writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: MyCryptocurrencyRepository? = null

        // For Singleton instantiation.
        fun getInstance(myCryptocurrencyDao: MyCryptocurrencyDao) =
                instance ?: synchronized(this) {
                    instance
                            ?: MyCryptocurrencyRepository(myCryptocurrencyDao).also { instance = it }
                }
    }
}