package com.eightcee.mk64recomp

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Diagnostics {
    private const val TAG = "MK64Diag"
    private const val MAX_LOG_BYTES = 2L * 1024L * 1024L
    private lateinit var logFile: File

    fun init(context: Context) {
        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
        logFile = File(dir, "mk64.log")
        rotateIfNeeded()
        info("==== MK64Recomp diagnostic session ====")
        info("App package=" + context.packageName)
        info("Android=" + Build.VERSION.RELEASE + " SDK=" + Build.VERSION.SDK_INT)
        info("Device=" + Build.MANUFACTURER + " " + Build.MODEL)
        info("ABI=" + Build.SUPPORTED_ABIS.joinToString())
    }
    fun info(message: String) = write("INFO", message)
    fun warn(message: String) = write("WARN", message)
    fun error(message: String, error: Throwable? = null) {
        var body = message
        if (error != null) body += "\n" + Log.getStackTraceString(error)
        write("ERROR", body)
    }
    fun getLogFile(): File = logFile
    private fun write(level: String, message: String) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "[" + stamp + "][" + level + "] " + message + "\n"
        val priority = when (level) { "ERROR" -> Log.ERROR; "WARN" -> Log.WARN; else -> Log.INFO }
        Log.println(priority, TAG, message)
        if (::logFile.isInitialized) runCatching { logFile.appendText(line) }
    }
    private fun rotateIfNeeded() {
        if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
            val old = File(logFile.parentFile, "mk64.previous.log")
            if (old.exists()) old.delete()
            logFile.renameTo(old)
        }
    }
}
