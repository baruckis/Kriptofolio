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

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView


open class CustomItemAnimator : DefaultItemAnimator(), RecyclerView.ItemAnimator.ItemAnimatorFinishedListener {


    interface OnItemAnimatorListener {
        fun getNumberOfItemsToRemove(): Int
        fun onAnimationsFinishedOnItemRemoved()
        fun getNumberOfItemsToAdd(): Int
        fun onAnimationsFinishedOnItemAdded()
    }


    enum class AnimationsFinishedType {
        REMOVE,
        ADD
    }


    private var onItemAnimatorListener: OnItemAnimatorListener? = null

    private var animationsFinishedType: AnimationsFinishedType? = null

    private var countAlreadyRemoved = 0
    private var countAlreadyAdded = 0


    override fun onAnimationsFinished() {
        if (onItemAnimatorListener != null) {
            when (animationsFinishedType) {
                AnimationsFinishedType.REMOVE -> {
                    countAlreadyRemoved++
                    if (countAlreadyRemoved == onItemAnimatorListener!!.getNumberOfItemsToRemove()) {
                        countAlreadyRemoved = 0
                        onItemAnimatorListener!!.onAnimationsFinishedOnItemRemoved()
                    }
                }
                AnimationsFinishedType.ADD -> {
                    countAlreadyAdded++
                    if (countAlreadyAdded == onItemAnimatorListener!!.getNumberOfItemsToAdd()) {
                        countAlreadyAdded = 0
                        onItemAnimatorListener!!.onAnimationsFinishedOnItemAdded()
                    }
                }
            }
        }
    }

    override fun onRemoveFinished(viewHolder: RecyclerView.ViewHolder?) {
        animationsFinishedType = AnimationsFinishedType.REMOVE
        isRunning(this)
    }

    override fun onAddFinished(item: RecyclerView.ViewHolder?) {
        animationsFinishedType = AnimationsFinishedType.ADD
        isRunning(this)
    }


    fun setOnItemAnimatorListener(onItemAnimatorListener: OnItemAnimatorListener) {
        this.onItemAnimatorListener = onItemAnimatorListener
    }

}