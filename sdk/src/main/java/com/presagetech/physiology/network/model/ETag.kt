package com.presagetech.physiology.network.model

/*
* Data class to help store ETage that we get from response header
* */
data class ETag(
    val ETag: String,
    val PartNumber: Int
)
