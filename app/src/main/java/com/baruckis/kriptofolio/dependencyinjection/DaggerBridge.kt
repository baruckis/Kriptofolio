/*
 * Copyright 2018-2026 Andrius Baruckis www.baruckis.com | kriptofolio.app
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

import org.koin.dsl.module

/**
 * While Dagger and Koin run side by side (docs/decisions/005), the objects Dagger still creates
 * reach Koin only through this module, so both graphs share one instance of each. A line goes
 * away when Koin creates that object itself; the module goes away with Dagger.
 */
fun daggerBridgeModule(appComponent: AppComponent) = module {
    single { appComponent.licensesRepository() }
    single { appComponent.cryptocurrencyRepository() }
    single { appComponent.stringsLocalization() }
    single { appComponent.context() }
}
