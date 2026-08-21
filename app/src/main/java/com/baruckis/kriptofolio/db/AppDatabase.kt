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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The Room database for this app.
 */
// exportSchema is on so that Room writes the schema of every version to app/schemas as JSON.
// That file is the database contract: it records exactly which tables, columns and types the
// shipped app expects, and its identity hash is what Room compares against on every open. Without
// it the contract exists only inside generated code, and an accidental change to an entity is
// invisible in review - which matters here more than in most apps, because portfolios live only
// on the device, there is no backup, and a mismatch is a deletion rather than an error.
@Database(entities = [MyCryptocurrency::class, Cryptocurrency::class], version = 1, exportSchema = true)

// App needs to use a custom data type whose value you would like to store in a single database
// column. To add this kind of support for custom types, you provide a TypeConverter, which converts
// a custom class to and from a known type that Room can persist.
@TypeConverters(Converters::class)

abstract class AppDatabase : RoomDatabase() {

    abstract fun myCryptocurrencyDao(): MyCryptocurrencyDao

    abstract fun cryptocurrencyDao(): CryptocurrencyDao
}