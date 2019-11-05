package com.zachpepsin.foursquareapidemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.item_list.*
import kotlinx.android.synthetic.main.item_list_content.view.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import kotlin.math.min

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListActivity : AppCompatActivity(), LocationDialogFragment.SelectionListener {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    private var isPageLoading = false // If a new page of items is currently being loaded
    private var encodedSearchText: String = "" // By default do not use search keywords
    private var encodedLocation = "Philadelphia%2C%20PA" // Default search location is Philadelphia
    private var isLatLong = false // True if we are searching by LatLong rather than keywords

    private val PERMISSION_LOCATION = 100

    // Number of items before the bottom we have to reach when scrolling to start loading next page
    private val visibleThreshold = 2

    // Number of venues to load per page (max of 50 per Foursquare API docs)
    private val itemsPerPageLoad = 50

    private var pagesLoaded = 0

    var dataset = Venues()

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        // Until we have internet confirmed, do not allow searches to be performed
        fab.setOnClickListener { view ->
            Snackbar.make(
                view,
                getString(R.string.snackbar_internet_required),
                Snackbar.LENGTH_LONG
            ).show()
        }

        if (item_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        // Use ConnectivityManager to check if we are connected to the
        // internet, and if so, what type of connection is in place
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        // This callback will be used if we don't have an initial connection and need to detect
        // when a network connection is established
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(
                    ItemDetailActivity::class.java.simpleName,
                    "Network connection now available"
                )

                // We are now connected and don't need this callback anymore
                // Note: If we were to check for disconnects and re-connects after initial
                // connection, we would keep this registered and use onLost
                connectivityManager.unregisterNetworkCallback(this)

                // We now have a network connection and can load the data
                // This has to be run on the UI
                //        fab.setOnClickListener { view ->
                //            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //                .setAction("Action", null).show()
                //        }thread because the callback is on a different thread
                runOnUiThread {
                    // Hide the 'no connection' message and re-display the recycler
                    // Reset the text of it back to the 'no items found' message in case the view is
                    // used again in the case that no venues are returned
                    text_venues_recycler_empty.visibility = View.GONE
                    text_venues_recycler_empty.text =
                        getString(R.string.text_venues_recycler_empty)

                    setupRecyclerView(recycler_venues)
                }
            }
        }

        /**
         * For SDK 22+, we can use the new getNetworkCapabilities method from the ConnectionManager
         * For SDK <22, we have to use the deprecated activeNetworkInfo method, because its
         * replacement is only available on SDK22+
         */
        var networkAvailable = false
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT > 22) {
            // For devices with API >= 23
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            // NET_CAPABILITY_VALIDATED - Indicates that connectivity on this network was successfully validated.
            // NET_CAPABILITY_INTERNET - Indicates that this network should be able to reach the internet.
            if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {

                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    networkAvailable = true
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    networkAvailable = true
                }
            }
        } else {
            // For devices with API < 23
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            val isConnected: Boolean = activeNetwork?.isConnected == true
            if (isConnected) {
                networkAvailable = true
            }
        }

        if (networkAvailable) {
            // We have a network connection
            Log.d(
                ItemDetailActivity::class.java.simpleName,
                "Network connection available"
            )
            // Proceed to set up recycler and load data
            setupRecyclerView(recycler_venues)
        } else {
            // We do not have a network connection
            Log.d(
                ItemDetailActivity::class.java.simpleName,
                "Network connection not available"
            )

            // Display a 'no connection' message
            recycler_venues.visibility = View.GONE
            text_venues_recycler_empty.text = getString(R.string.text_no_network_connection)
            text_venues_recycler_empty.visibility = View.VISIBLE

            // Register a network callback so if we do get a network connection, we can proceed
            val builder: NetworkRequest.Builder = NetworkRequest.Builder()
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_item_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_location -> {
                // User selected the location option.
                // Check for location permissions
                requestLocationPermission()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                val builder = AlertDialog.Builder(this)
                builder.setMessage(R.string.dialog_permission_location)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            PERMISSION_LOCATION
                        )
                    }
                builder.create().show() // Show dialog
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_LOCATION
                )
            }
        } else {
            // We already have location permissions
            // Launch the LocationDialogFragment
            val locationDialogFragment = LocationDialogFragment()
            val args = Bundle()
            args.putBoolean("isLocationGranted", true)
            locationDialogFragment.arguments = args
            locationDialogFragment.show(supportFragmentManager, "location_dialog_fragment")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                // Let the LocationDialogFragment know if permissions are granted
                val permissionGranted =
                    ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                val locationDialogFragment = LocationDialogFragment()
                val args = Bundle()
                args.putBoolean("isLocationGranted", permissionGranted)
                locationDialogFragment.arguments = args
                locationDialogFragment.show(supportFragmentManager, "location_dialog_fragment")
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the LocationDialogFragment.SelectionListener interface
    override fun onDialogPositiveClick(locationText: String, latLong: Boolean) {
        // User touched the dialog's positive button

        if (locationText.isBlank()) {
            // No location was entered
            return
        }
        isLatLong = latLong // Keep track of if we are searching using Lat/Long
        pagesLoaded = 0 // Reset number of pages because this is a new search

        // Clear the dataset and recycler
        val numItems = dataset.items.size
        dataset.items.clear()
        recycler_venues.adapter?.notifyItemRangeRemoved(0, numItems)

        // Encode the location text for the request
        encodedLocation = URLEncoder.encode(locationText, "UTF-8")

        // Perform the request
        runRequest()
    }

    // Runs an API request
    private fun runRequest() {
        var url =
            "https://api.foursquare.com/v2/venues/search?page=${pagesLoaded + 1}&per_page=$itemsPerPageLoad&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&v=$API_VERSION&query=$encodedSearchText"

        // Use either ll for lat/long or near for location keywords
        url += when (isLatLong) {
            true -> "&ll=$encodedLocation"
            false -> "&near=$encodedLocation"
        }
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
                // Load a page of venues with the response data
                LoadVenues().execute(responseData)

                // Run view-related code back on the main thread
                runOnUiThread {
                    // Hide the main progress bar
                    progress_bar_venues_center.visibility = View.GONE
                }
            }
        })
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        // Now that we have internet connection, also set up the FAB for searches
        setupSearchFAB()

        recyclerView.adapter =
            VenueItemRecyclerViewAdapter(this, dataset.items, twoPane)

        progress_bar_venues_center.visibility = View.VISIBLE  // Display the main progress bar

        pagesLoaded = 0
        dataset.items.clear()
        isLatLong = false

        // Add divider for recycler
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            (recyclerView.layoutManager as LinearLayoutManager).orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Execute HTTP Request to load first batch of venues
        runRequest()

        // Add scroll listener to detect when the end of the list has been reached
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // If we are within the threshold of the bottom of the list, and we are not
                // already loading a new page of items, then load the next page of items
                if (!isPageLoading
                    && totalItemCount <= (lastVisibleItem + visibleThreshold)
                ) {
                    // Load the next page of venues
                    isPageLoading = true
                    progress_bar_venues_page.visibility = View.VISIBLE

                    // Iterate the pages loaded counter so we load the next page
                    pagesLoaded++

                    runRequest()
                }

                // Hide FAB when scrolling down, show when scrolling up
                if (dy > 0) {
                    fab.hide()
                } else {
                    fab.show()
                }
            }
        })
    }

    private fun setupSearchFAB() {
        fab.setOnClickListener {
            // Show a dialog that allows the user to enter a search query
            // Show dialog to enter search keywords
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_header_venue_search))

            val dialogView = layoutInflater.inflate(R.layout.dialog_venue_search, null)

            val categoryEditText = dialogView.findViewById(R.id.venue_edit_text) as EditText

            builder.setView(dialogView)
                // Set up the search button
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    encodedSearchText = URLEncoder.encode(categoryEditText.text.toString(), "UTF-8")
                    if (encodedSearchText.isEmpty()) {
                        // Don't do anything if no text was submitted
                        dialog.dismiss()
                    }
                    pagesLoaded = 0 // New search, so reset number of pages loaded
                    // Clear the recycler and prepare for a new list to populate it
                    val itemCount = dataset.items.size
                    dataset.items.clear()
                    recycler_venues.adapter?.notifyItemRangeRemoved(0, itemCount)
                    text_venues_recycler_empty.visibility = View.GONE

                    runRequest()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    // Cancel button
                    dialog.cancel()
                }
                .setNeutralButton(getString(R.string.reset)) { _, _ ->
                    // Reset button.  Perform non-search query
                    setupRecyclerView(recycler_venues)
                }
            // Create the alert dialog using builder
            val dialog: AlertDialog = builder.create()
            // Display the dialog
            dialog.show()
        }
    }

    class VenueItemRecyclerViewAdapter(
        private val parentActivity: ItemListActivity,
        private val values: List<Venues.VenueItem>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<VenueItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Venues.VenueItem
                if (twoPane) {
                    val fragment = ItemDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(ItemDetailFragment.ARG_ITEM_ID, item.id)
                            putString(ItemDetailFragment.ARG_VENUE_NAME, item.name)
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.item_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
                        putExtra(ItemDetailFragment.ARG_VENUE_NAME, item.name)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.textName.text = item.name
            holder.textName.text = item.name

            if (item.address.isNotBlank())
                holder.textAddress.text = item.address
            else {
                holder.textAddress.text = parentActivity.getString(R.string.no_address)
            }

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textName: TextView = view.text_name
            val textAddress: TextView = view.text_address
        }
    }

    inner class LoadVenues : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {
            val response = params[0]
            if (response.isEmpty() || JSONObject(response).isNull("response") || JSONObject(response).getJSONObject(
                    "response"
                ).isNull("venues")
            ) {
                // We did not get a response
                Log.e(ItemDetailActivity::class.java.simpleName, "No response")
                return null
            }
            val venueJSONArray =
                JSONObject(response).getJSONObject("response").getJSONArray("venues")

            for (i in 0 until venueJSONArray.length()) {
                val jsonVenue = venueJSONArray.getJSONObject(i)

                // Get the address out of the location object if there is one
                var address = String()
                if (!jsonVenue.isNull("location") && !jsonVenue.getJSONObject("location").isNull("address")) {
                    address = jsonVenue.getJSONObject("location").getString("address")

                    // Also add distance
                    if (!jsonVenue.getJSONObject("location").isNull("distance")) {
                        address += ", ${jsonVenue.getJSONObject("location").getString("distance")} ${getString(
                            R.string.meters
                        )}"
                    }
                }

                dataset.addItem(
                    jsonVenue.getString("id"),
                    jsonVenue.getString("name"),
                    address
                )
            }
            return "temp"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result == null) {
                // Request failed
                Toast.makeText(
                    this@ItemListActivity,
                    getString(R.string.search_failed),
                    Toast.LENGTH_SHORT
                ).show()
                dataset.items.clear()
                recycler_venues.adapter?.notifyDataSetChanged()
                pagesLoaded = 0
                isPageLoading = false // We are done loading the page
                progress_bar_venues_page.visibility = View.GONE
                return
            }

            // Get the range of items added to notify the dataset how many items were added
            val firstItemAdded = (pagesLoaded) * itemsPerPageLoad
            val lastItemAdded = min(((pagesLoaded + 1) * itemsPerPageLoad), dataset.items.size) - 1

            // Check to make sure we still have this view, since the activity could be destroyed
            if (recycler_venues != null) {
                recycler_venues.adapter?.notifyItemRangeInserted(
                    firstItemAdded,
                    lastItemAdded
                )
                progress_bar_venues_page.visibility = View.INVISIBLE
            }

            if (dataset.items.size <= 0) {
                // No venues to display in list, show an empty list message
                recycler_venues.visibility = View.GONE
                text_venues_recycler_empty.visibility = View.VISIBLE
            } else if (recycler_venues.visibility == View.GONE) {
                // If the recycler is hidden and we have items to display, make it visible
                recycler_venues.visibility = View.VISIBLE
                text_venues_recycler_empty.visibility = View.GONE
            }

            isPageLoading = false // We are done loading the page
        }
    }

    companion object {
        const val CLIENT_ID = "OOYBRQQXO2OTWQFHTUSQTX1PLXKRL0L3JW20M1ZQDT0QMFMM"
        const val CLIENT_SECRET = "E0ME4WKBZQCM34DX4MVB2AHWFK1GKW2AUR1OC13YYHPV5CQU"
        const val API_VERSION = "20191101"
    }
}
