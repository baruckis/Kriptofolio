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

package com.baruckis.kriptofolio.utilities.localization

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.baruckis.kriptofolio.R
import com.baruckis.kriptofolio.dependencyinjection.Language
import java.util.*


interface Localization {
    var currentLanguage: Language
}

// A class for getting stored user preferences.
class LocalizationLanguage(
        private val context: Context,
        private val sharedPreferences: SharedPreferences
) : Localization {

    private var currentLanguageCache: Language? = null

    override var currentLanguage: Language
        get() {
            // We use the cached value if it’s present.
            val cachedValue = currentLanguageCache
            // If we don’t have it yet, we read from SharedPreferences.
            return if (cachedValue == null) {
                val storedValue = sharedPreferences.getString(
                        context.getString(R.string.pref_language_key),
                        context.getString(R.string.pref_default_language_value))
                // Then we try to get our enum from that value.
                val storedLanguage = if (storedValue == null) null else try {
                    Language.fromLocale(Locale(storedValue))
                } catch (ex: Exception) {
                    null
                }
                // If we can’t get it properly because there was no value in SharedPrefereces or
                // we read something that can’t be represented by our enum, we get a default
                // language.
                val language = storedLanguage ?: getDefaultLanguage()
                currentLanguage = language
                language
            } else cachedValue
        }
        set(value) {
            // We cache the language by calling the setter.
            currentLanguageCache = value
        }

    private fun getDefaultLanguage(): Language {
        // We read the Locale the app was started with from Configuration.
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        // We try to get our enum value from that Locale.
        return Language.fromLocale(locale)
    }

}