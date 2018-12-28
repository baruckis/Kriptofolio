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

package com.baruckis.mycryptocoins.ui.addsearchlist

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.databinding.ActivityAddSearchBinding
import com.baruckis.mycryptocoins.db.Cryptocurrency
import com.baruckis.mycryptocoins.dependencyinjection.Injectable
import com.baruckis.mycryptocoins.ui.addsearchlist.CryptocurrencyAmountDialog.Companion.DIALOG_CRYPTOCURRENCY_AMOUNT_TAG
import com.baruckis.mycryptocoins.ui.common.RetryCallback
import com.baruckis.mycryptocoins.utilities.DELAY_MILLISECONDS
import com.baruckis.mycryptocoins.utilities.onActionButtonClick
import com.baruckis.mycryptocoins.utilities.onDismissedAction
import com.baruckis.mycryptocoins.utilities.showSnackbar
import com.baruckis.mycryptocoins.vo.Status
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.content_add_search.*
import javax.inject.Inject

/**
 * UI for add crypto coins screen.
 */
class AddSearchActivity : AppCompatActivity(), Injectable, CryptocurrencyAmountDialog.CryptocurrencyAmountDialogListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: AddSearchViewModel

    lateinit var binding: ActivityAddSearchBinding

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var listAdapter: AddSearchListAdapter

    private var snackbar: Snackbar? = null

    companion object {
        const val EXTRA_ADD_TASK_DESCRIPTION = "add_task"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manage activity with data binding.
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_search)

        setSupportActionBar(binding.toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup ListView.
        listAdapter = AddSearchListAdapter(this) { cryptocurrency -> cryptocurrencyClick(cryptocurrency) }
        listview_activity_add_search.adapter = listAdapter

        swipeRefreshLayout = swiperefresh_activity_add_search
        swipeRefreshLayout.setOnRefreshListener {
            if (snackbar?.isShown == true) {
                snackbar?.dismiss()
            } else retry()
        }

        binding.myRetryCallback = object : RetryCallback {
            override fun retry() {
                viewModel.retry(false)
            }
        }

        subscribeUi()
    }

    private fun retry() {
        Handler().postDelayed({ viewModel.retry(true) }, DELAY_MILLISECONDS)
    }


    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the CryptocurrencyAmountDialog.CryptocurrencyAmountDialogListener interface.
    override fun onCryptocurrencyAmountDialogConfirmButtonClick(cryptocurrencyAmountDialog: CryptocurrencyAmountDialog) {
        // User touched the dialog's positive button.

        val amount = cryptocurrencyAmountDialog.getAmount()

        viewModel.selectedCryptocurrency?.let {
            it.amount = amount
            it.amountFiat = amount * it.priceFiat
            it.amountFiatChange24h = it.amountFiat!! * (it.pricePercentChange24h / (100))
        }

        val result = Intent()
        result.putExtra(EXTRA_ADD_TASK_DESCRIPTION, viewModel.selectedCryptocurrency)
        setResult(Activity.RESULT_OK, result)

        viewModel.selectedCryptocurrency = null

        cryptocurrencyAmountDialog.dismiss()

        finish()
    }

    override fun onCryptocurrencyAmountDialogCancel() {
        // User touched somewhere that dismissed dialog.

        viewModel.selectedCryptocurrency = null
    }


    private fun cryptocurrencyClick(cryptocurrency: Cryptocurrency) {

        // Create an instance of the dialog fragment and show it.
        val cryptocurrencyAmountDialog =
                CryptocurrencyAmountDialog.newInstance(
                        title = String.format(getString(R.string.dialog_cryptocurrency_amount_title), cryptocurrency.name),
                        hint = getString(R.string.dialog_cryptocurrency_amount_hint),
                        confirmButton = getString(R.string.dialog_cryptocurrency_amount_confirm_button),
                        cancelButton = getString(R.string.dialog_cryptocurrency_amount_cancel_button),
                        error = getString(R.string.dialog_cryptocurrency_amount_error))

        viewModel.selectedCryptocurrency = cryptocurrency

        // Display the alert dialog.
        cryptocurrencyAmountDialog.show(supportFragmentManager, DIALOG_CRYPTOCURRENCY_AMOUNT_TAG)
    }

    private fun subscribeUi() {

        // Obtain ViewModel from ViewModelProviders, using parent activity as LifecycleOwner.
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AddSearchViewModel::class.java)

        // Update the list when the data changes by observing data on the ViewModel, exposed as a LiveData.
        viewModel.mediatorLiveData.observe(this, Observer { listResource ->

            if (swipeRefreshLayout.isRefreshing) {
                if (listResource.status != Status.LOADING) swipeRefreshLayout.isRefreshing = false
            } else {
                binding.myListResource = listResource
            }

            listResource.data?.let { listAdapter.setData(it) }

            if (listResource.status == Status.ERROR && listResource.data != null) {

                snackbar = findViewById<CoordinatorLayout>(R.id.coordinator_add_search).showSnackbar(R.string.unable_refresh) {
                    onActionButtonClick { swipeRefreshLayout.isRefreshing = true }
                    onDismissedAction { retry() }
                }
            }

        })

        viewModel.liveDataLastUpdated.observe(this, Observer<String> { data ->
            last_updated_activity_add_search.text = StringBuilder(getString(R.string.string_last_updated_on_date_time, data)).toString()
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_search, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu?.findItem(R.id.search)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.maxWidth = Integer.MAX_VALUE // Expand to full width, to have close button set to the right side.

        return true
    }
}