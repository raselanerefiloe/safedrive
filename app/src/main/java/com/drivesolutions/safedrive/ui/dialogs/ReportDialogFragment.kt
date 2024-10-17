package com.drivesolutions.safedrive.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.drivesolutions.safedrive.R

class ReportDialogFragment : DialogFragment() {

    interface ReportDialogListener {
        fun onReportSubmit(reportType: String, description: String)
    }

    private var listener: ReportDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            context as ReportDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement ReportDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater: LayoutInflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.fragment_report_dialog, null)

        val reportTypeSpinner: Spinner = dialogView.findViewById(R.id.report_type_spinner)
        val descriptionInput: EditText = dialogView.findViewById(R.id.description_input)
        val submitButton: Button = dialogView.findViewById(R.id.submit_button)

        submitButton.setOnClickListener {
            val reportType = reportTypeSpinner.selectedItem.toString()
            val description = descriptionInput.text.toString()
            listener?.onReportSubmit(reportType, description)
            dismiss() // Close the dialog
        }

        builder.setView(dialogView)
        return builder.create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
