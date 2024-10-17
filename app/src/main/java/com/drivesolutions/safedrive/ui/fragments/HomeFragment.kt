package com.drivesolutions.safedrive.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.drivesolutions.safedrive.NotificationActivity
import com.drivesolutions.safedrive.R
import com.drivesolutions.safedrive.data.sources.remote.Appwrite
import com.drivesolutions.safedrive.ui.dialogs.ReportDialogFragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.drivesolutions.safedrive.data.models.Location as LocationModel

class HomeFragment : Fragment() {

    private lateinit var map: MapView
    private lateinit var originInput: EditText
    private lateinit var destinationInput: EditText
    private lateinit var originSuggestions: ListView
    private lateinit var destinationSuggestions: ListView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorMessage: TextView

    private var currentLocation: LatLng? = null
    private var locations = listOf<LocationModel>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var routeCoordinates: List<GeoPoint>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // OSMDroid configuration for the application
        Configuration.getInstance().load(requireContext(), requireActivity().getPreferences(Context.MODE_PRIVATE))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        // Initialize views
        map = view.findViewById(R.id.map)
        originInput = view.findViewById(R.id.origin_input)
        destinationInput = view.findViewById(R.id.destination_input)
        originSuggestions = view.findViewById(R.id.origin_suggestions)
        destinationSuggestions = view.findViewById(R.id.destination_suggestions)
        loadingIndicator = view.findViewById(R.id.loading_indicator)
        errorMessage = view.findViewById(R.id.error_message)

        // Setup MapView
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Fused Location Provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Get Locations
        fetchLocations()

        // Request Location Permission and update map immediately if granted
        requestLocationPermission()

        // Make the origin input non-editable
        originInput.isFocusable = false
        originInput.isFocusableInTouchMode = false
        originInput.setOnClickListener {
            Toast.makeText(requireContext(), "Origin is set to current location", Toast.LENGTH_SHORT).show()
        }

