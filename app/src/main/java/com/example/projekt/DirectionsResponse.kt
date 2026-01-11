package com.example.projekt

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("routes") val routes: List<Route>,
    @SerializedName("error_message") val errorMessage: String?
)

data class Route(
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline,
    @SerializedName("legs") val legs: List<Leg>
)

data class OverviewPolyline(
    @SerializedName("points") val points: String
)

data class Leg(
    @SerializedName("distance") val distance: Distance
)

data class Distance(
    @SerializedName("value") val value: Int
)
