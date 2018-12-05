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

package com.baruckis.mycryptocoins.ui.mainlist

import android.content.Context
import android.preference.PreferenceManager
import android.text.SpannableString
import androidx.lifecycle.*
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.repository.CryptocurrencyRepository
import com.baruckis.mycryptocoins.utilities.*
import com.baruckis.mycryptocoins.vo.Resource
import kotlinx.coroutines.*
import javax.inject.Inject


/**
 * The ViewModel class is designed to store and manage UI-related data in a lifecycle conscious way.
 * The ViewModel class allows data to survive configuration changes such as screen rotations.
 */

// ViewModel will require a CryptocurrencyRepository so we add @Inject code into ViewModel constructor.
class MainViewModel @Inject constructor(context: Context, private val cryptocurrencyRepository: CryptocurrencyRepository) : ViewModel() {

    private var currentCryptoCurrencyCode: String
    private var currentCryptoCurrencySign: String
    private var currentFiatCurrencyCode: String
    var currentFiatCurrencySign: String

    private val liveDataCurrentCryptocurrency: LiveData<Cryptocurrency>
    private val liveDataTotalHoldingsValueFiat: LiveData<Double>
    private val liveDataTotalHoldingsValueCrypto: LiveData<Double>
    private val liveDataTotalHoldingsValueFiat24h: LiveData<Double>

    val mediatorLiveDataMyCryptocurrencyList = MediatorLiveData<Resource<List<Cryptocurrency>>>()
    private var liveDataMyCryptocurrencyList: LiveData<Resource<List<Cryptocurrency>>>

    val liveDataTotalHoldingsValueOnDateText: LiveData<String>
    val liveDataTotalHoldingsValueFiat24hText: LiveData<SpannableString>
    val liveDataTotalHoldingsValueCryptoText: LiveData<String>
    val liveDataTotalHoldingsValueFiatText: LiveData<String>


    init {
        currentCryptoCurrencyCode = context.getString(R.string.default_crypto_code)
        currentCryptoCurrencySign = context.getString(R.string.default_crypto_sign)

        currentFiatCurrencyCode = PreferenceManager.getDefaultSharedPreferences(context).getString(context.resources.getString(R.string.pref_fiat_currency_key), context.resources.getString(R.string.pref_default_fiat_currency_value))!!
        currentFiatCurrencySign = getSupportedFiatCurrencySymbols(context).asSequence().filter { it.key.equals(currentFiatCurrencyCode) }.first().value


        liveDataMyCryptocurrencyList = cryptocurrencyRepository.getMyCryptocurrencyLiveDataResourceList()

        mediatorLiveDataMyCryptocurrencyList.addSource(liveDataMyCryptocurrencyList) { mediatorLiveDataMyCryptocurrencyList.value = it }


        liveDataCurrentCryptocurrency = cryptocurrencyRepository.getSpecificCryptocurrencyLiveData(currentCryptoCurrencyCode)

        // swithMap returns a new LiveData object rather than a value, i.e. it switches the actual LiveData for a new one.
        liveDataTotalHoldingsValueFiat24h = Transformations.switchMap(liveDataMyCryptocurrencyList) { _ -> MutableLiveData<Double>().apply { value = liveDataMyCryptocurrencyList.value?.data?.sumByDouble { it.amountFiatChange24h ?: 0.0 } ?: 0.0 } }
        liveDataTotalHoldingsValueFiat = Transformations.switchMap(liveDataMyCryptocurrencyList) { _ -> MutableLiveData<Double>().apply { value = liveDataMyCryptocurrencyList.value?.data?.sumByDouble { it.amountFiat ?: 0.0 } ?: 0.0 } }
        liveDataTotalHoldingsValueCrypto = countTotalHoldingsValueCrypto(liveDataTotalHoldingsValueFiat, liveDataCurrentCryptocurrency)

        liveDataTotalHoldingsValueOnDateText = Transformations.switchMap(cryptocurrencyRepository.getSpecificScreenStatusLiveData(DB_ID_SCREEN_MAIN_LIST)) { screenStatus -> MutableLiveData<String>().apply { value = formatDate(screenStatus?.timestamp, DATE_FORMAT_PATTERN) } }

        liveDataTotalHoldingsValueFiat24hText = Transformations.switchMap(liveDataTotalHoldingsValueFiat24h) {
            MutableLiveData<SpannableString>().apply { value = getSpannableValueStyled(context, liveDataTotalHoldingsValueFiat24h.value!!, SpannableValueColorStyle.Background, ValueType.Fiat, " $currentFiatCurrencySign ", " ") }
        }
        liveDataTotalHoldingsValueCryptoText = Transformations.switchMap(liveDataTotalHoldingsValueCrypto) { MutableLiveData<String>().apply { value = String.format("$currentCryptoCurrencySign ${roundValue(liveDataTotalHoldingsValueCrypto.value!!, ValueType.Crypto)}") } }
        liveDataTotalHoldingsValueFiatText = Transformations.switchMap(liveDataTotalHoldingsValueFiat) { MutableLiveData<String>().apply { value = String.format("$currentFiatCurrencySign ${roundValue(liveDataTotalHoldingsValueFiat.value!!, ValueType.Fiat)}") } }
    }


