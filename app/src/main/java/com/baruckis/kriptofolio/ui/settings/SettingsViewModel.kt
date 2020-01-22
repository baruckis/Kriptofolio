/*
 * Copyright 2018-2020 Andrius Baruckis www.baruckis.com | kriptofolio.app
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

package com.baruckis.kriptofolio.ui.settings

import androidx.lifecycle.ViewModel
import com.baruckis.kriptofolio.repository.CryptocurrencyRepository
import com.baruckis.kriptofolio.repository.LicensesRepository
import com.baruckis.kriptofolio.utilities.localization.StringsLocalization
import javax.inject.Inject


class SettingsViewModel @Inject constructor(
        cryptocurrencyRepository: CryptocurrencyRepository,
        licensesRepository: LicensesRepository,
        val stringsLocalization: StringsLocalization) : ViewModel() {

    val currentLanguage = cryptocurrencyRepository.getCurrentLanguage()

    val currentFiatCurrencyCode = cryptocurrencyRepository.getCurrentFiatCurrencyCode()

    val currentDateFormat = cryptocurrencyRepository.getCurrentDateFormat()

    val appLicenseData: String = licensesRepository.getAppLicense()

    val noBrowserFoundMessage: String = licensesRepository.getNoBrowserFoundMessage()
}