package com.eightcee.mk64recomp

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class RomValidation(
    val valid: Boolean,
    val sha1: String,
    val size: Long,
    val message: String
)

object RomManager {
    private const val EXPECTED_US_SHA1 = "579c48e211ae952530ffc8738709f078d5dd215e"
    private const val ROM_FILE_NAME = "baserom.us.z64"

    fun importRom(context: Context, uri: Uri): RomValidation {
        val romDir = File(context.filesDir, "rom").apply { mkdirs() }
        val target = File(romDir, ROM_FILE_NAME)

        Diagnostics.info("ROM import started uri=" + uri.scheme)

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected ROM" }
            FileOutputStream(target).use { output -> input.copyTo(output) }
        }

        val result = validate(target)
        Diagnostics.info(
            "ROM import complete valid=" + result.valid +
                " size=" + result.size +
                " sha1=" + result.sha1
        )

        if (!result.valid) {
            target.delete()
            Diagnostics.warn("ROM rejected: " + result.message)
        }
        return result
    }

    fun installedRom(context: Context): File =
        File(File(context.filesDir, "rom"), ROM_FILE_NAME)

    fun validate(file: File): RomValidation {
        if (!file.exists()) return RomValidation(false, "", 0L, "ROM file not found")

        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }

        val sha1 = digest.digest().joinToString("") { "%02x".format(it) }
        val valid = sha1.equals(EXPECTED_US_SHA1, ignoreCase = true)
        val message = if (valid) "Mario Kart 64 USA ROM accepted" else
            "Unsupported ROM. Expected Mario Kart 64 USA SHA-1 " + EXPECTED_US_SHA1

        return RomValidation(valid, sha1, file.length(), message)
    }
}
