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

package com.baruckis.mycryptocoins.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.api.*
import com.baruckis.mycryptocoins.db.Cryptocurrency
import com.baruckis.mycryptocoins.db.CryptocurrencyDao
import com.baruckis.mycryptocoins.db.MyCryptocurrency
import com.baruckis.mycryptocoins.db.MyCryptocurrencyDao
import com.baruckis.mycryptocoins.utilities.AbsentLiveData
import com.baruckis.mycryptocoins.utilities.stringLiveData
import com.baruckis.mycryptocoins.vo.Resource
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * The class for managing multiple data sources.
 */
@Singleton
class CryptocurrencyRepository @Inject constructor(
        private val context: Context,
        private val appExecutors: AppExecutors,
        private val myCryptocurrencyDao: MyCryptocurrencyDao,
        private val cryptocurrencyDao: CryptocurrencyDao,
        private val api: ApiService,
        private val sharedPreferences: SharedPreferences
) {

    // Just a simple helper variable to store selected fiat currency code during app lifecycle.
    // It is needed for main screen currency spinner. We set it to be same as in shared preferences.
    var selectedFiatCurrencyCode: String = getCurrentFiatCurrencyCode()


    fun getMyCryptocurrencyLiveDataResourceList(fiatCurrencyCode: String, shouldFetch: Boolean = false, myCryptocurrenciesIds: String? = null, callDelay: Long = 0): LiveData<Resource<List<MyCryptocurrency>>> {
        return object : NetworkBoundResource<List<MyCryptocurrency>, CoinMarketCap<HashMap<String, CryptocurrencyLatest>>>(appExecutors) {

            override fun saveCallResult(item: CoinMarketCap<HashMap<String, CryptocurrencyLatest>>) {

                val list: MutableList<CryptocurrencyLatest> = ArrayList()

                // We iterate over hashmap to make a list of CryptocurrencyLatest.
                if (!item.data.isNullOrEmpty()) {
                    for ((_, value) in item.data) {
                        list.add(value)
                    }
                }

                // Than we use common function to convert list response to the list compatible with
                // our created upsert function in dao.
                myCryptocurrencyDao.upsert(getMyCryptocurrencyListFromResponse(fiatCurrencyCode, list, item.status?.timestamp))
            }

            override fun shouldFetch(data: List<MyCryptocurrency>?): Boolean {
                return shouldFetch
            }

            override fun fetchDelayMillis(): Long {
                return callDelay
            }

            override fun loadFromDb(): LiveData<List<MyCryptocurrency>> {
                return myCryptocurrencyDao.getMyCryptocurrencyLiveDataList()
            }

            override fun createCall(): LiveData<ApiResponse<CoinMarketCap<HashMap<String, CryptocurrencyLatest>>>> {
                // Make a server call for to get new data by ids provided.
                return if (!myCryptocurrenciesIds.isNullOrEmpty())
                    api.getCryptocurrenciesById(fiatCurrencyCode, myCryptocurrenciesIds) else
                // Special case when user cryptocurrencies list is empty.
                    MediatorLiveData<ApiResponse<CoinMarketCap<HashMap<String, CryptocurrencyLatest>>>>().apply { value = ApiEmptyResponse() }
            }

        }.asLiveData()
    }


    fun getMyCryptocurrencyLiveDataList(): LiveData<List<MyCryptocurrency>> {
        return myCryptocurrencyDao.getMyCryptocurrencyLiveDataList()
    }

    fun getMyCryptocurrencyList(): List<MyCryptocurrency>? {
        return myCryptocurrencyDao.getMyCryptocurrencyList()
    }


    fun getMyCryptocurrencyIds(): String? {
        return myCryptocurrencyDao.getMyCryptocurrencyIds()
    }


    // The Resource wrapping of LiveData is useful to update the UI based upon the state.
    fun getAllCryptocurrencyLiveDataResourceList(fiatCurrencyCode: String, shouldFetch: Boolean = false, callDelay: Long = 0): LiveData<Resource<List<Cryptocurrency>>> {
        return object : NetworkBoundResource<List<Cryptocurrency>, CoinMarketCap<List<CryptocurrencyLatest>>>(appExecutors) {

            // Here we save the data fetched from web-service.
            override fun saveCallResult(item: CoinMarketCap<List<CryptocurrencyLatest>>) {

                val list = getCryptocurrencyListFromResponse(fiatCurrencyCode, item.data, item.status?.timestamp)

                cryptocurrencyDao.reloadCryptocurrencyList(list)
                myCryptocurrencyDao.reloadMyCryptocurrencyList(list)
            }

            // Returns boolean indicating if to fetch data from web or not, true means fetch the data from web.
            override fun shouldFetch(data: List<Cryptocurrency>?): Boolean {
                return data == null || shouldFetch
            }

            override fun fetchDelayMillis(): Long {
                return callDelay
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


    fun getCryptocurrencyLiveDataListBySearch(searchText: String): LiveData<List<Cryptocurrency>> {
        return cryptocurrencyDao.getCryptocurrencyLiveDataListBySearch(searchText)
    }


    fun getSpecificCryptocurrencyLiveData(specificCryptoCode: String): LiveData<Cryptocurrency> {
        return cryptocurrencyDao.getSpecificCryptocurrencyLiveDataByCryptoCode(specificCryptoCode)
    }


    fun upsertMyCryptocurrency(myCryptocurrency: MyCryptocurrency) {
        myCryptocurrencyDao.upsert(listOf(myCryptocurrency), true)
    }

    fun insertMyCryptocurrencyList(myCryptocurrencyList: List<MyCryptocurrency>): List<Long> {
        return myCryptocurrencyDao.insertCryptocurrencyList(myCryptocurrencyList)
    }

    fun deleteMyCryptocurrencyList(myCryptocurrencyList: List<MyCryptocurrency>) {
        myCryptocurrencyDao.deleteCryptocurrencyList(myCryptocurrencyList)
    }


    fun setNewCurrentFiatCurrencyCode(value: String) {
        selectedFiatCurrencyCode = value
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


    private fun getMyCryptocurrencyListFromResponse(fiatCurrencyCode: String, responseList: List<CryptocurrencyLatest>?, timestamp: Date?): ArrayList<MyCryptocurrency> {

        val myCryptocurrencyList: MutableList<MyCryptocurrency> = ArrayList()

        responseList?.forEach {
            val cryptocurrency = Cryptocurrency(it.id, it.name, it.cmcRank.toShort(),
                    it.symbol, fiatCurrencyCode, it.quote.currency.price,
                    it.quote.currency.percentChange1h,
                    it.quote.currency.percentChange7d, it.quote.currency.percentChange24h, timestamp)
            val myCryptocurrency = MyCryptocurrency(it.id, cryptocurrency)
            myCryptocurrencyList.add(myCryptocurrency)
        }

        return myCryptocurrencyList as ArrayList<MyCryptocurrency>
    }


    private fun getCryptocurrencyListFromResponse(fiatCurrencyCode: String, responseList: List<CryptocurrencyLatest>?, timestamp: Date?): ArrayList<Cryptocurrency> {

        val cryptocurrencyList: MutableList<Cryptocurrency> = ArrayList()

        responseList?.forEach {
            val cryptocurrency = Cryptocurrency(it.id, it.name, it.cmcRank.toShort(),
                    it.symbol, fiatCurrencyCode, it.quote.currency.price,
                    it.quote.currency.percentChange1h,
                    it.quote.currency.percentChange7d, it.quote.currency.percentChange24h, timestamp)
            cryptocurrencyList.add(cryptocurrency)
        }

        return cryptocurrencyList as ArrayList<Cryptocurrency>
    }

}