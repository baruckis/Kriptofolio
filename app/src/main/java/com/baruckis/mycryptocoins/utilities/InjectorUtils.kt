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

package com.baruckis.mycryptocoins.utilities

import android.content.Context
import com.baruckis.mycryptocoins.addsearchlist.AddSearchViewModelFactory
import com.baruckis.mycryptocoins.data.AppDatabase
import com.baruckis.mycryptocoins.data.CryptocurrencyRepository
import com.baruckis.mycryptocoins.mainlist.MainViewModelFactory

/**
 * Static methods used to inject classes needed for various Activities and Fragments.
 */
object InjectorUtils {

    private fun getCryptocurrencyRepository(context: Context): CryptocurrencyRepository {
        return CryptocurrencyRepository.getInstance(
                AppDatabase.getInstance(context).cryptocurrencyDao())
    }

    fun provideMainViewModelFactory(
            context: Context
    ): MainViewModelFactory {
        val repository = getCryptocurrencyRepository(context)
        return MainViewModelFactory(repository)
    }

    fun provideAddSearchViewModelFactory(
            context: Context
    ): AddSearchViewModelFactory {
        val repository = getCryptocurrencyRepository(context)
        return AddSearchViewModelFactory(repository)
    }
}