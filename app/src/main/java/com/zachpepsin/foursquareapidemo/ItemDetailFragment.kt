package com.zachpepsin.foursquareapidemo

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*
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
                activity?.toolbar_layout?.title = venueName
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
        val rootView = inflater.inflate(R.layout.item_detail, container, false)

        // Show the content as text in a TextView.
        rootView.item_detail.text = venueName

        return rootView
    }

    // Runs an API request
    private fun run(url: String) {
        val urlWithKeys =
            "$url&client_id=${ItemListActivity.CLIENT_ID}&client_secret=${ItemListActivity.CLIENT_SECRET}&v=${ItemListActivity.API_VERSION}"
        val request = Request.Builder()
            .url(urlWithKeys)
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


    inner class LoadVenueDetails : AsyncTask<String, Void, JSONObject>() {

        override fun doInBackground(vararg params: String): JSONObject? {
            val response = params[0]
            if (response.isEmpty()) {
                // We did not get a response
                Log.e(ItemDetailActivity::class.java.simpleName, "No response")
            }

            return JSONObject(response).getJSONObject("response")
        }

        override fun onPostExecute(result: JSONObject) {
            super.onPostExecute(result)

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
