package com.zachpepsin.foursquareapidemo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*
import okhttp3.OkHttpClient

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListActivity]
 * in two-pane mode (on tablets) or a [ItemDetailActivity]
 * on handsets.
 */
class ItemDetailFragment : Fragment() {

    // The venue title this fragment is presenting.
    private var item: Venues.VenueItem? = null

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
        item?.let {
            rootView.item_detail.text = it.name
        }

        return rootView
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
