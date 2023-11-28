package com.presagetech.smartspectra.network

class HttpResponse(
    val responseCode: Int,
    val responseHeaders: Map<String, List<String>>,
    val responseBody: String?
)
