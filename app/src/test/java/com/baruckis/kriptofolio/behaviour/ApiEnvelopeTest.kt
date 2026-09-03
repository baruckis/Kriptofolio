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

import com.baruckis.kriptofolio.api.ApiEmptyResponse
import com.baruckis.kriptofolio.api.ApiErrorResponse
import com.baruckis.kriptofolio.api.ApiResponse
import com.baruckis.kriptofolio.api.ApiSuccessResponse
import com.baruckis.kriptofolio.api.CoinMarketCap
import com.baruckis.kriptofolio.api.CryptocurrencyLatest
import com.baruckis.kriptofolio.utilities.API_SERVICE_AUTHENTICATION_NAME
import com.baruckis.kriptofolio.utilities.API_SERVICE_RESULTS_LIMIT
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response
import java.util.TimeZone

/**
 * Pins docs/BEHAVIOUR.md section 4, "The CoinMarketCap envelope", on the responses recorded in
 * src/test/resources/api. The fixtures go through the app's own Gson DTOs and its own
 * [ApiResponse] classifier, so a rewrite that changes either has to turn these exact bytes into
 * the same values.
 */
@RunWith(JUnit4::class)
class ApiEnvelopeTest {

    @Test
    fun `request constants`() {
        assertEquals("X-CMC_PRO_API_KEY", API_SERVICE_AUTHENTICATION_NAME)
        assertEquals(5000, API_SERVICE_RESULTS_LIMIT)
    }

    @Test
    fun `a listings response parses into a list with one quote per record`() {
        val response = Fixtures.listings("listings-latest-usd-200.json")
        assertEquals(0, response.status!!.errorCode)
        assertEquals(200, response.data!!.size)
        response.data!!.forEach { record ->
            assertNotNull("quote of ${record.symbol}", currencyField(record))
            assertTrue(record.quote.currency.price > 0)
        }
        val bitcoin = response.data!!.first()
        assertEquals(1, bitcoin.id)
        assertEquals("Bitcoin", bitcoin.name)
        assertEquals("BTC", bitcoin.symbol)
        assertEquals(1, bitcoin.cmcRank)
    }

    @Test
    fun `the quote is keyed by the fiat code and mapped through the alternate names`() {
        val eur = Fixtures.listings("listings-latest-eur-10.json")
        assertEquals(10, eur.data!!.size)
        eur.data!!.forEach { assertNotNull(currencyField(it)) }
        // The 93 accepted codes, in the order the annotation lists them.
        val names = quoteSerializedName()
        assertEquals("USD", names.value)
        assertEquals(92, names.alternate.size)
        assertEquals(93, (listOf(names.value) + names.alternate).distinct().size)
    }

    @Test
    fun `a quotes response is a map keyed by the coin id as a string`() {
        val response = Fixtures.quotes("quotes-latest-usd-1-1027.json")
        assertEquals(setOf("1", "1027"), response.data!!.keys)
        assertEquals("Ethereum", response.data!!.getValue("1027").name)
        assertNotNull(currencyField(response.data!!.getValue("1")))
    }

    @Test
    fun `the status timestamp is parsed as an instant, not as a local time`() {
        val response = Fixtures.listings("listings-edge-cases.json")
        // "2026-08-21T15:52:34.024Z" in the file.
        val expectedUtcMillis = 1_787_327_554_024L
        assertEquals(expectedUtcMillis, response.status!!.timestamp.time)
        assertEquals(20, response.status!!.creditCount)
    }

    @Test
    fun `an unsupported convert code is HTTP 200 with a null quote, not an error`() {
        val response = Fixtures.listings("response-200-unknown-convert.json")
        assertEquals(0, response.status!!.errorCode)
        val bitcoin = response.data!!.single()
        // Known behaviour K6: the non-null Kotlin property is null at runtime, because no
        // alternate name matches "XXX" and Gson assigns through reflection.
        assertNull(currencyField(bitcoin))
    }

    @Test
    fun `a null max_supply becomes zero in the non-null Double field`() {
        val edge = Fixtures.listings("listings-edge-cases.json")
        val ethereum = edge.data!!.first { it.symbol == "ETH" }
        val bitcoin = edge.data!!.first { it.symbol == "BTC" }
        // Known behaviour K13: Gson skips a JSON null for a primitive-backed field.
        assertEquals(0.0, maxSupplyField(ethereum), 0.0)
        assertEquals(21_000_000.0, maxSupplyField(bitcoin), 0.0)
    }

