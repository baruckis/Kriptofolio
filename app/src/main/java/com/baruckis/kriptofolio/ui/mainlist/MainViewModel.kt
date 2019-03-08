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

package com.baruckis.kriptofolio.ui.mainlist

import android.content.Context
import android.text.SpannableString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.baruckis.kriptofolio.R
import com.baruckis.kriptofolio.db.Cryptocurrency
import com.baruckis.kriptofolio.db.MyCryptocurrency
import com.baruckis.kriptofolio.repository.CryptocurrencyRepository
import com.baruckis.kriptofolio.ui.common.BaseViewModel
import com.baruckis.kriptofolio.utilities.*
import com.baruckis.kriptofolio.utilities.localization.StringsLocalization
import com.baruckis.kriptofolio.vo.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject


/**
 * The ViewModel class is designed to store and manage UI-related data in a lifecycle conscious way.
 * The ViewModel class allows data to survive configuration changes such as screen rotations.
 */

// ViewModel will require a CryptocurrencyRepository so we add @Inject code into ViewModel constructor.
class MainViewModel @Inject constructor(private val context: Context,
                                        private val cryptocurrencyRepository: CryptocurrencyRepository,
                                        private val stringsLocalization: StringsLocalization) : BaseViewModel() {

    private var currentCryptoCurrencyCode: String
    private var currentCryptoCurrencySign: String

    var liveDataCurrentDateFormat: LiveData<String>

    var liveDataCurrentFiatCurrencyCode: LiveData<String>
    val liveDataCurrentFiatCurrencySign: LiveData<String>

    private val liveDataCurrentMyCryptocurrency: LiveData<Cryptocurrency>
    private val liveDataTotalHoldingsValueFiat: LiveData<Double>
    private val liveDataTotalHoldingsValueCrypto: LiveData<Double>
    private val liveDataTotalHoldingsValueFiat24h: LiveData<Double>?

    val mediatorLiveDataMyCryptocurrencyResourceList = MediatorLiveData<Resource<List<MyCryptocurrency>>>()
    private var liveDataMyCryptocurrencyResourceList: LiveData<Resource<List<MyCryptocurrency>>>
    private val liveDataMyCryptocurrencyList: LiveData<List<MyCryptocurrency>>

    var liveDataTotalHoldingsValueOnDateText: LiveData<String>
    val liveDataTotalHoldingsValueFiat24hText: LiveData<SpannableString>
    val liveDataTotalHoldingsValueCryptoText: LiveData<String>
    val liveDataTotalHoldingsValueFiatText: LiveData<String>


    // This is additional helper variable to deal correctly with currency spinner and preference.
    // It is kept inside viewmodel not to be lost because of fragment/activity recreation.
    var newSelectedFiatCurrencyCode: String? = null

    // Helper variable to store state of swipe refresh layout.
    var isSwipeRefreshing: Boolean = false


    init {
        // Set a code value for default cryptocurrency in the app which is Bitcoin as a constant.
        currentCryptoCurrencyCode = context.getString(R.string.default_crypto_code)
        // Set a sign value for default cryptocurrency in the app which is Bitcoin as a constant.
        currentCryptoCurrencySign = context.getString(R.string.default_crypto_sign)


        liveDataCurrentDateFormat = cryptocurrencyRepository.getCurrentDateFormatLiveData()

        // Set a code value for our selected fiat currency which is stored in shared preferences.
        liveDataCurrentFiatCurrencyCode = cryptocurrencyRepository.getCurrentFiatCurrencyCodeLiveData()

        liveDataCurrentFiatCurrencySign = cryptocurrencyRepository.getCurrentFiatCurrencySignLiveData()


        // Set a resource value for a list of cryptocurrencies that user owns.
        liveDataMyCryptocurrencyResourceList = cryptocurrencyRepository.getMyCryptocurrencyLiveDataResourceList(cryptocurrencyRepository.getCurrentFiatCurrencyCode())


        // Declare additional variable to be able to reload data on demand.
        mediatorLiveDataMyCryptocurrencyResourceList.addSource(liveDataMyCryptocurrencyResourceList) {
            mediatorLiveDataMyCryptocurrencyResourceList.value = it
        }

        // Declare additional variable to get concrete data.
        liveDataMyCryptocurrencyList = cryptocurrencyRepository.getMyCryptocurrencyLiveDataList()


        liveDataTotalHoldingsValueOnDateText = MediatorLiveData<String>().apply {

            // We prepare a text to show a date and time when data was updated from the server.
            addSource(liveDataMyCryptocurrencyList) {
                value = formatDate(getMyCryptocurrencyListLastFetchedDate(),
                        liveDataCurrentDateFormat.value,
                        cryptocurrencyRepository.getCurrentTimeFormat(),
                        stringsLocalization.getString(R.string.time_format_am),
                        stringsLocalization.getString(R.string.time_format_pm))
            }

            // Or when date format is changed in settings.
            addSource(liveDataCurrentDateFormat) {
                value = formatDate(getMyCryptocurrencyListLastFetchedDate(),
                        liveDataCurrentDateFormat.value,
                        cryptocurrencyRepository.getCurrentTimeFormat(),
                        stringsLocalization.getString(R.string.time_format_am),
                        stringsLocalization.getString(R.string.time_format_pm))
            }

            // Or when time format is changed in settings.
            addSource(cryptocurrencyRepository.getCurrentTimeFormatLiveData()) { timeFormat ->
                value = formatDate(getMyCryptocurrencyListLastFetchedDate(),
                        liveDataCurrentDateFormat.value,
                        timeFormat,
                        stringsLocalization.getString(R.string.time_format_am),
                        stringsLocalization.getString(R.string.time_format_pm))
            }
        }


        // Helper function to count users cryptocurrency total amount and its change during last 24 hours.
        fun getMyCryptocurrencyListSumByDouble(sumFunc: (MyCryptocurrency) -> Double): Double {
            val code = liveDataCurrentFiatCurrencyCode.value ?: getCurrentFiatCurrencyCode()
            var sum = 0.0

            liveDataMyCryptocurrencyList.value?.forEach { myCryptocurrency ->
                if (myCryptocurrency.cryptoData.currencyFiat != code) {
                    sum = Double.NaN
                    return@forEach
                }
                sum += sumFunc(myCryptocurrency)
            }
            return sum
        }


        // Here we count total holdings value during last 24 hours when users cryptocurrency list
        // change. We set not a number value if currency code change was unsuccessful because
        // than we can not count sum correctly.
        liveDataTotalHoldingsValueFiat24h = MediatorLiveData<Double>().apply {

            fun countTotalAmountFiatChange24h(): Double {
                return getMyCryptocurrencyListSumByDouble { cryptocurrency ->
                    cryptocurrency.amountFiatChange24h ?: 0.0
                }
            }

            addSource(liveDataMyCryptocurrencyList) {
                value = countTotalAmountFiatChange24h()
            }

            addSource(liveDataCurrentFiatCurrencyCode) {
                value = countTotalAmountFiatChange24h()
            }

        }


        // Here we use helper function combining two LiveData sources to one to format text of our
        // total holdings amount change during last 24 hours. Prepared text is declared as
        // additional variable and now it can be shown on UI.
        liveDataTotalHoldingsValueFiat24hText = zip(liveDataCurrentFiatCurrencyCode, liveDataTotalHoldingsValueFiat24h)
        { currentFiatCurrencyCode, totalHoldingsValueFiat24h ->
            val currentFiatCurrencySign = cryptocurrencyRepository.getCurrentFiatCurrencySign(currentFiatCurrencyCode)
            getSpannableValueStyled(context, totalHoldingsValueFiat24h,
                    SpannableValueColorStyle.Background, ValueType.Fiat,
                    " $currentFiatCurrencySign ", " ", context.getString(R.string.string_no_number))
        }


        // Declare additional variable to store data of our default cryptocurrency (Bitcoin)
        // for further operations.
        liveDataCurrentMyCryptocurrency = cryptocurrencyRepository.getSpecificCryptocurrencyLiveData(currentCryptoCurrencyCode)


        // Declare additional variable to calculate all amount value of fiat currency that user has.
        // Here we count total holdings value when users cryptocurrency list change. We set not a
        // number value if currency code change was unsuccessful because than we can not count
        // sum correctly.
        liveDataTotalHoldingsValueFiat = MediatorLiveData<Double>().apply {

            fun countTotalAmountFiat(): Double {
                return getMyCryptocurrencyListSumByDouble { cryptocurrency ->
                    cryptocurrency.amountFiat ?: 0.0
                }
            }

            addSource(liveDataMyCryptocurrencyList) {
                value = countTotalAmountFiat()
            }

            addSource(liveDataCurrentFiatCurrencyCode) {
                value = countTotalAmountFiat()
            }

        }


        // Here we use helper function combining two LiveData sources to one to format text of our
        // total holdings amount. At last we can show user owned all cryptocurrencies portfolio
        // value in selected fiat currency as formatted text.
        liveDataTotalHoldingsValueFiatText = zip(liveDataCurrentFiatCurrencyCode, liveDataTotalHoldingsValueFiat)
        { currentFiatCurrencyCode, totalHoldingsValueFiat ->
            val currentFiatCurrencySign = cryptocurrencyRepository.getCurrentFiatCurrencySign(currentFiatCurrencyCode)
            String.format("$currentFiatCurrencySign ${if (totalHoldingsValueFiat.isNaN())
                context.getString(R.string.string_no_number) else
                roundValue(totalHoldingsValueFiat, ValueType.Fiat)}")
        }


        // Here we use helper function combining two LiveData sources to show counted all
        // cryptocurrencies portfolio amount in default crypto (Bitcoin).
        liveDataTotalHoldingsValueCrypto = zip(liveDataTotalHoldingsValueFiat, liveDataCurrentMyCryptocurrency)
        { totalHoldingsValueFiat, currentCryptocurrency ->
            totalHoldingsValueFiat / currentCryptocurrency.priceFiat
        }

        // At last we can show user owned all cryptocurrencies portfolio value in default crypto
        // (Bitcoin) as formatted text.
        liveDataTotalHoldingsValueCryptoText = Transformations.switchMap(liveDataTotalHoldingsValueCrypto) { totalHoldingsValueCrypto ->
            MutableLiveData<String>().apply {
                value = String.format("$currentCryptoCurrencySign ${
                if (totalHoldingsValueCrypto.isNaN()) context.getString(R.string.string_no_number)
                else roundValue(totalHoldingsValueCrypto, ValueType.Crypto)}")
            }
        }

    }


    // Helper function to find last fetched date of user's cryptocurrency list.
    private fun getMyCryptocurrencyListLastFetchedDate(): Date? {
        val lastFetchedDate: Date? =
                liveDataMyCryptocurrencyList.value?.elementAtOrNull(0)?.cryptoData?.lastFetchedDate
                        ?: return null

        liveDataMyCryptocurrencyList.value?.forEach { myCryptocurrency ->
            if (myCryptocurrency.cryptoData.lastFetchedDate != lastFetchedDate) return null
        }

        return lastFetchedDate
    }


    // Helper function to combine multiple sources of LiveData.
    private fun <A, B, C> zip(srcA: LiveData<A>, srcB: LiveData<B>, zipFunc: (A, B) -> C): LiveData<C> {

        return MediatorLiveData<C>().apply {
            var lastSrcA: A? = null
            var lastSrcB: B? = null

            // Nested function to take program modularization further.
            fun update() {
                // Don't send a success until we have both results.
                if (lastSrcA != null && lastSrcB != null)
                // Here we use a function that was passed as a parameter.
                    value = zipFunc(lastSrcA!!, lastSrcB!!)
            }

            addSource(srcA) {
                lastSrcA = it
                update()
            }
            addSource(srcB) {
                lastSrcB = it
                update()
            }
        }

    }


    /**
     * On retry we need to run sequential code. First we need to get owned crypto coins ids from
     * local database, wait for response and only after it use these ids to make a call with
     * retrofit to get updated owned crypto values. This can be done using Kotlin Coroutines.
     */
    fun retry(newFiatCurrencyCode: String? = null) {

        // Here we store new selected currency as additional variable or reset it.
        // Later if call to server is unsuccessful we will reuse it for retry functionality.
        newSelectedFiatCurrencyCode = newFiatCurrencyCode

        // Launch a coroutine in uiScope.
        uiScope.launch {
            // Make a call to the server after some delay for better user experience.
            updateMyCryptocurrencyList(newFiatCurrencyCode, SERVER_CALL_DELAY_MILLISECONDS)
        }
    }

    // Refresh the data from local database.
    fun refreshMyCryptocurrencyResourceList() {
        refreshMyCryptocurrencyResourceList(cryptocurrencyRepository.getMyCryptocurrencyLiveDataResourceList(cryptocurrencyRepository.getCurrentFiatCurrencyCode()))
    }

    // To implement a manual refresh without modifying your existing LiveData logic.
    private fun refreshMyCryptocurrencyResourceList(liveData: LiveData<Resource<List<MyCryptocurrency>>>) {
        mediatorLiveDataMyCryptocurrencyResourceList.removeSource(liveDataMyCryptocurrencyResourceList)
        liveDataMyCryptocurrencyResourceList = liveData
        mediatorLiveDataMyCryptocurrencyResourceList.addSource(liveDataMyCryptocurrencyResourceList)
        { mediatorLiveDataMyCryptocurrencyResourceList.value = it }
    }

    private suspend fun updateMyCryptocurrencyList(newFiatCurrencyCode: String? = null, callDelay: Long = 0) {

        val fiatCurrencyCode: String = newFiatCurrencyCode
                ?: cryptocurrencyRepository.getCurrentFiatCurrencyCode()

        isSwipeRefreshing = true

        // The function withContext is a suspend function. The withContext immediately shifts
        // execution of the block into different thread inside the block, and back when it
        // completes. IO dispatcher is suitable for execution the network requests in IO thread.
        val myCryptocurrencyIds = withContext(Dispatchers.IO) {
            // Suspend until getMyCryptocurrencyIds() returns a result.
            cryptocurrencyRepository.getMyCryptocurrencyIds()
        }

        // Here we come back to main worker thread. As soon as myCryptocurrencyIds has a result
        // and main looper is available, coroutine resumes on main thread, and
        // [getMyCryptocurrencyLiveDataResourceList] is called.
        // We wait for background operations to complete, without blocking the original thread.
        refreshMyCryptocurrencyResourceList(
                cryptocurrencyRepository.getMyCryptocurrencyLiveDataResourceList
                (fiatCurrencyCode, true, myCryptocurrencyIds, callDelay))
    }


    // This function will add user selected myCryptocurrency using coroutines.
    fun addCryptocurrency(myCryptocurrency: MyCryptocurrency) {
        // Launch a coroutine in uiScope.
        uiScope.launch {
            // As we make a multiple calls to database, we need to do them on different thread.
            // We cannot access database on the main thread since it may potentially lock the UI
            // for a long period of time.
            val refreshNeeded = withContext(Dispatchers.IO) {

                // Add selected cryptocurrency.
                cryptocurrencyRepository.upsertMyCryptocurrency(myCryptocurrency)

                // When we add selected cryptocurrency to users owned cryptocurrencies list, we need
                // to check if it is up to date and if it is presented in correct fiat currency.
                // If any of these conditions are not met than we request a data update from the
                // server.

                // Check if it is presented in correct fiat currency.
                if (myCryptocurrency.cryptoData.currencyFiat != getCurrentFiatCurrencyCode())
                    return@withContext true

                // Check if main list screen has newer fetch date compared to just added
                // cryptocurrency date from add search screen, which means that added cryptocurrency
                // data is old and needs to be updated from the network.

                val myCryptocurrencyList =
                        cryptocurrencyRepository.getMyCryptocurrencyList()

                if (myCryptocurrencyList.isNullOrEmpty()) return@withContext false

                // Check if all data in the list have the same fetch date.
                myCryptocurrencyList.forEach {
                    if (myCryptocurrencyList.first().cryptoData.lastFetchedDate !=
                            it.cryptoData.lastFetchedDate)
                        return@withContext true
                }
                return@withContext false
            }

            if (refreshNeeded) {

                // This is kinda workaround to show refreshing when we add selected cryptocurrency.
                // This small delay gives some time to load data from database which when is
                // successful turns off refreshing. After the delay new data is requested and
                // refreshing turned on while loading.
                delay(500)

                // Get the latest data from the network.
                updateMyCryptocurrencyList(callDelay = SERVER_CALL_DELAY_MILLISECONDS)
            } else {
                // Get the data from local database.
                refreshMyCryptocurrencyResourceList()
            }
        }
    }


    // This function will delete user selected cryptocurrencies using coroutines.
    fun deleteCryptocurrencyList(myCryptocurrencyList: List<MyCryptocurrency>) {
        // Launch a coroutine in uiScope.
        uiScope.launch {
            withContext(Dispatchers.IO) {
                // Again potentially not to lock the UI we do not access database on the main thread,
                // but instead do that on different one.
                cryptocurrencyRepository.deleteMyCryptocurrencyList(myCryptocurrencyList)
            }
        }
    }


    fun restoreCryptocurrencyList(myCryptocurrencyList: List<MyCryptocurrency>) {
        // Launch a coroutine in uiScope.
        uiScope.launch {
            withContext(Dispatchers.IO) {
                // Again potentially not to lock the UI we do not access database on the main
                // thread, but instead do that on different one.
                cryptocurrencyRepository.insertMyCryptocurrencyList(myCryptocurrencyList)
            }
        }
    }


    fun getCurrentFiatCurrencyCode(): String {
        // Get value from shared preferences.
        return cryptocurrencyRepository.getCurrentFiatCurrencyCode()
    }


    // Here we get additional helper variable to deal correctly with currency spinner and preference.
    fun getSelectedFiatCurrencyCodeFromRep(): String {
        // Get value from repository.
        return cryptocurrencyRepository.selectedFiatCurrencyCode
    }

    // Here we store additional helper variable to deal correctly with currency spinner and preference.
    fun setSelectedFiatCurrencyCodeFromRep(code: String) {
        // Set new value in repository.
        cryptocurrencyRepository.selectedFiatCurrencyCode = code
    }


    fun setNewCurrentFiatCurrencyCode(value: String) {
        cryptocurrencyRepository.setNewCurrentFiatCurrencyCode(value)
    }


    fun checkIfNewFiatCurrencyCodeSameToMyCryptocurrency(newFiatCurrencyCode: String): Boolean {
        liveDataMyCryptocurrencyList.value?.forEach { myCryptocurrency ->
            if (myCryptocurrency.cryptoData.currencyFiat != newFiatCurrencyCode) {
                return false
            }
        }
        return true
    }
}