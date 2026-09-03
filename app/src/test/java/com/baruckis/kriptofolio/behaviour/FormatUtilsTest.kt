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

package com.baruckis.kriptofolio.behaviour

import com.baruckis.kriptofolio.utilities.CRYPTO_FORMAT_PATTERN
import com.baruckis.kriptofolio.utilities.FIAT_FORMAT_PATTERN
import com.baruckis.kriptofolio.utilities.PERCENT_FORMAT_PATTERN
import com.baruckis.kriptofolio.utilities.TIME_12h_FORMAT_PATTERN
import com.baruckis.kriptofolio.utilities.TIME_24h_FORMAT_PATTERN
import com.baruckis.kriptofolio.utilities.TimeFormat
import com.baruckis.kriptofolio.utilities.ValueType
import com.baruckis.kriptofolio.utilities.formatDate
import com.baruckis.kriptofolio.utilities.getTextFirstChars
import com.baruckis.kriptofolio.utilities.roundValue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pins docs/BEHAVIOUR.md sections 2 and 3, "Number formatting" and "Date and time formatting".
 *
 * Two things are deliberately not pinned here. The sign-and-colour rule of
 * `getSpannableValueStyled` needs a real Android `SpannableString` and a `Context`; it is pinned
 * by the "before" screenshots listed in docs/ui-inventory.md, not by a JVM test, because adding
 * Robolectric to the 2019 build is not part of this stage. And the currency sign lookup is a
 * resource-array zip, pinned in [SettingsKeysTest] where the arrays are read from strings.xml.
 */
@RunWith(JUnit4::class)
class FormatUtilsTest {

    private lateinit var defaultLocale: Locale
    private lateinit var defaultZone: TimeZone

    @Before
    fun rememberDefaults() {
        defaultLocale = Locale.getDefault()
        defaultZone = TimeZone.getDefault()
        Locale.setDefault(Locale("EN"))
    }

    @After
    fun restoreDefaults() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultZone)
    }

    @Test
    fun `the three patterns`() {
        assertEquals("#,##0.00000000", CRYPTO_FORMAT_PATTERN)
        assertEquals("#,##0.00", FIAT_FORMAT_PATTERN)
        assertEquals("##0.00", PERCENT_FORMAT_PATTERN)
        assertEquals(CRYPTO_FORMAT_PATTERN, ValueType.Crypto.pattern)
        assertEquals(FIAT_FORMAT_PATTERN, ValueType.Fiat.pattern)
        assertEquals(PERCENT_FORMAT_PATTERN, ValueType.Percent.pattern)
        assertEquals("hh:mm:ss", TIME_12h_FORMAT_PATTERN)
        assertEquals("HH:mm:ss", TIME_24h_FORMAT_PATTERN)
    }

    @Test
    fun `values are truncated, never rounded`() {
        assertEquals("0.99999999", roundValue(0.999999999, ValueType.Crypto))
        assertEquals("1.99", roundValue(1.999, ValueType.Fiat))
        assertEquals("-0.00", roundValue(-0.005, ValueType.Percent))
        assertEquals("0.00", roundValue(0.009, ValueType.Percent))
        assertEquals("-1.99", roundValue(-1.999, ValueType.Fiat))
    }

    @Test
    fun `the fiat pattern groups thousands, the percent pattern does not`() {
        assertEquals("1,234,567.89", roundValue(1234567.891, ValueType.Fiat))
        assertEquals("1234567.89", roundValue(1234567.891, ValueType.Percent))
        assertEquals("1,000.00000000", roundValue(1000.0, ValueType.Crypto))
    }

    @Test
    fun `the 28 orders of magnitude of one response collapse to the pattern's digits`() {
        val edge = Fixtures.listings("listings-edge-cases.json").data!!
        val prices = edge.associate { it.symbol to it.quote.currency.price }
        assertEquals("0.00", roundValue(prices.getValue("TOMI"), ValueType.Fiat))
        assertEquals("0.00000000", roundValue(prices.getValue("TOMI"), ValueType.Crypto))
        assertEquals("3,741,731,042.48", roundValue(prices.getValue("MPRA"), ValueType.Fiat))
        assertEquals("0.99", roundValue(prices.getValue("USDC(WormHole)"), ValueType.Fiat))
    }

    @Test
    fun `known behaviour K2 and K3, the two cells formatted with the other pattern`() {
        val edge = Fixtures.listings("listings-edge-cases.json").data!!
        val blackPhoenix = edge.first { it.symbol == "BPX" }.quote.currency
        // The 7 d change is formatted with the Fiat pattern (MainRecyclerViewAdapter.kt:228) ...
        assertEquals("2,825.78", roundValue(blackPhoenix.percentChange7d, ValueType.Fiat))
        // ... where the 1 h and 24 h changes next to it use the Percent pattern.
        assertEquals("6141.08", roundValue(blackPhoenix.percentChange24h, ValueType.Percent))
        // The 24 h holding change is formatted with the Percent pattern (line 230), so a
        // change above a thousand has no grouping separator.
        assertEquals("1390.68", roundValue(1390.686, ValueType.Percent))
        assertEquals("1,390.68", roundValue(1390.686, ValueType.Fiat))
    }

    @Test
    fun `formatting follows the default locale, which the app sets to its UI language`() {
        Locale.setDefault(Locale("LT"))
        val symbols = DecimalFormatSymbols.getInstance(Locale("LT"))
        val expected = "1${symbols.groupingSeparator}234${symbols.decimalSeparator}56"
        assertEquals(expected, roundValue(1234.567, ValueType.Fiat))
        assertTrue("Lithuanian uses a comma as decimal separator", symbols.decimalSeparator == ',')

        Locale.setDefault(Locale("EN"))
        assertEquals("1,234.56", roundValue(1234.567, ValueType.Fiat))
    }

    @Test
    fun `first characters of a symbol for the icon fallback`() {
        assertEquals("BTC", getTextFirstChars("BTC", 3))
        assertEquals("USD", getTextFirstChars("USDC(WormHole)", 3))
        assertEquals("币安人", getTextFirstChars("币安人生", 3))
        assertEquals("XR", getTextFirstChars("XR", 3))
        assertEquals("", getTextFirstChars("", 3))
        assertEquals("", getTextFirstChars(null, 3))
    }

    // -- dates --

    private val instant = Date(1_787_327_554_024L) // 2026-08-21T15:52:34.024Z

    @Test
    fun `date and time are formatted in the device time zone although the label says UTC`() {
        // Known behaviour K1.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("21/08/2026 15:52:34", formatDate(instant, "dd/MM/yyyy", TimeFormat.Hours24()))
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Vilnius"))
        assertEquals("21/08/2026 18:52:34", formatDate(instant, "dd/MM/yyyy", TimeFormat.Hours24()))
    }

    @Test
    fun `the three date patterns`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("21/08/2026", formatDate(instant, "dd/MM/yyyy"))
        assertEquals("08/21/2026", formatDate(instant, "MM/dd/yyyy"))
        assertEquals("2026-08-21", formatDate(instant, "yyyy-MM-dd"))
    }

    @Test
    fun `twelve hour time appends the localized AM or PM word`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("21/08/2026 03:52:34 PM", formatDate(instant, "dd/MM/yyyy", TimeFormat.Hours12(), "AM", "PM"))
        assertEquals("21/08/2026 03:52:34 popiet", formatDate(instant, "dd/MM/yyyy", TimeFormat.Hours12(), "priešpiet", "popiet"))
        val morning = Date(instant.time - 12 * 3600 * 1000)
        assertEquals("21/08/2026 03:52:34 AM", formatDate(morning, "dd/MM/yyyy", TimeFormat.Hours12(), "AM", "PM"))
        // No AM/PM word given: the time is ambiguous and nothing is appended.
        assertEquals("21/08/2026 03:52:34", formatDate(instant, "dd/MM/yyyy", TimeFormat.Hours12()))
    }

    @Test
    fun `a missing date or pattern formats as an empty string`() {
        assertEquals("", formatDate(null, "dd/MM/yyyy"))
        assertEquals("", formatDate(instant, null))
    }
}