    @Test
    fun `the price range in one response spans 28 orders of magnitude`() {
        val edge = Fixtures.listings("listings-edge-cases.json").data!!
        val prices = edge.map { it.quote.currency.price }
        assertEquals(2.27526737415e-19, prices.min()!!, 0.0)
        assertEquals(3741731042.487026, prices.max()!!, 0.0)
        assertTrue(Math.log10(prices.max()!! / prices.min()!!) > 28.0)
    }

    @Test
    fun `an HTTP error with a JSON body reports the status error_message`() {
        val body = Fixtures.resourceText("api/error-401-invalid-key.json")
        val response = ApiResponse.create(Response.error<CoinMarketCap<List<CryptocurrencyLatest>>>(
                401, body.toResponseBody("application/json".toMediaType())))
        assertTrue(response is ApiErrorResponse)
        assertEquals("This API Key is invalid. ", (response as ApiErrorResponse).errorMessage)

        val notFound = Fixtures.resourceText("api/error-400-invalid-id.json")
        val response400 = ApiResponse.create(Response.error<CoinMarketCap<List<CryptocurrencyLatest>>>(
                400, notFound.toResponseBody("application/json".toMediaType())))
        assertEquals("No data found for 'id': '999999999'", (response400 as ApiErrorResponse).errorMessage)
    }

    @Test
    fun `an HTTP error with an empty body falls back to the HTTP reason phrase`() {
        val response = ApiResponse.create(Response.error<String>(
                503, "".toResponseBody("text/plain".toMediaType())))
        // Retrofit's Response.error() builds a raw response whose message is "Response.error()".
        assertEquals("Response.error()", (response as ApiErrorResponse).errorMessage)
    }

    @Test
    fun `a transport failure reports the exception message`() {
        val response = ApiResponse.create<String>(java.net.UnknownHostException("pro-api.coinmarketcap.com"))
        assertEquals("pro-api.coinmarketcap.com", response.errorMessage)
        val blank = ApiResponse.create<String>(RuntimeException())
        assertEquals("Unknown error.", blank.errorMessage)
    }

    @Test
    fun `a successful response with no body is the empty response`() {
        val response = ApiResponse.create(Response.success<String>(null))
        assertTrue(response is ApiEmptyResponse)
        val success = ApiResponse.create(Response.success("body"))
        assertTrue(success is ApiSuccessResponse)
    }

    @Test
    fun `nothing in the app reads error_code after a 2xx response`() {
        // Pins known behaviour K5 in the only way a JVM test can: the classifier accepts a
        // 2xx body with error_code != 0 as a success. The saving code then treats a null
        // data list as an empty listing, which empties the all_cryptocurrencies table.
        val body = """{"status":{"timestamp":"2026-08-21T15:54:01.771Z","error_code":1006,""" +
                """"error_message":"Your plan does not support this","elapsed":4,"credit_count":0},"data":null}"""
        val parsed = com.google.gson.Gson().fromJson(body, CoinMarketCap::class.java)
        val response = ApiResponse.create(Response.success(parsed))
        assertTrue(response is ApiSuccessResponse)
        assertEquals(1006, (response as ApiSuccessResponse).body.status!!.errorCode)
        assertNull(response.body.data)
    }

    // -- reflection helpers: the DTO declares these non-null, the JSON does not agree --

    private fun currencyField(record: CryptocurrencyLatest): Any? {
        val quote = record.quote
        val field = CryptocurrencyLatest.Quote::class.java.getDeclaredField("currency")
        field.isAccessible = true
        return field.get(quote)
    }

    private fun maxSupplyField(record: CryptocurrencyLatest): Double {
        val field = CryptocurrencyLatest::class.java.getDeclaredField("maxSupply")
        field.isAccessible = true
        return field.getDouble(record)
    }

    private fun quoteSerializedName(): SerializedName =
            CryptocurrencyLatest.Quote::class.java.getDeclaredField("currency")
                    .getAnnotation(SerializedName::class.java)!!

    companion object {
        init {
            // Gson's Date adapter falls back to ISO 8601 parsing, which is zone-aware; the test
            // sets a non-UTC default zone so a local-time parse would be caught.
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Vilnius"))
        }
    }
}
