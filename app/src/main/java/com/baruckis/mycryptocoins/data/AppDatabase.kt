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

    abstract fun cryptocurrencyDao(): CryptocurrencyDao

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
                                getInstance(context).cryptocurrencyDao().insertDataToAllCryptocurrencyList(PREPOPULATE_DATA)
                            }
                        }
                    })
                    .build()
        }

        // Sample data.
        val btc: Cryptocurrency = Cryptocurrency("Bitcoin", 1, 0.56822348, "BTC", 6972.90, 3962.16, 0.25, -14.05, -0.55, -21.79)
        val eth: Cryptocurrency = Cryptocurrency("Etherium", 2, 6.0, "ETH", 407.45, 2444.70, 0.31, -10.96, 0.13, 3.17)
        val xrp: Cryptocurrency = Cryptocurrency("XRP", 3, 0.0, "XRP", 0.423225, 0.0, -0.02, -5.30, -1.38, 0.0)
        val bch: Cryptocurrency = Cryptocurrency("Bitcoin Cash", 4, 0.0, "BCH", 693.52, 0.0, 0.30, -14.40, -0.46, 0.0)
        val eos: Cryptocurrency = Cryptocurrency("EOS", 5, 0.0, "EOS", 7.01, 0.0, 0.18, -11.80, -0.11, 0.0)

        val PREPOPULATE_DATA = listOf(btc, eth, xrp, bch, eos)
    }

}