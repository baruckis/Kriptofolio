/*
 * Copyright 2018-2019 Andrius Baruckis www.baruckis.com | kriptofolio.app
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
@Database(entities = [MyCryptocurrency::class, Cryptocurrency::class], version = 1, exportSchema = false)

// App needs to use a custom data type whose value you would like to store in a single database
// column. To add this kind of support for custom types, you provide a TypeConverter, which converts
// a custom class to and from a known type that Room can persist.
@TypeConverters(Converters::class)

abstract class AppDatabase : RoomDatabase() {

    abstract fun myCryptocurrencyDao(): MyCryptocurrencyDao

    abstract fun cryptocurrencyDao(): CryptocurrencyDao
}