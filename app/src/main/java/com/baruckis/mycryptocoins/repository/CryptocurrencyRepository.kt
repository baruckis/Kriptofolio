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
import com.baruckis.mycryptocoins.data.ScreenStatus
import com.baruckis.mycryptocoins.data.ScreenStatusDao
import com.baruckis.mycryptocoins.utilities.AbsentLiveData
import com.baruckis.mycryptocoins.utilities.DB_ID_SCREEN_ADD_SEARCH_LIST
import com.baruckis.mycryptocoins.utilities.DB_ID_SCREEN_MAIN_LIST
import com.baruckis.mycryptocoins.vo.Resource
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList


/**
 * The class for managing multiple data sources.
 */
@Singleton
class CryptocurrencyRepository @Inject constructor(
        private val appExecutors: AppExecutors,
        private val cryptocurrencyDao: CryptocurrencyDao,
        private val screenStatusDao: ScreenStatusDao,
        private val api: ApiService
) {

    fun getMyCryptocurrencyLiveDataResourceList(shouldFetch: Boolean = false, myCryptocurrenciesIds: String? = null): LiveData<Resource<List<Cryptocurrency>>> {
        return object : NetworkBoundResource<List<Cryptocurrency>, CoinMarketCap<HashMap<String, CryptocurrencyLatest>>>(appExecutors) {

            override fun saveCallResult(item: CoinMarketCap<HashMap<String, CryptocurrencyLatest>>) {

                val list: MutableList<CryptocurrencyLatest> = ArrayList<CryptocurrencyLatest>()

                // We iterate over hashmap to make a list of CryptocurrencyLatest.
                if (!item.data.isNullOrEmpty()) {
                    for ((key, value) in item.data) {
                        list.add(value)
                    }
                }

                // Than we use common function to convert list response to the list compatible with
                // our created upsert function in dao.
                cryptocurrencyDao.upsert(getCryptocurrencyListFromResponse(list))

                if (item.status != null) {
                    screenStatusDao.update(ScreenStatus(DB_ID_SCREEN_MAIN_LIST, item.status.timestamp))
                }
            }

            override fun shouldFetch(data: List<Cryptocurrency>?): Boolean {
                return shouldFetch
            }

            override fun loadFromDb(): LiveData<List<Cryptocurrency>> {
                return cryptocurrencyDao.getMyCryptocurrencyLiveDataList()
            }

            override fun createCall(): LiveData<ApiResponse<CoinMarketCap<HashMap<String, CryptocurrencyLatest>>>> {
                return if (!myCryptocurrenciesIds.isNullOrEmpty()) api.getCryptocurrenciesById("EUR", myCryptocurrenciesIds) else AbsentLiveData.create()
            }

        }.asLiveData()
    }


    fun getMyCryptocurrencyIds(): String? {
        return cryptocurrencyDao.getMyCryptocurrencyIds()
    }


    // The Resource wrapping of LiveData is useful to update the UI based upon the state.
    fun getAllCryptocurrencyLiveDataList(shouldFetch: Boolean = false): LiveData<Resource<List<Cryptocurrency>>> {
        return object : NetworkBoundResource<List<Cryptocurrency>, CoinMarketCap<List<CryptocurrencyLatest>>>(appExecutors) {

            // Here we save the data fetched from web-service.
            override fun saveCallResult(item: CoinMarketCap<List<CryptocurrencyLatest>>) {
                cryptocurrencyDao.upsert(getCryptocurrencyListFromResponse(item.data))

                if (item.status != null) {
                    screenStatusDao.update(ScreenStatus(DB_ID_SCREEN_ADD_SEARCH_LIST, item.status.timestamp))
                    screenStatusDao.update(ScreenStatus(DB_ID_SCREEN_MAIN_LIST, item.status.timestamp))
                }
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


    fun getSpecificCryptocurrencyLiveData(specificCryptoCode: String): LiveData<Cryptocurrency> {
        return cryptocurrencyDao.getSpecificCryptocurrencyLiveData(specificCryptoCode)
    }


    fun updateCryptocurrencyFromList(cryptocurrency: Cryptocurrency) {
        appExecutors.diskIO().execute {
            cryptocurrencyDao.updateCryptocurrency(cryptocurrency)
        }
    }


    fun getSpecificScreenStatusLiveData(specificScreenStatusId: String): LiveData<ScreenStatus> {
        return screenStatusDao.getSpecificScreenStatusLiveData(specificScreenStatusId)
    }


    private fun getCryptocurrencyListFromResponse(responseList: List<CryptocurrencyLatest>?): ArrayList<Cryptocurrency> {

        val cryptocurrencyList: MutableList<Cryptocurrency> = ArrayList<Cryptocurrency>()

        responseList?.forEach {
            val cryptocurrency = Cryptocurrency(it.id, it.name, it.cmcRank.toShort(),
                    null, it.symbol, "EUR", it.quote.currency.price,
                    null, it.quote.currency.percentChange1h,
                    it.quote.currency.percentChange7d, it.quote.currency.percentChange24h,
                    null)
            cryptocurrencyList.add(cryptocurrency)
        }

        return cryptocurrencyList as ArrayList<Cryptocurrency>
    }

}