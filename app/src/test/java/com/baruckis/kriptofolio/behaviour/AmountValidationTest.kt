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

import android.text.Editable
import android.widget.EditText
import com.baruckis.kriptofolio.mock
import com.baruckis.kriptofolio.utilities.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

/**
 * Pins docs/BEHAVIOUR.md section 6, "The amount dialog".
 *
 * The dialog's validator is a private six-line lambda inside a DialogFragment
 * (ui/addsearchlist/CryptocurrencyAmountDialog.kt:161-170): `text.toDouble()` in a try/catch,
 * handed to the `EditText.validate` extension. This test drives the real extension with the
 * same predicate, on a mocked EditText, so what it pins is the acceptance rule of the app and
 * the error/no-error side effect on the field. What it cannot pin is the `numberDecimal` input
 * filter that stands in front of the validator on a device, which is why most of the "accepted"
 * inputs below can never be typed; see K8.
 */
@RunWith(JUnit4::class)
class AmountValidationTest {

    private val error = "Valid number is required!"

    /** The predicate exactly as CryptocurrencyAmountDialog.onValidateAndConfirm writes it. */
    private val dialogPredicate: (String) -> Boolean = { text ->
        try {
            text.toDouble()
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun editTextWith(text: String): EditText {
        val editable = mock<Editable>()
        `when`(editable.toString()).thenReturn(text)
        val editText = mock<EditText>()
        `when`(editText.text).thenReturn(editable)
        return editText
    }

    private fun accepted(text: String): Boolean = editTextWith(text).validate(dialogPredicate, error)

    @Test
    fun `plain decimals are accepted`() {
        for (text in listOf("0", "1", "0.25", "1000", "1.23", ".5", "5.", "00012")) {
            assertTrue("'$text' should be accepted", accepted(text))
        }
    }

    @Test
    fun `everything Double_parseDouble accepts is accepted, keyboard or not`() {
        // Known behaviour K8. None of these can be typed through the numberDecimal filter.
        for (text in listOf("-5", "1e3", "1E-8", "Infinity", "NaN", "0x1p3", "12d", " 7 ", "1_000".replace("_", ""))) {
            assertTrue("'$text' should be accepted", accepted(text))
        }
    }

    @Test
    fun `text that is not a number is rejected with the error on the field`() {
        for (text in listOf("", ".", "abc", "1,5", "1.2.3", "1 000", "₿1", "--1")) {
            val editText = editTextWith(text)
            assertFalse("'$text' should be rejected", editText.validate(dialogPredicate, error))
            verify(editText).error = error
        }
    }

    @Test
    fun `a valid value clears the error`() {
        val editText = editTextWith("0.25")
        assertTrue(editText.validate(dialogPredicate, error))
        verify(editText).error = null
    }

    @Test
    fun `a decimal comma is rejected in every language`() {
        // The Lithuanian keyboard offers a comma and the numberDecimal filter accepts it in
        // that locale; the validator still uses Double.parseDouble, which does not.
        assertFalse(accepted("1,5"))
        assertEquals(1.5, "1.5".toDouble(), 0.0)
    }
}
