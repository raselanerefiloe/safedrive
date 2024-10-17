package com.drivesolutions.safedrive.utils


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drivesolutions.safedrive.R
import com.drivesolutions.safedrive.data.models.Report

class ReportsAdapter(private val reports: List<Report>) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val reportTypeTextView: TextView = view.findViewById(R.id.report_type)
        val descriptionTextView: TextView = view.findViewById(R.id.description)
        val statusTextView: TextView = view.findViewById(R.id.status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.reportTypeTextView.text = report.reportType.name
        holder.descriptionTextView.text = report.description
        holder.statusTextView.text = report.status.name
    }

    override fun getItemCount() = reports.size
}
