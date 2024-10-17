package com.drivesolutions.safedrive.data.models

data class Route(
    val origin: String,
    val destination: String,
    val trafficCount: Int,
    val coordinates: List<String>,
    val createdAt: String,
    val updatedAt: String
)