/**
 * The 93 fiat currencies × 4 UI languages. For every pair the on-screen price cell
 * ("<price> <CODE>") and header total ("<sign> <total>") are rebuilt the way the adapter and
 * the view model build them, and compared to what `java.text.NumberFormat` produces for the
 * same locale with two fraction digits, truncated. Nothing here is a hard-coded formatted
 * string: the expectation is derived from the locale, the code and the sign that the app
 * itself declares.
 */
@RunWith(Parameterized::class)
class FormatUtilsCurrencyLocaleTest(private val code: String, private val sign: String, private val locale: Locale) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} in {2}")
        fun data(): List<Array<Any>> {
            val strings = StringsXml.forLanguage("EN")
            val codes = strings.array("fiat_currency_code_array")
            val signs = strings.array("fiat_currency_sign_array")
            require(codes.size == 93 && signs.size == 93)
            return Fixtures.APP_LOCALES.flatMap { locale ->
                codes.indices.map { i -> arrayOf<Any>(codes[i], signs[i], locale) }
            }
        }

        private val samples = listOf(0.0, 0.004, 0.5, 1.0, 12.3456, 999.999, 1000.0, 78319.5336340814,
                3741731042.487026, -1.999, -0.001, 2.27526737415e-19)
    }

    private lateinit var defaultLocale: Locale

    @Before
    fun setLocale() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(locale) // LocalizationManager.updateResources does exactly this
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    private fun expectedFiat(value: Double): String {
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        nf.roundingMode = RoundingMode.DOWN
        nf.isGroupingUsed = true
        return nf.format(value)
    }

    @Test
    fun `price cell and header total are locale formatted with the code and sign the app declares`() {
        for (value in samples) {
            val expected = expectedFiat(value)
            // MainRecyclerViewAdapter.kt:225
            assertEquals("$expected $code", "${roundValue(value, ValueType.Fiat)} $code")
            // MainViewModel.kt:225-227
            assertEquals("$sign $expected", String.format("$sign ${roundValue(value, ValueType.Fiat)}"))
        }
    }

    @Test
    fun `the sign is never empty and the code is three upper case letters`() {
        assertTrue(sign.isNotEmpty())
        assertTrue(code.matches(Regex("[A-Z]{3}")))
    }
}
