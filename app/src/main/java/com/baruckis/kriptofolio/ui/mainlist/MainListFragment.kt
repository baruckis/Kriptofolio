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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.baruckis.kriptofolio.R
import com.baruckis.kriptofolio.databinding.FragmentMainListBinding
import com.baruckis.kriptofolio.db.MyCryptocurrency
import com.baruckis.kriptofolio.dependencyinjection.Injectable
import com.baruckis.kriptofolio.ui.addsearchlist.AddSearchActivity
import com.baruckis.kriptofolio.ui.common.CustomItemAnimator
import com.baruckis.kriptofolio.ui.settings.SettingsActivity
import com.baruckis.kriptofolio.utilities.*
import com.baruckis.kriptofolio.vo.Status
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import javax.inject.Inject


/**
 * UI part for main my crypto coins screen. A placeholder fragment containing a simple view.
 */
class MainListFragment : Fragment(), Injectable, PrimaryActionModeController.PrimaryActionModeListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyListView: View
    private lateinit var recyclerAdapter: MainRecyclerViewAdapter

    private lateinit var spinnerFiatCode: Spinner

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: MainViewModel

    lateinit var binding: FragmentMainListBinding

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var snackbarUnableRefresh: Snackbar? = null
    private var snackbarUndoDelete: Snackbar? = null

    // Helper to create contextual action mode over toolbar.
    private val primaryActionModeController = PrimaryActionModeController()

    // For doing items multi select, long press select and also survive life cycle instance we are
    // going to use selection library.
    private lateinit var recyclerSelectionTracker: SelectionTracker<String>
    private lateinit var recyclerSelectionTrackerItemKeyProvider: MainListItemKeyProvider

    private var deletedItems: ArrayList<MyCryptocurrency>? = null

    private var numberOfItemsToRemoveOrAdd: Int = 0


    companion object {
        private const val SELECTION_TRACKER_ID = "selection_tracker"
        private const val DELETED_ITEMS_KEY = "deleted_items"
        private const val SELECTION_SEQUENCES_TO_DELETE_KEY = "selection_sequences_to_delete"
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Manage fragment with data binding.
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_list, container, false)
        val v: View = binding.root

        recyclerView = v.findViewById(R.id.recyclerview_fragment_main_list)
        emptyListView = v.findViewById(R.id.layout_fragment_main_list_empty)
        swipeRefreshLayout = v.findViewById(R.id.swiperefresh_fragment_main_list)

        // Make swipe refresh layout circle colorful.
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorForSwipeRefreshProgress1,
                R.color.colorForSwipeRefreshProgress2,
                R.color.colorForSwipeRefreshProgress3)

        // We call this when we swipe and refresh.
        swipeRefreshLayout.setOnRefreshListener {
            snackbarUnableRefresh?.dismiss()
            spinnerFiatCode.isEnabled = false
            viewModel.retry()
        }

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let {

            setupList(it, savedInstanceState)

            subscribeUi(it)

            spinnerFiatCode = it.findViewById(R.id.spinner_fiat_code)

            // Every time activity is created, we set currently used fiat currency to be selected
            // one inside spinner component. We get data from shared preferences.
            spinnerFiatCode.setSelection(getFiatCurrencyPosition(viewModel.getCurrentFiatCurrencyCode()), false)

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

                    // Dismiss snackbar if needed.
                    snackbarUnableRefresh?.dismiss()
                    snackbarUndoDelete?.dismiss()

                    // First we check if new selected fiat currency is similar to the one used in
                    // my cryptocurrencies list. If so than we are ok to change selected fiat
                    // currency immediately as we don't need any data update from the server. If
                    // that is not true than we request data update and only if it is successful
                    // than change selected fiat currency.
                    if (viewModel.checkIfNewFiatCurrencyCodeSameToMyCryptocurrency(spinnerSelectedFiatCurrencyCode)) {
                        viewModel.setNewCurrentFiatCurrencyCode(spinnerSelectedFiatCurrencyCode)
                        viewModel.refreshMyCryptocurrencyResourceList()
                    } else {
                        spinnerFiatCode.isEnabled = false

                        // We will try to get new data from the server with new selected fiat currency.
                        viewModel.retry(spinnerSelectedFiatCurrencyCode)
                    }
                }
            }


            // We observe the LiveData changes of fiat currency code from shared preferences.
            viewModel.liveDataCurrentFiatCurrencyCode.observe(viewLifecycleOwner, Observer<String> { data ->
                data?.let {
                    // If value in shared preferences change, e.g. user sets new fiat currency from
                    // settings screen, then update the spinner, variables and try to get data from
                    // the server.
                    if (viewModel.getSelectedFiatCurrencyCodeFromRep() != data) {

                        snackbarUnableRefresh?.dismiss()
                        snackbarUndoDelete?.dismiss()

                        viewModel.setSelectedFiatCurrencyCodeFromRep(data)

                        spinnerFiatCode.setSelection(getFiatCurrencyPosition(data), false)

                        // Request data update from the server only if new selected fiat currency is
                        // not the same to the one used in my cryptocurrencies list.
                        if (viewModel.checkIfNewFiatCurrencyCodeSameToMyCryptocurrency(data)) {
                            viewModel.refreshMyCryptocurrencyResourceList()
                        } else {
                            spinnerFiatCode.isEnabled = false
                            viewModel.retry()
                        }
                    }
                }
            })


            // Set event for floating action button.
            it.fab.setOnClickListener { _ ->
                snackbarUndoDelete?.let { snackbar ->
                    clean()
                    snackbar.dismiss()
                }
                val intent = Intent(it, AddSearchActivity::class.java)
                // We will launch new activity with expectation to get back a result from it
                // - selected cryptocurrency to add .
                startActivityForResult(intent, ADD_TASK_REQUEST)
            }

        }
    }


    // This activity expects result from another activity.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // If new owned cryptocurrency was added.
        if (requestCode == ADD_TASK_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val cryptocurrency: MyCryptocurrency? = data?.getParcelableExtra(AddSearchActivity.EXTRA_ADD_TASK_DESCRIPTION)
                cryptocurrency?.let {
                    viewModel.addCryptocurrency(cryptocurrency)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save selection state to survive the lifecycle changes.
        recyclerSelectionTracker.onSaveInstanceState(outState)

        // When fragment is destroyed we want to save our deleted items if any to be able to restore
        // with undo snackbar later.
        deletedItems?.let { outState.putParcelableArrayList(DELETED_ITEMS_KEY, deletedItems) }

        // We also need to remember deleted selected items arrangement on screen, because later
        // if user press undo button on snackbar, we will need restore deleted items with animation.
        outState.putParcelable(SELECTION_SEQUENCES_TO_DELETE_KEY, recyclerAdapter.getSelectionSequencesToDelete())

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Restore selection state.
        recyclerSelectionTracker.onRestoreInstanceState(savedInstanceState)

        // Restore our deleted items if any.
        deletedItems = savedInstanceState?.getParcelableArrayList(DELETED_ITEMS_KEY)

        // If during deletion process undo snackbar was lost on configuration change
        // (e.g. screen rotation) than recreate it again.
        if (!deletedItems.isNullOrEmpty()) {
            showSnackbarUndoDelete()
        }
    }

    override fun onEnterActionMode() {
        swipeRefreshLayout.isEnabled = false
    }

    override fun onLeaveActionMode() {
        recyclerSelectionTracker.clearSelection()
        recyclerAdapter.clearSelected()
        swipeRefreshLayout.isEnabled = true
    }

    override fun onActionItemClick(item: MenuItem) {

        when (item.itemId) {
            R.id.action_select_all -> {
                val list = ArrayList<String>()
                recyclerAdapter.getData().forEach {
                    list.add(it.myId.toString())
                }
                recyclerSelectionTracker.setItemsSelected(list, true)
            }
            R.id.action_delete -> {

                // We delete selected items with animation and store their ids.
                deletedItems = recyclerAdapter.deleteSelectedItems() as ArrayList

                deletedItems?.let { deletedItems ->

                    numberOfItemsToRemoveOrAdd = deletedItems.size

                    // We need not to forget to update data inside selection tracker after it was changed.
                    recyclerSelectionTrackerItemKeyProvider.updataData(recyclerAdapter.getData())

                    // Than we close action mode.
                    primaryActionModeController.finishActionMode()

                    val isAllDeleted = recyclerAdapter.getData().isEmpty()

                    // Show empty list UI if we deleted all items.
                    binding.emptyList = binding.listResource?.data != null && isAllDeleted

                    // Delete items from database.
                    viewModel.deleteCryptocurrencyList(deletedItems)

                    // Finally we show snackbar which is the last chance for the user to undo deletion.
                    showSnackbarUndoDelete()
                }

            }
            R.id.action_settings -> {
                startActivity(Intent(activity, SettingsActivity::class.java))
            }
        }
    }


    private fun setupList(activity: FragmentActivity, savedInstanceState: Bundle?) {

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerAdapter = MainRecyclerViewAdapter()

        // Here we restore deleted selected items and their arrangement on screen if any. As you
        // see we pass data to recyclerview adapter immediately.
        savedInstanceState?.let {
            recyclerAdapter.setSelectionSequencesToDelete(savedInstanceState.getParcelable(SELECTION_SEQUENCES_TO_DELETE_KEY))
        }

        recyclerView.adapter = recyclerAdapter

        recyclerSelectionTrackerItemKeyProvider = MainListItemKeyProvider(recyclerAdapter.getData())


        // SelectionTracker can be built only after setting the adapter on the recylerview.
        // Here we create an instance of it.
        recyclerSelectionTracker = SelectionTracker.Builder<String>(SELECTION_TRACKER_ID,
                recyclerView, recyclerSelectionTrackerItemKeyProvider, MainListItemLookup(recyclerView),
                StorageStrategy.createStringStorage()).build()


        // We should invoke contextual action bar when we make a long click on recycler view item.
        // SelectionTracker will help to implement this functionality by observing selection change.
        recyclerSelectionTracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {

            override fun onSelectionChanged() {
                super.onSelectionChanged()
                if (recyclerSelectionTracker.hasSelection() && !primaryActionModeController.isInMode() && activity is AppCompatActivity) {
                    primaryActionModeController.startActionMode(this@MainListFragment.activity as AppCompatActivity, this@MainListFragment,
                            R.menu.menu_action_mode, getString(R.string.action_mode_title, recyclerSelectionTracker.selection.size()))
                } else if (!recyclerSelectionTracker.hasSelection() && primaryActionModeController.isInMode()) {
                    primaryActionModeController.finishActionMode()
                } else {
                    primaryActionModeController.setTitle(getString(R.string.action_mode_title, recyclerSelectionTracker.selection.size()))
                }

            }
        })

        recyclerAdapter.setSelectionTracker(recyclerSelectionTracker)


        // To find out when exactly delete and restore animations finnish for selected
        // cryptocurrencies, we create and use custom animator.
        val customAnimator = object : CustomItemAnimator() {}

        customAnimator.setOnItemAnimatorListener(object : CustomItemAnimator.OnItemAnimatorListener {

            override fun getNumberOfItemsToRemove(): Int = numberOfItemsToRemoveOrAdd

            override fun onAnimationsFinishedOnItemRemoved() {
                logConsoleVerbose("Main list deleted items: $numberOfItemsToRemoveOrAdd")
                viewModel.refreshMyCryptocurrencyResourceList()
            }

            override fun getNumberOfItemsToAdd(): Int = numberOfItemsToRemoveOrAdd

            override fun onAnimationsFinishedOnItemAdded() {
                logConsoleVerbose("Main list restored after delete items: $numberOfItemsToRemoveOrAdd")
                viewModel.refreshMyCryptocurrencyResourceList()
            }
        })

        customAnimator.supportsChangeAnimations = false

        recyclerView.itemAnimator = customAnimator


        val appBarLayout = activity.findViewById<AppBarLayout>(R.id.app_bar_layout)

        // Enable SwipeRefreshLayout only when AppBarLayout expanded completely.
        appBarLayout.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                    // Is expanded.
                    if (verticalOffset == 0 && !swipeRefreshLayout.isEnabled) {
                        swipeRefreshLayout.isEnabled = true
                    }
                    // Is collapsed.
                    else if (verticalOffset < 0 && swipeRefreshLayout.isEnabled && !swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isEnabled = false
                    }
                })

    }


    private fun subscribeUi(activity: FragmentActivity) {

        // Obtain ViewModel from ViewModelProvider, using parent activity as LifecycleOwner.
        viewModel = ViewModelProvider(activity, viewModelFactory)[MainViewModel::class.java]

        binding.viewmodel = viewModel

        // Update the list when the data changes by observing data on the ViewModel, exposed as a LiveData.
        viewModel.mediatorLiveDataMyCryptocurrencyResourceList.observe(viewLifecycleOwner, Observer { listResource ->

            logConsoleVerbose("Main list resource status: " + listResource.status.toString())

            if (listResource.status != Status.LOADING || listResource.status == Status.LOADING &&
                    binding.emptyList == null && recyclerAdapter.itemCount == 0) {
                binding.listResource = listResource
                // Show or hide empty list ui message.
                binding.emptyList = listResource.data != null && listResource.data.isEmpty()
            }

            // This is important when we rotate screen. Our view model helper variable indicates
            // there is loading in progress and we should do some restore actions.
            if (viewModel.isSwipeRefreshing && !swipeRefreshLayout.isRefreshing) {
                snackbarUnableRefresh?.dismiss()
                snackbarUndoDelete?.dismiss()

                // Restore swipe refresh layout state.
                swipeRefreshLayout.isRefreshing = true
                spinnerFiatCode.isEnabled = false

                // Also restore spinner selection.
                if (viewModel.newSelectedFiatCurrencyCode != null)
                    spinnerFiatCode.setSelection(
                            getFiatCurrencyPosition(viewModel.newSelectedFiatCurrencyCode), false
                    )
            }

            // We ignore any response where data is null.
            listResource.data?.let {
                recyclerAdapter.setData(it)
                recyclerSelectionTrackerItemKeyProvider.updataData(it)

                // First we check if there was an error from the server.
                if (listResource.status == Status.ERROR) {
                    viewModel.isSwipeRefreshing = false
                    swipeRefreshLayout.isRefreshing = false
                    spinnerFiatCode.isEnabled = true

                    snackbarUnableRefresh = this.view!!.showSnackbar(R.string.unable_refresh) {
                        onActionButtonClick {
                            viewModel.isSwipeRefreshing = true
                            swipeRefreshLayout.isRefreshing = true
                            spinnerFiatCode.isEnabled = false
                        }
                        onDismissedAction {
                            // When we retry to get data from the server with selected fiat currency.
                            if (viewModel.newSelectedFiatCurrencyCode != null) {
                                spinnerFiatCode.setSelection(
                                        getFiatCurrencyPosition(viewModel.newSelectedFiatCurrencyCode)
                                )
                            } else viewModel.retry()
                        }
                        onDismissedAnyOfEvents(
                                listOf(Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE,
                                        Snackbar.Callback.DISMISS_EVENT_SWIPE)) {
                            viewModel.newSelectedFiatCurrencyCode = null
                        }
                    }
                    // If there was an error when trying to load data with new selected fiat currency,
                    // than restore previous one.
                    if (viewModel.newSelectedFiatCurrencyCode != null) {
                        spinnerFiatCode.setSelection(getFiatCurrencyPosition(viewModel.getCurrentFiatCurrencyCode()))
                    }
                } else if (listResource.status == Status.SUCCESS_DB ||
                        listResource.status == Status.SUCCESS_NETWORK) {

                    if (viewModel.newSelectedFiatCurrencyCode != null) {
                        viewModel.setNewCurrentFiatCurrencyCode(viewModel.newSelectedFiatCurrencyCode.toString())
                        viewModel.newSelectedFiatCurrencyCode = null
                    }

                    viewModel.isSwipeRefreshing = false
                    swipeRefreshLayout.isRefreshing = false
                    spinnerFiatCode.isEnabled = true
                }
            }

        })
    }

    // We find an array position of currently used fiat currency in the app.
    private fun getFiatCurrencyPosition(newFiatCurrencyCode: String?): Int {
        return resources.getStringArray(R.array.fiat_currency_code_array).indexOf(newFiatCurrencyCode)
    }


    private fun showSnackbarUndoDelete() {

        // After successful deletion we provide ability for the user to undo deletion action
        // by showing snackbar message for some time.
        snackbarUndoDelete = this.view?.showSnackbar(getString(R.string.deleted, deletedItems!!.size), Snackbar.LENGTH_LONG) {
            swipeRefreshLayout.isEnabled = false

            onActionButtonClick(R.string.undo) {

                deletedItems?.let { deletedItems ->

                    numberOfItemsToRemoveOrAdd = deletedItems.size

                    // If user confirmed to undo deletion than restore items on UI visually.
                    recyclerAdapter.restoreDeletedItems()

                    // Again don't forget to update selection tracker to work it correctly.
                    recyclerSelectionTrackerItemKeyProvider.updataData(recyclerAdapter.getData())

                    // Than restore items inside database by adding them back.
                    viewModel.restoreCryptocurrencyList(deletedItems)
                }

                clean()
            }

            // When snackbar is dismissed it means that user did not wanted to undo deletion
            // and we can confidently forget deleted items.
            onDismissedAnyOfEvents(
                    listOf(Snackbar.Callback.DISMISS_EVENT_TIMEOUT,
                            Snackbar.Callback.DISMISS_EVENT_SWIPE)) {
                clean()
            }
        }
    }

    // After restoration or deletion from database we should also clear variables that we save
    // with instance.
    private fun clean() {
        deletedItems = null
        recyclerAdapter.setSelectionSequencesToDelete(null)
        swipeRefreshLayout.isEnabled = true
    }

}