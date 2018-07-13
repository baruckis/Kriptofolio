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

package com.baruckis.mycryptocoins.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.baruckis.mycryptocoins.utilities.DATABASE_NAME
import com.baruckis.mycryptocoins.utilities.ioThread

/**
 * The Room database for this app.
 */
@Database(entities = [Cryptocurrency::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun myCryptocurrencyDao(): MyCryptocurrencyDao


    // The AppDatabase a singleton to prevent having multiple instances of the database opened at the same time.
    companion object {

        // Marks the JVM backing field of the annotated property as volatile, meaning that writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: AppDatabase? = null

        // For Singleton instantiation.
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Creates and pre-populates the database.
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    // Prepopulate the database after onCreate was called.
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Insert the data on the IO Thread.
                            ioThread {
                                getInstance(context).myCryptocurrencyDao().insertDataToMyCryptocurrencyList(PREPOPULATE_DATA)
                            }
                        }
                    })
                    .build()
        }

        // Sample data.
        val btc: Cryptocurrency = Cryptocurrency("Bitcoin", 1, 0.56822348, "BTC", 8328.77, 4732.60, 0.19, -10.60, 0.44, 20.82)
        val eth: Cryptocurrency = Cryptocurrency("Etherium", 2, 6.0, "ETH", 702.99, 4217.94, 0.13, -7.38, 0.79, 33.32)

        val PREPOPULATE_DATA = listOf(btc, eth)

    }

}