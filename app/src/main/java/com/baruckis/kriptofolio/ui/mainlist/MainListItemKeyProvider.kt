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

package com.baruckis.kriptofolio.ui.mainlist

import androidx.recyclerview.selection.ItemKeyProvider
import com.baruckis.kriptofolio.db.MyCryptocurrency


/**
 * This class decide on the key type used to identify selected items. For each item we need unique
 * key that can be three types: Parcelable, String, and Long ItemKey provider conjunction of stable
 * IDs. It will allow for a quick mapping between the IDs and the items that will handle the
 * selection by the selection library.
 */
class MainListItemKeyProvider(private var myCryptocurrencyList: List<MyCryptocurrency>,
                              scope: Int = ItemKeyProvider.SCOPE_CACHED) : ItemKeyProvider<String>(scope) {

    private lateinit var keyToPosition: MutableMap<String, Int>

    init {
        updataData(myCryptocurrencyList)
    }

    fun updataData(newCryptocurrencyList: List<MyCryptocurrency>) {
        myCryptocurrencyList = newCryptocurrencyList
        keyToPosition = HashMap(myCryptocurrencyList.size)

        for ((i, cryptocurrency) in myCryptocurrencyList.withIndex()) {
            keyToPosition[cryptocurrency.myId.toString()] = i
        }
    }


    override fun getKey(position: Int): String? {
        // As unique identifier lets make id which is also unique for each cryptocurrency.
        return myCryptocurrencyList[position].myId.toString()
    }

    override fun getPosition(key: String): Int {
        return keyToPosition.get(key) ?: -1
    }
}