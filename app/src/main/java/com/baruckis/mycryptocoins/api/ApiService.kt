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

package com.baruckis.mycryptocoins.api

import androidx.lifecycle.LiveData
import com.baruckis.mycryptocoins.utilities.API_SERVICE_RESULTS_LIMIT
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * REST API access points.
 */
interface ApiService {

    // The @GET annotation tells retrofit that this request is a get type request.
    // The string value tells retrofit that the path of this request is
    // baseUrl + v1/cryptocurrency/listings/latest + query parameter.
    @GET("v1/cryptocurrency/listings/latest")
    // Annotation @Query is used to define query parameter for request. It is possible to pass
    // multiple query parameters too. Finally the request url will look like that
    // https://sandbox-api.coinmarketcap.com/v1/cryptocurrency/listings/latest?convert=EUR&limit=5000
    fun getAllCryptocurrencies(@Query("convert") currency: String,
                               @Query("limit") size: Int = API_SERVICE_RESULTS_LIMIT):
            LiveData<ApiResponse<CoinMarketCap<List<CryptocurrencyLatest>>>>
    // The return type for this function is Call with its type CryptocurrencyLatest.


    //https://sandbox-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?convert=EUR&id=1,1027
    @GET("v1/cryptocurrency/quotes/latest")
    fun getCryptocurrenciesById(@Query("convert") currency: String,
                                @Query("id") id: String):
            LiveData<ApiResponse<CoinMarketCap<HashMap<String, CryptocurrencyLatest>>>>
}