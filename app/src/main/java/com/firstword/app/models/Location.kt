package com.firstword.app.models

data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "",
    val country: String = "",
    val address: String = ""
)