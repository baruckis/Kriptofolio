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
import android.text.SpannableString
import androidx.lifecycle.*
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.db.Cryptocurrency
import com.baruckis.mycryptocoins.repository.CryptocurrencyRepository
import com.baruckis.mycryptocoins.utilities.*
import com.baruckis.mycryptocoins.vo.Resource
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject


/**
 * The ViewModel class is designed to store and manage UI-related data in a lifecycle conscious way.
 * The ViewModel class allows data to survive configuration changes such as screen rotations.
 */

// ViewModel will require a CryptocurrencyRepository so we add @Inject code into ViewModel constructor.
class MainViewModel @Inject constructor(val context: Context, val cryptocurrencyRepository: CryptocurrencyRepository) : ViewModel() {

    private var currentCryptoCurrencyCode: String
    private var currentCryptoCurrencySign: String

    var liveDataCurrentFiatCurrencyCode: LiveData<String>

    val liveDataCurrentFiatCurrencySign: LiveData<String>

    private val liveDataCurrentCryptocurrency: LiveData<Cryptocurrency>
    private val liveDataTotalHoldingsValueFiat: LiveData<Double>
    private val liveDataTotalHoldingsValueCrypto: LiveData<Double>
    private val liveDataTotalHoldingsValueFiat24h: LiveData<Double>?

    val mediatorLiveDataMyCryptocurrencyResourceList = MediatorLiveData<Resource<List<Cryptocurrency>>>()
    var liveDataMyCryptocurrencyResourceList: LiveData<Resource<List<Cryptocurrency>>>
    private val liveDataMyCryptocurrencyList: LiveData<List<Cryptocurrency>>

    var liveDataTotalHoldingsValueOnDateText = MediatorLiveData<String>()
    val liveDataTotalHoldingsValueFiat24hText: LiveData<SpannableString>
    val liveDataTotalHoldingsValueCryptoText: LiveData<String>
    val liveDataTotalHoldingsValueFiatText: LiveData<String>


    // This is additional helper variable to deal correctly with currency spinner and preference.
    // It is kept inside viewmodel not to be lost because of fragment/activity recreation.
    var newSelectedFiatCurrencyCode: String? = null



