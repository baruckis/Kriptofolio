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

package com.baruckis.mycryptocoins.ui.mainlist

import androidx.recyclerview.selection.ItemKeyProvider
import com.baruckis.mycryptocoins.db.Cryptocurrency


/**
 * This class decide on the key type used to identify selected items. For each item we need unique
 * key that can be three types: Parcelable, String, and Long ItemKey provider conjunction of stable
 * IDs. It will allow for a quick mapping between the IDs and the items that will handle the
 * selection by the selection library.
 */
class MainListItemKeyProvider(private var cryptocurrencyList: List<Cryptocurrency>,
                              scope: Int = ItemKeyProvider.SCOPE_CACHED) : ItemKeyProvider<String>(scope) {

    private lateinit var keyToPosition: MutableMap<String, Int>

    init {
        updataData(cryptocurrencyList)
    }

    fun updataData(newCryptocurrencyList: List<Cryptocurrency>) {
        cryptocurrencyList = newCryptocurrencyList
        keyToPosition = HashMap(cryptocurrencyList.size)

        for ((i, cryptocurrency) in cryptocurrencyList.withIndex()) {
            keyToPosition[cryptocurrency.id.toString()] = i
        }
    }


    override fun getKey(position: Int): String? {
        // As unique identifier lets make id which is also unique for each cryptocurrency.
        return cryptocurrencyList[position].id.toString()
    }

    override fun getPosition(key: String): Int {
        return keyToPosition.get(key) ?: -1
    }
}