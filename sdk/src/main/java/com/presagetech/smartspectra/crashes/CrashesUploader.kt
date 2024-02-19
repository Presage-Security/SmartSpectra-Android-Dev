package com.presagetech.smartspectra.crashes

import android.content.Context
import com.presagetech.smartspectra.network.HttpMethods
import com.presagetech.smartspectra.network.SDKApiService.Companion.getUrl
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.lang.Thread.sleep

object CrashesUploader {
    private var initialized = false

    fun tryUploadCrashReports(context: Context) {
        // only upload once
        if (initialized) return
        initialized = true
        try {
            val folder = MiddlewareExceptionHandler.getCrashReportsFolder(context)
            uploadCrashReports(folder)
        } catch (e: Throwable) {
            Timber.e(e, "Failed to upload crash reports")
        }
    }

    private fun uploadCrashReports(folder: File) {
        if (!folder.exists()) return
        val reports = folder.listFiles() ?: return
        if (reports.isEmpty()) return

        Thread({
            runBlocking {
                sleep(1_000)  // wait for application to startup without our overhead
                try {
                    reports.forEach { report ->
                        val content = report.readText()
                        Timber.i("Uploading crash report: ${report.name}")
                        val success = postCrashReport(content)
                        if (success) {
                            Timber.i("${report.name} uploaded")
                            report.delete()
                        } else {
                            Timber.e("Failed to upload crash report: ${report.name}")
                            return@runBlocking
                        }
                    }
                } catch (e: Throwable) {
                    Timber.e(e, "Failed to upload crash reports")
                }
            }
        }, "Smartspectra CrashesUploader").start()
    }

    private suspend fun postCrashReport(body: String): Boolean {
        return HttpMethods.post(
            url = getUrl("v1/android-crash-report"),
            body = body,
        ).responseCode in 200..299
    }
}