    init {
        // Set a code value for default cryptocurrency in the app which is Bitcoin as a constant.
        currentCryptoCurrencyCode = context.getString(R.string.default_crypto_code)
        // Set a sign value for default cryptocurrency in the app which is Bitcoin as a constant.
        currentCryptoCurrencySign = context.getString(R.string.default_crypto_sign)


        // Set a code value for our selected fiat currency which is stored in shared preferences.
        liveDataCurrentFiatCurrencyCode = cryptocurrencyRepository.getCurrentFiatCurrencyCodeLiveData()

        liveDataCurrentFiatCurrencySign = cryptocurrencyRepository.getCurrentFiatCurrencySignLiveData()


        // Set a resource value for a list of cryptocurrencies that user owns.
        liveDataMyCryptocurrencyResourceList = cryptocurrencyRepository.getMyCryptocurrencyLiveDataResourceList(cryptocurrencyRepository.getCurrentFiatCurrencyCode())

        // Declare additional variable to be able to reload data on demand.
        mediatorLiveDataMyCryptocurrencyResourceList.addSource(liveDataMyCryptocurrencyResourceList) {
            mediatorLiveDataMyCryptocurrencyResourceList.value = it }

        // Declare additional variable to filter out concrete response data from resource.
        // We use swithMap which returns a new LiveData object rather than a value, i.e. it switches the actual LiveData for a new one.
        liveDataMyCryptocurrencyList = Transformations.switchMap(mediatorLiveDataMyCryptocurrencyResourceList) { data ->
            // We ignore resource such as Loading where data list can be null.
            MutableLiveData<List<Cryptocurrency>>().takeIf { data?.data != null }?.apply { value = data.data}
        }


        // We prepare a text to show a date and time when data was updated from the server.
        liveDataTotalHoldingsValueOnDateText.addSource(cryptocurrencyRepository.getSpecificScreenStatusLiveData(DB_ID_SCREEN_MAIN_LIST)) { data ->
            liveDataTotalHoldingsValueOnDateText.value = formatDate(data?.timestamp, DATE_FORMAT_PATTERN)
        }


        fun getMyCryptocurrencyListSumByDouble(sumFunc: (Cryptocurrency) -> Double):Double {
            val code = liveDataCurrentFiatCurrencyCode.value ?: getCurrentFiatCurrencyCode()
            var sum = 0.0

            liveDataMyCryptocurrencyList.value?.forEach {
                if (it.currencyFiat != code) {
                    sum = Double.NaN
                    return@forEach
                }
                sum += sumFunc(it)
            }
            return sum
        }


        // Declare additional variable to transform filtered out concrete response data to the total holdings amount change during last 24 hours.
        liveDataTotalHoldingsValueFiat24h = Transformations.switchMap(liveDataMyCryptocurrencyList)
        { _ -> MutableLiveData<Double>().apply {value = getMyCryptocurrencyListSumByDouble{cryptocurrency -> cryptocurrency.amountFiatChange24h ?: 0.0}} }

        // Here we use helper function combining two LiveData sources to one to format text of our
        // total holdings amount change during last 24 hours. Prepared text is declared as
        // additional variable and now it can be shown on UI.
        liveDataTotalHoldingsValueFiat24hText = zip(liveDataCurrentFiatCurrencyCode, liveDataTotalHoldingsValueFiat24h)
        { currentFiatCurrencyCode, totalHoldingsValueFiat24h ->
            val currentFiatCurrencySign = cryptocurrencyRepository.getCurrentFiatCurrencySign(currentFiatCurrencyCode)
            getSpannableValueStyled(context, totalHoldingsValueFiat24h, SpannableValueColorStyle.Background, ValueType.Fiat, " $currentFiatCurrencySign ", " ", context.getString(R.string.string_no_number))
        }


        // Declare additional variable to store data of our default cryptocurrency (Bitcoin) for further operations.
        liveDataCurrentCryptocurrency = cryptocurrencyRepository.getSpecificCryptocurrencyLiveData(currentCryptoCurrencyCode)


        // Declare additional variable to calculate all amount value of fiat currency that user has.
        liveDataTotalHoldingsValueFiat = Transformations.switchMap(liveDataMyCryptocurrencyList)
        { _ -> MutableLiveData<Double>().apply { value = getMyCryptocurrencyListSumByDouble{cryptocurrency -> cryptocurrency.amountFiat ?: 0.0} } }


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
        liveDataTotalHoldingsValueCrypto = zip(liveDataTotalHoldingsValueFiat, liveDataCurrentCryptocurrency)
        { totalHoldingsValueFiat, currentCryptocurrency ->
            totalHoldingsValueFiat / currentCryptocurrency.priceFiat }

        // At last we can show user owned all cryptocurrencies portfolio value in default crypto (Bitcoin) as formatted text.
        liveDataTotalHoldingsValueCryptoText = Transformations.switchMap(liveDataTotalHoldingsValueCrypto) { totalHoldingsValueCrypto ->
            MutableLiveData<String>().apply {
                value = String.format("$currentCryptoCurrencySign ${
                if (totalHoldingsValueCrypto.isNaN()) context.getString(R.string.string_no_number)
                else roundValue(totalHoldingsValueCrypto, ValueType.Crypto)}") } }

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
    fun retry(currentFiatCurrencyCode:String?) {
        // Launch a coroutine in uiScope.
        uiScope.launch {
            updateMyCryptocurrencyList(currentFiatCurrencyCode ?: cryptocurrencyRepository.getCurrentFiatCurrencyCode())
        }

    }

    // To implement a manual refresh without modifying your existing LiveData logic.
    fun refreshMyCryptocurrencyResourceList(liveData: LiveData<Resource<List<Cryptocurrency>>>) {
        mediatorLiveDataMyCryptocurrencyResourceList.removeSource(liveDataMyCryptocurrencyResourceList)
        liveDataMyCryptocurrencyResourceList = liveData
        mediatorLiveDataMyCryptocurrencyResourceList.addSource(liveDataMyCryptocurrencyResourceList) { mediatorLiveDataMyCryptocurrencyResourceList.value = it }
    }

    private suspend fun updateMyCryptocurrencyList(currentFiatCurrencyCode:String = cryptocurrencyRepository.getCurrentFiatCurrencyCode()) {
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
        refreshMyCryptocurrencyResourceList(cryptocurrencyRepository.
                getMyCryptocurrencyLiveDataResourceList(currentFiatCurrencyCode, true, myCryptocurrencyIds))
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

    // This function will add user selected cryptocurrency using coroutines.
    fun addCryptocurrency(cryptocurrency: Cryptocurrency) {

        // Launch a coroutine in uiScope.
        uiScope.launch {
            val screenTimestampDifference = withContext(Dispatchers.IO) {
                // As we make a multiple calls to database, we need to do them on different thread.
                // We cannot access database on the main thread since it may potentially lock the UI
                // for a long period of time.
                cryptocurrencyRepository.updateCryptocurrency(cryptocurrency)
                getAddSearchListScreenStatusTimestamp() < getMainListScreenStatusTimestamp()
            }

            // When we add selected crytocurrency to users owned cryptocurrencies list, we need to
            // check if it is up to date and if it is presented in correct fiat currency. If these
            // conditions are not met than we request a data update from the server.
            if (screenTimestampDifference || cryptocurrency.currencyFiat != getCurrentFiatCurrencyCode()) {
                liveDataTotalHoldingsValueOnDateText.value = ""
                updateMyCryptocurrencyList()
            } else {
                refreshMyCryptocurrencyResourceList(cryptocurrencyRepository.
                        getMyCryptocurrencyLiveDataResourceList(cryptocurrencyRepository.getCurrentFiatCurrencyCode()))
            }

        }
    }


    private fun getMainListScreenStatusTimestamp():Date {
        return cryptocurrencyRepository.getSpecificScreenStatusData(DB_ID_SCREEN_MAIN_LIST).timestamp
    }

    private fun getAddSearchListScreenStatusTimestamp():Date {
        return cryptocurrencyRepository.getSpecificScreenStatusData(DB_ID_SCREEN_ADD_SEARCH_LIST).timestamp
    }


    fun getCurrentFiatCurrencyCode(): String {
        // Get value from shared preferences.
        return cryptocurrencyRepository.getCurrentFiatCurrencyCode()
    }

    fun setNewCurrentFiatCurrencyCode(value: String) {
        // Set new value in shared preferences.
        cryptocurrencyRepository.setNewCurrentFiatCurrencyCode(value)
    }


    // Here we get additional helper variable to deal correctly with currency spinner and preference.
    fun getSelectedFiatCurrencyCodeFromRep(): String? {
        // Get value from repository.
        return cryptocurrencyRepository.selectedFiatCurrencyCode
    }

    // Here we store additional helper variable to deal correctly with currency spinner and preference.
    fun setSelectedFiatCurrencyCodeFromRep(code: String?) {
        // Set new value in repository.
        cryptocurrencyRepository.selectedFiatCurrencyCode = code
    }

}