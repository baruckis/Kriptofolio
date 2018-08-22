/*
 * Copyright 2018 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com
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

package com.baruckis.mycryptocoins.mainlist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.view.Menu
import android.view.MenuItem
import com.baruckis.mycryptocoins.addsearchlist.AddSearchActivity
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.settings.SettingsActivity
import com.baruckis.mycryptocoins.utilities.InjectorUtils
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the default values only for the first time when the user still hasn't used the preferences-screen.
        PreferenceManager.setDefaultValues(this, R.xml.pref_main, false);

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ ->
            val intent = Intent(this@MainActivity, AddSearchActivity::class.java)
            startActivity(intent)
        }

        subscribeUi()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeUi() {

        val factory = InjectorUtils.provideMainViewModelFactory(application)

        // Obtain ViewModel from ViewModelProviders, using this activity as LifecycleOwner.
        val viewModel: MainViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)


        // Update the all these separate text fields when the data changes by observing data on the ViewModel, exposed as a LiveData.

        viewModel.liveDataTotalHoldingsValueFiat24hText.observe(this, Observer<SpannableString> { data ->
            textview_total_value_change_24h.text = data
        })

        viewModel.liveDataTotalHoldingsValueFiatText.observe(this, Observer<String> { data ->
            textview_fiat_value.text = data
            textview_fiat_value.requestLayout() // After text view size changed, force it to refresh to align vertically in the center correctly.
        })

        viewModel.liveDataTotalHoldingsValueCryptoText.observe(this, Observer<String> { data ->
            textview_crypto_value.text = data
            textview_crypto_value.requestLayout() // After text view size changed, force it to refresh to align vertically in the center correctly.
        })

    }
}
