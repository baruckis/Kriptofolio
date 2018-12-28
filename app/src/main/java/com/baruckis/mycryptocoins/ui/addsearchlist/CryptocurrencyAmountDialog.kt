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

package com.baruckis.mycryptocoins.ui.addsearchlist

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.utilities.nonEmpty
import com.baruckis.mycryptocoins.utilities.validate
import kotlinx.android.synthetic.main.dialog_add_crypto_amount.view.*

/**
 * UI for add crypto coins screen dialog where you enter amount of the coins that you have.
 */
class CryptocurrencyAmountDialog : DialogFragment() {

    companion object {

        const val DIALOG_CRYPTOCURRENCY_AMOUNT_TAG = "cryptocurrency_amount_dialog"

        private const val EXTRA_TITLE = "title"
        private const val EXTRA_HINT = "hint"
        private const val EXTRA_CONFIRM_BUTTON = "confirm_button"
        private const val EXTRA_CANCEL_BUTTON = "cancel_button"
        private const val EXTRA_ERROR = "error"

        fun newInstance(title: String, hint: String, confirmButton: String, cancelButton: String, error: String): CryptocurrencyAmountDialog {
            val dialog = CryptocurrencyAmountDialog()
            val args = Bundle().apply {
                putString(EXTRA_TITLE, title)
                putString(EXTRA_HINT, hint)
                putString(EXTRA_CONFIRM_BUTTON, confirmButton)
                putString(EXTRA_CANCEL_BUTTON, cancelButton)
                putString(EXTRA_ERROR, error)
            }
            dialog.arguments = args
            return dialog
        }

    }

    // Use this instance of the interface to deliver action events.
    internal lateinit var mListener: CryptocurrencyAmountDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface CryptocurrencyAmountDialogListener {
        fun onCryptocurrencyAmountDialogConfirmButtonClick(cryptocurrencyAmountDialog: CryptocurrencyAmountDialog)
        fun onCryptocurrencyAmountDialogCancel()
    }

    private lateinit var editTextAmount: EditText
    private var valueAmount: Double = 0.0


    // Override the Fragment.onAttach() method to instantiate the CryptocurrencyAmountDialogListener.
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface.
        try {
            // Instantiate the CryptocurrencyAmountDialogListener so we can send events to the host.
            mListener = context as CryptocurrencyAmountDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception.
            throw ClassCastException((context.toString() +
                    " must implement CryptocurrencyAmountDialogListener."))
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = arguments?.getString(EXTRA_TITLE)
        val hint = arguments?.getString(EXTRA_HINT)
        val confirmButton = arguments?.getString(EXTRA_CONFIRM_BUTTON)
        val cancelButton = arguments?.getString(EXTRA_CANCEL_BUTTON)
        val error = arguments?.getString(EXTRA_ERROR) ?: ""

        val dialog = activity?.let {
            val builder = AlertDialog.Builder(it)

            // Set a title for alert dialog.
            builder.setTitle(title)

            // Pass null as the parent view because its going in the dialog layout.
            val dialogView = it.layoutInflater.inflate(R.layout.dialog_add_crypto_amount, null)

            // Set hint for edit text.
            dialogView.edit_text_amount.hint = hint

            // Set the layout for the dialog.
            builder.setView(dialogView)

            builder.setCancelable(true)

            // Set the alert dialog positive/ok button.
            builder.setPositiveButton(confirmButton) { _, _ -> } // We will setup listener later.

            // Set the alert dialog neutral/cancel button.
            builder.setNeutralButton(cancelButton) { _, _ ->
                // Send the neutral button event back to the host activity.
                mListener.onCryptocurrencyAmountDialogCancel()
            }

            editTextAmount = dialogView.edit_text_amount

            // Initialize the AlertDialog using builder object.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null!")

        // Show keyboard for amount input when dialog is created.
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        dialog.setOnShowListener {
            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            editTextAmount.nonEmpty(
                    { buttonPositive.isEnabled = false },
                    { buttonPositive.isEnabled = true })

            buttonPositive.setOnClickListener {
                if (onValidateAndConfirm(error)) {

                    // Send the positive button event back to the host activity.
                    mListener.onCryptocurrencyAmountDialogConfirmButtonClick(this)
                }
            }
        }

        return dialog
    }

    // DialogFragment own the Dialog.setOnCancelListener callback. You must not set it yourself.
    // To find out about this event, we need to override onCancel.
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        mListener.onCryptocurrencyAmountDialogCancel()
    }

    // We check if user entered amount number is valid before confirmation actions.
    private fun onValidateAndConfirm(errorMsg: String): Boolean {
        return editTextAmount.validate({ text ->
            try {
                valueAmount = text.toDouble()
                true
            } catch (e: Throwable) {
                false
            }
        }, errorMsg)
    }

    // Get the amount which user entered.
    fun getAmount(): Double {
        return valueAmount
    }

}