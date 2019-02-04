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
import androidx.room.*
import kotlinx.android.parcel.Parcelize

/**
 * Entity object for Room database.
 */
@Entity(tableName = "my_cryptocurrencies")
@Parcelize
// To hide warning that embedded cryptocurrency primary key will be ignored when merged.
@SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
data class MyCryptocurrency(
        @PrimaryKey
        @ColumnInfo(name = "my_id")
        val myId: Int,
        // Because one data class cannot inherit from another data class, we will use composition
        // instead.
        // If sub fields of an embedded field has PrimaryKey annotation, they will not be considered
        // as primary keys in the owner entity.
        @Embedded var cryptoData: Cryptocurrency,
        @ColumnInfo(name = "amount")
        var amount: Double? = null,
        @ColumnInfo(name = "amount_fiat")
        var amountFiat: Double? = null,
        @ColumnInfo(name = "amount_fiat_change_24h")
        var amountFiatChange24h: Double? = null) : Parcelable