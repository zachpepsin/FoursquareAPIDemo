package com.zachpepsin.foursquareapidemo

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListActivity]
 * in two-pane mode (on tablets) or a [ItemDetailActivity]
 * on handsets.
 */
class ItemDetailFragment : Fragment() {

    private var venueId: String? = null
    private var venueName: String? = null

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID) && !it.getString(ARG_ITEM_ID).isNullOrEmpty()) {
                // Load the venue title specified by the fragment arguments
                venueId = it.getString(ARG_ITEM_ID)
                venueName = it.getString(ARG_VENUE_NAME)
            } else {
                // A venue name was not passed into the fragment
                Log.w(ItemDetailActivity::class.java.simpleName, "Venue ID not supplied")
                onDestroy()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.item_detail, container, false)
    }

    override fun onResume() {
        super.onResume()
        // Set the header to the venue name
        activity?.toolbar_layout?.title = venueName

        // Execute HTTP Request to load venue details
        run()
    }

    // Runs an API request
    private fun run() {
        val url =
            "https://api.foursquare.com/v2/venues/$venueId?client_id=${ItemListActivity.CLIENT_ID}&client_secret=${ItemListActivity.CLIENT_SECRET}&v=${ItemListActivity.API_VERSION}"
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("Request Failed", e.message!!)
            }

            override fun onResponse(call: Call?, response: Response) {
                val responseData = response.body()?.string()
                LoadVenueDetails().execute(responseData)
            }
        })
    }


    inner class LoadVenueDetails : AsyncTask<String, Void, JSONObject?>() {

        override fun doInBackground(vararg params: String): JSONObject? {
            val response = params[0]
            if (response.isEmpty()) {
                // We did not get a response
                Log.e(ItemDetailActivity::class.java.simpleName, "No response")
                return null
            }

            // If we don't have a response JSON the request failed
            if (JSONObject(response).isNull("response"))
                return null

            return JSONObject(response).getJSONObject("response")
        }

        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)
            if (result == null) {
                // We did not get a response
                Log.e(ItemDetailActivity::class.java.simpleName, "No response")
                return
            }

            // Populate the views on the page by extracting data from the JSON response
            val venueJSON = result.getJSONObject("venue")

            // Location info
            val locationJSON = venueJSON.getJSONObject("location")
            if (!locationJSON.isNull("address")
                && !locationJSON.isNull("city")
                && !locationJSON.isNull("state")
            ) {
                text_address_street.text = locationJSON.getString("address")
                val cityStateString =
                    "${locationJSON.getString("city")}, ${locationJSON.getString("state")}"
                text_address_city_state.text = cityStateString
            }

            // Retrieve the photo to be used in the header
            val bestPhotoJSON = venueJSON.getJSONObject("bestPhoto")
            val prefix = bestPhotoJSON.getString("prefix")
            val suffix = bestPhotoJSON.getString("suffix")
            val imgUrl = "${prefix}500x300${suffix}"

            Picasso.with(context!!).load(imgUrl).into(object :Target {
                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                    Log.d("HIYA", "onBitmapLoaded")
                    //val d = BitmapDrawable(resources, bitmap)
                    //detail_toolbar.background = d
                }

                override fun onBitmapFailed(errorDrawable: Drawable) {
                    Log.d("HIYA", "onBitmapFailed")
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable) {
                    Log.d("HIYA", "onPrepareLoad")
                }
            })

            // Run activity view-related code back on the main thread
            activity?.runOnUiThread {
                // Hide the progress bar
                progress_bar_venue_details.visibility = View.GONE
            }
        }
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "id"
        const val ARG_VENUE_NAME = "name"
    }
}
