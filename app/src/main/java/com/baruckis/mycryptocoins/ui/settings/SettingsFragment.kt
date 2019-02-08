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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.dependencyinjection.Injectable
import com.baruckis.mycryptocoins.repository.CryptocurrencyRepository
import com.baruckis.mycryptocoins.utilities.formatDate
import java.util.*
import javax.inject.Inject


/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragmentCompat(), Injectable {

    @Inject
    lateinit var cryptocurrencyRepository: CryptocurrencyRepository


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)

        val preferenceFiatCurrency = findPreference(getString(R.string.pref_fiat_currency_key)) as Preference

        // Set the initial value for fiat currency preference summary.
        setListPreferenceSummary(preferenceFiatCurrency, cryptocurrencyRepository.getCurrentFiatCurrencyCode())

        // Change the fiat currency preference summary when preference value changed.
        preferenceFiatCurrency.setOnPreferenceChangeListener { preference, newValue ->

            val newCode: String = newValue.toString()

            setListPreferenceSummary(preference, newCode)
            true
        }


        val preferenceDateFormat = findPreference(getString(R.string.pref_date_format_key)) as Preference

        // Set the initial value for date format preference summary.
        setPreferenceDateFormatSummary(preferenceDateFormat, cryptocurrencyRepository.getCurrentDateFormat())

        // Change the date format preference summary when preference value changed.
        preferenceDateFormat.setOnPreferenceChangeListener { preference, newValue ->

            val newFormat: String = newValue.toString()

            setPreferenceDateFormatSummary(preference, newFormat)
            true
        }
    }

    private fun setListPreferenceSummary(preference: Preference, value: String) {
        if (preference is ListPreference) {
            val index = preference.findIndexOfValue(value)
            val entry = preference.entries[index]
            preference.summary = entry
        }
    }

    private fun setPreferenceDateFormatSummary(preference: Preference, value: String) {
        setListPreferenceSummary(preference, value)
        val todayDate = Calendar.getInstance().time
        preference.summary = preference.summary.toString() + " (" + formatDate(todayDate, value) + ")"
    }

}