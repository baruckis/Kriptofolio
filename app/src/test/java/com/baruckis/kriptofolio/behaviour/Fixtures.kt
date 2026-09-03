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

import com.baruckis.kriptofolio.api.CoinMarketCap
import com.baruckis.kriptofolio.api.CryptocurrencyLatest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.Locale

/**
 * Shared helpers for the characterization tests in this package.
 *
 * The tests pin the behaviour described in docs/BEHAVIOUR.md, section by section, on the
 * 2019 code as it ships in 1.2.3. They are written to be moved to the rewrite by changing only
 * the package name, so everything they need is either a public function of the app, a recorded
 * fixture under src/test/resources, or a resource file read from disk here.
 */
object Fixtures {

    /** The four UI languages exactly as the app builds them (dependencyinjection/LanguageCodes.kt). */
    val APP_LOCALES: List<Locale> = listOf("EN", "HE", "LT", "SW").map { Locale(it) }

    /** Resource folder qualifier for each language, as used under src/main/res. */
    val LOCALE_RES_DIR: Map<String, String> = mapOf(
            "EN" to "values", "HE" to "values-iw", "LT" to "values-lt", "SW" to "values-sw-rKE")

    private val gson = Gson()

    fun resourceText(path: String): String =
            Fixtures::class.java.classLoader!!.getResource(path)?.readText()
                    ?: throw IllegalStateException("missing test resource $path")

    fun resourceBytes(path: String): ByteArray =
            Fixtures::class.java.classLoader!!.getResource(path)?.readBytes()
                    ?: throw IllegalStateException("missing test resource $path")

    /** A recorded listings/latest response parsed with the app's own DTO types. */
    fun listings(name: String): CoinMarketCap<List<CryptocurrencyLatest>> {
        val type = object : TypeToken<CoinMarketCap<List<CryptocurrencyLatest>>>() {}.type
        return gson.fromJson(resourceText("api/$name"), type)
    }

    /** A recorded quotes/latest response parsed with the app's own DTO types. */
    fun quotes(name: String): CoinMarketCap<HashMap<String, CryptocurrencyLatest>> {
        val type = object : TypeToken<CoinMarketCap<HashMap<String, CryptocurrencyLatest>>>() {}.type
        return gson.fromJson(resourceText("api/$name"), type)
    }

    /**
     * The module directory. Gradle runs unit tests with the module as the working directory, an
     * IDE may run them from the project root; both are accepted.
     */
    val moduleDir: File by lazy {
        listOf(File("."), File("app")).map { it.absoluteFile }
                .firstOrNull { File(it, "src/main/res/values/strings.xml").isFile }
                ?: throw IllegalStateException("cannot find app/src/main/res from ${File(".").absolutePath}")
    }

    fun mainResFile(relative: String): File = File(moduleDir, "src/main/res/$relative")

    fun schemaFile(): File = File(moduleDir, "schemas/com.baruckis.kriptofolio.db.AppDatabase/1.json")
}
