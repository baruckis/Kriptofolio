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

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Reads a strings.xml the way the tests need it: string values by name and string arrays by
 * name, with the DOCTYPE entities the file declares expanded by the XML parser itself.
 *
 * Android's resource compiler strips the surrounding quotes of a `"quoted"` value; this reader
 * does the same so the values compare equal to what the app sees at runtime.
 */
class StringsXml(file: File) {

    private val strings = HashMap<String, String>()
    private val arrays = HashMap<String, List<String>>()

    init {
        val factory = DocumentBuilderFactory.newInstance()
        val doc = factory.newDocumentBuilder().parse(file)
        val root = doc.documentElement
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            when (node.tagName) {
                "string" -> strings[name] = unquote(node.textContent)
                "string-array" -> {
                    val items = node.getElementsByTagName("item")
                    arrays[name] = (0 until items.length).map { unquote(items.item(it).textContent) }
                }
            }
        }
    }

    private fun unquote(raw: String): String {
        val s = raw.trim()
        return if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) s.substring(1, s.length - 1) else s
    }

    fun string(name: String): String = strings[name] ?: throw NoSuchElementException("no string $name")

    fun stringOrNull(name: String): String? = strings[name]

    fun array(name: String): List<String> = arrays[name] ?: throw NoSuchElementException("no array $name")

    fun has(name: String): Boolean = name in strings

    companion object {
        fun forLanguage(code: String): StringsXml =
                StringsXml(Fixtures.mainResFile("${Fixtures.LOCALE_RES_DIR.getValue(code)}/strings.xml"))
    }
}
