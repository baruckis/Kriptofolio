/*
 * Copyright 2018-2020 Andrius Baruckis www.baruckis.com | kriptofolio.app
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

package com.baruckis.kriptofolio.ui.mainlist

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.baruckis.kriptofolio.R
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Tests for the main screen.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainListFragmentTest {

    /**
     * [ActivityTestRule] is a JUnit [@Rule][Rule] to launch your activity under test.
     *
     * Rules are interceptors which are executed for each test method and are important building
     * blocks of Junit tests.
     */
    @Rule
    @JvmField
    val mainActivityTestRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Test
    @Throws(Exception::class)
    fun clickAddFab_opensAddCryptoUi() {

        val evidenceRunId = InstrumentationRegistry.getArguments()
                .getString("scenarioEvidenceRunId")
        if (evidenceRunId != null) {
            if (!evidenceRunId.matches(Regex("run-[A-Za-z0-9-]{1,80}"))) {
                throw AssertionError("Invalid scenario evidence run ID")
            }
            awaitScenarioEvidenceRecorder(evidenceRunId)

            onView(withId(R.id.fab)).check(matches(isDisplayed()))
            onView(withId(R.id.layout_fragment_main_list_empty)).check(matches(isDisplayed()))
            saveEvidenceCheckpoint(evidenceRunId, "before-main-list")
        }

        // Click on the add crypto button
        onView(withId(R.id.fab)).perform(click())

        // Check if the add crypto screen is displayed
        onView(withId(R.id.coordinator_add_search)).check(matches(isDisplayed()))

        if (evidenceRunId != null) {
            saveEvidenceCheckpoint(evidenceRunId, "add-search-screen")
            if (InstrumentationRegistry.getArguments().getString("scenarioEvidenceProbe") ==
                    "assertion-failure") {
                throw AssertionError("Scenario evidence intentional failure probe")
            }
        }
    }

    private fun awaitScenarioEvidenceRecorder(runId: String) {
        val marker = File(InstrumentationRegistry.getTargetContext().cacheDir,
                "scenario-evidence-$runId-recording-ready")
        val deadline = SystemClock.elapsedRealtime() + 20_000L
        while (!marker.isFile && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(50)
        }
        if (!marker.isFile) {
            throw AssertionError("Scenario evidence recorder did not become ready")
        }
        marker.delete()
    }

    private fun saveEvidenceCheckpoint(runId: String, checkpointId: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
                ?: throw AssertionError("Scenario evidence screenshot was unavailable")
        val name = "scenario-evidence-$runId-$checkpointId"

        try {
            FileOutputStream(File(targetContext.cacheDir, "$name.png")).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw AssertionError("Scenario evidence screenshot could not be encoded")
                }
            }
            File(targetContext.cacheDir, "$name.ms").writeText(
                    SystemClock.elapsedRealtime().toString(), Charsets.US_ASCII)
        } finally {
            bitmap.recycle()
        }
    }

}
