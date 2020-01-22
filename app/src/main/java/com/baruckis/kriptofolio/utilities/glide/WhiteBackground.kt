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

package com.baruckis.kriptofolio.utilities.glide

import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest


/**
 * A Glide custom transformation to add white background for the image.
 */

class WhiteBackground : BitmapTransformation() {

    override fun transform(
            @NonNull pool: BitmapPool, @NonNull toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return Transformation.whiteBackground(pool, toTransform, outWidth, outHeight)
    }

    override fun equals(other: Any?): Boolean {
        return other is WhiteBackground
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }

    override fun updateDiskCacheKey(@NonNull messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    companion object {
        private val VERSION = 1
        private val ID = "com.baruckis.kriptofolio.utilities.glide.WhiteBackground.$VERSION"
        private val ID_BYTES = ID.toByteArray(Key.CHARSET)
    }
}