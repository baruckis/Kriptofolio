/*
 * Copyright 2018-2019 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com
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

package com.baruckis.mycryptocoins.ui.mainlist

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.SpannableString
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.ui.settings.SettingsActivity
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main_list.*
import javax.inject.Inject

/**
 * UI for main my crypto coins screen.
 */

// To support injecting fragments which belongs to this activity we need to implement HasSupportFragmentInjector.
// We would not need to implement it, if our activity did not contain any fragments or the fragments did not need to inject anything.
class MainActivity : AppCompatActivity(), HasSupportFragmentInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: MainViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtain ViewModel from ViewModelProviders, using this activity as LifecycleOwner.
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)

        // Load the default values only for the first time when the user still hasn't used the preferences-screen.
        PreferenceManager.setDefaultValues(this, R.xml.pref_main, false)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        subscribeUi()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu. This adds items to the onActionButtonClick bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle onActionButtonClick bar item clicks here. The onActionButtonClick bar will
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

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = dispatchingAndroidInjector


    // Update the all these separate text fields when the data changes by observing data on the ViewModel, exposed as a LiveData.
    private fun subscribeUi() {

        viewModel.liveDataTotalHoldingsValueOnDateText.observe(this, Observer<String> { data ->
            val txt = StringBuilder(getString(R.string.string_total_value_holdings))
            if (data.toString().isNotEmpty())
                txt.append(getString(R.string.string_total_value_on_date_time, data))
            textview_total_value_on_date_time.text = txt
        })

        viewModel.liveDataTotalHoldingsValueFiat24hText.observe(this, Observer<SpannableString> { data ->
            textview_total_value_change_24h.text = data
        })

        viewModel.liveDataTotalHoldingsValueFiatText.observe(this, Observer<String> { data ->
            textview_fiat_value.text = data
            textview_fiat_value.requestLayout() // After text view size changed, force it to retry to align vertically in the center correctly.
        })

        viewModel.liveDataTotalHoldingsValueCryptoText.observe(this, Observer<String> { data ->
            textview_crypto_value.text = data
            textview_crypto_value.requestLayout() // After text view size changed, force it to retry to align vertically in the center correctly.
        })

        viewModel.liveDataCurrentFiatCurrencySign.observe(this, Observer<String> { data ->
            text_column_coin_fiat_btc_price.text = StringBuilder(getString(R.string.string_column_coin_fiat_price_amount, data, data))
            text_column_coin_change_24h_1h_7d.text = StringBuilder(getString(R.string.string_column_coin_change_24h_1h_7d, data))
        })

    }
}