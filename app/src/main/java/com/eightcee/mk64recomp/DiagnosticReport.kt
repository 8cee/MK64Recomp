package com.eightcee.mk64recomp

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticReport {
    fun create(context: Context): File {
        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val output = File(dir, "diagnostic_" + stamp + ".txt")
        val log = runCatching { Diagnostics.getLogFile().readText() }.getOrDefault("")

        val report = buildString {
            appendLine("MK64Recomp Diagnostic Report")
            appendLine("Package: " + context.packageName)
            appendLine("Version: " + BuildConfig.VERSION_NAME)
            appendLine("Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")")
            appendLine("Device: " + Build.MANUFACTURER + " " + Build.MODEL)
            appendLine("ABI: " + Build.SUPPORTED_ABIS.joinToString())
            appendLine()
            appendLine("ROM")
            val rom = RomManager.installedRom(context)
            val validation = RomManager.validate(rom)
            appendLine("Present: " + rom.exists())
            appendLine("Size: " + validation.size)
            appendLine("SHA1: " + validation.sha1)
            appendLine("Valid: " + validation.valid)
            appendLine()
            appendLine("SESSION LOG")
            append(log)
        }

        output.writeText(report)
        Diagnostics.info("Diagnostic report created path=" + output.name)
        return output
    }
}
