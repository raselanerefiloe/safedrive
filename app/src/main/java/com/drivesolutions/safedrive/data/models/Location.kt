package com.drivesolutions.safedrive.data.models

data class Location(
    val documentId: String,
    val name: String,              // Human-readable name of the location
    val coordinates: String,       // Coordinates stored as a string (e.g., "latitude,longitude")
    val physicalAddress: String,   // The physical address of the location
    val createdAt: String,         // Timestamp of when the location was created, stored as a String
    val updatedAt: String          // Timestamp of when the location was last updated, stored as a String
)

