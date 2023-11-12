package com.presagetech.physiology.network

class HttpResponse(
    val responseCode: Int,
    val responseHeaders: Map<String, List<String>>,
    val responseBody: String?
)
