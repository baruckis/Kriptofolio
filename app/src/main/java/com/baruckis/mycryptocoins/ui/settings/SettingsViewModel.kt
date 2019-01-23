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

package com.baruckis.mycryptocoins.ui.settings

import com.baruckis.mycryptocoins.repository.CryptocurrencyRepository
import com.baruckis.mycryptocoins.ui.common.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class SettingsViewModel @Inject constructor(var cryptocurrencyRepository: CryptocurrencyRepository) : BaseViewModel() {

    fun setMainListScreenStatusTimestamp(timestamp: Date?) {
        // Launch a coroutine in uiScope.
        uiScope.launch {
            withContext(Dispatchers.IO) {
                cryptocurrencyRepository.setMainListScreenStatusTimestamp(timestamp)
            }
        }
    }
}