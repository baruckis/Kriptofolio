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

package com.baruckis.kriptofolio.dependencyinjection

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.baruckis.kriptofolio.utilities.localization.Localization
import com.baruckis.kriptofolio.utilities.localization.LocalizationLanguage
import com.baruckis.kriptofolio.utilities.localization.StringsLocalization
import org.koin.dsl.module
import java.util.*


// We generate a map of resources for each language. It used to come from map multibindings; in Koin
// the map is one definition, with the languages in the same order.
val localizationModule = module {

    single<Map<Language, Resources>> {
        val context: Context = get()
        mapOf(
                Language.English to getLocalizedResources(context, Language.English.locale),
                Language.Hebrew to getLocalizedResources(context, Language.Hebrew.locale),
                Language.Lithuanian to getLocalizedResources(context, Language.Lithuanian.locale),
                Language.Swahili to getLocalizedResources(context, Language.Swahili.locale))
    }

    single<Localization> { LocalizationLanguage(get(), get()) }

    single { StringsLocalization(get(), get()) }
}


private fun getLocalizedResources(context: Context, locale: Locale): Resources {
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(locale)
    val localizedContext = context.createConfigurationContext(configuration)
    return localizedContext.resources
}
