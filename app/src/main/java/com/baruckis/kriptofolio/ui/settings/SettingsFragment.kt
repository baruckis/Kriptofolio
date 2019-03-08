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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import java.util.*
import javax.inject.Inject


/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragmentCompat(), Injectable, RewardedVideoAdListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SettingsViewModel

    private lateinit var rewardedVideoAd: RewardedVideoAd


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

            // Obtain ViewModel from ViewModelProviders, using parent activity as LifecycleOwner.
            viewModel = ViewModelProviders.of(activity, viewModelFactory).get(SettingsViewModel::class.java)


            // Use an activity context to get the rewarded video instance.
            rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity)
            rewardedVideoAd.rewardedVideoAdListener = this

            // It is highly recommended that you call load ad as early as possible to allow videos
            // to be preloaded.
            loadRewardedVideoAd()


            val preferenceLanguage = findPreference(getString(R.string.pref_language_key)) as Preference

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


            val preferenceFiatCurrency = findPreference(getString(R.string.pref_fiat_currency_key)) as Preference

            // Set the initial value for fiat currency preference summary.
            setListPreferenceSummary(preferenceFiatCurrency, viewModel.currentFiatCurrencyCode)

            // Change the fiat currency preference summary when preference value changed.
            preferenceFiatCurrency.setOnPreferenceChangeListener { preference, newValue ->

                val newCode: String = newValue.toString()

                setListPreferenceSummary(preference, newCode)
                true
            }


            val preferenceDateFormat = findPreference(getString(R.string.pref_date_format_key)) as Preference

            // Set the initial value for date format preference summary.
            setPreferenceDateFormatSummary(preferenceDateFormat, viewModel.currentDateFormat)

            // Change the date format preference summary when preference value changed.
            preferenceDateFormat.setOnPreferenceChangeListener { preference, newValue ->

                val newFormat: String = newValue.toString()

                setPreferenceDateFormatSummary(preference, newFormat)
                true
            }


            val preferenceRateApp = findPreference(getString(R.string.pref_rate_app_key)) as Preference

            preferenceRateApp.setOnPreferenceClickListener {

                openAppInPlayStore()

                true
            }


            val preferenceShareApp = findPreference(getString(R.string.pref_share_app_key)) as Preference

            preferenceShareApp.setOnPreferenceClickListener {

                shareApp()

                true
            }


            val preferenceDonateView = findPreference(getString(R.string.pref_donate_view_key)) as Preference

            preferenceDonateView.setOnPreferenceClickListener {

                logConsoleVerbose("Video ad is requested.")

                if (rewardedVideoAd.isLoaded) {
                    rewardedVideoAd.show()
                } else {
                    viewModel.videoAdIsRequested = true
                    Toast.makeText(activity, viewModel.stringsLocalization
                            .getString(R.string.video_ad_loading), Toast.LENGTH_SHORT).show()
                    loadRewardedVideoAd()
                }

                true
            }


            val preferenceDonateCrypto = findPreference(getString(R.string.pref_donate_crypto_key)) as Preference

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


            val preferenceBuyMeCoffee = findPreference(getString(R.string.pref_buy_me_coffee_key)) as Preference

            preferenceBuyMeCoffee.isVisible = BuildConfig.IS_DEMO

            preferenceBuyMeCoffee.setOnPreferenceClickListener {

                browseUrl(getString(R.string.pref_buy_me_coffee_url))

                true
            }


            val preferenceWebsite = findPreference(getString(R.string.pref_website_key)) as Preference

            preferenceWebsite.setOnPreferenceClickListener {

                browseUrl(getString(R.string.pref_website_url))

                true
            }


            val preferenceAuthor = findPreference(getString(R.string.pref_author_key)) as Preference

            preferenceAuthor.setOnPreferenceClickListener {

                browseUrl(getString(R.string.pref_author_url))

                true
            }


            val preferenceSource = findPreference(getString(R.string.pref_source_key)) as Preference

            preferenceSource.setOnPreferenceClickListener {

                browseUrl(getString(R.string.pref_source_url))

                true
            }


            val preferenceThirdPartySoftware = findPreference(getString(R.string.pref_third_party_software_key)) as Preference

            preferenceThirdPartySoftware.setOnPreferenceClickListener {

                Navigation.findNavController(activity, R.id.nav_host_fragment)
                        .navigate(R.id.action_settings_dest_to_libraries_licenses_dest)

                true
            }


            val preferenceLicense = findPreference(getString(R.string.pref_license_key)) as Preference

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


    override fun onPause() {
        super.onPause()
        rewardedVideoAd.pause(activity)
    }

    override fun onResume() {
        super.onResume()
        rewardedVideoAd.resume(activity)
    }

    override fun onDestroy() {
        super.onDestroy()
        rewardedVideoAd.destroy(activity)
    }


    override fun onRewardedVideoAdClosed() {

        logConsoleVerbose("onRewardedVideoAdClosed")

        loadRewardedVideoAd()
    }

    override fun onRewardedVideoAdLeftApplication() {

        logConsoleVerbose("onRewardedVideoAdLeftApplication")
    }

    override fun onRewardedVideoAdLoaded() {

        logConsoleVerbose("onRewardedVideoAdLoaded")

        if (viewModel.videoAdIsRequested) {
            viewModel.videoAdIsRequested = false
            rewardedVideoAd.show()
        }
    }

    override fun onRewardedVideoAdOpened() {

        logConsoleVerbose("onRewardedVideoAdOpened")
    }

    override fun onRewardedVideoCompleted() {

        logConsoleVerbose("onRewardedVideoCompleted")
    }

    override fun onRewarded(reward: RewardItem?) {

        logConsoleVerbose("onRewarded! Points: ${reward?.type} Amount: ${reward?.amount}")

        Toast.makeText(activity, viewModel.stringsLocalization
                .getString(R.string.video_ad_thank_you), Toast.LENGTH_LONG).show()
    }

    override fun onRewardedVideoStarted() {

        logConsoleVerbose("onRewardedVideoStarted")
    }

    override fun onRewardedVideoAdFailedToLoad(p0: Int) {

        logConsoleVerbose("onRewardedVideoAdFailedToLoad")

        if (viewModel.videoAdIsRequested) {
            viewModel.videoAdIsRequested = false
            Toast.makeText(activity, viewModel.stringsLocalization
                    .getString(R.string.video_ad_failed_load), Toast.LENGTH_LONG).show()
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

    private fun loadRewardedVideoAd() {
        // The easiest way to load test ads is to use dedicated test ad unit ID for Android
        // rewarded video. It's been specially configured to return test ads for every request,
        // and you're free to use it in your own apps while coding, testing, and debugging.
        rewardedVideoAd.loadAd(ADMOB_AD_UNIT_ID,
                AdRequest.Builder().build())
    }

    private fun browseUrl(uriString: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, viewModel.noBrowserFoundMessage, Toast.LENGTH_LONG).show()
        }
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

}