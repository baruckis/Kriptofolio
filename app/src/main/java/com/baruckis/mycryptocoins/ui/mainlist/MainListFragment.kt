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

package com.baruckis.mycryptocoins.ui.mainlist

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.databinding.FragmentMainListBinding
import com.baruckis.mycryptocoins.dependencyinjection.Injectable
import com.baruckis.mycryptocoins.utilities.DELAY_MILLISECONDS
import com.baruckis.mycryptocoins.utilities.onActionButtonClick
import com.baruckis.mycryptocoins.utilities.onDismissedAction
import com.baruckis.mycryptocoins.utilities.showSnackbar
import com.baruckis.mycryptocoins.vo.Status
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject


/**
 * UI part for main my crypto coins screen. A placeholder fragment containing a simple view.
 */
class MainListFragment : Fragment(), Injectable {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyListView: View
    private lateinit var recyclerAdapter: MainRecyclerViewAdapter

    private lateinit var spinnerFiatCode: Spinner

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: MainViewModel

    lateinit var binding: FragmentMainListBinding

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var snackbar: Snackbar? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Manage fragment with data binding.
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_list, container, false)
        val v: View = binding.root

        recyclerView = v.findViewById(R.id.recyclerview_fragment_main_list)
        emptyListView = v.findViewById(R.id.layout_fragment_main_list_empty)
        swipeRefreshLayout = v.findViewById(R.id.swiperefresh_fragment_main_list)

        swipeRefreshLayout.setOnRefreshListener {
            retry(null)
        }

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupList()

        activity?.let {
            subscribeUi(it)

            spinnerFiatCode = it.findViewById(R.id.spinner_fiat_code)

            // Every time activity is created, we set currently used fiat currency to be selected
            // one inside spinner component. We get data from shared preferences.
            spinnerFiatCode.setSelection(getFiatCurrencyPosition(viewModel.getCurrentFiatCurrencyCode()))

            spinnerFiatCode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing.
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val spinnerSelectedFiatCurrencyCode = parent?.getItemAtPosition(position) as String

                    // Pay attention that item will be selected multiple times during activity
                    // creation, other events and not just when user clicks on spinner item.
                    // So with this check we will avoid unwanted calls to the server.
                    if (spinnerSelectedFiatCurrencyCode == viewModel.getSelectedFiatCurrencyCodeFromRep()) {
                        // Do nothing, just exit.
                        return
                    }

                    // We will retry the data from the server only when user
                    // selected new currency to be active one.
                    swipeRefreshLayout.isRefreshing = true
                    spinnerFiatCode.isEnabled = false
                    retry(spinnerSelectedFiatCurrencyCode)
                }
            }


            // We observe the LiveData changes of fiat currency code from shared preferences.
            viewModel.liveDataCurrentFiatCurrencyCode.observe(this, Observer<String> { data ->
                data?.let {
                    if (viewModel.getSelectedFiatCurrencyCodeFromRep() == null) {
                        // If there is no fiat currency code stored inside repository, we set it
                        // to be same as in shared preferences and update the spinner.
                        viewModel.setSelectedFiatCurrencyCodeFromRep(data)
                        spinnerFiatCode.setSelection(getFiatCurrencyPosition(data))
                    }
                    // If value in shared preferences change, e.g. user sets new fiat currency from
                    // settings screen, then update the spinner.
                    else if (viewModel.getSelectedFiatCurrencyCodeFromRep() != data) {
                        spinnerFiatCode.setSelection(getFiatCurrencyPosition(data))
                    }
                }
            })

        }

    }


    private fun setupList() {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerAdapter = MainRecyclerViewAdapter()
        recyclerView.adapter = recyclerAdapter
    }

    private fun subscribeUi(activity: FragmentActivity) {

        // Obtain ViewModel from ViewModelProviders, using parent activity as LifecycleOwner.
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(MainViewModel::class.java)

        binding.viewmodel = viewModel

        // Update the list when the data changes by observing data on the ViewModel, exposed as a LiveData.
        viewModel.mediatorLiveDataMyCryptocurrencyResourceList.observe(this, Observer { listResource ->

            if (swipeRefreshLayout.isRefreshing) {
                if (listResource.data != null) {
                    swipeRefreshLayout.isRefreshing = false
                    spinnerFiatCode.isEnabled = true
                }
            } else {
                binding.listResource = listResource
            }

            // We ignore any response where data is null.
            listResource.data?.let {
                recyclerAdapter.setData(it)

                // First we check if there was an error from the server.
                if (listResource.status == Status.ERROR) {
                    snackbar = this.view!!.showSnackbar(R.string.unable_refresh) {
                        onActionButtonClick {
                            swipeRefreshLayout.isRefreshing = true
                            spinnerFiatCode.isEnabled = false
                        }
                        onDismissedAction {
                            // When we retry to get data from the server with selected fiat currency.
                            if (viewModel.newSelectedFiatCurrencyCode!= null)
                                spinnerFiatCode.setSelection(getFiatCurrencyPosition(viewModel.newSelectedFiatCurrencyCode))
                            else retry(null)
                        }
                    }
                    // If there was an error when trying to load data with new selected fiat currency,
                    // than restore previous one.
                    if (viewModel.newSelectedFiatCurrencyCode != null) {
                        if (viewModel.newSelectedFiatCurrencyCode == viewModel.getCurrentFiatCurrencyCode())
                            viewModel.setSelectedFiatCurrencyCodeFromRep(viewModel.newSelectedFiatCurrencyCode)
                        spinnerFiatCode.setSelection(getFiatCurrencyPosition(viewModel.getCurrentFiatCurrencyCode()))
                    }
                }
                // This another check is to filter successful response or empty data response
                // from database when user selected new fiat currency.
                else if ((listResource.status == Status.SUCCESS || listResource.data.isEmpty()) &&
                        viewModel.newSelectedFiatCurrencyCode != null) {

                    // Set new value in shared preferences and in the repository.
                    viewModel.setNewCurrentFiatCurrencyCode(viewModel.newSelectedFiatCurrencyCode!!)
                    viewModel.setSelectedFiatCurrencyCodeFromRep(viewModel.newSelectedFiatCurrencyCode)

                    viewModel.newSelectedFiatCurrencyCode = null
                }
            }

        })

    }


    // We find an array position of currently used fiat currency in the app.
    private fun getFiatCurrencyPosition(newFiatCurrencyCode: String?): Int {
        return resources.getStringArray(R.array.fiat_currency_code_array).indexOf(newFiatCurrencyCode)
    }

    // Dismiss snackbar if needed and get new data from the server.
    private fun retry(newFiatCurrencyCode: String?) {
        viewModel.newSelectedFiatCurrencyCode = newFiatCurrencyCode
        if (snackbar?.isShown == true) snackbar?.dismiss()
        // Make a call to the server after some delay for better user experience.
        Handler().postDelayed({ viewModel.retry(newFiatCurrencyCode) }, DELAY_MILLISECONDS)
    }

}