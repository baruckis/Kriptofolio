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

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.databinding.ActivityAddSearchBinding
import com.baruckis.mycryptocoins.dependencyinjection.Injectable
import com.baruckis.mycryptocoins.ui.common.RetryCallback
import com.baruckis.mycryptocoins.utilities.DELAY_MILLISECONDS
import com.baruckis.mycryptocoins.vo.Status
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.content_add_search.*
import javax.inject.Inject


class AddSearchActivity : AppCompatActivity(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: AddSearchViewModel

    lateinit var binding: ActivityAddSearchBinding

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var listAdapter: AddSearchListAdapter
    private lateinit var snackbar: Snackbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manage activity with data binding.
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_search)

        //setContentView(R.layout.activity_add_search)
        setSupportActionBar(binding.toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup ListView.
        listAdapter = AddSearchListAdapter(this) { cryptocurrency -> cryptocurrencyClick(cryptocurrency) }
        listview_activity_add_search.adapter = listAdapter

        swipeRefreshLayout = swiperefresh_activity_add_search
        swipeRefreshLayout.setOnRefreshListener {
            snackbar.dismiss()
            retry()
        }

        snackbar = Snackbar.make(findViewById(android.R.id.content), "Error", Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("Retry") {
            swipeRefreshLayout.isRefreshing = true
            retry()
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

    private fun cryptocurrencyClick(cryptocurrency: Cryptocurrency) {

        val cryptocurrencyAmountDialog =
                CryptocurrencyAmountDialog.newInstance(
                        title = String.format(getString(R.string.dialog_cryptocurrency_amount_title), cryptocurrency.name),
                        hint = getString(R.string.dialog_cryptocurrency_amount_hint),
                        confirmButton = getString(R.string.dialog_cryptocurrency_amount_confirm_button),
                        cancelButton = getString(R.string.dialog_cryptocurrency_amount_cancel_button))
        cryptocurrencyAmountDialog.onConfirm = { viewModel.addCryptocurrency(cryptocurrency) }

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
                snackbar.show()
            }

        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_search, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu?.findItem(R.id.search)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.maxWidth = Integer.MAX_VALUE // expand to full width, to have close button set to the right side.

        return true
    }
}