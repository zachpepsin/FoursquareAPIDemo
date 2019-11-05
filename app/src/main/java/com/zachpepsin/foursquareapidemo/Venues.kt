package com.zachpepsin.foursquareapidemo

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import java.util.*

class Venues {
    /**
     * An array of items
     */
    val items: MutableList<VenueItem> = ArrayList()

    /**
     * A map of items, by ID
     */
    private val itemMap: MutableMap<String, VenueItem> = HashMap()

    fun addItem(id: String, name: String, address: String, categoryIcon: Bitmap) {
        val item = VenueItem(id, name, address, categoryIcon)
        items.add(item)
        itemMap[item.id] = item
    }

    /**
     * Data class
     */
    data class VenueItem(
        val id: String,
        val name: String,
        val address: String,
        val categoryIcon: Bitmap
    ) {
        override fun toString(): String = name
    }
}