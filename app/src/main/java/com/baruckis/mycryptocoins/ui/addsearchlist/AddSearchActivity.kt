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

package com.baruckis.mycryptocoins.ui.addsearchlist

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.baruckis.mycryptocoins.db.MyCryptocurrency
import com.baruckis.mycryptocoins.dependencyinjection.Injectable
import com.baruckis.mycryptocoins.ui.addsearchlist.CryptocurrencyAmountDialog.Companion.DIALOG_CRYPTOCURRENCY_AMOUNT_TAG
import com.baruckis.mycryptocoins.ui.common.RetryCallback
import com.baruckis.mycryptocoins.utilities.*
import com.baruckis.mycryptocoins.vo.Status
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.content_add_search.*
import kotlinx.coroutines.*
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

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var searchQuery: String? = null


    companion object {
        const val EXTRA_ADD_TASK_DESCRIPTION = "add_task"
        private const val SEARCH_KEY = "search"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We recover our search query that we saved manually.
        if (savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(SEARCH_KEY, null)
        }

        // Manage activity with data binding.
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_search)

        setSupportActionBar(binding.toolbar2)
        // Get a support ActionBar corresponding to this toolbar and enable the Up button.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup ListView.
        listAdapter = AddSearchListAdapter(this) { cryptocurrency -> cryptocurrencyClick(cryptocurrency) }
        listview_activity_add_search.adapter = listAdapter

        swipeRefreshLayout = swiperefresh_activity_add_search

        // We call this when we swipe and refresh.
        swipeRefreshLayout.setOnRefreshListener {
            // Dismiss snackbar if needed
            snackbar?.dismiss()

            enableSearchMenuItem(false)

            viewModel.isSwipeRefreshing = true
            viewModel.retry()
        }

        binding.myRetryCallback = object : RetryCallback {
            override fun retry() {
                this@AddSearchActivity.retry()
            }
        }

        subscribeUi()
    }


    // SearchView used as an action view inside a collapsed menu item does not have its state saved
    // or restored automatically like normal views. So we need to do it manually. This function will
    // be called before the activity is destroyed and will save search query if any.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        searchQuery = searchView?.query.toString()
        outState.putString(SEARCH_KEY, searchQuery)
    }


    override fun onDestroy() {
        textChangeDelayJob?.cancel()
        super.onDestroy()
    }


    // Get new data from the server.
    private fun retry() {
        // Dismiss snackbar if needed
        snackbar?.dismiss()

        enableSearchMenuItem(false)

        // Make a call to the server.
        viewModel.retry()
    }


    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the CryptocurrencyAmountDialog.CryptocurrencyAmountDialogListener interface.
    override fun onCryptocurrencyAmountDialogConfirmButtonClick(cryptocurrencyAmountDialog: CryptocurrencyAmountDialog) {
        // User touched the dialog's positive button.

        val amount = cryptocurrencyAmountDialog.getAmount()

        viewModel.selectedCryptocurrency?.let { myCryptocurrency ->
            myCryptocurrency.amount = amount
            myCryptocurrency.amountFiat =
                    getAmountFiatCounted(amount, myCryptocurrency.cryptoData.priceFiat)
            myCryptocurrency.amountFiatChange24h =
                    getAmountFiatChange24hCounted(myCryptocurrency.amountFiat, myCryptocurrency.cryptoData.pricePercentChange24h)
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

        viewModel.selectedCryptocurrency = MyCryptocurrency(cryptocurrency.id, cryptocurrency)

        // Display the alert dialog.
        cryptocurrencyAmountDialog.show(supportFragmentManager, DIALOG_CRYPTOCURRENCY_AMOUNT_TAG)
    }

    private fun subscribeUi() {

        // Obtain ViewModel from ViewModelProviders, using parent activity as LifecycleOwner.
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AddSearchViewModel::class.java)

        // Update the list when the data changes by observing data on the ViewModel, exposed as a LiveData.
        viewModel.mediatorLiveDataCryptocurrencyResourceList.observe(this, Observer { listResource ->

            if (listResource.status != Status.LOADING || listAdapter.isEmpty) {
                binding.myListResource = listResource
                enableSearchMenuItem(false)
            }

            // Restore swipe refresh layout state from our view model using helper variable.
            // This is important when we rotate screen.
            if (viewModel.isSwipeRefreshing && !swipeRefreshLayout.isRefreshing) {
                showSwipeRefreshing(true)
                enableSearchMenuItem(false)
            }

            listResource.data?.let {
                listAdapter.setData(it)

                viewModel.lastUpdatedOnDate = if (!it.isEmpty()) formatDate(it.first().lastFetchedDate, DATE_FORMAT_PATTERN) else ""
                info_activity_add_search.text = StringBuilder(getString(R.string.string_info_last_updated_on_date_time, viewModel.lastUpdatedOnDate)).toString()


                // First we check if there was an error from the server.
                if (listResource.status == Status.ERROR) {
                    showSwipeRefreshing(false)
                    enableSearchMenuItem(true)

                    snackbar = findViewById<CoordinatorLayout>(R.id.coordinator_add_search).showSnackbar(R.string.unable_refresh) {
                        onActionButtonClick {
                            showSwipeRefreshing(true)
                            enableSearchMenuItem(false)
                        }
                        onDismissedAction { retry() }
                    }

                } else if (listResource.status == Status.SUCCESS_DB || listResource.status == Status.SUCCESS_NETWORK) {
                    showSwipeRefreshing(false)
                    enableSearchMenuItem(true)
                }
            }

        })

    }


    private fun showSwipeRefreshing(isRefreshing: Boolean) {
        viewModel.isSwipeRefreshing = isRefreshing
        swipeRefreshLayout.isRefreshing = isRefreshing
    }

    private fun enableSearchMenuItem(isEnabled: Boolean) {
        searchMenuItem?.isEnabled = isEnabled
        viewModel.isSearchMenuItemEnabled = isEnabled
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_search, menu)

        // Associate searchable configuration with the SearchView.
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchMenuItem = menu?.findItem(R.id.search)
        searchMenuItem?.setOnActionExpandListener(searchExpandListener)
        searchMenuItem?.isEnabled = viewModel.isSearchMenuItemEnabled

        searchView = searchMenuItem?.actionView as SearchView
        searchView?.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView?.maxWidth = Integer.MAX_VALUE // Expand to full width, to have close button set to the right side.
        searchView?.setOnQueryTextListener(searchListener)

        if (!searchQuery.isNullOrEmpty()) {
            searchMenuItem?.expandActionView()
            searchView?.setQuery(searchQuery, true)
        }

        return true
    }


    private var textChangeDelayJob: Job? = null

    // Coroutine will be launched in the main UI thread.
    private val uiScope = CoroutineScope(Dispatchers.Main)

    // This listener reacts to text change inside search area. We expect the search results to get
    // filtered with every key stroke.
    private val searchListener = object : SearchView.OnQueryTextListener {

        override fun onQueryTextSubmit(query: String?): Boolean {
            query?.let { search(it) }
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {

            textChangeDelayJob?.cancel()

            // To avoid too many requests and optimize search experience, we add a small delay to
            // trigger a search when user finished typing.
            textChangeDelayJob = uiScope.launch() {
                newText?.let {
                    delay(SEARCH_TYPING_DELAY_MILLISECONDS)
                    search(it)
                }
            }

            return true
        }

        private fun search(searchText: String) {
            // The percent sign represents zero, one, or multiple numbers or characters.
            // It finds any values that have searchText in any position.
            viewModel.search("%$searchText%").observe(this@AddSearchActivity, Observer {
                if (it == null) return@Observer
                listAdapter.setData(it)
                if (searchMenuItem?.isActionViewExpanded == true) {
                    info_activity_add_search.text = StringBuilder(getString(R.string.string_info_results_of_search, it.size.toString())).toString()
                }
            })
        }
    }

    // This listener lets you identify when search was launched and closed.
    private val searchExpandListener = object : MenuItem.OnActionExpandListener {

        override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
            swipeRefreshLayout.isEnabled = false
            info_activity_add_search.text = StringBuilder(getString(R.string.string_info_results_of_search, listAdapter.count.toString())).toString()
            return true
        }

        override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
            textChangeDelayJob?.cancel()
            swipeRefreshLayout.isEnabled = true
            info_activity_add_search.text = StringBuilder(getString(R.string.string_info_last_updated_on_date_time, viewModel.lastUpdatedOnDate)).toString()
            return true
        }

    }

}