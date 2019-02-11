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

package com.baruckis.mycryptocoins.dependencyinjection

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.room.Room
import com.baruckis.mycryptocoins.App
import com.baruckis.mycryptocoins.BuildConfig
import com.baruckis.mycryptocoins.api.ApiService
import com.baruckis.mycryptocoins.api.AuthenticationInterceptor
import com.baruckis.mycryptocoins.db.AppDatabase
import com.baruckis.mycryptocoins.db.CryptocurrencyDao
import com.baruckis.mycryptocoins.db.MyCryptocurrencyDao
import com.baruckis.mycryptocoins.utilities.API_SERVICE_BASE_URL
import com.baruckis.mycryptocoins.utilities.DATABASE_NAME
import com.baruckis.mycryptocoins.utilities.LiveDataCallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * AppModule will provide app-wide dependencies for a part of the application.
 * It should initialize objects used across our application, such as Room database, Retrofit, Shared Preference, etc.
 */
@Module(includes = [ViewModelsModule::class])
class AppModule() {

    @Provides // Annotation informs Dagger compiler that this method is the constructor for the Context return type.
    @Singleton // Annotation informs Dagger compiler that the instance should be created only once in the entire lifecycle of the application.
    fun provideContext(app: App): Context = app // Using provide as a prefix is a common convention but not a requirement.

    @Provides
    @Singleton
    fun provideHttpClient(): OkHttpClient {
        // We need to prepare a custom OkHttp client because need to use our custom call interceptor.
        // to be able to authenticate our requests.
        val builder = OkHttpClient.Builder()
        // We add the interceptor to OkHttpClient.
        // It will add authentication headers to every call we make.
        builder.interceptors().add(AuthenticationInterceptor())

        // Configure this client not to retry when a connectivity problem is encountered.
        builder.retryOnConnectionFailure(false)

        // Log requests and responses.
        // Add logging as the last interceptor, because this will also log the information which
        // you added or manipulated with previous interceptors to your request.
        builder.interceptors().add(HttpLoggingInterceptor().apply {
            // For production environment to enhance apps performance we will be skipping any
            // logging operation. We will show logs just for debug builds.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApiService(httpClient: OkHttpClient): ApiService {
        return Retrofit.Builder() // Create retrofit builder.
                .baseUrl(API_SERVICE_BASE_URL) // Base url for the api has to end with a slash.
                .addConverterFactory(GsonConverterFactory.create()) // Use GSON converter for JSON to POJO object mapping.
                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .client(httpClient) // Here we set the custom OkHttp client we just created.
                .build().create(ApiService::class.java) // We create an API using the interface we defined.
    }

    @Provides
    @Singleton
    fun provideDb(app: App): AppDatabase {
        return Room
                .databaseBuilder(app, AppDatabase::class.java, DATABASE_NAME)
                // At current moment we don't want to provide migrations and specifically want database to be cleared when upgrade the version.
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    @Singleton
    fun provideMyCryptocurrencyDao(db: AppDatabase): MyCryptocurrencyDao {
        return db.myCryptocurrencyDao()
    }

    @Provides
    @Singleton
    fun provideCryptocurrencyDao(db: AppDatabase): CryptocurrencyDao {
        return db.cryptocurrencyDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(app: App): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
}