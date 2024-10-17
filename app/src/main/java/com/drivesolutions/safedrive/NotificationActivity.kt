package com.drivesolutions.safedrive

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drivesolutions.safedrive.data.models.Report
import com.drivesolutions.safedrive.data.sources.remote.Appwrite
import com.drivesolutions.safedrive.utils.ReportsAdapter
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    private val reports = mutableListOf<Report>()
    private lateinit var reportsAdapter: ReportsAdapter // Declare your RecyclerView adapter here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // Enable the back arrow in the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize the RecyclerView
        setupRecyclerView()

        // Fetch reports from Appwrite
        fetchReports()
    }

    // Handle the back arrow click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button action
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchReports() {
        lifecycleScope.launch {
            try {
                val userReports = Appwrite.getReports() // Fetch reports from your Appwrite backend
                reports.clear()
                reports.addAll(userReports)
                reportsAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                // Handle any errors here
                e.printStackTrace()
            }
        }
    }

    private fun setupRecyclerView() {
        // Initialize your RecyclerView and adapter here
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_reports)
        reportsAdapter = ReportsAdapter(reports)
        recyclerView.adapter = reportsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
}
