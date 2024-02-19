package com.presagetech.smartspectra.crashes

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.presagetech.smartspectra.utils.ProcessNameHelper

class InitializationContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        initializeExceptionHandler()
        return true // Return true to indicate successful loading
    }

    // Note that Timber logging is not initialized at this point, so we use Log
    private fun initializeExceptionHandler() {
        val context = context!!.applicationContext
        val processName = ProcessNameHelper.getCurrentProcessName(context)
        val packageName = context.packageName
        val isMainProcess = processName == packageName
        if (!isMainProcess) {
            return
        }
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        val interceptor = MiddlewareExceptionHandler(context)
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            interceptor.uncaughtException(thread, exception)
            originalHandler?.uncaughtException(thread, exception)
        }
        CrashesUploader.tryUploadCrashReports(context)
    }

    // Implement the remaining abstract methods with no-op implementations
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}
