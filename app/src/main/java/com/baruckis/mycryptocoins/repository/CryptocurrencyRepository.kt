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

package com.baruckis.mycryptocoins.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.baruckis.mycryptocoins.api.ApiResponse
import com.baruckis.mycryptocoins.api.ApiService
import com.baruckis.mycryptocoins.api.CoinMarketCap
import com.baruckis.mycryptocoins.api.CryptocurrencyLatest
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.data.CryptocurrencyDao
import com.baruckis.mycryptocoins.utilities.AbsentLiveData
import com.baruckis.mycryptocoins.vo.Resource
import javax.inject.Inject
import javax.inject.Singleton


/**
 * The class for managing multiple data sources.
 */
@Singleton
class CryptocurrencyRepository @Inject constructor(
        private val appExecutors: AppExecutors,
        private val cryptocurrencyDao: CryptocurrencyDao,
        private val api: ApiService
) {

    fun getMyCryptocurrencyLiveDataList(): LiveData<List<Cryptocurrency>> {
        return cryptocurrencyDao.getMyCryptocurrencyLiveDataList()
    }

    fun getSpecificCryptocurrencyLiveData(specificCryptoCode: String): LiveData<Cryptocurrency> {
        return cryptocurrencyDao.getSpecificCryptocurrencyLiveData(specificCryptoCode)
    }


    // The Resource wrapping of LiveData is useful to update the UI based upon the state.
    fun getAllCryptocurrencyLiveDataList(shouldFetch: Boolean = false): LiveData<Resource<List<Cryptocurrency>>> {
        return object : NetworkBoundResource<List<Cryptocurrency>, CoinMarketCap<List<CryptocurrencyLatest>>>(appExecutors) {

            // Here we save the data fetched from web-service.
            override fun saveCallResult(item: CoinMarketCap<List<CryptocurrencyLatest>>) {

                val allCryptocurrencyList: MutableList<Cryptocurrency> = ArrayList<Cryptocurrency>()

                item.data?.forEach {
                    val cryptocurrency = Cryptocurrency(it.name, it.cmcRank.toShort(),
                            0.0, it.symbol, "EUR", it.quote.currency.price,
                            0.0, it.quote.currency.percentChange1h,
                            it.quote.currency.percentChange7d, it.quote.currency.percentChange24h,
                            0.0)
                    allCryptocurrencyList.add(cryptocurrency)
                }

                cryptocurrencyDao.insertDataToAllCryptocurrencyList(allCryptocurrencyList)
            }

            // Returns boolean indicating if to fetch data from web or not, true means fetch the data from web.
            override fun shouldFetch(data: List<Cryptocurrency>?): Boolean {
                return data == null || shouldFetch
            }

            // Contains the logic to get data from the Room database.
            override fun loadFromDb(): LiveData<List<Cryptocurrency>> {

                return Transformations.switchMap(cryptocurrencyDao.getAllCryptocurrencyLiveDataList()) { data ->
                    if (data.isEmpty()) {
                        AbsentLiveData.create()
                    } else {
                        cryptocurrencyDao.getAllCryptocurrencyLiveDataList()
                    }
                }
            }

            // Contains the logic to get data from web-service using Retrofit.
            override fun createCall(): LiveData<ApiResponse<CoinMarketCap<List<CryptocurrencyLatest>>>> = api.getAllCryptocurrencies("EUR")

        }.asLiveData()
    }

}