package com.zachpepsin.foursquareapidemo

import android.app.Dialog
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.dialog_location.view.*
import java.util.*


class LocationDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    private lateinit var listener: SelectionListener

    private var isLatLong = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface SelectionListener {
        fun onDialogPositiveClick(locationText: String, isLatLong: Boolean)
    }

    // Override the Fragment.onAttach() method to instantiate the LocationDialogFragment
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the SelectionListener so we can send events to the host
            listener = context as SelectionListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                (context.toString() +
                        " must implement SelectionListener")
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_location, null)

            val context = activity!!.applicationContext
            // Set up location client
            fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)


            // Hide the location button if location permissions are not granted
            if (arguments == null || !arguments!!.getBoolean("isLocationGranted")) {
                view.image_button_location.isEnabled = false
                view.image_button_location.visibility = View.GONE
            }

            view.image_button_location.setOnClickListener {
                // Location button was clicked
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location == null) {
                            // We did not get a location
                            Toast.makeText(context, "NULL LOCATION", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        // Build the string and set the EditText
                        val locationString = "${location.latitude}, ${location.longitude}"
                        view.edit_text_location.setText(locationString)
                        isLatLong = true
                    }
                    .addOnFailureListener {
                        // We were not able to get location (possibly due to permissions)
                        Toast.makeText(
                            context,
                            getString(R.string.location_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            view.edit_text_location.addTextChangedListener {
                // If the EditText is manually changed then we are not using LatLong
                // This is in case someone hits the location button, then changes the input after
                isLatLong = false
            }

            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                .setMessage(R.string.set_location)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // Send the positive button event back to the host activity
                    listener.onDialogPositiveClick(view.edit_text_location.text.toString(), isLatLong)
                }
                .setCancelable(true)
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}