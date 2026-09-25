package com.eightcee.mk64recomp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        Diagnostics.init(applicationContext)
        CrashHandler.install(applicationContext)
        Diagnostics.info("MainActivity.onCreate")
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        status = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val importButton = Button(this).apply {
            text = "Import Mario Kart 64 ROM"
            setOnClickListener { chooseRom() }
        }

        val launchButton = Button(this).apply {
            text = "Initialize Native Runtime"
            setOnClickListener { initializeRuntime() }
        }

        val diagnosticsButton = Button(this).apply {
            text = "Create Diagnostic Report"
            setOnClickListener {
                val report = DiagnosticReport.create(this@MainActivity)
                Toast.makeText(
                    this@MainActivity,
                    "Diagnostic report: " + report.name,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
                addView(status)
                addView(importButton)
                addView(launchButton)
                addView(diagnosticsButton)
            }
        )

        refreshStatus()
        Diagnostics.info("Launcher UI initialized")
    }

    private fun chooseRom() {
        Diagnostics.info("Opening ROM picker")
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            },
            REQUEST_ROM
        )
    }

    @Deprecated("Deprecated in Android SDK but retained for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_ROM || resultCode != RESULT_OK) return

        val uri = data?.data ?: return
        runCatching {
            RomManager.importRom(this, uri)
        }.onSuccess { validation ->
            Toast.makeText(this, validation.message, Toast.LENGTH_LONG).show()
            refreshStatus()
        }.onFailure {
            Diagnostics.error("ROM import failed", it)
            Toast.makeText(this, "ROM import failed: " + it.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeRuntime() {
        val rom = RomManager.installedRom(this)
        val validation = RomManager.validate(rom)

        if (!validation.valid) {
            Diagnostics.warn("Native runtime initialization blocked: ROM invalid")
            Toast.makeText(this, validation.message, Toast.LENGTH_LONG).show()
            refreshStatus()
            return
        }

        val result = runCatching {
            val version = NativeBridge.nativeVersion()
            Diagnostics.info("Native version=" + version)
            NativeBridge.initialize(rom.absolutePath)
        }.getOrElse {
            Diagnostics.error("Native runtime initialization failed", it)
            -99
        }

        if (result == 0) {
            Diagnostics.info("Native runtime initialized successfully")
            Toast.makeText(this, "Native runtime initialized", Toast.LENGTH_LONG).show()
        } else {
            Diagnostics.error("Native runtime returned error=" + result)
            Toast.makeText(this, "Native initialization failed: " + result, Toast.LENGTH_LONG).show()
        }
        refreshStatus()
    }

    private fun refreshStatus() {
        val rom = RomManager.installedRom(this)
        val validation = RomManager.validate(rom)
        status.text = buildString {
            appendLine("MK64Recomp Android")
            appendLine()
            appendLine("Diagnostics: active")
            append("ROM: ")
            append(if (validation.valid) "Mario Kart 64 USA ready" else "not installed")
        }
    }

    override fun onResume() {
        super.onResume()
        Diagnostics.info("MainActivity.onResume")
    }

    override fun onPause() {
        Diagnostics.info("MainActivity.onPause")
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { NativeBridge.shutdown() }
        Diagnostics.info("MainActivity.onDestroy")
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_ROM = 1001
    }
}
