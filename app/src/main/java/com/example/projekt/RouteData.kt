package com.example.projekt

import com.google.android.gms.maps.model.LatLng

data class RouteData(
    val id: String = "",
    val name: String = "",
    val points: List<MyLatLng> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val distanceKm: Double = 0.0
)

data class MyLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

fun LatLng.toMyLatLng() = MyLatLng(latitude, longitude)
fun MyLatLng.toLatLng() = LatLng(latitude, longitude)
