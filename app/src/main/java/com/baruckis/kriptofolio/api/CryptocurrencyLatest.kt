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

package com.baruckis.kriptofolio.api

import com.google.gson.annotations.SerializedName

/**
 * Data class to handle the response from the server.
 */
data class CryptocurrencyLatest(
        val id: Int,
        val name: String,
        val symbol: String,
        val slug: String,
        // The annotation to a model property lets you pass the serialized and deserialized
        // name as a string. This is useful if you don't want your model class and the JSON
        // to have identical naming.
        @SerializedName("circulating_supply")
        val circulatingSupply: Double,
        @SerializedName("total_supply")
        val totalSupply: Double,
        @SerializedName("max_supply")
        val maxSupply: Double,
        @SerializedName("date_added")
        val dateAdded: String,
        @SerializedName("num_market_pairs")
        val numMarketPairs: Int,
        @SerializedName("cmc_rank")
        val cmcRank: Int,
        @SerializedName("last_updated")
        val lastUpdated: String,
        val quote: Quote
) {

    data class Quote(
            // For additional option during deserialization you can specify value or alternative
            // values. Gson will check the JSON for all names we specify and try to find one to
            // map it to the annotated property.
            @SerializedName(value = "USD", alternate = [
                "ALL", "DZD", "ARS", "AMD", "AUD", "AZN", "BHD", "BDT", "BYN", "BMD", "BOB", "BAM",
                "BRL", "BGN", "KHR", "CAD", "CLP", "CNY", "COP", "CRC", "HRK", "CUP", "CZK", "DKK",
                "DOP", "EGP", "EUR", "GEL", "GHS", "GTQ", "HNL", "HKD", "HUF", "ISK", "INR", "IDR",
                "IRR", "IQD", "ILS", "JMD", "JPY", "JOD", "KZT", "KES", "KWD", "KGS", "LBP", "MKD",
                "MYR", "MUR", "MXN", "MDL", "MNT", "MAD", "MMK", "NAD", "NPR", "TWD", "NZD", "NIO",
                "NGN", "NOK", "OMR", "PKR", "PAB", "PEN", "PHP", "PLN", "GBP", "QAR", "RON", "RUB",
                "SAR", "RSD", "SGD", "ZAR", "KRW", "SSP", "VES", "LKR", "SEK", "CHF", "THB", "TTD",
                "TND", "TRY", "UGX", "UAH", "AED", "UYU", "UZS", "VND"
            ])
            val currency: Currency
    ) {

        data class Currency(
                val price: Double,
                @SerializedName("volume_24h")
                val volume24h: Double,
                @SerializedName("percent_change_1h")
                val percentChange1h: Double,
                @SerializedName("percent_change_24h")
                val percentChange24h: Double,
                @SerializedName("percent_change_7d")
                val percentChange7d: Double,
                @SerializedName("market_cap")
                val marketCap: Double,
                @SerializedName("last_updated")
                val lastUpdated: String
        )
    }
}