        // Setup Listeners
        setupListeners()
    }

    private fun setupListeners() {
//        originInput.setOnClickListener {
//            // Show suggestions based on input
//            showOriginSuggestions()
//        }

        destinationInput.setOnClickListener {
            // Show suggestions based on input
            showDestinationSuggestions()
        }

        view?.findViewById<Button>(R.id.get_directions_button)?.setOnClickListener {
            getDirections()
        }
    }
    // Inflate the action bar menu
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)

        // Find the notification action view and set the count dynamically
        val notificationMenuItem = menu.findItem(R.id.action_notifications)
        val actionView = notificationMenuItem.actionView
        val badgeTextView = actionView?.findViewById<TextView>(R.id.notification_badge_count)
        val alertItem = menu.findItem(R.id.action_alert)
        val alertIcon = alertItem.icon
        DrawableCompat.setTint(alertIcon!!, ContextCompat.getColor(requireContext(), R.color.white))

        val notificationItem = menu.findItem(R.id.action_notifications)
        val notificationIcon = notificationItem.icon
        DrawableCompat.setTint(notificationIcon!!, ContextCompat.getColor(requireContext(), R.color.white))
        // Update the notification badge count (this should be updated dynamically based on your data)
        if (badgeTextView != null) {
            updateNotificationBadge(badgeTextView, 5)
        } // Example count
    }

    // Handle the menu item clicks
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                // Handle notification action (e.g., open a list of notifications)
                val intent = Intent(requireContext(), NotificationActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_alert -> {
                // Handle sending an alert about road condition or accident
                sendAlertNotification()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // Function to update the notification badge count
    private fun updateNotificationBadge(badgeTextView: TextView, count: Int) {
        if (count > 0) {
            badgeTextView.text = count.toString()
            badgeTextView.visibility = View.VISIBLE
        } else {
            badgeTextView.visibility = View.GONE
        }
    }

    // Function to send an alert notification about road conditions or accidents
    private fun sendAlertNotification() {
        val dialog = ReportDialogFragment()
        dialog.show(parentFragmentManager, "ReportDialogFragment")
    }


    private fun fetchLocations() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch all locations from Appwrite
                locations = Appwrite.fetchAllLocations() // Ensure this method returns a List<Location>
                Log.d("Locations", "Fetched All $locations")
                withContext(Dispatchers.Main) {
                    if (locations.isNotEmpty()) {
                        Toast.makeText(requireContext(), "Locations loaded", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage.text = "Failed to load locations: ${e.message}"
                    errorMessage.visibility = View.VISIBLE
                    Log.d("Locations", "Error Fetching Locations ${e.message}")
                }
            }
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            // Permissions are already granted, get the last known location
            getLastKnownLocation()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getLastKnownLocation() {
        // Fetch the last known location
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    // Update current location and map
                    currentLocation = LatLng(it.latitude, it.longitude)
                    updateMapLocation(currentLocation)
                    // Set origin input to the current location coordinates
                    originInput.setText("${it.latitude}, ${it.longitude}") // Display current location
                } ?: run {
                    // Handle case where location is null (not available)
                    errorMessage.text = "Unable to retrieve current location."
                    errorMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateMapLocation(location: LatLng?) {
        location?.let {
            // Move the map to the user's current location
            val mapController = map.controller
            mapController.setZoom(15.0) // Set zoom level
            mapController.setCenter(org.osmdroid.util.GeoPoint(it.latitude, it.longitude))

            // Clear existing markers, if any
            map.overlays.clear()

            // Add a new marker to the map
            val marker = Marker(map).apply {
                position = org.osmdroid.util.GeoPoint(it.latitude, it.longitude)
                title = "Your Location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Center the marker correctly
            }
            map.overlays.add(marker) // Add the marker to the map
            map.invalidate() // Refresh the map to show the new marker
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setIntervalMillis(10000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .build()

        // Initialize the LocationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)

                    // Check if destination is set and redraw the polyline
                    if (!destinationInput.text.isNullOrEmpty() && routeCoordinates != null) {
                        updateMapWithRoute(currentLocation!!, routeCoordinates!!)
                    }else{
                        updateMapLocation(currentLocation)
                    }
                }
            }
        }

        // Start location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, null)
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback!!) // Stop location updates
        map.onPause() // Handle map pause
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates() // Start location updates
        map.onResume() // Handle map resume
    }

    override fun onDestroy() {
        super.onDestroy()
        routeCoordinates = null
        map.onDetach() // Handle map detach
    }

//    private fun showOriginSuggestions() {
//        val input = originInput.text.toString()
//        val filteredLocations = locations.filter { it.name.contains(input, ignoreCase = true) }
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, filteredLocations.map { it.name })
//        originSuggestions.adapter = adapter
//        originSuggestions.visibility = if (filteredLocations.isNotEmpty()) View.VISIBLE else View.GONE
//        originSuggestions.setOnItemClickListener { parent, _, position, _ ->
//            originInput.setText(filteredLocations[position].coordinates)
//            originSuggestions.visibility = View.GONE
//        }
//    }

    private fun showDestinationSuggestions() {
        val input = destinationInput.text.toString()
        val filteredLocations = locations.filter { it.name.contains(input, ignoreCase = true) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, filteredLocations.map { it.name })
        destinationSuggestions.adapter = adapter
        destinationSuggestions.visibility = if (filteredLocations.isNotEmpty()) View.VISIBLE else View.GONE
        destinationSuggestions.setOnItemClickListener { parent, _, position, _ ->
            destinationInput.setText(filteredLocations[position].coordinates)
            destinationSuggestions.visibility = View.GONE
        }
    }



    private fun getDirections() {
        val originCoordinates = originInput.text.toString().trim().replace(" ", "")
        val destinationCoordinates = destinationInput.text.toString().trim().replace(" ", "")

        Log.d("Location", "Original Origin Coordinates: $originCoordinates")
        Log.d("Location", "Original Destination Coordinates: $destinationCoordinates")

        // Swap the coordinates from {latitude,longitude} to {longitude,latitude}
        val formattedOrigin = swapLatLonToLonLat(originCoordinates)
        val formattedDestination = swapLatLonToLonLat(destinationCoordinates)

        Log.d("Location", "Formatted Origin Coordinates: $formattedOrigin")
        Log.d("Location", "Formatted Destination Coordinates: $formattedDestination")

        // Ensure both coordinates are not empty and properly formatted
        if (formattedOrigin.isEmpty() || formattedDestination.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both origin and destination.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if both coordinates are valid before making the request
        if (!isValidCoordinateFormat(formattedOrigin) || !isValidCoordinateFormat(formattedDestination)) {
            Toast.makeText(requireContext(), "Invalid coordinate format. Please use {longitude},{latitude}.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val encodedOrigin = URLEncoder.encode(formattedOrigin, "UTF-8")
                val encodedDestination = URLEncoder.encode(formattedDestination, "UTF-8")
                val osrmUrl = "https://router.project-osrm.org/route/v1/driving/$encodedOrigin;$encodedDestination?overview=full&geometries=geojson"
                Log.d("OSRM URL", "Constructed URL: $osrmUrl")

                val url = URL(osrmUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }

                connection.connect()
                val responseCode = connection.responseCode
                Log.d("HTTP Response", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStream = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("OSRM Response", "Response Body: $responseStream")

                    withContext(Dispatchers.Main) {
                        parseAndDisplayDirections(responseStream)
                    }
                } else {
                    Log.e("HTTP Error", "Failed to fetch directions: HTTP $responseCode")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to get directions. HTTP error code: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("General Error", "Unexpected error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun swapLatLonToLonLat(coordinate: String): String {
        val parts = coordinate.split(",")
        return if (parts.size == 2) {
            "${parts[1]},${parts[0]}" // Swaps latitude and longitude to longitude,latitude
        } else {
            "" // Returns an empty string if the coordinate format is invalid
        }
    }

    private fun isValidCoordinateFormat(coordinate: String): Boolean {
        val parts = coordinate.split(",")
        if (parts.size != 2) return false

        val longitude = parts[0].toDoubleOrNull()
        val latitude = parts[1].toDoubleOrNull()

        // Check that latitude and longitude fall within valid ranges
        return longitude != null && longitude in -180.0..180.0 && latitude != null && latitude in -90.0..90.0
    }


    // Parses the JSON response from OSRM and draws the polyline on the map
    private fun parseAndDisplayDirections(response: String) {
        try {
            val jsonResponse = JSONObject(response)
            val routesArray = jsonResponse.getJSONArray("routes")
            if (routesArray.length() > 0) {
                val route = routesArray.getJSONObject(0)
                val geometry = route.getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")

                // Parse the coordinates into GeoPoints for the map
                val geoPoints = mutableListOf<GeoPoint>()
                for (i in 0 until coordinates.length()) {
                    val point = coordinates.getJSONArray(i)
                    val longitude = point.getDouble(0)
                    val latitude = point.getDouble(1)
                    geoPoints.add(GeoPoint(latitude, longitude))
                }

                // Store the route coordinates
                routeCoordinates = geoPoints

                // Display the route on the map using the GeoPoints
               startLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "No routes found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error parsing directions: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // Draws the parsed polyline on the OSMDroid map
    private fun updateMapWithRoute(currentLocation: LatLng, geoPoints: List<GeoPoint>) {
        // Clear existing overlays if any
        map.overlays.clear()

        // Create a Polyline object and set its properties
        val roadOverlay = Polyline(map).apply {
            setPoints(geoPoints)
            color = android.graphics.Color.BLUE // Set the color of the polyline
            width = 8.0f // Set the width of the polyline
        }
        // Add a new marker to the map
        val marker = Marker(map).apply {
            position = org.osmdroid.util.GeoPoint(currentLocation.latitude, currentLocation.longitude)
            title = "Your Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Center the marker correctly
        }
        map.overlays.add(marker) // Add the marker to the map
        // Add the Polyline to the map overlays
        map.overlays.add(roadOverlay)

        // Adjust the map view to fit the polyline bounds
        if (geoPoints.isNotEmpty()) {
            map.controller.setCenter(geoPoints[0]) // Center map to the starting point of the route
            map.controller.setZoom(16.0) // Set an appropriate zoom level
        }

        // Refresh the map to display the new polyline
        map.invalidate()
    }



}
