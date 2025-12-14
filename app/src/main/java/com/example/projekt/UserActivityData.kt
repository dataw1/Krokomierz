package com.example.projekt

import com.google.firebase.database.ServerValue

data class UserActivityData(
    val name: String = "",
    val steps: Int = 0,
    val distance: Double = 0.0,
    val calories: Float = 0.0f,
    val timestamp: Any = ServerValue.TIMESTAMP
)
