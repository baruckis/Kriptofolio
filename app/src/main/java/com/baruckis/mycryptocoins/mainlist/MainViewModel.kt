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

package com.baruckis.mycryptocoins.mainlist

import android.arch.lifecycle.*
import android.content.Context
import android.preference.PreferenceManager
import android.text.SpannableString
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.data.CryptocurrencyRepository
import com.baruckis.mycryptocoins.utilities.SpannableValueColorStyle
import com.baruckis.mycryptocoins.utilities.ValueType
import com.baruckis.mycryptocoins.utilities.getSpannableValueStyled
import com.baruckis.mycryptocoins.utilities.roundValue
import javax.inject.Inject


/**
 * The ViewModel class is designed to store and manage UI-related data in a lifecycle conscious way.
 * The ViewModel class allows data to survive configuration changes such as screen rotations.
 */

// ViewModel will require a CryptocurrencyRepository so we add @Inject code into ViewModel constructor.
class MainViewModel @Inject constructor(context: Context, cryptocurrencyRepository: CryptocurrencyRepository) : ViewModel() {

    private var currentCryptoCurrencyCode: String
    private var currentCryptoCurrencySign: String
    private var currentFiatCurrencyCode: String
    var currentFiatCurrencySign: String

    private val liveDataCurrentCryptocurrency: LiveData<Cryptocurrency>
    private val liveDataTotalHoldingsValueFiat: LiveData<Double>
    private val liveDataTotalHoldingsValueCrypto: LiveData<Double>
    private val liveDataTotalHoldingsValueFiat24h: LiveData<Double>

    val liveDataMyCryptocurrencyList: LiveData<List<Cryptocurrency>>
    val liveDataTotalHoldingsValueFiat24hText: LiveData<SpannableString>
    val liveDataTotalHoldingsValueCryptoText: LiveData<String>
    val liveDataTotalHoldingsValueFiatText: LiveData<String>


    init {
        currentCryptoCurrencyCode = context.getString(R.string.default_crypto_code)
        currentCryptoCurrencySign = context.getString(R.string.default_crypto_sign)

        currentFiatCurrencyCode = PreferenceManager.getDefaultSharedPreferences(context).getString(context.resources.getString(R.string.pref_fiat_currency_key), context.resources.getString(R.string.pref_default_fiat_currency_value))!!
        currentFiatCurrencySign = getSupportedFiatCurrencySymbols(context).asSequence().filter { it.key.equals(currentFiatCurrencyCode) }.first().value

        liveDataMyCryptocurrencyList = cryptocurrencyRepository.getMyCryptocurrencyLiveDataList()
        liveDataCurrentCryptocurrency = cryptocurrencyRepository.getSpecificCryptocurrencyLiveData(currentCryptoCurrencyCode)

        // swithMap returns a new LiveData object rather than a value, i.e. it switches the actual LiveData for a new one.
        liveDataTotalHoldingsValueFiat24h = Transformations.switchMap(liveDataMyCryptocurrencyList) { _ -> MutableLiveData<Double>().apply { value = liveDataMyCryptocurrencyList.value?.sumByDouble { it.amountFiatChange24h } } }
        liveDataTotalHoldingsValueFiat = Transformations.switchMap(liveDataMyCryptocurrencyList) { _ -> MutableLiveData<Double>().apply { value = liveDataMyCryptocurrencyList.value?.sumByDouble { it.amountFiat } } }
        liveDataTotalHoldingsValueCrypto = countTotalHoldingsValueCrypto(liveDataTotalHoldingsValueFiat, liveDataCurrentCryptocurrency)

        liveDataTotalHoldingsValueFiat24hText = Transformations.switchMap(liveDataTotalHoldingsValueFiat24h) {
            MutableLiveData<SpannableString>().apply { value = getSpannableValueStyled(context, liveDataTotalHoldingsValueFiat24h.value!!, SpannableValueColorStyle.Background, ValueType.Fiat, " $currentFiatCurrencySign ", " ") } }
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

        result.addSource(totalHoldingsValueFiat) { value ->
            result.value = combineLatestData()
        }
        result.addSource(currentCryptocurrency) { value ->
            result.value = combineLatestData()
        }

        return result
    }

}