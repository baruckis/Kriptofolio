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
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION_CODES.N
import android.preference.PreferenceManager
import com.baruckis.kriptofolio.R
import java.util.*

// A singleton is created by simply declaring an object.
object LocalizationManager {

    fun setLocale(context: Context): Context {
        return updateResources(context, getLanguage(context))
    }

    fun getLocale(res: Resources): Locale {
        val config = res.configuration
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= N) config.locales.get(0) else config.locale
    }


    private fun getLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_language_key),
                context.getString(R.string.pref_default_language_value))
                ?: context.getString(R.string.pref_default_language_value)
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        configuration.setLocale(locale)

        // When using createConfigurationContext you need to invoke attachBaseContext in all the
        // components like application, activities, services to update the resources for them.
        // Besides you can’t actually update the resources for application after you change the
        // language at runtime since attachBaseContext is never called again. Therefore, you have
        // to restart the application to update the resources.
        return context.createConfigurationContext(configuration)
    }
}