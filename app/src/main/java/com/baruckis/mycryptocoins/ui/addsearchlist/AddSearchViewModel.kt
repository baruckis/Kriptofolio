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

package com.baruckis.mycryptocoins.ui.addsearchlist

import androidx.lifecycle.*
import com.baruckis.mycryptocoins.db.Cryptocurrency
import com.baruckis.mycryptocoins.db.MyCryptocurrency
import com.baruckis.mycryptocoins.repository.CryptocurrencyRepository
import com.baruckis.mycryptocoins.utilities.DATE_FORMAT_PATTERN
import com.baruckis.mycryptocoins.utilities.DB_ID_SCREEN_ADD_SEARCH_LIST
import com.baruckis.mycryptocoins.utilities.formatDate
import com.baruckis.mycryptocoins.vo.Resource
import javax.inject.Inject

class AddSearchViewModel @Inject constructor(var cryptocurrencyRepository: CryptocurrencyRepository) : ViewModel() {

    val mediatorLiveData = MediatorLiveData<Resource<List<Cryptocurrency>>>()
    private var liveData: LiveData<Resource<List<Cryptocurrency>>> = cryptocurrencyRepository.getAllCryptocurrencyLiveDataResourceList(cryptocurrencyRepository.getCurrentFiatCurrencyCode())

    val liveDataLastUpdated: LiveData<String>

    // Helper variable to store temporary cryptocurrency which user clicked on to add. It is stored
    // in view model to avoid loosing value during configuration change, e.g. device rotation.
    var selectedCryptocurrency: MyCryptocurrency? = null


    init {
        // A mediator to observe the changes. Room will automatically notify all active observers when the data changes.
        mediatorLiveData.addSource(liveData) { mediatorLiveData.value = it }

        liveDataLastUpdated = Transformations.switchMap(cryptocurrencyRepository.getSpecificScreenStatusLiveData(DB_ID_SCREEN_ADD_SEARCH_LIST))
        { screenStatus ->
            MutableLiveData<String>().apply { value = formatDate(screenStatus?.timestamp, DATE_FORMAT_PATTERN) }
        }
    }

    fun retry(shouldFetch: Boolean) {
        mediatorLiveData.removeSource(liveData)
        liveData = cryptocurrencyRepository.getAllCryptocurrencyLiveDataResourceList(cryptocurrencyRepository.getCurrentFiatCurrencyCode(), shouldFetch)
        mediatorLiveData.addSource(liveData) { mediatorLiveData.value = it }
    }

    fun search(searchText: String): LiveData<List<Cryptocurrency>> {
        return cryptocurrencyRepository.getCryptocurrencyLiveDataListBySearch(searchText)
    }

}