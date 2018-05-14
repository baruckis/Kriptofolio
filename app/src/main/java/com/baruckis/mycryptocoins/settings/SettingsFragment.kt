/*
 * Copyright 2018 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com
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

package com.baruckis.mycryptocoins.settings

import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v4.app.Fragment
import com.baruckis.mycryptocoins.R

/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.pref_main)
    }
}
