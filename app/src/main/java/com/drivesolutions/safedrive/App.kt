package com.drivesolutions.safedrive

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.drivesolutions.safedrive.data.sources.remote.Appwrite
import com.drivesolutions.safedrive.ui.dialogs.ReportDialogFragment
import com.drivesolutions.safedrive.ui.fragments.ProfileFragment
import com.drivesolutions.safedrive.ui.fragments.HomeFragment
import com.drivesolutions.safedrive.ui.fragments.ObjectDetectionFragment
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppActivity : AppCompatActivity(), ReportDialogFragment.ReportDialogListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    private val speedLimit = 80f // Speed limit in km/h
    private val speedLimitInMetersPerSecond = speedLimit / 3.6f // Convert km/h to m/s

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up location request
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Create the location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location != null) {
                        currentLocation = location
                        checkSpeed(location.speed) // Check the speed of the driver
                    }
                }
            }
        }

        // Create notification channel
        createNotificationChannel()

        // Check if user is logged in
        CoroutineScope(Dispatchers.Main).launch {
            if (!Appwrite.isLoggedIn()) {
                val intent = Intent(this@AppActivity, SignInActivity::class.java)
                startActivity(intent)
                finish()
                return@launch
            } else {
                startLocationUpdates() // Start receiving location updates

                // Set the initial fragment to Home
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                }

                // Set up bottom navigation
                findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnNavigationItemSelectedListener { menuItem ->
                    var selectedFragment: Fragment? = null
                    when (menuItem.itemId) {
                        R.id.nav_home -> selectedFragment = HomeFragment()
                        R.id.nav_object_detection -> selectedFragment = ObjectDetectionFragment()
                        R.id.nav_profile -> selectedFragment = ProfileFragment()
                    }
                    if (selectedFragment != null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit()
                    }
                    true
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun checkSpeed(speed: Float) {
        // Check if the speed exceeds the limit
        if (speed > speedLimitInMetersPerSecond) {
            sendSpeedWarning(speed)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSpeedWarning(speed: Float) {
        // Build and display notification
        val notificationBuilder = NotificationCompat.Builder(this, "SPEED_WARNING_CHANNEL")
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Speed Warning")
            .setContentText("You are driving at ${speed * 3.6f} km/h. Slow down!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(1, notificationBuilder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SPEED_WARNING_CHANNEL",
                "Speed Warning Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for speed warnings."
            }
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates() // Start location updates if permission granted
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onReportSubmit(reportType: String, description: String) {
        createRoadReport(reportType, description)
    }

    private fun createRoadReport(reportType: String, description: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Appwrite.createRoadReport(reportType, description, currentLocation)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AppActivity, "Road report submitted successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AppActivity, "Failed to submit road report: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
