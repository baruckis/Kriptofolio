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
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.utilities.InjectorUtils

/**
 * A placeholder fragment containing a simple view.
 */
class MainListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyListView: View
    private lateinit var recyclerAdapter: MainRecyclerViewAdapter

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v: View = inflater.inflate(R.layout.fragment_main_list, container, false)

        recyclerView = v.findViewById(R.id.recyclerview_fragment_main_list)
        emptyListView = v.findViewById(R.id.layout_fragment_main_list_empty)

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)

        setupList()
        subscribeUi()
    }

    private fun setupList() {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerAdapter = MainRecyclerViewAdapter()
        recyclerView.adapter = recyclerAdapter
    }

    private fun subscribeUi() {

        val factory = InjectorUtils.provideMainViewModelFactory(requireContext())

        // Obtain ViewModel from ViewModelProviders, using this fragment as LifecycleOwner.
        viewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        // Update the list when the data changes by observing data on the ViewModel, exposed as a LiveData.
        viewModel.liveData.observe(this, Observer<List<Cryptocurrency>> { data ->
            if (data != null && data.isNotEmpty()) {
                emptyListView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerAdapter.setData(data)
            } else {
                recyclerView.visibility = View.GONE
                emptyListView.visibility = View.VISIBLE
            }
        })

    }

}