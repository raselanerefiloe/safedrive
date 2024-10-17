package com.drivesolutions.safedrive.data.sources.remote

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.drivesolutions.safedrive.data.models.GeoCoordinate
import com.drivesolutions.safedrive.data.models.Location
import com.drivesolutions.safedrive.data.models.Report
import com.drivesolutions.safedrive.data.models.ReportType
import com.drivesolutions.safedrive.data.models.Status
import com.drivesolutions.safedrive.data.models.UserStatus
import com.drivesolutions.safedrive.utils.sendNotification
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Document
import io.appwrite.models.DocumentList
import io.appwrite.models.Session
import io.appwrite.models.User
import io.appwrite.services.Account
import io.appwrite.services.Avatars
import io.appwrite.services.Databases
import io.appwrite.services.Realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


object Appwrite {
    // Client and services instances
    lateinit var client: Client
    lateinit var account: Account
    lateinit var database: Databases
    private lateinit var avatars: Avatars
    private lateinit var realtime: Realtime

    // Configuration for database and collection IDs
    const val projectId = "670a14330016e2617725"
    const val databaseId = "670a7a3b00055e6835be" // Replace with your Appwrite database ID
    const val usersCollectionId = "670a7a5a0002061acaf2" // Replace with your Users collection ID
    const val routesCollectionId = "670a7c240002e9ce1a41" // Replace with your Routes collection ID
    const val roadReportsCollectionId = "670b593b000d80b148a8"
    const val locationsCollectionId = "670a810200320328f7ef" // Replace with your Locations collection ID

    fun init(context: Context) {
        // Initialize the Appwrite client
        client = Client(context)
            .setEndpoint("https://cloud.appwrite.io/v1") // Replace with your Appwrite endpoint
            .setProject("670a14330016e2617725") // Replace with your Appwrite project ID

        // Initialize the Account and Database services
        account = Account(client)
        avatars = Avatars(client);
        database = Databases(client)
        realtime = Realtime(client)
    }

