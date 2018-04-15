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

package com.baruckis.mycryptocoins.AddSearchList

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.widget.ListView
import com.baruckis.mycryptocoins.R
import kotlinx.android.synthetic.main.activity_add_search.*


class AddSearchActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_search)
        setSupportActionBar(toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

        val adapter = AddSearchListAdapter(this, data)

        listView = findViewById(R.id.listview_activity_add_search)
        listView.adapter = adapter

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
