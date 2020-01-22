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

package com.baruckis.kriptofolio.utilities

/**
 * Constants used throughout the app.
 */
const val LOG_TAG = "kriptofolio"
const val DATABASE_NAME = "kriptofolio-db"
const val CRYPTO_FORMAT_PATTERN = "#,##0.00000000"
const val FIAT_FORMAT_PATTERN = "#,##0.00"
const val PERCENT_FORMAT_PATTERN = "##0.00"
const val ADD_TASK_REQUEST = 1
const val SERVER_CALL_DELAY_MILLISECONDS: Long = 1000
const val FLIPVIEW_CHARACTER_LIMIT = 3

const val API_SERVICE_AUTHENTICATION_NAME = "X-CMC_PRO_API_KEY"

const val API_SERVICE_RESULTS_LIMIT = 5000 // This is max number from CoinMarketCap API.
const val TIME_12h_FORMAT_PATTERN = "hh:mm:ss"
const val TIME_24h_FORMAT_PATTERN = "HH:mm:ss"
const val CRYPTOCURRENCY_IMAGE_URL = "https://s2.coinmarketcap.com/static/img/coins"
const val CRYPTOCURRENCY_IMAGE_SIZE_PX = "128x128"
const val CRYPTOCURRENCY_IMAGE_FILE = ".png"
const val SEARCH_TYPING_DELAY_MILLISECONDS: Long = 500

const val GOOGLE_PLAY_STORE_APPS_URL = "https://play.google.com/store/apps/"
const val GOOGLE_PLAY_STORE_APP_DETAILS_PATH = "details?id="
const val FEEDBACK_EMAIL = "hello@kriptofolio.app"