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

package com.baruckis.mycryptocoins.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

/**
 * Entity object for Room database.
 */
@Entity(tableName = "cryptocurrencies")
// We will need to pass our custom data structure between different components in our app. For
// that we are going to use Android recommended way - Parcelable interface. Instead of making
// Parcelable class manually, we will use automatic Parcelable implementation generator. We just
// need to declare the serialized properties in a primary constructor and add a @Parcelize
// annotation, and writeToParcel()/createFromParcel() methods will be created automatically.
@Parcelize
data class Cryptocurrency(@PrimaryKey
                          val id: Int,
                          var name: String,
                          var rank: Short,
                          var amount: Double?,
                          var symbol: String,
                          var currencyFiat: String,
                          var priceFiat: Double,
                          var amountFiat: Double?,
                          var pricePercentChange1h: Double,
                          var pricePercentChange7d: Double,
                          var pricePercentChange24h: Double,
                          var amountFiatChange24h: Double?) : Parcelable