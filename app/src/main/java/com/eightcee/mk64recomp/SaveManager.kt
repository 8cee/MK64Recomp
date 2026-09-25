package com.eightcee.mk64recomp

import android.content.Context
import java.io.File

object SaveManager {
    fun saveDir(context: Context): File =
        File(context.filesDir, "saves").apply { mkdirs() }

    fun eepromFile(context: Context): File =
        File(saveDir(context), "mk64.eep")

    fun backupFile(context: Context): File =
        File(saveDir(context), "mk64.eep.bak")

    fun ensure(context: Context) {
        val dir = saveDir(context)
        Diagnostics.info("Save directory=" + dir.absolutePath)
    }

    fun backup(context: Context): Boolean {
        val src = eepromFile(context)
        if (!src.exists()) return false
        val dst = backupFile(context)
        src.copyTo(dst, overwrite = true)
        Diagnostics.info("Save backup written bytes=" + dst.length())
        return true
    }
}
