package com.trobatapp.utils

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * asin(sqrt(a))
    }
}
