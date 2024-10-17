package com.drivesolutions.safedrive.data.models

// Define the UserStatus enum with three possible values
enum class UserStatus {
    PENDING,
    ACTIVE,
    BLOCKED
}

data class User(
    val accountId: String,          // Unique identifier for the user's account
    val names: String,              // User's full names
    val email: String,              // User's email address
    val avatar: String?,            // URL to the user's avatar, nullable if no avatar is set
    val Created: String,          // Timestamp of when the user was created, stored as a String
    val Updated: String,          // Timestamp of when the user was last updated, stored as a String
    val status: UserStatus          // User's current status
)
