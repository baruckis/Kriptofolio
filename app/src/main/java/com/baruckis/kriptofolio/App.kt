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

package com.baruckis.kriptofolio

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.baruckis.kriptofolio.dependencyinjection.appKoinModules
import com.baruckis.kriptofolio.utilities.localization.LocalizationManager
import com.baruckis.kriptofolio.utilities.logConsoleVerbose
import org.koin.core.context.startKoin


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start Koin, which provides the app-wide objects and the ViewModels (docs/decisions/005).
        startKoin {
            modules(appKoinModules(this@App))
        }

        DebugTools.init(this)

    }


    // Android resets the locale for the top level resources back to the device default on every
    // application restart and configuration change. So make sure you perform a new update there.

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocalizationManager.setLocale(base))
        logConsoleVerbose("attachBaseContext " + this@App.toString())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocalizationManager.setLocale(this)
        logConsoleVerbose("onConfigurationChanged")
    }

}