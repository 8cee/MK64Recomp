package com.eightcee.mk64recomp

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class DiagnosticsActivity : Activity() {
    private lateinit var body: TextView
    private var lastReport: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        body = TextView(this).apply {
            textSize = 13f
            setTextIsSelectable(true)
            setPadding(24, 24, 24, 24)
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER

            addView(button("Refresh") { refresh() })
            addView(button("Copy") { copyToClipboard() })
            addView(button("Export") { exportReport() })
            addView(button("Clear") { clearLog() })
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(actions)
            addView(
                ScrollView(this@DiagnosticsActivity).apply { addView(body) },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        setContentView(root)
        refresh()
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun refresh() {
        val report = DiagnosticReport.create(this)
        lastReport = report
        body.text = runCatching { report.readText() }
            .getOrElse { "Unable to read diagnostic report: " + it.message }
    }

    private fun copyToClipboard() {
        val text = body.text?.toString().orEmpty()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MK64 diagnostic report", text))
        Toast.makeText(this, "Diagnostic report copied", Toast.LENGTH_SHORT).show()
    }

    private fun exportReport() {
        if (lastReport == null) refresh()
        val report = lastReport ?: return

        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, report.name)
            },
            REQUEST_EXPORT
        )
    }

    @Deprecated("Deprecated in SDK but retained for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_EXPORT || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val report = lastReport ?: return

        runCatching {
            contentResolver.openOutputStream(uri)?.use { output ->
                report.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Unable to open destination")
        }.onSuccess {
            Toast.makeText(this, "Diagnostic report exported", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Export failed: " + it.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun clearLog() {
        runCatching {
            Diagnostics.getLogFile().writeText("")
            lastReport?.delete()
            lastReport = null
        }
        Diagnostics.info("Diagnostic log cleared by user")
        refresh()
    }

    companion object {
        private const val REQUEST_EXPORT = 2001
    }
}
