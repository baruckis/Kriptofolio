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

package com.baruckis.mycryptocoins.dependencyinjection

import com.baruckis.mycryptocoins.ui.addsearchlist.AddSearchActivity
import com.baruckis.mycryptocoins.ui.mainlist.MainActivity
import com.baruckis.mycryptocoins.ui.settings.SettingsActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * All activities intended to use Dagger @Inject should be listed here.
 */
@Module
abstract class ActivityBuildersModule {

    @ContributesAndroidInjector(modules = [MainListFragmetBuildersModule::class]) // Where to apply the injection.
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun contributeAddSearchActivity(): AddSearchActivity

    @ContributesAndroidInjector(modules = [SettingsFragmetBuildersModule::class]) // Where to apply the injection.
    abstract fun contributeSettingsActivity(): SettingsActivity
}