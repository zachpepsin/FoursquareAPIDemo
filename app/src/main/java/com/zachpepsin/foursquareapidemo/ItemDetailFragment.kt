package com.zachpepsin.foursquareapidemo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

    // Simulates an API request to avoid being rate limited
    private fun runTest() {
        val responseData =
            "{\"meta\":{\"code\":200,\"requestId\":\"5dc0b119bcbf7a0039d5a13b\"},\"response\":{\"venue\":{\"id\":\"4f29e8e3e4b02f0aff55b2a7\",\"name\":\"City Hall Courtyard\",\"contact\":{},\"location\":{\"address\":\"1 Penn Sq\",\"lat\":39.95248416722534,\"lng\":-75.16359241655339,\"labeledLatLngs\":[{\"label\":\"display\",\"lat\":39.95248416722534,\"lng\":-75.16359241655339}],\"postalCode\":\"19102\",\"cc\":\"US\",\"state\":\"Pennsylvania\",\"country\":\"United States\",\"formattedAddress\":[\"1 Penn Sq\",\"PA 19102\",\"United States\"]},\"canonicalUrl\":\"https://foursquare.com/v/city-hall-courtyard/4f29e8e3e4b02f0aff55b2a7\",\"categories\":[{\"id\":\"4bf58dd8d48988d164941735\",\"name\":\"Plaza\",\"pluralName\":\"Plazas\",\"shortName\":\"Plaza\",\"icon\":{\"prefix\":\"https://ss3.4sqi.net/img/categories_v2/parks_outdoors/plaza_\",\"suffix\":\".png\"},\"primary\":true}],\"verified\":false,\"stats\":{\"tipCount\":3},\"likes\":{\"count\":54,\"groups\":[{\"type\":\"others\",\"count\":54,\"items\":[]}],\"summary\":\"54 Likes\"},\"dislike\":false,\"ok\":false,\"rating\":8.9,\"ratingColor\":\"73CF42\",\"ratingSignals\":61,\"beenHere\":{\"count\":0,\"unconfirmedCount\":0,\"marked\":false,\"lastCheckinExpiredAt\":0},\"specials\":{\"count\":0,\"items\":[]},\"photos\":{\"count\":202,\"groups\":[{\"type\":\"venue\",\"name\":\"Venue photos\",\"count\":202,\"items\":[{\"id\":\"507b2152e4b0801689af9323\",\"createdAt\":1350246738,\"source\":{\"name\":\"Foursquare for Android\",\"url\":\"https://foursquare.com/download/#/android\"},\"prefix\":\"https://fastly.4sqi.net/img/general/\",\"suffix\":\"/20728224_jSoaZVGRg-QWlJBdnRYmhjW0G_MgNNXiDQjt4pzc03w.jpg\",\"width\":405,\"height\":720,\"user\":{\"id\":\"20728224\",\"firstName\":\"Brian\",\"lastName\":\"Smith\",\"gender\":\"male\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/E0HLM0J1LRKZHTYM.jpg\"}},\"visibility\":\"public\"}]}]},\"reasons\":{\"count\":1,\"items\":[{\"summary\":\"Lots of people like this place\",\"type\":\"general\",\"reasonName\":\"rawLikesReason\"}]},\"hereNow\":{\"count\":0,\"summary\":\"Nobody here\",\"groups\":[]},\"createdAt\":1328146659,\"tips\":{\"count\":3,\"groups\":[{\"type\":\"others\",\"name\":\"All tips\",\"count\":3,\"items\":[{\"id\":\"516de617498ebc58f01ca748\",\"createdAt\":1366156823,\"text\":\"Beautiful old stone building. A real shame what the city and it's politicians have done to it\",\"type\":\"user\",\"canonicalUrl\":\"https://foursquare.com/item/516de617498ebc58f01ca748\",\"likes\":{\"count\":1,\"groups\":[{\"type\":\"others\",\"count\":1,\"items\":[{\"id\":\"322687\",\"firstName\":\"Cam\",\"gender\":\"male\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/322687_80fkwdYa_tLXWuLl2dhPd0RkEhnqnZ1Pnh8TkiZFIL69w2Lw4swQgJ_4wYVAN9pNl4VYVlXD2\"}}]}],\"summary\":\"1 like\"},\"logView\":true,\"agreeCount\":1,\"disagreeCount\":2,\"todo\":{\"count\":0},\"user\":{\"id\":\"36510758\",\"firstName\":\"Dave\",\"lastName\":\"McDevitt\",\"gender\":\"male\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/QPHRHOKXXBBYFZMK.jpg\"}}}]}]},\"shortUrl\":\"http://4sq.com/ypx5SJ\",\"timeZone\":\"America/New_York\",\"listed\":{\"count\":21,\"groups\":[{\"type\":\"others\",\"name\":\"Lists from other people\",\"count\":21,\"items\":[{\"id\":\"514e065de4b066e1f6b4b633\",\"name\":\"Philadelphia\",\"description\":\"Cool places around Philadelphia.\",\"type\":\"others\",\"user\":{\"id\":\"40077564\",\"firstName\":\"Luke\",\"lastName\":\"C\",\"gender\":\"none\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/40077564_t4yOQr0O_V4sqKMQB1APskB70Hs0DB2CUijthHQ6BeanXgVOIz_zQ9zItPsimCTb8TjkVkzf4.jpg\"}},\"editable\":false,\"public\":true,\"collaborative\":false,\"url\":\"/user/40077564/list/philadelphia\",\"canonicalUrl\":\"https://foursquare.com/user/40077564/list/philadelphia\",\"createdAt\":1364067933,\"updatedAt\":1438724989,\"photo\":{\"id\":\"4f493f0be4b0a644edad05ff\",\"createdAt\":1330200331,\"prefix\":\"https://fastly.4sqi.net/img/general/\",\"suffix\":\"/ZfOi0dH4AiC2biSBwBxNfghL5uIO9oMbaPwS8m8zFKI.jpg\",\"width\":720,\"height\":540,\"user\":{\"id\":\"11869015\",\"firstName\":\"Brian\",\"lastName\":\"Hamilton\",\"gender\":\"male\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/IDWXE3MMXRONSCSF.jpg\"}},\"visibility\":\"public\"},\"followers\":{\"count\":2},\"listItems\":{\"count\":125,\"items\":[{\"id\":\"v4f29e8e3e4b02f0aff55b2a7\",\"createdAt\":1382885990}]}},{\"id\":\"5b3a4ea7f62f2b002c0d9454\",\"name\":\"Phili + DC\",\"description\":\"\",\"type\":\"others\",\"user\":{\"id\":\"453146105\",\"firstName\":\"Vasek\",\"lastName\":\"Vasik\",\"gender\":\"male\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/453146105_z1pSW3y7_IKUua4LmBr_FnyCl1fCdEiuLAhdasmYwuq4waaNaFZP5400tHPX5r16jVHSFBvYW.jpg\"}},\"editable\":false,\"public\":true,\"collaborative\":false,\"url\":\"/user/453146105/list/phili--dc\",\"canonicalUrl\":\"https://foursquare.com/user/453146105/list/phili--dc\",\"createdAt\":1530547879,\"updatedAt\":1532518682,\"photo\":{\"id\":\"518d4f43498e8af2c1fede76\",\"createdAt\":1368215363,\"prefix\":\"https://fastly.4sqi.net/img/general/\",\"suffix\":\"/32917095_K9AvbZDWGirmW8Jtz5Y7yQc3HNhPTOcNLZ3vkaY12uQ.jpg\",\"width\":1440,\"height\":1440,\"user\":{\"id\":\"32917095\",\"firstName\":\"Ashley\",\"lastName\":\"Nicole\",\"gender\":\"female\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/1XVRTZWLOSLCGDOB.jpg\"}},\"visibility\":\"public\"},\"followers\":{\"count\":0},\"listItems\":{\"count\":18,\"items\":[{\"id\":\"v4f29e8e3e4b02f0aff55b2a7\",\"createdAt\":1532518682}]}},{\"id\":\"591324ebb040562b025aa344\",\"name\":\"ФИЛАДЕЛЬФИЯ\",\"description\":\"\",\"type\":\"others\",\"user\":{\"id\":\"51253717\",\"firstName\":\"Andrey\",\"gender\":\"male\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/51253717_g2gsePFy_E59TOxPhbmGRlt1fqmTMQ5gQSnC8a7RGQpD-w97KkGDtA-VlUMGPv6dA1pHEnZTA.jpg\"}},\"editable\":false,\"public\":true,\"collaborative\":false,\"url\":\"/user/51253717/list/%D1%84%D0%B8%D0%BB%D0%B0%D0%B4%D0%B5%D0%BB%D1%8C%D1%84%D0%B8%D1%8F\",\"canonicalUrl\":\"https://foursquare.com/user/51253717/list/%D1%84%D0%B8%D0%BB%D0%B0%D0%B4%D0%B5%D0%BB%D1%8C%D1%84%D0%B8%D1%8F\",\"createdAt\":1494426859,\"updatedAt\":1495289904,\"photo\":{\"id\":\"5187f9c4498e300c59055237\",\"createdAt\":1367865796,\"prefix\":\"https://fastly.4sqi.net/img/general/\",\"suffix\":\"/2790218_QW64qx9kYXSFufaXQtp0GDmhfNODj-G3YiudHJ9fTd0.jpg\",\"width\":612,\"height\":612,\"user\":{\"id\":\"2790218\",\"firstName\":\"Arnaud\",\"lastName\":\"C\",\"gender\":\"male\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/14OBRIVJZ20HDSPF.jpg\"}},\"visibility\":\"public\"},\"followers\":{\"count\":1},\"listItems\":{\"count\":140,\"items\":[{\"id\":\"v4f29e8e3e4b02f0aff55b2a7\",\"createdAt\":1494447556}]}},{\"id\":\"580f9d14d67c39c49bb5390b\",\"name\":\"US\",\"description\":\"\",\"type\":\"others\",\"user\":{\"id\":\"42709964\",\"firstName\":\"clarekelsio\",\"gender\":\"none\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/42709964-EM3F31CGWMQEDWOD.jpg\"}},\"editable\":false,\"public\":true,\"collaborative\":false,\"url\":\"/user/42709964/list/us\",\"canonicalUrl\":\"https://foursquare.com/user/42709964/list/us\",\"createdAt\":1477418260,\"updatedAt\":1477466719,\"photo\":{\"id\":\"51c1a7f2498e1c8d7e953afa\",\"createdAt\":1371645938,\"prefix\":\"https://fastly.4sqi.net/img/general/\",\"suffix\":\"/14735285_d-88q3ImavgP0AnG7xBh-bI7EpbdBjb1pROJEwAB22U.jpg\",\"width\":612,\"height\":612,\"user\":{\"id\":\"14735285\",\"firstName\":\"Lorena\",\"lastName\":\"Fernandez\",\"gender\":\"female\",\"photo\":{\"prefix\":\"https://fastly.4sqi.net/img/user/\",\"suffix\":\"/23BKFFOVVPNOJUUV.jpg\"}},\"visibility\":\"public\"},\"followers\":{\"count\":0},\"listItems\":{\"count\":72,\"items\":[{\"id\":\"v4f29e8e3e4b02f0aff55b2a7\",\"createdAt\":1477422517}]}}]}]},\"pageUpdates\":{\"count\":0,\"items\":[]},\"inbox\":{\"count\":0,\"items\":[]},\"parent\":{\"id\":\"4a689350f964a52095ca1fe3\",\"name\":\"Philadelphia City Hall\",\"location\":{\"address\":\"1601 Walnut Street\",\"crossStreet\":\"at Market St\",\"lat\":39.95007196,\"lng\":-75.1676195,\"labeledLatLngs\":[{\"label\":\"display\",\"lat\":39.95007196,\"lng\":-75.1676195}],\"postalCode\":\"19102\",\"cc\":\"US\",\"neighborhood\":\"Center City West\",\"city\":\"Philadelphia\",\"state\":\"PA\",\"country\":\"United States\",\"formattedAddress\":[\"1601 Walnut Street (at Market St)\",\"Philadelphia, PA 19102\",\"United States\"]},\"categories\":[{\"id\":\"4bf58dd8d48988d129941735\",\"name\":\"City Hall\",\"pluralName\":\"City Halls\",\"shortName\":\"City Hall\",\"icon\":{\"prefix\":\"https://ss3.4sqi.net/img/categories_v2/building/cityhall_\",\"suffix\":\".png\"},\"primary\":true}],\"venuePage\":{\"id\":\"556354549\"}},\"hierarchy\":[{\"name\":\"Philadelphia City Hall\",\"lang\":\"en\",\"id\":\"4a689350f964a52095ca1fe3\",\"canonicalUrl\":\"https://foursquare.com/v/philadelphia-city-hall/4a689350f964a52095ca1fe3\"}],\"attributes\":{\"groups\":[]},\"bestPhoto\":{\"id\":\"507b2152e4b0801689af9323\",\"createdAt\":1350246738,\"source\":{\"name\":\"Foursquare for Android\",\"url\":\"https://foursquare.com/download/#/android\"},\"prefix\":\"https://fastly.4sqi.net/img/general/\",\"suffix\":\"/20728224_jSoaZVGRg-QWlJBdnRYmhjW0G_MgNNXiDQjt4pzc03w.jpg\",\"width\":405,\"height\":720,\"visibility\":\"public\"},\"colors\":{\"highlightColor\":{\"photoId\":\"507b2152e4b0801689af9323\",\"value\":-12036000},\"highlightTextColor\":{\"photoId\":\"507b2152e4b0801689af9323\",\"value\":-1},\"algoVersion\":3}}}}"
        LoadVenueDetails().execute(responseData)
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

            // Get the name.  This shouldn't be different than what is passed into the activity, but
            // just in case it is, and also to simplify unit testing, update the title here as well
            venueName = venueJSON.getString("name")
            activity?.toolbar_layout?.title = venueName  // Set the header to the venue name

            // Photo to be used in the header
            if(!venueJSON.isNull("bestPhoto")) {
                val bestPhotoJSON = venueJSON.getJSONObject("bestPhoto")
                val prefix = bestPhotoJSON.getString("prefix")
                val suffix = bestPhotoJSON.getString("suffix")
                val imgUrl = "${prefix}500x300${suffix}"
                Picasso.with(activity).load(imgUrl).placeholder(android.R.drawable.picture_frame)
                    .into(object : Target {
                        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                            // Load cover photo into CollapsingToolbarLayout of parent activity
                            val d = BitmapDrawable(resources, bitmap)
                            activity!!.toolbar_layout.background = d
                        }

                        override fun onBitmapFailed(errorDrawable: Drawable) {}
                        override fun onPrepareLoad(placeHolderDrawable: Drawable) {}
                    })
            }

            // Address
            val locationJSON = venueJSON.getJSONObject("location")
            var addressText = ""
            // If we have a formatted address, use that.  Otherwise, build an address string
            if (!locationJSON.isNull("formattedAddress") && locationJSON.getJSONArray("formattedAddress").length() > 0) {
                for (i in 0 until locationJSON.getJSONArray("formattedAddress").length()) {
                    addressText += locationJSON.getJSONArray("formattedAddress").getString(i)
                    if (i < locationJSON.getJSONArray("formattedAddress").length() - 1) {
                        addressText += "\n"  // Append a new line unless it is the last line
                    }
                }
            } else {
                if (!locationJSON.isNull("address")) {
                    addressText += locationJSON.getString("address")
                }
                if (!locationJSON.isNull("city")) {
                    addressText += "\n${locationJSON.getString("city")}"
                    if (!locationJSON.isNull("city")) {
                        addressText += ", ${locationJSON.getString("state")}"
                    }
                } else if (!locationJSON.isNull("state")) {
                    addressText += "\n${locationJSON.getString("state")}"
                }
            }
            text_address_body.text = addressText

            // Set FAB of activity to open maps for the location
            activity!!.fab.setOnClickListener {
                // Launch map to lat and long
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    val latitude = locationJSON.getDouble("lat")
                    val longitude = locationJSON.getDouble("lng")
                    data = Uri.parse("geo:$latitude,$longitude?z=23") // z=23 is max zoom
                }
                if (intent.resolveActivity(activity!!.packageManager) != null) {
                    startActivity(intent)
                }
            }

            // Hours
            if (venueJSON.isNull("hours") || venueJSON.getJSONObject("hours").isNull("status")) {
                // No hours information provided
                text_hours_body.text = getString(R.string.no_hours)
            } else {
                // Use the "status" parameter to simply have a line that descripes the open status
                text_hours_body.text = venueJSON.getJSONObject("hours").getString("status")

                // Make the text green or red depending on if it is open or not
                if (venueJSON.getJSONObject("hours").getBoolean("isOpen")) {
                    text_hours_body.setTextColor(
                        ContextCompat.getColor(
                            context!!,
                            android.R.color.holo_green_dark
                        )
                    )
                } else {
                    text_hours_body.setTextColor(
                        ContextCompat.getColor(
                            context!!,
                            android.R.color.holo_red_dark
                        )
                    )
                }
            }

            // Rating
            if (!venueJSON.isNull("rating")) {
                rating_venue.rating = venueJSON.getDouble("rating").toFloat()
            }

            // Description
            if (venueJSON.isNull("description")) {
                // No description provided
                text_description_body.text = getString(R.string.no_description)
            } else {
                text_description_body.text = venueJSON.getString("description")
            }

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
