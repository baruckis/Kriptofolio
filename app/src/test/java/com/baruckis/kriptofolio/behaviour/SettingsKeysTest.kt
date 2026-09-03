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

import com.baruckis.kriptofolio.api.CryptocurrencyLatest
import com.baruckis.kriptofolio.dependencyinjection.Language
import com.baruckis.kriptofolio.dependencyinjection.LanguageCodes
import com.google.gson.annotations.SerializedName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins docs/BEHAVIOUR.md section 7, "Settings", and the resource arrays of section 2.
 *
 * The preference keys are the second user-data contract of this app (the first is the database).
 * They live in strings.xml today; the rewrite moves them to Kotlin constants (ADR-010) and this
 * test is what has to stay green when it does, with the resource lookups replaced by the
 * constants.
 */
@RunWith(JUnit4::class)
class SettingsKeysTest {

    private val en = StringsXml.forLanguage("EN")

    @Test
    fun `the four stored preference keys, exactly as written to the preference file`() {
        assertEquals("preference language", en.string("pref_language_key"))
        assertEquals("preference fiat currency", en.string("pref_fiat_currency_key"))
        assertEquals("preference date format", en.string("pref_date_format_key"))
        assertEquals("preference 24h switch", en.string("pref_24h_switch_key"))
    }

    @Test
    fun `the action preference keys`() {
        assertEquals("rate app", en.string("pref_rate_app_key"))
        assertEquals("share app", en.string("pref_share_app_key"))
        assertEquals("preference donate crypto", en.string("pref_donate_crypto_key"))
        assertEquals("buy me coffee", en.string("pref_buy_me_coffee_key"))
        assertEquals("contact", en.string("pref_contact_key"))
        assertEquals("website", en.string("pref_website_key"))
        assertEquals("author", en.string("pref_author_key"))
        assertEquals("source", en.string("pref_source_key"))
        assertEquals("privacy policy", en.string("pref_privacy_policy_key"))
        assertEquals("third party software", en.string("pref_third_party_software_key"))
        assertEquals("license", en.string("pref_license_key"))
        assertEquals("app", en.string("pref_app_key"))
    }

    @Test
    fun `keys are not translated`() {
        for (code in listOf("HE", "LT", "SW")) {
            val other = StringsXml.forLanguage(code)
            for (key in listOf("pref_language_key", "pref_fiat_currency_key", "pref_date_format_key", "pref_24h_switch_key")) {
                assertFalse("$key must not be overridden in $code", other.has(key))
            }
        }
    }

    @Test
    fun `defaults differ by UI language`() {
        val expected = mapOf(
                "EN" to Triple("EN", "USD", "dd/MM/yyyy"),
                "HE" to Triple("HE", "ILS", "dd/MM/yyyy"),
                "LT" to Triple("LT", "EUR", "yyyy-MM-dd"),
                "SW" to Triple("SW", "USD", "dd/MM/yyyy"))
        for ((code, triple) in expected) {
            val strings = StringsXml.forLanguage(code)
            assertEquals("language default in $code", triple.first, strings.string("pref_default_language_value"))
            assertEquals("fiat default in $code", triple.second, strings.string("pref_default_fiat_currency_value"))
            assertEquals("date format default in $code", triple.third, strings.string("pref_default_date_format_value"))
        }
    }

