package com.zachpepsin.foursquareapidemo

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

    fun addItem(id: String, name: String) {
        val item = VenueItem(id, name)
        items.add(item)
        itemMap[item.id] = item
    }

    /**
     * Data class
     */
    data class VenueItem(
        val id: String,
        val name: String
    ) {
        override fun toString(): String = name
    }
}