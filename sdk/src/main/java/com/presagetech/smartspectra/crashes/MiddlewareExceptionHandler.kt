package com.presagetech.smartspectra.crashes

import android.content.Context
import android.os.Build
import java.io.File

class MiddlewareExceptionHandler(
    private val context: Context,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        val ourException = crashContainsLibraryCode(e)
        if (ourException) {
            saveCrashReport(e)
        }
    }

    private fun crashContainsLibraryCode(e: Throwable): Boolean {
        val prefix = getOurPackage()
        // check string representation of stack trace,
        // however it could be truncated
        if (e.stackTraceToString().contains(prefix)) {
            return true
        }
        // so we also check the stack trace objects, just in case
        val stackAndCause = e.stackTrace + e.cause?.stackTrace.orEmpty()
        stackAndCause.forEach {
            if (it.className.startsWith(prefix)) {
                return true
            }
        }
        return false
    }

    private fun getOurPackage(): String {
        // it's possible for package name to change because of obfuscation
        val thisClassPackage = MiddlewareExceptionHandler::class.java.`package`!!.name
        // get the first 3 parts of the package name "com.presagetech.smartspectra"
        return thisClassPackage.split(".").take(3).joinToString(".")
    }

    private fun saveCrashReport(e: Throwable) {
        val fileName = "crash-${System.currentTimeMillis()}.txt"
        val folder = getCrashReportsFolder(context)
        File(folder, fileName).writeText(getReport(e))
    }

    private fun getReport(e: Throwable): String {
        val timestamp = System.currentTimeMillis() / 1_000 // convert to seconds
        val device = getDeviceInfo()
        val stackTrace = e.stackTraceToString()
        return """
Timestamp: $timestamp
Device: $device
AppPackage: ${context.packageName}
LibraryPackage: ${getOurPackage()}

$stackTrace
""".trimStart()  // remove leading newline
    }

    private fun getDeviceInfo(): String {
        val deviceInfo = mapOf(
            "MANUFACTURER" to Build.MANUFACTURER,
            "DEVICE" to Build.DEVICE,
            "MODEL" to Build.MODEL,
        )
        return deviceInfo
            .map { (key, value) -> "\"$key\": \"$value\"" }
            .joinToString(",", "{", "}")
    }

    companion object {
        private const val CRASH_REPORT_FOLDER = "com.presagetech.smartspectra.crashes"
        fun getCrashReportsFolder(context: Context): File {
            return File(context.cacheDir, CRASH_REPORT_FOLDER).also {
                if (!it.exists()) it.mkdirs()
            }
        }
    }
}
