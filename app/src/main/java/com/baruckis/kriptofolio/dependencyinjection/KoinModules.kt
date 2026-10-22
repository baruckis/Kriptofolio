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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baruckis.kriptofolio.ui.settings.thirdpartysoft.LibrariesLicensesViewModel
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

// Every Koin module the app starts. App.onCreate and KoinModulesTest both use this list, so a module
// added here is verified as well.
fun appKoinModules(appComponent: AppComponent) = listOf(daggerBridgeModule(appComponent), viewModelsKoinModule)

// ViewModels that Koin creates. A screen moves here from ViewModelsModule in one step.
val viewModelsKoinModule = module {
    factory { LibrariesLicensesViewModel(get()) }
}

/**
 * Creates a ViewModel from Koin when ViewModelProvider has none stored yet, so a screen keeps its
 * ViewModelProvider call and its scope. koin-android's viewModel() delegates would do the same,
 * but they need newer AndroidX libraries than this app uses (spike #36).
 */
object KoinViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GlobalContext.get().get(modelClass.kotlin)
    }
}
