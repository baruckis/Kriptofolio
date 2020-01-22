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

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup


/**
 * ItemDetails implementation provides the selection library with access to information about a
 * specific RecyclerView item. This class is a key component in controlling the behaviors of the
 * selection library in the context of a specific activity.
 */
class MainListItemDetails(private val identifier: String, private val adapterPosition: Int) : ItemDetailsLookup.ItemDetails<String>() {

    override fun getSelectionKey(): String? {
        return identifier
    }

    override fun getPosition(): Int {
        return adapterPosition
    }

    override fun inSelectionHotspot(e: MotionEvent): Boolean {
        return false
    }

    override fun inDragRegion(e: MotionEvent): Boolean {
        return false
    }
}