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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import com.baruckis.kriptofolio.R
import kotlinx.android.synthetic.main.dialog_donate_crypto.view.*


class DonateCryptoDialog : DialogFragment() {

    companion object {

        const val DIALOG_DONATE_CRYPTO_TAG = "donate_crypto_dialog"

        private const val EXTRA_TITLE = "title"
        private const val EXTRA_POSITIVE_BUTTON = "positive_button"

        fun newInstance(title: String, positiveButton: String): DonateCryptoDialog {
            val dialog = DonateCryptoDialog()
            val args = Bundle().apply {
                putString(EXTRA_TITLE, title)
                putString(EXTRA_POSITIVE_BUTTON, positiveButton)
            }
            dialog.arguments = args
            return dialog
        }
    }


    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = arguments?.getString(DonateCryptoDialog.EXTRA_TITLE)
        val positiveButton = arguments?.getString(DonateCryptoDialog.EXTRA_POSITIVE_BUTTON)

        val dialog = activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)

            // Set a title for alert dialog.
            builder.setTitle(title)

            // Pass null as the parent view because its going in the dialog layout.
            val dialogView = activity.layoutInflater.inflate(R.layout.dialog_donate_crypto, null)

            dialogView.item_bitcoin_address.setOnClickListener {
                copyCryptoAddressToClipBoard(activity, getString(R.string.dialog_donate_crypto_bitcoin_address))
                Toast.makeText(context, getString(R.string.dialog_donate_crypto_bitcoin_address_copy_confirmation), Toast.LENGTH_SHORT).show()
            }

            dialogView.item_ethereum_address.setOnClickListener {
                copyCryptoAddressToClipBoard(activity, getString(R.string.dialog_donate_crypto_ethereum_address))
                Toast.makeText(context, getString(R.string.dialog_donate_crypto_ethereum_address_copy_confirmation), Toast.LENGTH_SHORT).show()
            }

            dialogView.scrollview.viewTreeObserver.addOnScrollChangedListener {
                controlScrollDividersVisibility(dialogView.scrollview, dialogView.divider_bottom, dialogView.divider_top)
            }

            // OnGlobalLayoutListener is *massively* overkill 99% of the time. That is why
            // android-ktx has doOnLayout() and doOnNextLayout() which use View.OnLayoutChangeListener.
            dialogView.scrollview.doOnLayout {
                controlScrollDividersVisibility(dialogView.scrollview, dialogView.divider_bottom, dialogView.divider_top)
            }

            // Set the layout for the dialog.
            builder.setView(dialogView)

            builder.setCancelable(true)

            // Set the alert dialog positive button.
            builder.setPositiveButton(positiveButton) { _, _ ->
                // Do nothing.
            }

            // Initialize the AlertDialog using builder object.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null!")

        return dialog
    }


    private fun controlScrollDividersVisibility(scrollView: ScrollView,
                                                dividerBottom: View,
                                                dividerTop: View) {
        if (!scrollView.canScrollVertically(1)) {
            // Bottom of scroll view.
            dividerBottom.visibility = View.INVISIBLE
        } else dividerBottom.visibility = View.VISIBLE
        if (!scrollView.canScrollVertically(-1)) {
            // Top of scroll view.
            dividerTop.visibility = View.INVISIBLE
        } else dividerTop.visibility = View.VISIBLE
    }

    private fun copyCryptoAddressToClipBoard(context: Context, address: String) {
        val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText(null, address)
        clipboard?.primaryClip = clip
    }

}