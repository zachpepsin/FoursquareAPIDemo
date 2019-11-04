package com.zachpepsin.foursquareapidemo

import android.app.Dialog
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.dialog_location.view.*
import java.util.*


class LocationDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    private lateinit var listener: SelectionListener

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface SelectionListener {
        fun onDialogPositiveClick(locationText: String)
        fun onDialogNegativeClick(dialog: DialogFragment)
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

                        // Get the address, city, and state
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        /**
                         * TODO Foursquare API is very particular about having valid address numbers
                         * Unfortunately, the google lcoation callback is not and just gives it's
                         * best estimate at a number.  To avoid failed
                         * requests, just use city, state and zip.
                         *
                         * It would be better to use `ll` instead of `near` in the query and pass in
                         * the Lat and Long returned to get the most accurate results
                         */
                        //val number = addresses[0].subThoroughfare
                        //val street = addresses[0].thoroughfare
                        val cityName = addresses[0].locality
                        val stateName = addresses[0].adminArea
                        val postalCode = addresses[0].postalCode

                        // Build the string and set the EditText
                        var locationString = ""
                        //if(number != null ) locationString += number
                        //if(street != null ) locationString += " $street"
                        if (cityName != null) locationString += cityName
                        if (stateName != null) locationString += ", $stateName"
                        if (postalCode != null) locationString += " $postalCode"
                        view.edit_text_location.setText(locationString)
                        Toast.makeText(context, locationString, Toast.LENGTH_SHORT).show()
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

            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                .setMessage(R.string.set_location)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // Send the positive button event back to the host activity
                    listener.onDialogPositiveClick(view.edit_text_location.text.toString())
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