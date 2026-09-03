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

import com.baruckis.kriptofolio.utilities.DATABASE_NAME
import com.baruckis.kriptofolio.utilities.getAmountFiatChange24hCounted
import com.baruckis.kriptofolio.utilities.getAmountFiatCounted
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins docs/BEHAVIOUR.md section 11, "Data persistence", on two real database files written by
 * released builds (see src/test/resources/db/README.md) — the upgrade-test contract for the
 * rewrite's database module.
 *
 * What "opens with the current AppDatabase" means here: Room opens a database by comparing the
 * identity hash stored in `room_master_table` with the hash of its generated schema, which is
 * the hash exported to app/schemas. A file whose hash matches, and whose tables are created by
 * the exact SQL the schema records, is a file the shipped 1.2.3 code opens without touching
 * `fallbackToDestructiveMigration()`. This test checks both, on the JVM, through a plain SQLite
 * driver; it deliberately does not add Robolectric to the 2019 build. The rewrite's own
 * migration tests (Room's MigrationTestHelper) open the same files for real.
 */
@RunWith(Parameterized::class)
class LegacyDatabaseTest(private val version: String, private val fiat: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> = listOf(arrayOf("1.2.1", "EUR"), arrayOf("1.2.3", "USD"))

        /** The synthetic portfolio every recorded file holds: id, symbol, amount. */
        private val PORTFOLIO = listOf(
                Triple(1, "BTC", 0.25),
                Triple(1027, "ETH", 2.0),
                Triple(2, "LTC", 10.0),
                Triple(74, "DOGE", 1000.0))

        private const val IDENTITY_HASH = "ad1c80913f23361aa985d56ecf84d645"
    }

    /** The "database" object of app/schemas/.../1.json, read with the Gson this project already has. */
    private fun schemaJson(): JsonObject =
            JsonParser().parse(Fixtures.schemaFile().readText()).asJsonObject.getAsJsonObject("database")

    private fun open(): Connection {
        // Copy the resource to a temporary file so the test never writes into the resource
        // itself (SQLite may create a journal next to the file it opens).
        val bytes = Fixtures.resourceBytes("db/kriptofolio-v$version.db")
        val copy = File.createTempFile("kriptofolio-v$version-", ".db")
        copy.deleteOnExit()
        copy.writeBytes(bytes)
        return DriverManager.getConnection("jdbc:sqlite:${copy.absolutePath}")
    }

    @Test
    fun `the database name is what the app opens`() {
        assertEquals("kriptofolio-db", DATABASE_NAME)
    }

    @Test
    fun `the identity hash matches the exported schema, so Room opens it without a migration`() {
        val schema = schemaJson()
        assertEquals(IDENTITY_HASH, schema.get("identityHash").asString)
        assertEquals(1, schema.get("version").asInt)
        open().use { db ->
            db.createStatement().use { st ->
                val rs = st.executeQuery("SELECT id, identity_hash FROM room_master_table")
                assertTrue(rs.next())
                assertEquals(42, rs.getInt(1))
                assertEquals(IDENTITY_HASH, rs.getString(2))
                assertTrue("exactly one row", !rs.next())
                val version = st.executeQuery("PRAGMA user_version")
                assertTrue(version.next())
                assertEquals(1, version.getInt(1))
            }
        }
    }

    @Test
    fun `both tables were created by exactly the SQL the schema records`() {
        val schema = schemaJson()
        val expected = HashMap<String, String>()
        for (entity in schema.getAsJsonArray("entities")) {
            val obj = entity.asJsonObject
            val table = obj.get("tableName").asString
            expected[table] = obj.get("createSql").asString.replace("IF NOT EXISTS ", "").replace("\${TABLE_NAME}", table)
        }
        assertEquals(setOf("my_cryptocurrencies", "all_cryptocurrencies"), expected.keys)
        open().use { db ->
            db.createStatement().use { st ->
                val rs = st.executeQuery("SELECT name, sql FROM sqlite_master WHERE type = 'table'")
                val actual = HashMap<String, String>()
                while (rs.next()) actual[rs.getString(1)] = rs.getString(2)
                assertEquals(setOf("android_metadata", "my_cryptocurrencies", "all_cryptocurrencies", "room_master_table"), actual.keys)
                for ((table, sql) in expected) {
                    assertEquals("CREATE statement of $table", sql, actual.getValue(table))
                }
            }
        }
    }

    @Test
    fun `the synthetic portfolio reads back, priced in the fiat currency that build was set to`() {
        open().use { db ->
            db.createStatement().use { st ->
                val rs = st.executeQuery("SELECT my_id, id, symbol, amount, currency_fiat, price_fiat, " +
                        "amount_fiat, amount_fiat_change_24h, price_percent_change_24h, last_fetched_date, rank " +
                        "FROM my_cryptocurrencies ORDER BY my_id")
                val rows = ArrayList<Map<String, Any>>()
                while (rs.next()) {
                    rows.add(mapOf(
                            "my_id" to rs.getInt("my_id"), "id" to rs.getInt("id"), "symbol" to rs.getString("symbol"),
                            "amount" to rs.getDouble("amount"), "currency_fiat" to rs.getString("currency_fiat"),
                            "price_fiat" to rs.getDouble("price_fiat"), "amount_fiat" to rs.getDouble("amount_fiat"),
                            "amount_fiat_change_24h" to rs.getDouble("amount_fiat_change_24h"),
                            "price_percent_change_24h" to rs.getDouble("price_percent_change_24h"),
                            "last_fetched_date" to rs.getLong("last_fetched_date"), "rank" to rs.getInt("rank")))
                }
                assertEquals(PORTFOLIO.size, rows.size)
                for ((expected, row) in PORTFOLIO.sortedBy { it.first }.zip(rows)) {
                    assertEquals(expected.first, row["my_id"])
                    assertEquals("my_id equals the embedded id", row["my_id"], row["id"])
                    assertEquals(expected.second, row["symbol"])
                    assertEquals(expected.third, row["amount"] as Double, 0.0)
                    assertEquals(fiat, row["currency_fiat"])
                    assertTrue("rank is positive", (row["rank"] as Int) > 0)
                    // last_fetched_date is the server timestamp in epoch milliseconds, in 2026.
                    val date = row["last_fetched_date"] as Long
                    assertTrue("$date looks like 2026 in ms", date > 1_767_225_600_000L && date < 1_798_761_600_000L)
                }
            }
        }
    }

    @Test
    fun `the stored computed columns are what CalculateUtils produces from the stored inputs`() {
        // Section 1: amount_fiat and amount_fiat_change_24h are stored, and they are exactly
        // the two functions applied to the row's own amount, price and 24 h percentage.
        open().use { db ->
            db.createStatement().use { st ->
                val rs = st.executeQuery("SELECT symbol, amount, price_fiat, price_percent_change_24h, amount_fiat, amount_fiat_change_24h FROM my_cryptocurrencies")
                var rows = 0
                while (rs.next()) {
                    rows++
                    val amountFiat = getAmountFiatCounted(rs.getDouble("amount"), rs.getDouble("price_fiat"))!!
                    assertEquals(rs.getString("symbol"), rs.getDouble("amount_fiat"), amountFiat, 0.0)
                    val change = getAmountFiatChange24hCounted(amountFiat, rs.getDouble("price_percent_change_24h"))!!
                    assertEquals(rs.getString("symbol"), rs.getDouble("amount_fiat_change_24h"), change, 0.0)
                }
                assertEquals(PORTFOLIO.size, rows)
            }
        }
    }

    @Test
    fun `the listing cache holds the 5000 coins the app asks for, in the same currency`() {
        open().use { db ->
            db.createStatement().use { st ->
                val rs = st.executeQuery("SELECT count(*), count(DISTINCT currency_fiat), min(currency_fiat), min(rank), max(rank) FROM all_cryptocurrencies")
                assertTrue(rs.next())
                assertEquals(5000, rs.getInt(1))
                assertEquals(1, rs.getInt(2))
                assertEquals(fiat, rs.getString(3))
                assertEquals(1, rs.getInt(4))
                assertTrue(rs.getInt(5) >= 5000)
                val btc = st.executeQuery("SELECT id, name FROM all_cryptocurrencies WHERE symbol = 'BTC' LIMIT 1")
                assertTrue(btc.next())
                assertEquals(1, btc.getInt(1))
                assertEquals("Bitcoin", btc.getString(2))
            }
        }
    }

    @Test
    fun `the preference file of the same install carries the four keys with their exact names`() {
        val xml = Fixtures.resourceText("db/kriptofolio-v$version-preferences.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
        val entries = HashMap<String, Pair<String, String>>() // name -> (type, value)
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? org.w3c.dom.Element ?: continue
            val value = if (e.tagName == "string") e.textContent else e.getAttribute("value")
            entries[e.getAttribute("name")] = e.tagName to value
        }
        assertEquals(setOf("preference language", "preference fiat currency", "preference date format", "preference 24h switch"), entries.keys)
        assertEquals("string" to "EN", entries.getValue("preference language"))
        assertEquals("string" to fiat, entries.getValue("preference fiat currency"))
        assertEquals("string", entries.getValue("preference date format").first)
        assertEquals("boolean", entries.getValue("preference 24h switch").first)
        assertNotNull(StringsXml.forLanguage("EN").array("pref_date_format_list_values").contains(entries.getValue("preference date format").second))
    }
}
