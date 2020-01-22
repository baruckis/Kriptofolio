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

package com.baruckis.kriptofolio.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.baruckis.kriptofolio.R
import kotlinx.android.synthetic.main.fragment_license.view.*


/**
 * A simple [Fragment] subclass.
 *
 */
class LicenseFragment : Fragment() {

    companion object {
        // The fragment initialization parameters.
        private const val SUBTITLE_ARGUMENT = "subtitle"
        private const val LICENSE_ARGUMENT = "license"

        fun createArguments(subtitle: String, license: String): Bundle {
            val bundle = Bundle()
            bundle.putSerializable(SUBTITLE_ARGUMENT, subtitle)
            bundle.putSerializable(LICENSE_ARGUMENT, license)
            return bundle
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let { activity ->
            activity.title = getString(R.string.fragment_license_title)
            if (activity is AppCompatActivity) activity.supportActionBar?.subtitle =
                    arguments?.getString(SUBTITLE_ARGUMENT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment.
        val view = inflater.inflate(R.layout.fragment_license, container, false)

        val textViewLicense = view.license
        textViewLicense.text = arguments?.getString(LICENSE_ARGUMENT)

        return view
    }
}
