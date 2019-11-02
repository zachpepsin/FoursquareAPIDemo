package com.zachpepsin.foursquareapidemo

import android.content.Context
import android.content.Intent
import android.net.*
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
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
    private var isSearchPerformed = false
    private var encodedSearchText: String = ""

    // Number of items before the bottom we have to reach when scrolling to start loading next page
    private val visibleThreshold = 2

    // Number of venues to load per page (max of 50 per Foursquare API docs)
    private val itemsPerPageLoad = 50

    private var pagesLoaded = 1

    var dataset = Venues()

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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
                // This has to be run on the UI thread because the callback is on a different thread
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
                // User selected the location option.  Display the location dialog
                val locationDialogFragment = LocationDialogFragment()
                locationDialogFragment.show(supportFragmentManager, "location_dialog_fragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the LocationDialogFragment.SelectionListener interface
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User touched the dialog's positive button
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    // Runs an API request
    private fun run(url: String) {
        val urlWithKeys = "$url&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&v=$API_VERSION"
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
                if (!isSearchPerformed) {
                    // We are not performing a search, just loading a page of venues
                    LoadVenues().execute(responseData)
                } else {
                    // We did perform a search
                    // TODO Perform Search
                    //LoadVenuesSearch().execute(responseData)
                }

                // Run view-related code back on the main thread
                runOnUiThread {
                    // Hide the main progress bar
                    progress_bar_venues_center.visibility = View.GONE
                }
            }
        })
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter =
            VenueItemRecyclerViewAdapter(this, dataset.items, twoPane)

        progress_bar_venues_center.visibility = View.VISIBLE  // Display the main progress bar

        pagesLoaded = 1
        dataset.items.clear()

        // Add divider for recycler
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            (recyclerView.layoutManager as LinearLayoutManager).orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Execute HTTP Request to load first batch of venues
        run("https://api.foursquare.com/v2/venues/search?page=$pagesLoaded&per_page=$itemsPerPageLoad")

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

                    if (!isSearchPerformed) {
                        run("https://api.foursquare.com/v2/venues/search?page=$pagesLoaded&per_page=$itemsPerPageLoad")
                    } else {
                        // If we have less search results than however many we tried to load by now,
                        // Then we are at the end of the list of results
                        run("https://api.foursquare.com/v2/venues/search?query=$encodedSearchText&page=$pagesLoaded&per_page=$itemsPerPageLoad")
                    }
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
            if (response.isEmpty()) {
                // We did not get a response
                Log.e(ItemDetailActivity::class.java.simpleName, "No response")
            }
            val venueJSONArray =
                JSONObject(response).getJSONObject("response").getJSONArray("venues")

            for (i in 0 until venueJSONArray.length()) {
                val jsonVenue = venueJSONArray.getJSONObject(i)

                // Get the address out of the location object if there is one
                var address = String()
                if (!jsonVenue.isNull("location") && !jsonVenue.getJSONObject("location").isNull("address")) {
                    address = jsonVenue.getJSONObject("location").getString("address")
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

            // Get the range of items added to notify the dataset how many items were added
            val firstItemAdded = (pagesLoaded - 1) * itemsPerPageLoad
            val lastItemAdded = ((pagesLoaded) * itemsPerPageLoad) - 1

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
        const val API_VERSION =
            "20191101&near=Philadelphia%2C%20PA" //TODO set up location instead of using here
    }
}
