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
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.baruckis.mycryptocoins.R
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Static methods used to format and style financial values.
 */

// Enum is a special data type that allows a variable to hold a value only from a set of predefined constants.
enum class ValueType(val pattern: String) {
    Crypto(CRYPTO_FORMAT_PATTERN),
    Fiat(FIAT_FORMAT_PATTERN),
    Percent(PERCENT_FORMAT_PATTERN)
}

// Sealed class represent restricted class hierarchies, where a value can have a type from a restricted set.
sealed class SpannableValueColorStyle {
    object Foreground : SpannableValueColorStyle()
    object Background : SpannableValueColorStyle()
}

// Round value depending on it's type by applying specific pattern as we want to set defined number of digits after decimal point.
fun roundValue(number: Double, type: ValueType): String {
    val df = DecimalFormat(type.pattern)
    df.roundingMode = RoundingMode.DOWN
    return df.format(number)
}

// Get value formatted and with special style applied.
fun getSpannableValueStyled(context: Context, value: Double, style: SpannableValueColorStyle, type: ValueType, left: String = "", right: String = ""): SpannableString {
    val valueSpannable: SpannableString
    var valueColor = ContextCompat.getColor(context, R.color.colorForMainListItemText)

    // Nested function to take program modularization further.
    fun getColorSpan(color: Int): CharacterStyle {
        return when (style) {
            is SpannableValueColorStyle.Foreground -> ForegroundColorSpan(color)
            is SpannableValueColorStyle.Background -> BackgroundColorSpan(color)
        }
    }

    var leftMod = left

    when {
        value > 0 -> {
            valueColor = ContextCompat.getColor(context, R.color.colorForValueChangePositive)
            leftMod = leftMod.plus("+")
        }
        value < 0 -> {
            valueColor = ContextCompat.getColor(context, R.color.colorForValueChangeNegative)
        }
    }

    valueSpannable = SpannableString("$leftMod${roundValue(value, type)}$right")
    valueSpannable.setSpan(getColorSpan(valueColor), 0, valueSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    return valueSpannable
}