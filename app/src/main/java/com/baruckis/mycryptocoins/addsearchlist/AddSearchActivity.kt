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

package com.baruckis.mycryptocoins.addsearchlist

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.api.ApiService
import com.baruckis.mycryptocoins.api.AuthenticationInterceptor
import com.baruckis.mycryptocoins.api.CryptocurrenciesLatest
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.dependencyinjection.Injectable
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_add_search.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject


class AddSearchActivity : AppCompatActivity(), Injectable {

    private lateinit var listView: ListView
    private lateinit var listAdapter: AddSearchListAdapter

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: AddSearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_search)
        setSupportActionBar(toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listView = findViewById(R.id.listview_activity_add_search)

        setupList()
        subscribeUi()

        // Later we will setup Retrofit correctly, but for now we do all in one place just for quick start.
        setupRetrofitTemporarily()
    }

    private fun setupList() {
        listAdapter = AddSearchListAdapter(this)
        listView.adapter = listAdapter
    }

    private fun subscribeUi() {

        // Obtain ViewModel from ViewModelProviders, using parent activity as LifecycleOwner.
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AddSearchViewModel::class.java)

        // Update the list when the data changes by observing data on the ViewModel, exposed as a LiveData.
        viewModel.liveData.observe(this, Observer<List<Cryptocurrency>> { data ->
            if (data != null && data.isNotEmpty()) {
                listView.visibility = View.VISIBLE
                listAdapter.setData(data)
            } else {
                listView.visibility = View.GONE
            }
        })

    }

    private fun setupRetrofitTemporarily() {

        // We need to prepare a custom OkHttp client because need to use our custom call interceptor.
        // to be able to authenticate our requests.
        val builder = OkHttpClient.Builder()
        // We add the interceptor to OkHttpClient.
        // It will add authentication headers to every call we make.
        builder.interceptors().add(AuthenticationInterceptor())
        val client = builder.build()


        val api = Retrofit.Builder() // Create retrofit builder.
                .baseUrl("https://sandbox-api.coinmarketcap.com/") // Base url for the api has to end with a slash.
                .addConverterFactory(GsonConverterFactory.create()) // Use GSON converter for JSON to POJO object mapping.
                .client(client) // Here we set the custom OkHttp client we just created.
                .build().create(ApiService::class.java) // We create an API using the interface we defined.


        val adapterData: MutableList<Cryptocurrency> = ArrayList<Cryptocurrency>()

        val currentFiatCurrencyCode = "EUR"

        // Let's make asynchronous network request to get all latest cryptocurrencies from the server.
        // For query parameter we pass "EUR" as we want to get prices in euros.
        val call = api.getAllCryptocurrencies("EUR")
        val result = call.enqueue(object : Callback<CryptocurrenciesLatest> {

            // You will always get a response even if something wrong went from the server.
            override fun onFailure(call: Call<CryptocurrenciesLatest>, t: Throwable) {

                Snackbar.make(findViewById(android.R.id.content),
                        // Throwable will let us find the error if the call failed.
                        "Call failed! " + t.localizedMessage,
                        Snackbar.LENGTH_INDEFINITE).show()
            }

            override fun onResponse(call: Call<CryptocurrenciesLatest>, response: Response<CryptocurrenciesLatest>) {

                // Check if the response is successful, which means the request was successfully
                // received, understood, accepted and returned code in range [200..300).
                if (response.isSuccessful) {

                    // If everything is OK, let the user know that.
                    Toast.makeText(this@AddSearchActivity, "Call OK.", Toast.LENGTH_LONG).show()

                    // Than quickly map server response data to the ListView adapter.
                    val cryptocurrenciesLatest: CryptocurrenciesLatest? = response.body()
                    cryptocurrenciesLatest!!.data.forEach {
                        val cryptocurrency = Cryptocurrency(it.name, it.cmcRank.toShort(),
                                0.0, it.symbol, currentFiatCurrencyCode, it.quote.currency.price,
                                0.0, it.quote.currency.percentChange1h,
                                it.quote.currency.percentChange7d, it.quote.currency.percentChange24h,
                                0.0)
                        adapterData.add(cryptocurrency)
                    }

                    listView.visibility = View.VISIBLE
                    listAdapter.setData(adapterData)

                }
                // Else if the response is unsuccessful it will be defined by some special HTTP
                // error code, which we can show for the user.
                else Snackbar.make(findViewById(android.R.id.content),
                        "Call error with HTTP status code " + response.code() + "!",
                        Snackbar.LENGTH_INDEFINITE).show()

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
