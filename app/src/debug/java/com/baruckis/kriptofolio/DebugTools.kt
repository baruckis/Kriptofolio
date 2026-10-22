/*
 * Copyright 2026 Andrius Baruckis www.baruckis.com | kriptofolio.app
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

package com.baruckis.kriptofolio

import android.app.Application
import com.facebook.stetho.Stetho

// Debug build only. Stetho lets Chrome developer tools inspect network traffic, database files
// and view layouts. The release build has an empty twin of this object and no Stetho dependency.
object DebugTools {

    fun init(application: Application) {
        Stetho.initializeWithDefaults(application)
    }

}
