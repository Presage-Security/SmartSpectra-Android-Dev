package com.presagetech.physiology.network

import java.io.*
import java.lang.Math.min
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object HttpMethods {
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse = suspendCoroutine { continuation ->
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).buffered().use {
            it.readText()
        }
        continuation.resume(HttpResponse(
            connection.responseCode,
            connection.headerFields,
            responseBody
        ))
        connection.disconnect()
    }

    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse = suspendCoroutine { continuation ->
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
        connection.readTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        BufferedOutputStream(connection.outputStream).use {
            it.write(body.toByteArray())
        }
        val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).buffered().use {
            it.readText()
        }
        continuation.resume(HttpResponse(
            connection.responseCode,
            connection.headerFields,
            responseBody
        ))
        connection.disconnect()
    }

    suspend fun uploadFile(
        url: String,
        body: ByteArray,
        headers: Map<String, String> = emptyMap(),
        progressListener: ((Float) -> Unit)? = null
    ): HttpResponse = suspendCoroutine { continuation ->
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "PUT"
        connection.doOutput = true
        connection.doInput = true
        connection.connectTimeout = TimeUnit.MINUTES.toMillis(5).toInt()
        connection.readTimeout = TimeUnit.MINUTES.toMillis(5).toInt()
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.setRequestProperty("Content-Length", body.size.toString())
        connection.setRequestProperty("Content-Encoding", "gzip")
        connection.setRequestProperty("Content-Type", " ")
        connection.outputStream.use {
            var pos = 0
            val chunkSize = 8 * 1024
            while(pos < body.size) {
                it.write(body, pos, min(chunkSize, body.size - pos))
                pos += chunkSize
                val progress = (1.0f * pos / body.size).coerceAtMost(1.0f)
                progressListener?.invoke(progress)
            }
        }
        val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).buffered().use {
            it.readText()
        }
        continuation.resume(HttpResponse(
            connection.responseCode,
            connection.headerFields,
            responseBody
        ))
        connection.disconnect()
    }
}
