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

package com.baruckis.kriptofolio.ui.common

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.baruckis.kriptofolio.BuildConfig
import com.baruckis.kriptofolio.R
import com.baruckis.kriptofolio.utilities.localization.LocalizationManager
import com.baruckis.kriptofolio.utilities.logConsoleVerbose
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

    /**
     * Keeps the window layout this app was designed for, whatever the platform does.
     *
     * Apps that target Android 15 are drawn edge to edge, and apps that target Android 16 cannot
     * opt out of it any more, so the window extends behind the status and navigation bars. This
     * gives the space back as padding on [content], and gives the two views included from
     * layout/system_bar_backgrounds.xml the height of each bar, so those areas keep the colours
     * the theme used to paint. Window.setStatusBarColor, which did that before, is a no operation
     * from Android 15 on.
     *
     * There is deliberately no version check. On Android 14 and below the decor view still keeps
     * the window inside the system bars, so the listener is handed zero insets, the padding stays
     * zero and both bar views stay zero height - the screen is laid out exactly as it was before.
     *
     * The display cutout is asked for together with the system bars because the pre Android 15
     * window avoided it as well, and in landscape it is the only thing keeping text out of the
     * notch.
     *
     * Call it after setContentView, with the view that holds the screen's content. The padding is
     * set rather than added, because the listener runs again on every configuration change and an
     * addition would accumulate.
     */
    protected fun fitContentInsideSystemBars(content: View) {
        val statusBarBackground: View = findViewById(R.id.status_bar_background)
        val navigationBarBackground: View = findViewById(R.id.navigation_bar_background)

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout())

            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            setViewHeight(statusBarBackground, insets.top)
            setViewHeight(navigationBarBackground, insets.bottom)

            windowInsets
        }
    }

    private fun setViewHeight(view: View, height: Int) {
        val layoutParams = view.layoutParams
        if (layoutParams.height != height) {
            layoutParams.height = height
            view.layoutParams = layoutParams
        }
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