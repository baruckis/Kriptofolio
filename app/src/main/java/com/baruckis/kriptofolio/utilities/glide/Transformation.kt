/*
 * Copyright 2018-2019 Andrius Baruckis www.baruckis.com | kriptofolio.app
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

package com.baruckis.kriptofolio.utilities.glide

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import androidx.annotation.NonNull
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.util.Synthetic
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


/**
 * An utility class with methods to efficiently modify Bitmaps. It is based on Glide library
 * TransformationUtils.java class.
 */

class Transformation {

    companion object {
        //define static functions here

        // See #738.
        private val MODELS_REQUIRING_BITMAP_LOCK = HashSet(
                Arrays.asList(
                        // Moto X gen 2
                        "XT1085",
                        "XT1092",
                        "XT1093",
                        "XT1094",
                        "XT1095",
                        "XT1096",
                        "XT1097",
                        "XT1098",
                        // Moto G gen 1
                        "XT1031",
                        "XT1028",
                        "XT937C",
                        "XT1032",
                        "XT1008",
                        "XT1033",
                        "XT1035",
                        "XT1034",
                        "XT939G",
                        "XT1039",
                        "XT1040",
                        "XT1042",
                        "XT1045",
                        // Moto G gen 2
                        "XT1063",
                        "XT1064",
                        "XT1068",
                        "XT1069",
                        "XT1072",
                        "XT1077",
                        "XT1078",
                        "XT1079"
                )
        )

        /**
         * https://github.com/bumptech/glide/issues/738 On some devices, bitmap drawing is not thread
         * safe.
         * This lock only locks for these specific devices. For other types of devices the lock is always
         * available and therefore does not impact performance
         */
        private val BITMAP_DRAWABLE_LOCK = if (MODELS_REQUIRING_BITMAP_LOCK.contains(Build.MODEL))
            ReentrantLock()
        else
            NoLock()


        /**
         * Take the image and add white background to it.
         *
         * @param pool   The BitmapPool obtain a bitmap from.
         * @param inBitmap   The Bitmap for which need to add white background.
         * @param destWidth    The width in pixels of the final Bitmap.
         * @param destHeight   The height in pixels of the final Bitmap.
         * @return The modified Bitmap with white background.
         */
        fun whiteBackground(@NonNull pool: BitmapPool, @NonNull inBitmap: Bitmap, destWidth: Int, destHeight: Int): Bitmap {

            val config = getNonNullConfig(inBitmap)
            val result = pool.get(destWidth, destHeight, config)

            BITMAP_DRAWABLE_LOCK.lock()
            try {
                val canvas = Canvas(result)
                canvas.drawColor(Color.WHITE) // Here we set our bitmap background to be white!
                canvas.drawBitmap(inBitmap, 0f, 0f, null)
                clear(canvas)
            } finally {
                BITMAP_DRAWABLE_LOCK.unlock()
            }

            return result
        }

        @NonNull
        private fun getNonNullConfig(@NonNull bitmap: Bitmap): Bitmap.Config {
            return if (bitmap.config != null) bitmap.config else Bitmap.Config.ARGB_8888
        }

        // Avoids warnings in M+.
        private fun clear(canvas: Canvas) {
            canvas.setBitmap(null)
        }

    }

    private class NoLock @Synthetic
    internal constructor() : Lock {

        override fun lock() {
            // do nothing
        }

        @Throws(InterruptedException::class)
        override fun lockInterruptibly() {
            // do nothing
        }

        override fun tryLock(): Boolean {
            return true
        }

        @Throws(InterruptedException::class)
        override fun tryLock(time: Long, @NonNull unit: TimeUnit): Boolean {
            return true
        }

        override fun unlock() {
            // do nothing
        }

        @NonNull
        override fun newCondition(): Condition {
            throw UnsupportedOperationException("Should not be called")
        }
    }

}