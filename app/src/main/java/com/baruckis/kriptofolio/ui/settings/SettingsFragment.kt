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

package com.baruckis.kriptofolio.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.baruckis.kriptofolio.BuildConfig
import com.baruckis.kriptofolio.R
import com.baruckis.kriptofolio.dependencyinjection.Injectable
import com.baruckis.kriptofolio.ui.mainlist.MainActivity
import com.baruckis.kriptofolio.ui.settings.DonateCryptoDialog.Companion.DIALOG_DONATE_CRYPTO_TAG
import com.baruckis.kriptofolio.utilities.*
import java.util.*
import javax.inject.Inject

const val SERVICE_ACTION = "android.support.customtabs.action.CustomTabsService"
const val CHROME_PACKAGE = "com.android.chrome"

/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragmentCompat(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SettingsViewModel


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let { activity ->
            activity.title = viewModel.stringsLocalization.getString(R.string.title_activity_settings)
            if (activity is AppCompatActivity) activity.supportActionBar?.subtitle = ""
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)

        activity?.let { activity ->

            // Obtain ViewModel from ViewModelProvider, using parent activity as LifecycleOwner.
            viewModel = ViewModelProvider(activity, viewModelFactory)[SettingsViewModel::class.java]


            val preferenceLanguage = findPreference<Preference>(getString(R.string.pref_language_key))

            preferenceLanguage?.let {

                // Set the initial value for fiat currency preference summary.
                setListPreferenceSummary(preferenceLanguage, viewModel.currentLanguage)

                preferenceLanguage.setOnPreferenceChangeListener { preference, newValue ->

                    val newLanguage: String = newValue.toString()

                    if (newLanguage == viewModel.currentLanguage)
                    // False means not to update the state of the preference with the new value.
                        return@setOnPreferenceChangeListener false

                    setListPreferenceSummary(preference, newLanguage)

                    context?.let {

                        viewModel.stringsLocalization.setLanguage(newLanguage)

                        // The current activity and the other activities in the back stack is using the
                        // previous locale to show content. We have somehow to refresh them. The simplest
                        // way is to clear the existing task and start a new one.
                        val i = Intent(activity, MainActivity::class.java)
                        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                or Intent.FLAG_ACTIVITY_NEW_TASK))
                    }

                    true
                }
            }


            val preferenceFiatCurrency = findPreference<Preference>(getString(R.string.pref_fiat_currency_key))

            preferenceFiatCurrency?.let {

                // Set the initial value for fiat currency preference summary.
                setListPreferenceSummary(preferenceFiatCurrency, viewModel.currentFiatCurrencyCode)

                // Change the fiat currency preference summary when preference value changed.
                preferenceFiatCurrency.setOnPreferenceChangeListener { preference, newValue ->

                    val newCode: String = newValue.toString()

                    setListPreferenceSummary(preference, newCode)
                    true
                }

            }


            val preferenceDateFormat = findPreference<Preference>(getString(R.string.pref_date_format_key))

            preferenceDateFormat?.let {

                // Set the initial value for date format preference summary.
                setPreferenceDateFormatSummary(preferenceDateFormat, viewModel.currentDateFormat)

                // Change the date format preference summary when preference value changed.
                preferenceDateFormat.setOnPreferenceChangeListener { preference, newValue ->

                    val newFormat: String = newValue.toString()

                    setPreferenceDateFormatSummary(preference, newFormat)
                    true
                }

            }


            val preferenceRateApp = findPreference<Preference>(getString(R.string.pref_rate_app_key))

            preferenceRateApp?.let {

                preferenceRateApp.setOnPreferenceClickListener {

                    openAppInPlayStore()

                    true
                }

            }


            val preferenceShareApp = findPreference<Preference>(getString(R.string.pref_share_app_key))

            preferenceShareApp?.let {

                preferenceShareApp.setOnPreferenceClickListener {

                    shareApp()

                    true
                }

            }


            val preferenceDonateCrypto = findPreference<Preference>(getString(R.string.pref_donate_crypto_key))

            preferenceDonateCrypto?.let {

                // Removed donation methods just for FULL release as Google forbids other payments
                // besides Google Play. This is requirement to meet Google Play policy.
                preferenceDonateCrypto.isVisible = BuildConfig.IS_DEMO

                preferenceDonateCrypto.setOnPreferenceClickListener {

                    // Create an instance of the dialog fragment and show it.
                    val donateCryptoDialog =
                            DonateCryptoDialog.newInstance(
                                    title = viewModel.stringsLocalization
                                            .getString(R.string.dialog_donate_crypto_title),
                                    positiveButton = viewModel.stringsLocalization
                                            .getString(R.string.dialog_donate_crypto_positive_button))

                    // Display the alert dialog.
                    donateCryptoDialog.show(activity.supportFragmentManager, DIALOG_DONATE_CRYPTO_TAG)

                    true
                }

            }


            val preferenceBuyMeCoffee = findPreference<Preference>(getString(R.string.pref_buy_me_coffee_key))

            preferenceBuyMeCoffee?.let {

                preferenceBuyMeCoffee.isVisible = BuildConfig.IS_DEMO

                preferenceBuyMeCoffee.setOnPreferenceClickListener {

                    browseUrl(getString(R.string.pref_buy_me_coffee_url))

                    true
                }

            }


            val preferenceContact = findPreference<Preference>(getString(R.string.pref_contact_key))

            preferenceContact?.let {

                preferenceContact.setOnPreferenceClickListener {

                    val subject = viewModel.stringsLocalization.getString(R.string.app_name) +
                            " " + viewModel.stringsLocalization.getString(R.string.app_version)

                    sendEmailFeedback(FEEDBACK_EMAIL,
                            viewModel.stringsLocalization.getString(R.string.feedback_email_subject, subject))

                    true
                }

            }


            val preferenceWebsite = findPreference<Preference>(getString(R.string.pref_website_key))

            preferenceWebsite?.let {

                preferenceWebsite.setOnPreferenceClickListener {

                    browseUrl(getString(R.string.pref_website_url))

                    true
                }

            }


            val preferenceAuthor = findPreference<Preference>(getString(R.string.pref_author_key))

            preferenceAuthor?.let {

                preferenceAuthor.setOnPreferenceClickListener {

                    browseUrl(getString(R.string.pref_author_url))

                    true
                }

            }


            val preferenceSource = findPreference<Preference>(getString(R.string.pref_source_key))

            preferenceSource?.let {

                preferenceSource.setOnPreferenceClickListener {

                    browseUrl(getString(R.string.pref_source_url))

                    true
                }

            }


            val preferencePrivacyPolicy = findPreference<Preference>(getString(R.string.pref_privacy_policy_key))

            preferencePrivacyPolicy?.let {

                preferencePrivacyPolicy.setOnPreferenceClickListener {

                    browseUrlWithChromeCustomTab(getString(R.string.pref_privacy_policy_url))

                    true
                }

            }


            val preferenceThirdPartySoftware = findPreference<Preference>(getString(R.string.pref_third_party_software_key))

            preferenceThirdPartySoftware?.let {

                preferenceThirdPartySoftware.setOnPreferenceClickListener {

                    Navigation.findNavController(activity, R.id.nav_host_fragment)
                            .navigate(R.id.action_settings_dest_to_libraries_licenses_dest)

                    true
                }

            }


            val preferenceLicense = findPreference<Preference>(getString(R.string.pref_license_key))

            preferenceLicense?.let {

                preferenceLicense.setOnPreferenceClickListener {

                    Navigation.findNavController(activity, R.id.nav_host_fragment)
                            .navigate(R.id.action_settings_dest_to_license_dest,
                                    LicenseFragment.createArguments(
                                            viewModel.stringsLocalization.getString(R.string.app_name),
                                            viewModel.appLicenseData))

                    true
                }

            }

        }

    }

    private fun setListPreferenceSummary(preference: Preference, value: String) {
        if (preference is ListPreference) {
            val index = preference.findIndexOfValue(value)
            val entry = preference.entries[index]
            preference.summary = entry
        }
    }

    private fun setPreferenceDateFormatSummary(preference: Preference, value: String) {
        setListPreferenceSummary(preference, value)
        val todayDate = Calendar.getInstance().time
        preference.summary = preference.summary.toString() + " (" + formatDate(todayDate, value) + ")"
    }

    private fun browseUrl(uriString: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, viewModel.noBrowserFoundMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun browseUrlWithChromeCustomTab(uriString: String) {
        activity?.let { context ->
            /*
            * Chrome Custom Tabs give apps more control over their web experience, and make
            * transitions between native and web content more seamless without having to
            * resort to a WebView. Chrome Custom Tabs allow an app to customize how Chrome
            * looks and feels.
            * */
            if (context.isChromeCustomTabsSupported()) {
                CustomTabsIntent.Builder().apply {
                    setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                }.build().launchUrl(context, Uri.parse(uriString))
            } else {
                browseUrl(uriString)
            }
        }
    }

    private fun Context.isChromeCustomTabsSupported(): Boolean {
        val serviceIntent = Intent(SERVICE_ACTION)
        serviceIntent.setPackage(CHROME_PACKAGE)
        val resolveInfos = packageManager.queryIntentServices(serviceIntent, 0)
        return resolveInfos.isNotEmpty()
    }

    // Link in Google Play store app on the phone.
    private fun openAppInPlayStore() {
        val uri = Uri.parse("market://" + GOOGLE_PLAY_STORE_APP_DETAILS_PATH +
                getPackageName())
        val goToMarketIntent = Intent(Intent.ACTION_VIEW, uri)

        var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        flags = if (Build.VERSION.SDK_INT >= 21) {
            flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        } else {
            flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        goToMarketIntent.addFlags(flags)

        try {
            startActivity(goToMarketIntent, null)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_STORE_APPS_URL +
                            GOOGLE_PLAY_STORE_APP_DETAILS_PATH + getPackageName()))

            startActivity(intent, null)
        }
    }

    // Share entire app with share intent.
    private fun shareApp() {
        val intent = Intent()
        intent.type = "text/plain"
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_SUBJECT, viewModel.stringsLocalization.getString(R.string.app_name))
        intent.putExtra(Intent.EXTRA_TEXT,
                viewModel.stringsLocalization.getString(R.string.share_app_message) +
                        GOOGLE_PLAY_STORE_APPS_URL + GOOGLE_PLAY_STORE_APP_DETAILS_PATH +
                        getPackageName())
        startActivity(Intent.createChooser(intent,
                viewModel.stringsLocalization.getString(R.string.share_app_chooser)))
    }

    private fun getPackageName(): String? {
        // Get a string resource item generated at build time by Gradle.
        val suffix = getString(R.string.app_id_suffix)
        return context?.packageName?.removeSuffix(suffix)
    }


    private fun sendEmailFeedback(toEmail: String, subject: String) {

        val uriBuilder = StringBuilder("mailto:" + Uri.encode(toEmail))
        uriBuilder.append("?subject=" + Uri.encode(subject))
        val uriString = uriBuilder.toString()

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uriString))

        try {
            startActivity(intent)
        } catch (e: Exception) {
            logConsoleError(e.localizedMessage ?: "Exception error")

            // If there is no email client application, than show error message for the user.
            if (e is ActivityNotFoundException) {
                Toast.makeText(
                        activity,
                        viewModel.stringsLocalization.getString(R.string.no_application_handle)
                                + " " + viewModel.stringsLocalization.getString(R.string.install_email, toEmail),
                        Toast.LENGTH_LONG).show()
            }
        }
    }

}