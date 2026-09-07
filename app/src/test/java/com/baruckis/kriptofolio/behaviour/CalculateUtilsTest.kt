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

import com.baruckis.kriptofolio.utilities.getAmountFiatChange24hCounted
import com.baruckis.kriptofolio.utilities.getAmountFiatCounted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Pins docs/BEHAVIOUR.md section 1, "Portfolio maths".
 *
 * The two functions under test are the whole of the app's arithmetic. Everything else that
 * looks like maths (the totals in the header) is a plain sum of the values these produce, and
 * that sum lives inside a ViewModel that cannot be constructed on the JVM; it is pinned by the
 * numbers in the recorded database files instead (see LegacyDatabaseTest).
 */
@RunWith(JUnit4::class)
class CalculateUtilsTest {

    @Test
    fun `holding value is amount times price`() {
        assertEquals(2.0 * 2417.21014311148, getAmountFiatCounted(2.0, 2417.21014311148)!!, 0.0)
        assertEquals(0.0, getAmountFiatCounted(0.0, 78319.5336340814)!!, 0.0)
    }

    @Test
    fun `holding value is null when the amount is null`() {
        assertNull(getAmountFiatCounted(null, 78319.5336340814))
    }

    @Test
    fun `holding 24h change is the value times the percentage over one hundred`() {
        val value = 19579.8834085203
        assertEquals(value * (2.00310461 / 100), getAmountFiatChange24hCounted(value, 2.00310461)!!, 0.0)
        assertEquals(value * (-96.59454464 / 100), getAmountFiatChange24hCounted(value, -96.59454464)!!, 0.0)
    }

    @Test
    fun `holding 24h change is null when the holding value is null`() {
        assertNull(getAmountFiatChange24hCounted(null, 2.00310461))
    }

    @Test
    fun `the arithmetic is plain double arithmetic with its usual imprecision`() {
        // 0.1 + 0.2 style: nothing rounds, nothing uses BigDecimal. The rewrite moves the
        // domain to BigDecimal (decision 10); this test records what it has to stay equal to
        // at the displayed precision.
        val value = getAmountFiatCounted(3.0, 0.1)!!
        assertTrue(value != 0.3)
        assertEquals(0.3, value, 1e-15)
    }

    @Test
    fun `numbers the 1_2_1 build stored on a device are reproduced exactly`() {
        // Bitcoin row of src/test/resources/db/kriptofolio-v1.2.1.db: amount 0.25 at the
        // recorded price gives the recorded amount_fiat, and that gives the recorded change.
        val price = 78319.5336340814
        val amountFiat = getAmountFiatCounted(0.25, price)!!
        assertEquals(19579.8834085203, amountFiat, 1e-9)
        val change = getAmountFiatChange24hCounted(amountFiat, 2.00310461)!!
        assertEquals(392.205547188696, change, 1e-9)
    }

    @Test
    fun `values from the edge case fixture survive the arithmetic`() {
        val edge = Fixtures.listings("listings-edge-cases.json").data!!
        val smallest = edge.first { it.symbol == "TOMI" }.quote.currency
        val largest = edge.first { it.name == "Maya Preferred PRA" }.quote.currency

        // 28 orders of magnitude apart, and a plain Double carries both without overflow.
        assertEquals(2.27526737415e-19 * 1000.0, getAmountFiatCounted(1000.0, smallest.price)!!, 0.0)
        assertEquals(3741731042.487026 * 2.0, getAmountFiatCounted(2.0, largest.price)!!, 0.0)

        val biggestLoss = edge.first { it.symbol == "FELIS" }.quote.currency
        val change = getAmountFiatChange24hCounted(100.0, biggestLoss.percentChange24h)!!
        assertTrue("a 96 % loss is a negative change", change < -96.0 && change > -97.0)
    }
}
