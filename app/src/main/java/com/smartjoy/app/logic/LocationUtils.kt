package com.smartjoy.app.logic

import android.location.Location
import com.smartjoy.app.model.PathGraph
import com.smartjoy.app.model.PathNode
import com.smartjoy.app.model.ZooLocation

object LocationUtils {
    /**
     * Calculates distance between two ZooLocations in meters using Android's distanceBetween.
     */
    fun calculateDistance(start: ZooLocation, end: ZooLocation): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
    }

    /**
     * Calculates distance between a ZooLocation and raw coordinates.
     */
    fun calculateDistance(loc: ZooLocation, lat: Double, lon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, lat, lon, results)
        return results[0]
    }

    fun findNearestNode(graph: PathGraph, location: ZooLocation): PathNode {
        return graph.nodes.minBy { calculateDistance(it.location, location) }
    }
}
