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

package com.baruckis.mycryptocoins.utilities

/**
 * Constants used throughout the app.
 */
const val DATABASE_NAME = "my-crypto-coins-db"
const val CRYPTO_FORMAT_PATTERN = "#,##0.00000000"
const val FIAT_FORMAT_PATTERN = "#,##0.00"
const val PERCENT_FORMAT_PATTERN = "##0.00"
const val DELAY_MILLISECONDS: Long = 1000
const val FLIPVIEW_CHARACTER_LIMIT = 3
const val API_SERVICE_BASE_URL = "https://sandbox-api.coinmarketcap.com/"
const val API_SERVICE_AUTHENTICATION_NAME = "X-CMC_PRO_API_KEY"
const val API_SERVICE_AUTHENTICATION_KEY = "" // TODO: Use your API Key provided by CoinMarketCap Professional API Developer Portal.
const val API_SERVICE_RESULTS_LIMIT = 5000 // This is max number from CoinMarketCap API.
const val DB_ID_SCREEN_MAIN_LIST = "main_list"
const val DB_ID_SCREEN_ADD_SEARCH_LIST = "add_search_list"
const val DATE_FORMAT_PATTERN = "MM/dd/yyyy HH:mm:ss"
const val CRYPTOCURRENCY_IMAGE_URL = "https://s2.coinmarketcap.com/static/img/coins"
const val CRYPTOCURRENCY_IMAGE_SIZE_PX = "128x128"
const val CRYPTOCURRENCY_IMAGE_FILE = ".png"