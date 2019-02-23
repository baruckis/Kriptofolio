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

package com.baruckis.mycryptocoins.ui.common

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.baruckis.mycryptocoins.BuildConfig
import com.baruckis.mycryptocoins.utilities.localization.LocalizationManager
import com.baruckis.mycryptocoins.utilities.logConsoleVerbose
import java.util.*


abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // All invocations of getResources will be delegated to the new resources instead of the
        // top level instance.
        // Cannot use Dagger @Inject for LocalizationManager to pass parameters because injections
        // happen after attachBaseContext.
        super.attachBaseContext(LocalizationManager.setLocale(newBase))
        logConsoleVerbose("attachBaseContext " + this@BaseActivity.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logConsoleVerbose("onCreate " + this@BaseActivity.toString())
        resetActivityTitle(this)
        logLocalizationInfo()
    }

    // This is a possible workaround to set activity titles using local resources instance.
    // It intends to break the dependency on the cache and the top level resources.
    private fun resetActivityTitle(a: Activity) {
        try {
            val info = a.packageManager.
                    getActivityInfo(a.componentName, PackageManager.GET_META_DATA)
            if (info.labelRes != 0) {
                a.setTitle(info.labelRes)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }


    private fun logLocalizationInfo() {
        if (BuildConfig.DEBUG) {
            val topLevelRes = getTopLevelResources(this)
            val appRes = application.resources
            val actRes = resources
            val defLanguage = Locale.getDefault().language

            logConsoleVerbose("Language top level: " + LocalizationManager.getLocale(topLevelRes).language)
            logConsoleVerbose("Language application: " + LocalizationManager.getLocale(appRes).language)
            logConsoleVerbose("Language activity: " + LocalizationManager.getLocale(actRes).language)
            logConsoleVerbose("Language default: $defLanguage")
        }
    }

    private fun getTopLevelResources(a: Activity): Resources {
        try {
            return a.packageManager.getResourcesForApplication(a.applicationInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }
}