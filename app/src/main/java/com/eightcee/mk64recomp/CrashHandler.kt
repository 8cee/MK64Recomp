package com.eightcee.mk64recomp

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val appContext: Context, private val previous: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            Diagnostics.error("Uncaught exception on thread=" + thread.name, throwable)
            val dir = File(appContext.filesDir, "diagnostics").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val report = buildString {
                appendLine("MK64Recomp crash report")
                appendLine("Thread: " + thread.name)
                appendLine("Message: " + throwable.message)
                appendLine()
                appendLine(throwable.stackTraceToString())
            }
            File(dir, "crash_" + stamp + ".txt").writeText(report)
        }
        previous?.uncaughtException(thread, throwable)
    }
    companion object {
        fun install(context: Context) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext, previous))
        }
    }
}
