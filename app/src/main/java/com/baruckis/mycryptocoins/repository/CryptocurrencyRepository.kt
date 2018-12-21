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

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.api.ApiResponse
import com.baruckis.mycryptocoins.api.ApiService
import com.baruckis.mycryptocoins.api.CoinMarketCap
import com.baruckis.mycryptocoins.api.CryptocurrencyLatest
import com.baruckis.mycryptocoins.db.Cryptocurrency
import com.baruckis.mycryptocoins.db.CryptocurrencyDao
import com.baruckis.mycryptocoins.db.ScreenStatus
import com.baruckis.mycryptocoins.db.ScreenStatusDao
import com.baruckis.mycryptocoins.utilities.AbsentLiveData
import com.baruckis.mycryptocoins.utilities.DB_ID_SCREEN_ADD_SEARCH_LIST
import com.baruckis.mycryptocoins.utilities.DB_ID_SCREEN_MAIN_LIST
import com.baruckis.mycryptocoins.utilities.stringLiveData
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
        private val context: Context,
        private val appExecutors: AppExecutors,
        private val cryptocurrencyDao: CryptocurrencyDao,
        private val screenStatusDao: ScreenStatusDao,
        private val api: ApiService,
        private val sharedPreferences: SharedPreferences
) {

    // Just a simple helper variable to store selected fiat currency code during app lifecycle.
    // It is needed for main screen currency spinner.
    var selectedFiatCurrencyCode: String? = null


    fun getMyCryptocurrencyLiveDataResourceList(fiatCurrencyCode: String, shouldFetch: Boolean = false, myCryptocurrenciesIds: String? = null): LiveData<Resource<List<Cryptocurrency>>> {
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
                cryptocurrencyDao.upsert(getCryptocurrencyListFromResponse(fiatCurrencyCode, list))

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
                return if (!myCryptocurrenciesIds.isNullOrEmpty()) api.getCryptocurrenciesById(fiatCurrencyCode, myCryptocurrenciesIds) else AbsentLiveData.create()
            }

        }.asLiveData()
    }


    fun getMyCryptocurrencyIds(): String? {
        return cryptocurrencyDao.getMyCryptocurrencyIds()
    }


    // The Resource wrapping of LiveData is useful to update the UI based upon the state.
    fun getAllCryptocurrencyLiveDataList(fiatCurrencyCode: String, shouldFetch: Boolean = false): LiveData<Resource<List<Cryptocurrency>>> {
        return object : NetworkBoundResource<List<Cryptocurrency>, CoinMarketCap<List<CryptocurrencyLatest>>>(appExecutors) {

            // Here we save the data fetched from web-service.
            override fun saveCallResult(item: CoinMarketCap<List<CryptocurrencyLatest>>) {
                cryptocurrencyDao.upsert(getCryptocurrencyListFromResponse(fiatCurrencyCode, item.data))

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
            override fun createCall(): LiveData<ApiResponse<CoinMarketCap<List<CryptocurrencyLatest>>>> = api.getAllCryptocurrencies(fiatCurrencyCode)

        }.asLiveData()
    }


    fun getSpecificCryptocurrencyLiveData(specificCryptoCode: String): LiveData<Cryptocurrency> {
        return cryptocurrencyDao.getSpecificCryptocurrencyLiveDataByCryptoCode(specificCryptoCode)
    }


    fun updateCryptocurrencyFromList(cryptocurrency: Cryptocurrency) {
        appExecutors.diskIO().execute {
            cryptocurrencyDao.updateCryptocurrency(cryptocurrency)
        }
    }

    fun updateCryptocurrency(cryptocurrency: Cryptocurrency): Int? {
        return cryptocurrencyDao.updateCryptocurrency(cryptocurrency)
    }


    fun getSpecificScreenStatusLiveData(specificScreenStatusId: String): LiveData<ScreenStatus> {
        return screenStatusDao.getSpecificScreenStatusLiveData(specificScreenStatusId)
    }

    fun getSpecificScreenStatusData(specificScreenStatusId: String): ScreenStatus {
        return screenStatusDao.getSpecificScreenStatusData(specificScreenStatusId)
    }


    fun setNewCurrentFiatCurrencyCode(value: String) {
        sharedPreferences.edit().putString(context.resources.getString(R.string.pref_fiat_currency_key), value).apply()
    }

    fun getCurrentFiatCurrencyCode(): String {
        return sharedPreferences.getString(context.resources.getString(R.string.pref_fiat_currency_key), context.resources.getString(R.string.pref_default_fiat_currency_value))
                ?: context.resources.getString(R.string.pref_default_fiat_currency_value)
    }

    fun getCurrentFiatCurrencyCodeLiveData(): LiveData<String> {
        return sharedPreferences.stringLiveData(context.resources.getString(R.string.pref_fiat_currency_key), context.resources.getString(R.string.pref_default_fiat_currency_value))
    }


    fun getCurrentFiatCurrencySign(fiatCurrencyCode: String): String {

        val fiatCurrencySymbols: HashMap<String, String> = HashMap()

        val keys = context.resources.getStringArray(R.array.fiat_currency_code_array)
        val values = context.resources.getStringArray(R.array.fiat_currency_sign_array)

        for (i in 0 until Math.min(keys.size, values.size)) {
            fiatCurrencySymbols.put(keys[i], values[i])
        }

        return fiatCurrencySymbols.asSequence().filter { it.key == fiatCurrencyCode }.first().value
    }

    fun getCurrentFiatCurrencySignLiveData(): LiveData<String> {
        return Transformations.switchMap(getCurrentFiatCurrencyCodeLiveData()) { data ->
            MutableLiveData<String>().takeIf { data != null }?.apply { value = getCurrentFiatCurrencySign(data) }
        }
    }


    private fun getCryptocurrencyListFromResponse(fiatCurrencyCode: String, responseList: List<CryptocurrencyLatest>?): ArrayList<Cryptocurrency> {

        val cryptocurrencyList: MutableList<Cryptocurrency> = ArrayList<Cryptocurrency>()

        responseList?.forEach {
            val cryptocurrency = Cryptocurrency(it.id, it.name, it.cmcRank.toShort(),
                    null, it.symbol, fiatCurrencyCode, it.quote.currency.price,
                    null, it.quote.currency.percentChange1h,
                    it.quote.currency.percentChange7d, it.quote.currency.percentChange24h,
                    null)
            cryptocurrencyList.add(cryptocurrency)
        }

        return cryptocurrencyList as ArrayList<Cryptocurrency>
    }

}