package com.presagetech.smartspectra.network

import java.io.*
import java.lang.Math.min
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object HttpMethods {
    suspend fun get(
        url: String, headers: Map<String, String> = emptyMap()
    ): HttpResponse = suspendCoroutine { continuation ->
        var connection: HttpsURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpsURLConnection).also {
                it.requestMethod = "GET"
                headers.forEach { (k, v) -> it.setRequestProperty(k, v) }
            }
            val responseBody =
                BufferedReader(InputStreamReader(connection.inputStream)).buffered().use {
                        it.readText()
                    }
            continuation.resume(
                HttpResponse(
                    connection.responseCode, connection.headerFields, responseBody
                )
            )
        } catch (e: IOException) {
            continuation.resumeWithException(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun post(
        url: String, body: String, headers: Map<String, String> = emptyMap()
    ): HttpResponse = suspendCoroutine { continuation ->
        var connection: HttpsURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpsURLConnection).also {
                it.requestMethod = "POST"
                it.doOutput = true
                it.connectTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
                it.readTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
                headers.forEach { (k, v) -> it.setRequestProperty(k, v) }
            }
            BufferedOutputStream(connection.outputStream).use {
                it.write(body.toByteArray())
            }
            val responseBody =
                BufferedReader(InputStreamReader(connection.inputStream)).buffered().use {
                        it.readText()
                    }
            continuation.resume(
                HttpResponse(
                    connection.responseCode, connection.headerFields, responseBody
                )
            )
        } catch (e: IOException) {
            continuation.resumeWithException(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun uploadFile(
        url: String,
        body: ByteArray,
        headers: Map<String, String> = emptyMap(),
        progressListener: ((Float) -> Unit)? = null
    ): HttpResponse = suspendCoroutine { continuation ->
        var connection: HttpsURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpsURLConnection).also {
                it.requestMethod = "PUT"
                it.doOutput = true
                it.doInput = true
                it.connectTimeout = TimeUnit.MINUTES.toMillis(5).toInt()
                it.readTimeout = TimeUnit.MINUTES.toMillis(5).toInt()
                headers.forEach { (k, v) -> it.setRequestProperty(k, v) }
                it.setRequestProperty("Content-Length", body.size.toString())
                it.setRequestProperty("Content-Encoding", "gzip")
                it.setRequestProperty("Content-Type", " ")
            }
            connection.outputStream.use {
                var pos = 0
                val chunkSize = 8 * 1024
                while (pos < body.size) {
                    it.write(body, pos, min(chunkSize, body.size - pos))
                    pos += chunkSize
                    val progress = (1.0f * pos / body.size).coerceAtMost(1.0f)
                    progressListener?.invoke(progress)
                }
            }
            val responseBody =
                BufferedReader(InputStreamReader(connection.inputStream)).buffered().use {
                        it.readText()
                    }
            continuation.resume(
                HttpResponse(
                    connection.responseCode, connection.headerFields, responseBody
                )
            )
        } catch (e: IOException) {
            continuation.resumeWithException(e)
        } finally {
            connection?.disconnect()
        }
    }
}
