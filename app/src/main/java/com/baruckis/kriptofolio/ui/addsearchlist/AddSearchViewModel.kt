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

package com.baruckis.kriptofolio.ui.addsearchlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.baruckis.kriptofolio.db.Cryptocurrency
import com.baruckis.kriptofolio.db.MyCryptocurrency
import com.baruckis.kriptofolio.repository.CryptocurrencyRepository
import com.baruckis.kriptofolio.ui.common.BaseViewModel
import com.baruckis.kriptofolio.utilities.SERVER_CALL_DELAY_MILLISECONDS
import com.baruckis.kriptofolio.utilities.TimeFormat
import com.baruckis.kriptofolio.vo.Resource
import javax.inject.Inject

class AddSearchViewModel @Inject constructor(var cryptocurrencyRepository: CryptocurrencyRepository) : BaseViewModel() {

    val mediatorLiveDataCryptocurrencyResourceList = MediatorLiveData<Resource<List<Cryptocurrency>>>()
    private var liveDataCryptocurrencyResourceList: LiveData<Resource<List<Cryptocurrency>>> =
            cryptocurrencyRepository.getAllCryptocurrencyLiveDataResourceList(cryptocurrencyRepository.getCurrentFiatCurrencyCode())

    // Helper variable to store temporary cryptocurrency which user clicked on to add. It is stored
    // in view model to avoid loosing value during configuration change, e.g. device rotation.
    var selectedCryptocurrency: MyCryptocurrency? = null

    // Helper variable to store state of swipe refresh layout.
    var isSwipeRefreshing: Boolean = false

    var lastUpdatedOnDate: String = ""

    var isSearchMenuItemEnabled: Boolean = true


    init {
        // A mediator to observe the changes. Room will automatically notify all active observers when the data changes.
        mediatorLiveDataCryptocurrencyResourceList.addSource(liveDataCryptocurrencyResourceList) {
            mediatorLiveDataCryptocurrencyResourceList.value = it
        }
    }


    fun retry() {
        // Make a call to the server after some delay for better user experience.
        refreshCryptocurrencyResourceList(SERVER_CALL_DELAY_MILLISECONDS)
    }

    private fun refreshCryptocurrencyResourceList(callDelay: Long = 0) {
        mediatorLiveDataCryptocurrencyResourceList.removeSource(liveDataCryptocurrencyResourceList)
        liveDataCryptocurrencyResourceList = cryptocurrencyRepository.getAllCryptocurrencyLiveDataResourceList(
                cryptocurrencyRepository.getCurrentFiatCurrencyCode(),
                true, callDelay)
        mediatorLiveDataCryptocurrencyResourceList.addSource(liveDataCryptocurrencyResourceList) {
            mediatorLiveDataCryptocurrencyResourceList.value = it
        }
    }

    fun search(searchText: String): LiveData<List<Cryptocurrency>> {
        return cryptocurrencyRepository.getCryptocurrencyLiveDataListBySearch(searchText)
    }


    fun getCurrentDateFormat(): String {
        return cryptocurrencyRepository.getCurrentDateFormat()
    }

    fun getCurrentTimeFormat(): TimeFormat {
        return cryptocurrencyRepository.getCurrentTimeFormat()
    }

}