    @Test
    fun `pref_main uses those keys and defaults, and the 24h switch defaults to on`() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Fixtures.mainResFile("xml/pref_main.xml"))
        val byKey = HashMap<String, org.w3c.dom.Element>()
        val all = doc.getElementsByTagName("*")
        for (i in 0 until all.length) {
            val e = all.item(i) as org.w3c.dom.Element
            val key = e.getAttribute("android:key")
            if (key.isNotEmpty()) byKey[key] = e
        }
        assertEquals("@string/pref_default_language_value", byKey.getValue("@string/pref_language_key").getAttribute("android:defaultValue"))
        assertEquals("@string/pref_default_fiat_currency_value", byKey.getValue("@string/pref_fiat_currency_key").getAttribute("android:defaultValue"))
        assertEquals("@string/pref_default_date_format_value", byKey.getValue("@string/pref_date_format_key").getAttribute("android:defaultValue"))
        assertEquals("true", byKey.getValue("@string/pref_24h_switch_key").getAttribute("android:defaultValue"))
        assertEquals("SwitchPreference", byKey.getValue("@string/pref_24h_switch_key").tagName)
        assertEquals(16, byKey.size)
    }

    @Test
    fun `language values and codes`() {
        assertEquals(listOf("EN", "HE", "LT", "SW"), en.array("pref_language_list_values"))
        assertEquals(listOf("English", "עִברִית", "Lietuvių", "Swahili"), en.array("pref_language_list_entries"))
        assertEquals(listOf("EN", "HE", "LT", "SW"), listOf(LanguageCodes.ENGLISH, LanguageCodes.HEBREW, LanguageCodes.LITHUANIAN, LanguageCodes.SWAHILI))
        assertEquals(Language.English, Language.DEFAULT)
        assertEquals(Language.Hebrew, Language.fromLocale(Locale("he", "IL")))
        assertEquals(Language.Hebrew, Language.fromLocale(Locale("iw")))
        assertEquals(Language.English, Language.fromLocale(Locale("de")))
    }

    @Test
    fun `date format values`() {
        assertEquals(listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd"), en.array("pref_date_format_list_values"))
        assertEquals(en.array("pref_date_format_list_values"), en.array("pref_date_format_list_entries"))
        assertEquals("13:00", en.string("pref_24h_switch_24h_summary"))
        assertEquals("01:00 PM", en.string("pref_24h_switch_12h_summary"))
    }

    @Test
    fun `93 fiat currencies, four parallel arrays in the same order`() {
        val codes = en.array("fiat_currency_code_array")
        val signs = en.array("fiat_currency_sign_array")
        val values = en.array("pref_fiat_currency_list_values")
        val entries = en.array("pref_fiat_currency_list_entries")
        assertEquals(93, codes.size)
        assertEquals(93, signs.size)
        assertEquals(codes, values)
        assertEquals(93, entries.size)
        assertEquals(93, codes.distinct().size)
        assertEquals("USD", codes.first())
        assertEquals("VND", codes.last())
        for ((i, entry) in entries.withIndex()) {
            assertTrue("entry $i starts with its code", entry.startsWith(codes[i] + " - "))
        }
        // Every language lists the same 93 codes, in the same order, under the entries.
        for (code in listOf("HE", "LT", "SW")) {
            val other = StringsXml.forLanguage(code)
            assertEquals(codes, other.array("pref_fiat_currency_list_entries").map { it.substringBefore(" - ") })
        }
    }

    @Test
    fun `the 93 codes are exactly the codes the API mapping accepts, in the same order`() {
        val annotation = CryptocurrencyLatest.Quote::class.java.getDeclaredField("currency")
                .getAnnotation(SerializedName::class.java)!!
        val accepted = listOf(annotation.value) + annotation.alternate
        assertEquals(en.array("fiat_currency_code_array"), accepted)
    }

    @Test
    fun `known behaviour K4, the currency signs as they are today`() {
        val codes = en.array("fiat_currency_code_array")
        val signs = en.array("fiat_currency_sign_array")
        val sign = codes.zip(signs).toMap()
        assertEquals("$", sign.getValue("USD"))
        assertEquals("€", sign.getValue("EUR"))
        assertEquals("₪", sign.getValue("ILS"))
        // The three copy-paste signs and the wrong rand sign, recorded rather than fixed.
        assertEquals("₾\"", sign.getValue("GEL"))
        assertEquals("₾₵", sign.getValue("GHS"))
        assertEquals("₾Q", sign.getValue("GTQ"))
        assertEquals("Rs", sign.getValue("ZAR"))
        val entries = codes.zip(en.array("pref_fiat_currency_list_entries")).toMap()
        assertEquals("GEL - Georgian Lari (₾))", entries.getValue("GEL"))
        assertEquals("SEK - Swedish Krona ( kr)", entries.getValue("SEK"))
    }

    @Test
    fun `strings that carry a UTC label next to a local time`() {
        // Known behaviour K1: the label is fixed text, the time is formatted in the device zone.
        assertEquals(" (%1\$s UTC)", en.string("string_total_value_on_date_time"))
        assertEquals("Last updated (%1\$s UTC)", en.string("string_info_last_updated_on_date_time"))
        assertEquals("― ― ―", en.string("string_no_number"))
    }
}
