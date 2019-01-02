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

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

/**
 * Glide v4 uses an annotation processor to generate an API that allows applications to access all
 * options in RequestBuilder, RequestOptions and any included integration libraries in a single
 * fluent API.
 *
 * The generated API serves two purposes:
 * Integration libraries can extend Glide’s API with custom options.
 * Applications can extend Glide’s API by adding methods that bundle commonly used options.
 *
 * Although both of these tasks can be accomplished by hand by writing custom subclasses of
 * RequestOptions, doing so is challenging and produces a less fluent API.
 */
@GlideModule
class AppGlideModule : AppGlideModule()