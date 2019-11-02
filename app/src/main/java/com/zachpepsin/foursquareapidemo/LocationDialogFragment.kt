package com.zachpepsin.foursquareapidemo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class LocationDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    internal lateinit var listener: SelectionListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface SelectionListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    // Override the Fragment.onAttach() method to instantiate the LocationDialogFragment
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as SelectionListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                (context.toString() +
                        " must implement NoticeDialogListener")
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.set_location)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // Send the positive button event back to the host activity
                    listener.onDialogPositiveClick(this)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    // Send the negative button event back to the host activity
                    listener.onDialogNegativeClick(this)
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}