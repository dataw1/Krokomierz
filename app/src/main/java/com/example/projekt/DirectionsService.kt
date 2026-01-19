package com.example.projekt

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DirectionsService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Header("X-Android-Package") packageName: String,
        @Header("X-Android-Cert") certFingerprint: String,
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String?,
        @Query("mode") mode: String = "walking",
        @Query("key") apiKey: String
    ): DirectionsResponse

    @GET("maps/api/geocode/json")
    suspend fun geocode(
        @Header("X-Android-Package") packageName: String,
        @Header("X-Android-Cert") certFingerprint: String,
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
}
