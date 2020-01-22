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

package com.baruckis.kriptofolio.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Entity object for Room database.
 */
@Entity(tableName = "all_cryptocurrencies")
// We will need to pass our custom data structure between different components in our app. For
// that we are going to use Android recommended way - Parcelable interface. Instead of making
// Parcelable class manually, we will use automatic Parcelable implementation generator. We just
// need to declare the serialized properties in a primary constructor and add a @Parcelize
// annotation, and writeToParcel()/createFromParcel() methods will be created automatically.
@Parcelize
data class Cryptocurrency(@PrimaryKey
                          @ColumnInfo(name = "id")
                          val id: Int,
        // It’s recommended to add @ColumnInfo annotation for all persisted
        // fields, in order to avoid problems when refactoring, especially when a field is renamed.
                          @ColumnInfo(name = "name")
                          var name: String,
                          @ColumnInfo(name = "rank")
                          var rank: Short,
                          @ColumnInfo(name = "symbol")
                          var symbol: String,
                          @ColumnInfo(name = "currency_fiat")
                          var currencyFiat: String,
                          @ColumnInfo(name = "price_fiat")
                          var priceFiat: Double,
                          @ColumnInfo(name = "price_percent_change_1h")
                          var pricePercentChange1h: Double,
                          @ColumnInfo(name = "price_percent_change_7d")
                          var pricePercentChange7d: Double,
                          @ColumnInfo(name = "price_percent_change_24h")
                          var pricePercentChange24h: Double,
                          @ColumnInfo(name = "last_fetched_date")
                          var lastFetchedDate: Date?) : Parcelable