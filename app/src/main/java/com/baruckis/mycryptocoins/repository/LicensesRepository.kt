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

package com.baruckis.mycryptocoins.repository

import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.db.LibraryLicenseInfo
import com.baruckis.mycryptocoins.utilities.localization.StringsLocalization
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicensesRepository @Inject constructor(
        private val stringsLocalization: StringsLocalization
) {

    fun getLibrariesLicensesList(): List<LibraryLicenseInfo> {

        val data = ArrayList<LibraryLicenseInfo>()

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_kotlin),
                stringsLocalization.getString(R.string.library_developer_kotlin),
                stringsLocalization.getString(R.string.library_link_kotlin),
                stringsLocalization.getString(R.string.library_license_name_kotlin),
                stringsLocalization.getString(R.string.license_kotlin_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_coroutines),
                stringsLocalization.getString(R.string.library_developer_coroutines),
                stringsLocalization.getString(R.string.library_link_coroutines),
                stringsLocalization.getString(R.string.library_license_name_coroutines),
                stringsLocalization.getString(R.string.license_coroutines_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_coroutines_android),
                stringsLocalization.getString(R.string.library_developer_coroutines_android),
                stringsLocalization.getString(R.string.library_link_coroutines_android),
                stringsLocalization.getString(R.string.library_license_name_coroutines_android),
                stringsLocalization.getString(R.string.license_coroutines_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_ktx),
                stringsLocalization.getString(R.string.library_developer_ktx),
                stringsLocalization.getString(R.string.library_link_ktx),
                stringsLocalization.getString(R.string.library_license_name_ktx),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_appcompat),
                stringsLocalization.getString(R.string.library_developer_android_appcompat),
                stringsLocalization.getString(R.string.library_link_android_appcompat),
                stringsLocalization.getString(R.string.library_license_name_android_appcompat),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_cardview),
                stringsLocalization.getString(R.string.library_developer_android_cardview),
                stringsLocalization.getString(R.string.library_link_android_cardview),
                stringsLocalization.getString(R.string.library_license_name_android_cardview),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_preference),
                stringsLocalization.getString(R.string.library_developer_android_preference),
                stringsLocalization.getString(R.string.library_link_android_preference),
                stringsLocalization.getString(R.string.library_license_name_android_preference),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_recyclerview),
                stringsLocalization.getString(R.string.library_developer_android_recyclerview),
                stringsLocalization.getString(R.string.library_link_android_recyclerview),
                stringsLocalization.getString(R.string.library_license_name_android_recyclerview),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_recyclerview_selection),
                stringsLocalization.getString(R.string.library_developer_android_recyclerview_selection),
                stringsLocalization.getString(R.string.library_link_android_recyclerview_selection),
                stringsLocalization.getString(R.string.library_license_name_android_recyclerview_selection),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_constraint_layout),
                stringsLocalization.getString(R.string.library_developer_android_constraint_layout),
                stringsLocalization.getString(R.string.library_link_android_constraint_layout),
                stringsLocalization.getString(R.string.library_license_name_android_constraint_layout),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_material_components),
                stringsLocalization.getString(R.string.library_developer_android_material_components),
                stringsLocalization.getString(R.string.library_link_android_material_components),
                stringsLocalization.getString(R.string.library_license_name_android_material_components),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_data_binding),
                stringsLocalization.getString(R.string.library_developer_android_data_binding),
                stringsLocalization.getString(R.string.library_link_android_data_binding),
                stringsLocalization.getString(R.string.library_license_name_android_data_binding),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_lifecycle_aware),
                stringsLocalization.getString(R.string.library_developer_android_lifecycle_aware),
                stringsLocalization.getString(R.string.library_link_android_lifecycle_aware),
                stringsLocalization.getString(R.string.library_license_name_android_lifecycle_aware),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_room),
                stringsLocalization.getString(R.string.library_developer_android_room),
                stringsLocalization.getString(R.string.library_link_android_room),
                stringsLocalization.getString(R.string.library_license_name_android_room),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_navigation),
                stringsLocalization.getString(R.string.library_developer_android_navigation),
                stringsLocalization.getString(R.string.library_link_android_navigation),
                stringsLocalization.getString(R.string.library_license_name_android_navigation),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_dagger),
                stringsLocalization.getString(R.string.library_developer_dagger),
                stringsLocalization.getString(R.string.library_link_dagger),
                stringsLocalization.getString(R.string.library_license_name_dagger),
                stringsLocalization.getString(R.string.license_dagger_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_retrofit),
                stringsLocalization.getString(R.string.library_developer_retrofit),
                stringsLocalization.getString(R.string.library_link_retrofit),
                stringsLocalization.getString(R.string.library_license_name_retrofit),
                stringsLocalization.getString(R.string.license_retrofit_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_okhttp),
                stringsLocalization.getString(R.string.library_developer_okhttp),
                stringsLocalization.getString(R.string.library_link_okhttp),
                stringsLocalization.getString(R.string.library_license_name_okhttp),
                stringsLocalization.getString(R.string.license_okhttp_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_okhttp_logging_interceptor),
                stringsLocalization.getString(R.string.library_developer_okhttp_logging_interceptor),
                stringsLocalization.getString(R.string.library_link_okhttp_logging_interceptor),
                stringsLocalization.getString(R.string.library_license_name_okhttp_logging_interceptor),
                stringsLocalization.getString(R.string.license_okhttp_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_gson),
                stringsLocalization.getString(R.string.library_developer_gson),
                stringsLocalization.getString(R.string.library_link_gson),
                stringsLocalization.getString(R.string.library_license_name_gson),
                stringsLocalization.getString(R.string.license_gson_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_glide),
                stringsLocalization.getString(R.string.library_developer_glide),
                stringsLocalization.getString(R.string.library_link_glide),
                stringsLocalization.getString(R.string.library_license_name_glide),
                stringsLocalization.getString(R.string.license_glide)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_glide_okhttp),
                stringsLocalization.getString(R.string.library_developer_glide_okhttp),
                stringsLocalization.getString(R.string.library_link_glide_okhttp),
                stringsLocalization.getString(R.string.library_license_name_glide_okhttp),
                stringsLocalization.getString(R.string.license_glide)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_flipview),
                stringsLocalization.getString(R.string.library_developer_flipview),
                stringsLocalization.getString(R.string.library_link_flipview),
                stringsLocalization.getString(R.string.library_license_name_flipview),
                stringsLocalization.getString(R.string.license_flipview_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_stetho),
                stringsLocalization.getString(R.string.library_developer_stetho),
                stringsLocalization.getString(R.string.library_link_stetho),
                stringsLocalization.getString(R.string.library_license_name_stetho),
                stringsLocalization.getString(R.string.license_stetho_bsd)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_google_ads),
                stringsLocalization.getString(R.string.library_developer_google_ads),
                stringsLocalization.getString(R.string.library_link_google_ads),
                stringsLocalization.getString(R.string.library_license_name_google_ads),
                stringsLocalization.getString(R.string.license_android_sdk)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_oss_licenses),
                stringsLocalization.getString(R.string.library_developer_oss_licenses),
                stringsLocalization.getString(R.string.library_link_oss_licenses),
                stringsLocalization.getString(R.string.library_license_name_oss_licenses),
                stringsLocalization.getString(R.string.license_android_sdk)))


        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_test),
                stringsLocalization.getString(R.string.library_developer_android_test),
                stringsLocalization.getString(R.string.library_link_android_test),
                stringsLocalization.getString(R.string.library_license_name_android_test),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_android_test_espresso),
                stringsLocalization.getString(R.string.library_developer_android_test_espresso),
                stringsLocalization.getString(R.string.library_link_android_test_espresso),
                stringsLocalization.getString(R.string.library_license_name_android_test_espresso),
                stringsLocalization.getString(R.string.license_android_apache_v2_copyright)+
                        stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice)+
                        stringsLocalization.getString(R.string.license_apache_v2)))

        data.add(LibraryLicenseInfo(
                stringsLocalization.getString(R.string.library_title_junit),
                stringsLocalization.getString(R.string.library_developer_junit),
                stringsLocalization.getString(R.string.library_link_junit),
                stringsLocalization.getString(R.string.library_license_name_junit),
                stringsLocalization.getString(R.string.license_junit_epl_v1)))


        return data
    }

    fun getOssLicensesTitle(): String {
        return stringsLocalization.getString(R.string.activity_oss_licenses_menu_title)
    }

    fun getNoBrowserFoundMessage(): String {
        return stringsLocalization.getString(R.string.no_application_handle) + " " +
                stringsLocalization.getString(R.string.install_web_browser)
    }

    fun getAppLicense(): String {
        return stringsLocalization.getString(R.string.license_mycryptocoins_apache_v2_copyright) +
                stringsLocalization.getString(R.string.license_apache_v2_boilerplate_notice) +
                stringsLocalization.getString(R.string.license_apache_v2)
    }

}