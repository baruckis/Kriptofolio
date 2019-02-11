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

package com.baruckis.mycryptocoins

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.baruckis.mycryptocoins.dependencyinjection.AppInjector
import com.baruckis.mycryptocoins.utilities.localization.LocalizationManager
import com.baruckis.mycryptocoins.utilities.logConsoleVerbose
import com.facebook.stetho.Stetho
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import javax.inject.Inject


class App : Application(), HasActivityInjector {

    @Inject // It implements Dagger machinery of finding appropriate injector factory for a type.
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>


    override fun onCreate() {
        super.onCreate()

        // Initialize in order to automatically inject activities and fragments if they implement Injectable interface.
        AppInjector.init(this)

        Stetho.initializeWithDefaults(this)
    }


    // This is required by HasActivityInjector interface to setup Dagger for Activity.
    override fun activityInjector(): AndroidInjector<Activity> = dispatchingAndroidInjector


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