    private fun getSupportedFiatCurrencySymbols(context: Context): HashMap<String, String> {
        val fiatCurrencySymbols: HashMap<String, String> = HashMap()

        val keys = context.resources.getStringArray(R.array.fiat_currency_code_array)
        val values = context.resources.getStringArray(R.array.fiat_currency_sign_array)

        for (i in 0 until Math.min(keys.size, values.size)) {
            fiatCurrencySymbols.put(keys[i], values[i])
        }

        return fiatCurrencySymbols
    }

    // When you want to combine multiple sources of LiveData use MediatorLiveData.
    private fun countTotalHoldingsValueCrypto(totalHoldingsValueFiat: LiveData<Double>, currentCryptocurrency: LiveData<Cryptocurrency>): LiveData<Double> {

        // Nested function to take program modularization further.
        fun combineLatestData(): Double {

            val totalFiat = totalHoldingsValueFiat.value
            val currentCrypto = currentCryptocurrency.value

            // Don't send a success until we have both results.
            if (totalFiat == null || currentCrypto == null) {
                return 0.0
            }

            return totalFiat / currentCrypto.priceFiat
        }

        val result = MediatorLiveData<Double>()

        result.addSource(totalHoldingsValueFiat) { _ ->
            result.value = combineLatestData()
        }
        result.addSource(currentCryptocurrency) { _ ->
            result.value = combineLatestData()
        }

        return result
    }

    // In Kotlin, all coroutines run inside a CoroutineScope.
    // A scope controls the lifetime of coroutines through its job.
    private val viewModelJob = Job()
    // Since uiScope has a default dispatcher of Dispatchers.Main, this coroutine will be launched
    // in the main thread.
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    /**
     * On retry we need to run sequential code. First we need to get owned crypto coins ids from
     * local database, wait for response and only after it use these ids to make a call with
     * retrofit to get updated owned crypto values. This can be done using Kotlin Coroutines.
     */
    fun retry(shouldFetch: Boolean) {
        // Launch a coroutine in uiScope.
        uiScope.launch {
            // The function withContext is a suspend function. The withContext immediately shifts
            // execution of the block into different thread inside the block, and back when it
            // completes. IO dispatcher is suitable for execution the network requests in IO thread.
            val myCryptocurrencyIds = withContext(Dispatchers.IO) {
                // Suspend until getMyCryptocurrencyIds() returns a result.
                cryptocurrencyRepository.getMyCryptocurrencyIds()
            }
            // Here we come back to main worker thread. As soon as myCryptocurrencyIds has a result
            // and main looper is available, coroutine resumes on main thread, and
            // getMyCryptocurrencyLiveDataResourceList(shouldFetch, myCryptocurrencyIds) is called.
            // We wait for background operations to complete, without blocking the original thread.
            mediatorLiveDataMyCryptocurrencyList.removeSource(liveDataMyCryptocurrencyList)
            liveDataMyCryptocurrencyList = cryptocurrencyRepository.getMyCryptocurrencyLiveDataResourceList(shouldFetch, myCryptocurrencyIds)
            mediatorLiveDataMyCryptocurrencyList.addSource(liveDataMyCryptocurrencyList) { mediatorLiveDataMyCryptocurrencyList.value = it }
        }

    }

    // onCleared is called when the ViewModel is no longer used and will be destroyed.
    // This typically happens when the user navigates away from the Activity or Fragment that was
    // using the ViewModel.
    override fun onCleared() {
        super.onCleared()
        // When you cancel the job of a scope, it cancels all coroutines started in that scope.
        // It's important to cancel any coroutines that are no longer required to avoid unnecessary
        // work and memory leaks.
        viewModelJob.cancel()
    }

}