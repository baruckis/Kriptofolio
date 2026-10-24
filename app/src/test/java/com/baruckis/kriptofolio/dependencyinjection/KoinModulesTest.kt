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

package com.baruckis.kriptofolio.dependencyinjection

import android.content.Context
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import org.mockito.Mockito.mock
import java.util.concurrent.Executor

/**
 * Koin's module verification: every constructor parameter of every Koin definition must be
 * defined in the app's Koin modules. It runs on the JVM without starting the app, so a missing
 * definition fails the unit test gate instead of crashing a screen. It checks the same list of
 * modules that App starts, [appKoinModules].
 */
@OptIn(KoinExperimentalAPI::class)
@RunWith(JUnit4::class)
class KoinModulesTest {

    private val appModules = module {
        includes(appKoinModules(mock(Context::class.java)))
    }

    @Test
    fun koinModulesVerify() {
        // AppExecutors also has a constructor that takes three executors, kept for tests that run
        // everything on one thread. Koin calls only the constructor without parameters.
        appModules.verify(extraTypes = listOf(Executor::class))
    }
}
