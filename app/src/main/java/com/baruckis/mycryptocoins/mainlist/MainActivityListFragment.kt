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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.baruckis.mycryptocoins.R

/**
 * A placeholder fragment containing a simple view.
 */
class MainActivityListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: MainRecyclerViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v: View = inflater.inflate(R.layout.fragment_main_list, container, false)

        recyclerView = v.findViewById(R.id.recyclerview_fragment_main_list)

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)

        setupList()
    }

    private fun setupList() {

        val data = ArrayList<String>()
        data.add("Bitcoin")
        data.add("Etherium")
        data.add("Ripple")
        data.add("Bitcoin Cash")
        data.add("Litecoin")
        data.add("NEO")
        data.add("Stellar")
        data.add("EOS")
        data.add("Cardano")
        data.add("Stellar")
        data.add("IOTA")
        data.add("Dash")
        data.add("Monero")
        data.add("TRON")
        data.add("NEM")
        data.add("ICON")
        data.add("Bitcoin Gold")
        data.add("Zcash")
        data.add("Verge")

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerAdapter = MainRecyclerViewAdapter(data)
        recyclerView.adapter = recyclerAdapter
    }
}