    // Function to check if the user is logged in
    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Attempt to fetch the current user account
            account.get() // This is a suspending function
            true // If successful, the user is logged in
        } catch (e: AppwriteException) {
            // Handle the exception for a logged-out user
            false
        } catch (e: Exception) {
            // Handle other unexpected exceptions
            false
        }
    }


    suspend fun onLogin(
        email: String,
        password: String,
    ): Session {
        // Handle user login with email and password
        return account.createEmailPasswordSession(
            email,
            password,
        )
    }

    suspend fun onRegister(
        name: String,
        email: String,
        password: String,
    ): User<Map<String, Any>> {
        // Handle user registration with email and password
        return account.create(
            userId = ID.unique(),
            email,
            password,
            name,
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createUser(fullName: String, email: String, password: String): Document<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                // Create the account
                val newAccount = account.create(
                    userId = ID.unique(),
                    email = email,
                    password = password,
                    name = fullName
                )

                // Check if account creation was successful

                // Manually generate avatar URL based on the fullName
                val encodedFullName = URLEncoder.encode(fullName, "UTF-8")
                val avatarUrl = "https://cloud.appwrite.io/v1/avatars/initials?name=$encodedFullName&project=$projectId"
                println("Avatar URL: $avatarUrl")

                // Check if avatarUrl is a valid URL string
                if (avatarUrl.isEmpty() || avatarUrl.length > 2000) {
                    throw IllegalArgumentException("Invalid avatar URL format.")
                }
                // Sign in the user
                //onLogin(email, password)

                // Create a new user document in the database
                val newUser = database.createDocument(
                    databaseId = Appwrite.databaseId,
                    collectionId = Appwrite.usersCollectionId,
                    documentId = ID.unique(),
                    data = mapOf(
                        "accountId" to newAccount.id,
                        "email" to email,
                        "avatar" to avatarUrl,
                        "names" to fullName
                    )
                )

                return@withContext newUser
            } catch (error: Exception) {
                println(error.message)
                throw error // Rethrow the error
            }
        }
    }

    suspend fun onLogout() {
        // Handle user logout for the current session
        account.deleteSession("current")
    }

    suspend fun fetchAllLocations(): List<Location> = withContext(Dispatchers.IO) {
        try {
            // Logging to indicate the fetching process has started
            println("Fetching all locations from the database...")

            // Fetching all documents in the locations collection
            val locationsResponse: DocumentList<Map<String, Any>> = database.listDocuments(
                databaseId = databaseId,
                collectionId = locationsCollectionId
            )

            // Log the total number of documents fetched
            println("Total locations fetched: ${locationsResponse.total}")

            // Check if documents are empty
            if (locationsResponse.documents.isEmpty()) {
                println("No locations found in the collection.")
            } else {
                println("Documents found: ${locationsResponse.documents.size}")
            }

            // Process and convert the document data into Location objects
            return@withContext locationsResponse.documents.map { doc ->
                println("Processing document: ${doc.id} with data: ${doc.data}")
                Location(
                    documentId = doc.id,
                    name = doc.data["name"] as String,
                    coordinates = doc.data["coordinates"] as String,
                    physicalAddress = doc.data["physicalAddress"] as String,
                    createdAt = doc.data["createdAt"] as String,
                    updatedAt = doc.data["updatedAt"] as String
                )
            }
        } catch (e: AppwriteException) {
            println("Error fetching locations: ${e.message}")
            return@withContext emptyList() // Return empty list on error
        } catch (e: Exception) {
            println("An unexpected error occurred: ${e.message}")
            return@withContext emptyList() // Return empty list on unexpected errors
        }
    }



    // Function to get the currently logged-in user
    suspend fun getCurrentUser(): User<Map<String, Any>>? = withContext(Dispatchers.IO) {
        return@withContext try {
            // Attempt to fetch the current user account
            account.get() // This is already a suspending function
        } catch (e: AppwriteException) {
            println("Error fetching current user: ${e.message}")
            null // Return null if there's an error (e.g., user not logged in)
        } catch (e: Exception) {
            println("An unexpected error occurred: ${e.message}")
            null // Return null on unexpected errors
        }
    }
    // Function to fetch user details from userCollection
    suspend fun getUserDetails(): com.drivesolutions.safedrive.data.models.User? {
        return try {
            // Get the current user's account ID
            val currentUser = getCurrentUser() ?: return null
            val accountId = currentUser.id // This is the account ID from the current user

            // Fetch the user document from the user collection using the account ID
            val userDocuments = database.listDocuments(
                databaseId,
                usersCollectionId,
                queries = listOf(Query.equal("accountId", accountId))
            )

            // Get the first document if available
            val userDocument = userDocuments.documents.firstOrNull() ?: return null
            Log.d("User Details", "Fetched user $userDocument")
            // Map the fetched document to the User data class
            val user = com.drivesolutions.safedrive.data.models.User(
                accountId = userDocument.id,
                names = userDocument.data["names"] as String,
                email = userDocument.data["email"] as String,
                avatar = userDocument.data["avatar"] as String?,
                Created = userDocument.data["\$createdAt"] as String,
                Updated = userDocument.data["\$updatedAt"] as String,
                status = UserStatus.valueOf(userDocument.data["status"] as String)
            )
            Log.d("User Details", "Mapped user $user")
            user
        } catch (e: AppwriteException) {
            println("Error fetching user details: ${e.message}")
            null // Return null if there's an error
        } catch (e: Exception) {
            println("An unexpected error occurred: ${e.message}")
            null // Return null on unexpected errors
        }
    }


    // Function to fetch the route with the least traffic, or create a new route if it doesn't exist
    suspend fun fetchRouteWithLeastTraffic(
        origin: GeoPoint,
        destination: GeoPoint,
        fastestRoute: List<GeoPoint>
    ): List<GeoPoint> = withContext(Dispatchers.IO) {
        try {
            // Query the routes collection to get all available routes between origin and destination
            val routesResponse: DocumentList<Map<String, Any>> = database.listDocuments(
                databaseId = databaseId,
                collectionId = routesCollectionId,
                queries = listOf(
                    Query.equal("origin", "${origin.latitude},${origin.longitude}"),
                    Query.equal("destination", "${destination.latitude},${destination.longitude}")
                )
            )

            // Check if routes were found in the database
            if (routesResponse.documents.isEmpty()) {
                // If no routes are found, create a new route in the database
                println("No existing route found in the database. Creating a new route entry.")

                // Prepare the coordinates for the fastest route as an array of strings
                val routeCoordinates = fastestRoute.map { "${it.latitude},${it.longitude}" }
                println("Route coordinates: $routeCoordinates")
                // Create a new route document with the fastest route as the default option
                val newRoute = database.createDocument(
                    databaseId = databaseId,
                    collectionId = routesCollectionId,
                    documentId = ID.unique(),
                    data = mapOf(
                        "origin" to "${origin.latitude},${origin.longitude}",
                        "destination" to "${destination.latitude},${destination.longitude}",
                        "trafficCount" to 0, // Initialize traffic count to 0 for a new route
                        "coordinates" to routeCoordinates
                    )
                )

                println("New route created with ID: ${newRoute.id}")

                // Return the fastest route as it was created in the database
                return@withContext fastestRoute
            } else {
                // Process the routes to find the one with the least traffic
                var optimalRoute: List<GeoPoint> = fastestRoute
                var lowestTrafficCount = Int.MAX_VALUE

                for (routeDocument in routesResponse.documents) {
                    val trafficCount = routeDocument.data["trafficCount"] as? Int ?: Int.MAX_VALUE
                    val routeCoordinates = parseRouteCoordinates(routeDocument.data["coordinates"] as List<String>)

                    // If the fastest route has less than 10 cars, select it by default
                    if (optimalRoute == fastestRoute && trafficCount < 10) {
                        return@withContext fastestRoute
                    }

                    // Otherwise, choose the route with the lowest number of cars
                    if (trafficCount < lowestTrafficCount) {
                        lowestTrafficCount = trafficCount
                        optimalRoute = routeCoordinates
                    }
                }

                return@withContext optimalRoute
            }
        } catch (e: AppwriteException) {
            println("Error fetching or creating route: ${e.message}")
            return@withContext fastestRoute // Return the fastest route in case of an error
        }
    }

    // Helper function to parse route coordinates from a list of strings
    private fun parseRouteCoordinates(coordinates: List<String>): List<GeoPoint> {
        return coordinates.map { coordString ->
            val latLong = coordString.split(",")
            val latitude = latLong[0].toDouble()
            val longitude = latLong[1].toDouble()
            GeoPoint(latitude, longitude)
        }
    }


    /**
     * Listen to changes in the road_reports collection and trigger alerts for nearby users.
     */
    fun listenToRoadReports(context: Context) {
        Log.d("Realtime Listener", "Setting up subscription for road reports...")

        val subscription = realtime.subscribe("databases.$databaseId.collections.$roadReportsCollectionId.documents") { event ->
            val payloadData = event.payload.toString()
            Log.d("Realtime Event", "Event received payLoadData: $payloadData")

            try {
                // Check if the payload indicates a deletion
                if (payloadData.contains("\"\$deleted\"")) {
                    Log.d("Realtime Event", "Report was deleted; no notification will be sent.")
                    return@subscribe // Exit early if the report was deleted
                }
                // Remove the curly braces
                val trimmedPayloadData = payloadData.trim().removePrefix("{").removeSuffix("}")
                // Split the string into key-value pairs
                val keyValuePairs = trimmedPayloadData.split(", ")

                // Initialize variables to hold extracted values
                var reportType = "Unknown"
                var location = "0.0,0.0"
                var description = ""
                var status = "PENDING"
                var userId = "Unknown User"

                // Loop through the key-value pairs and extract relevant values
                for (pair in keyValuePairs) {
                    val keyValue = pair.split("=", limit = 2)
                    if (keyValue.size == 2) {
                        val key = keyValue[0].trim()
                        val value = keyValue[1].trim()

                        when (key) {
                            "report_type" -> reportType = value
                            "location" -> location = value
                            "description" -> description = value
                            "status" -> status = value
                            "user_id" -> userId = value
                        }
                    } else {
                        Log.w("Realtime Event", "Unrecognized format for pair: $pair")
                    }
                }

                // Parse location string to extract latitude and longitude
                val locationParts = location.split(",")
                val latitude = locationParts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val longitude = locationParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0

                // Send notification
                sendNotification(context, reportType, description, location)

            } catch (e: Exception) {
                Log.e("Realtime Event", "Failed to parse event payload: ${e.message}")
            }
        }

        Log.d("Realtime Listener", "Subscription setup completed")
    }


    suspend fun createRoadReport(reportType: String, description: String, currentLocation: android.location.Location?) {
        try {
            // Prepare the data to be saved
            val data = mutableMapOf<String, Any?>(
                "report_type" to reportType,
                "description" to description,
                "status" to "ACTIVE",
                "user_id" to getCurrentUser()?.id
            )

            // Add location to the data if available
            if (currentLocation != null) {
                data["location"] = "${currentLocation.latitude},${currentLocation.longitude}"
            }

            // Create the new road report document using the specified approach
            val result = database.createDocument(
                databaseId, // Your database ID
                roadReportsCollectionId, // Road reports collection ID
                ID.unique(), // Generate a unique ID
                data = data
            )

            // Handle success
            Log.d("Road Report","Road report created successfully: ${result.data}")
        } catch (e: AppwriteException) {
            // Handle any errors that might occur during the creation process
            Log.d("Road Report","Failed to create road report: ${e.message}")
        }
    }

    suspend fun getReports(): List<Report> {
        return try {
            val documentList = database.listDocuments(databaseId, roadReportsCollectionId)
            documentList.documents.map { document ->
                Log.d("Reports", "Fetched report $document")
                // Get the location string from the document
                val locationString = document.data["location"] as String
                // Split the string to extract latitude and longitude
                val (latitude, longitude) = locationString.split(",").map { it.trim().toDouble() }
                Log.d("Reports", "Fetched report location $locationString")
                Report(
                    reportType = ReportType.valueOf(document.data["report_type"] as String),
                    location = GeoCoordinate(
                        latitude = latitude,
                        longitude = longitude
                    ),
                    description = document.data["description"] as String,
                    userId = document.data["userId"] as? String ?: "unknown",
                    status = Status.valueOf(document.data["status"] as String)
                )
            }
        } catch (e: AppwriteException) {
            Log.d("Reports Error", "Error $e")
            e.printStackTrace()
            emptyList() // Return an empty list on error
        }
    }

}

