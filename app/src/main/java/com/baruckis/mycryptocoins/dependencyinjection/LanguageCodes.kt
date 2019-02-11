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

package com.baruckis.mycryptocoins.dependencyinjection

import java.util.*

// Locale codes to avoid hardcoding values.
object LanguageCodes {
    const val ENGLISH = "EN"
    const val HEBREW = "HE"
    const val LITHUANIAN = "LT"
}

// Represents our languages.
enum class Language(val locale: Locale) {
    English(Locale(LanguageCodes.ENGLISH)),
    Hebrew(Locale(LanguageCodes.HEBREW)),
    Lithuanian(Locale(LanguageCodes.LITHUANIAN));

    companion object {
        val DEFAULT = English

        // We try to get our enum value from provided Locale. In worst case scenario we end up with
        // the default value.
        fun fromLocale(locale: Locale): Language = values().
                firstOrNull { it.locale.language == locale.language } ?: DEFAULT
    }
}