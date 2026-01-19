package com.example.projekt

import com.google.gson.annotations.SerializedName

data class GeocodingResponse(
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: List<GeocodingResult>,
    @SerializedName("error_message") val errorMessage: String?
)

data class GeocodingResult(
    @SerializedName("geometry") val geometry: Geometry
)

data class Geometry(
    @SerializedName("location") val location: LocationCoord
)

data class LocationCoord(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)
