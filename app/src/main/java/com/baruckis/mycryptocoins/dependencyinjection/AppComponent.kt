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

import com.baruckis.mycryptocoins.App
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

/**
 * Singleton component interface for the app. It ties all the modules together.
 * The component is used to connect objects to their dependencies.
 * Dagger will auto-generate DaggerAppComponent which is used for initialization at Application.
 */
@Singleton
@Component(
        modules = [
            // AndroidSupportInjectionModule is a class of Dagger and we don't need to create it.
            // If you want to use injection in fragment then you should use AndroidSupportInjectionModule.class else use AndroidInjectionModule.
            AndroidSupportInjectionModule::class,
            AppModule::class,
            ActivityBuildersModule::class
        ]
)
interface AppComponent {

    @Component.Builder // Used for instantiation of a component.
    interface Builder {

        @BindsInstance // Bind our application instance to our Dagger graph.
        fun application(application: App): Builder

        fun build(): AppComponent
    }

    // The application which is allowed to request the dependencies declared by the modules
    // (by means of the @Inject annotation) should be declared here with individual inject() methods.
    fun inject(app: App)
}