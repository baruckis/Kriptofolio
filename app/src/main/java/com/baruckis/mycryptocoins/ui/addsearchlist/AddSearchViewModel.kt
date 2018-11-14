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

package com.baruckis.mycryptocoins.ui.addsearchlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.repository.CryptocurrencyRepository
import com.baruckis.mycryptocoins.vo.Resource
import javax.inject.Inject

class AddSearchViewModel @Inject constructor(var cryptocurrencyRepository: CryptocurrencyRepository) : ViewModel() {

    val mediatorLiveData = MediatorLiveData<Resource<List<Cryptocurrency>>>()
    private var liveData: LiveData<Resource<List<Cryptocurrency>>> = cryptocurrencyRepository.getAllCryptocurrencyLiveDataList()

    init {
        // A mediator to observe the changes. Room will automatically notify all active observers when the data changes.
        mediatorLiveData.addSource(liveData) { mediatorLiveData.value = it }
    }

    fun retry(shouldFetch: Boolean) {
        mediatorLiveData.removeSource(liveData)
        liveData = cryptocurrencyRepository.getAllCryptocurrencyLiveDataList(shouldFetch)
        mediatorLiveData.addSource(liveData) { mediatorLiveData.value = it }
    }

    fun addCryptocurrency(cryptocurrency: Cryptocurrency) {

    }

}