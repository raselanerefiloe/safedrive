package com.drivesolutions.safedrive.data.models

data class Report(
    val reportType: ReportType,
    val location: GeoCoordinate,
    val description: String,
    val userId: String,
    val status: Status
)
enum class ReportType {
    ACCIDENT,
    CONSTRUCTION,
    HEAVY_TRAFFIC,
    ROAD_BLOCK
}

enum class Status {
    ACTIVE,
    RESOLVED,
    PENDING,
}

