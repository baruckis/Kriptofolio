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

package com.baruckis.mycryptocoins.utilities.localization

import android.content.res.Resources
import androidx.annotation.StringRes
import com.baruckis.mycryptocoins.dependencyinjection.Language
import com.baruckis.mycryptocoins.utilities.logConsoleError
import com.baruckis.mycryptocoins.utilities.logConsoleWarn
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// Helper class that will provide string resources from the generated map.
@Singleton
class StringsLocalization @Inject constructor(
        private val localization: Localization,
        private val resMap: Map<Language, @JvmSuppressWildcards Resources>
) {
    fun setLanguage(languageCode: String) {
        val newLanguage = try {
            Language.fromLocale(Locale(languageCode))
        } catch (ex: Exception) {
            logConsoleError("Language for code $languageCode is not found.")
            null
        }
        newLanguage?.let { localization.currentLanguage = it }
    }

    // This method takes string resource id, just like the regular Activity.getString() method.
    // It will get the language from Localization and use it to retrieve the proper resources from
    // the map. If the map does not contain anything at that language key for some reason, we will
    // use fallback resources as a last resort before failing to provide a string.
    fun getString(@StringRes stringId: Int): String = resMap.
            getOrElse(localization.currentLanguage, this::getFallbackResources).getString(stringId)


    private fun getFallbackResources(): Resources {
        val defaultLanguage =
                if (Language.DEFAULT in resMap) Language.DEFAULT else resMap.keys.firstOrNull()

        if (defaultLanguage != null) {
            logConsoleWarn("Current language resources not found. Fallback to: $defaultLanguage")
            localization.currentLanguage = defaultLanguage
            return resMap.getValue(defaultLanguage)
        } else {
            throw ResourcesNotFoundException("String resources not found.")
        }
    }
}

class ResourcesNotFoundException(message: String) : RuntimeException(message)