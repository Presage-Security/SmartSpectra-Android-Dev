package com.presagetech.smartspectra.network

import org.json.JSONObject


class SDKApiService(private val token: String) {
    suspend fun postUploadURL(body: JSONObject): JSONObject? {
        return HttpMethods.post(
            url = getUrl("v1/upload-url"),
            body = body.toString(),
            headers = getDefaultHeaders()
        ).responseBody?.let {
            JSONObject(it)
        }
    }

    suspend fun postComplete(body: JSONObject): String? {
        return HttpMethods.post(
            url = getUrl("v1/complete"),
            body = body.toString(),
            headers = getDefaultHeaders()
        ).responseBody
    }

    suspend fun postRetrieveData(id: String): String? {
        val requestBody = JSONObject().apply {
            put("id", id)
        }
        return HttpMethods.post(
            url = getUrl("retrieve-data"),
            body = requestBody.toString(),
            headers = getDefaultHeaders()
        ).responseBody
    }

    suspend fun putSendFileChunkFile(
        url: String,
        body: ByteArray,
        listener: (Float) -> Unit,
    ): String {
        val response = HttpMethods.uploadFile(url, body, progressListener = listener)
        return response.responseHeaders["ETag"]?.get(0)
            ?: throw IllegalStateException("missing etag in response headers")
    }

    private fun getDefaultHeaders(): Map<String, String> = mapOf("x-api-key" to token)

    companion object {
        const val BASE_URL = "https://api.physiology.presagetech.com/"

        fun getUrl(url: String): String = "$BASE_URL$url"
    }
}
