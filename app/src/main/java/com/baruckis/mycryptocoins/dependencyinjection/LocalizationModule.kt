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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import com.baruckis.mycryptocoins.utilities.localization.Localization
import com.baruckis.mycryptocoins.utilities.localization.LocalizationLanguage
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import java.util.*
import javax.inject.Singleton

// We can generate a map of resources for each language and for that we use Dagger map multibindings.
@Module
class LocalizationModule {

    @Provides
    @IntoMap
    @LanguageKey(Language.English)
    fun provideEnglishResources(context: Context): Resources =
            getLocalizedResources(context, Language.English.locale)

    @Provides
    @IntoMap
    @LanguageKey(Language.Hebrew)
    fun provideHebrewResources(context: Context): Resources =
            getLocalizedResources(context, Language.Hebrew.locale)

    @Provides
    @IntoMap
    @LanguageKey(Language.Lithuanian)
    fun provideLithuanianResources(context: Context): Resources =
            getLocalizedResources(context, Language.Lithuanian.locale)


    @Provides
    @Singleton
    fun provideLocalization(context: Context, sharedPreferences: SharedPreferences): Localization =
            LocalizationLanguage(context, sharedPreferences)


    private fun getLocalizedResources(context: Context, locale: Locale): Resources {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        val localizedContext = context.createConfigurationContext(configuration)
        return localizedContext.resources
    }

}