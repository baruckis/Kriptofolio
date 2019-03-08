/*
 * Copyright 2018-2019 Andrius Baruckis www.baruckis.com | kriptofolio.app
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

package com.baruckis.kriptofolio.utilities

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Extension functions allow you to add behaviour to a class without the need of getting to its
 * source code, since it can be declared outside the scope of its class.
 */

// If the text changes, do some actions.
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object: TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
    })
}

// Check if edit text is empty or not and invoke actions accordingly.
fun EditText.nonEmpty(onEmpty: (() -> Unit), onNotEmpty: (() -> Unit)) {
    if (this.text.toString().isEmpty()) onEmpty.invoke()
    this.afterTextChanged {
        if (it.isEmpty()) onEmpty.invoke()
        if (it.isNotEmpty()) onNotEmpty.invoke()
    }
}

// Validate user input with custom validator and show error if validation did not pass.
fun EditText.validate(validator: (String) -> Boolean, message: String):Boolean {
    val isValid = validator(this.text.toString())
    this.error = if (isValid) null else message
    return isValid
}