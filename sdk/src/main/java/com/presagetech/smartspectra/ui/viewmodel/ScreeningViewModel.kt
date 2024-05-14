package com.presagetech.smartspectra.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.presagetech.smartspectra.network.SDKApiService
import com.presagetech.smartspectra.network.model.ETag
import com.presagetech.smartspectra.ui.summary.UploadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Integer.min
import java.util.zip.GZIPOutputStream

class ScreeningViewModel(
    private val sdkApiService: SDKApiService
) : ViewModel() {
    private val MAX_UPLOAD_SIZE = 5 * 1024 * 1024

    private var jsonData: String? = null

    private val _uploadProgressLiveData = MutableLiveData<UploadingState>()
    val uploadProgressLiveData: LiveData<UploadingState> = _uploadProgressLiveData

    private val _rrHRAveragePairLiveData = MutableLiveData<RetrievedData>()
    val rrHRAveragePairLiveData: LiveData<RetrievedData> = _rrHRAveragePairLiveData

    fun setJsonData(json: String) {
        jsonData = json
    }

    suspend fun startUploadingProcess() = withContext(Dispatchers.IO) {
        val jsonData = jsonData ?: throw IllegalStateException("jsonData is null")
        val bytesToSend = gzipString2ByteArray(jsonData)
        Timber.i("json length=${jsonData.length}, compressed size=${bytesToSend.size}")
        try {
            _uploadProgressLiveData.postValue(UploadingState.Uploading(0.0f))
            val uploadingURLBody = generateUploadURLRequestBody(bytesToSend.size)
            val output = sdkApiService.postUploadURL(uploadingURLBody)
                ?: throw IllegalStateException("output is null")

            val jsonUrlsArray = output.getJSONArray("urls")
            val uploadUrls = List<String>(jsonUrlsArray.length()) { jsonUrlsArray.getString(it) }
            uploadJSONToUrls(
                uploadUrls,
                output.getString("upload_id"),
                output.getString("id"),
                bytesToSend
            )
        } catch (e: IOException) {
            Timber.e(e, "startUploadingProcess: failed to upload")
            _uploadProgressLiveData.postValue(UploadingState.Failed)
        }
    }

    private fun gzipString2ByteArray(json: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter().use { it.write(json) }
        return bos.toByteArray()
    }

    private suspend fun uploadJSONToUrls(
        uploadURLs: List<String>,
        uploadID: String,
        vidID: String,
        jsonByteArray: ByteArray,
    ) = withContext(Dispatchers.IO) {
        var pos = 0
        val jsonSize = jsonByteArray.size
        require(uploadURLs.size * MAX_UPLOAD_SIZE > jsonSize) {
            "max chunk size is $MAX_UPLOAD_SIZE, got ${uploadURLs.size} urls for uploading" +
                    " but total size is $jsonSize"
        }
        val eTagList = ArrayList<ETag>()
        for (i in uploadURLs.indices) {
            if (pos >= jsonSize) {
                break
            }
            val nextPart = jsonByteArray.copyOfRange(pos, min(jsonSize, pos + MAX_UPLOAD_SIZE))
            Timber.i("about to upload chunk #$i; pos=$pos; chunk size=${nextPart.size}")
            val eTag = sdkApiService.putSendFileChunkFile(uploadURLs[i], nextPart) {
                val progress =
                    ((pos + it * nextPart.size) / jsonSize.toFloat()).coerceAtMost(1.0f)
                _uploadProgressLiveData.postValue(UploadingState.Uploading(progress))
            }
            eTagList.add(ETag(eTag, i + 1))
            Timber.i("chunk #$i successfully uploaded")
            pos += nextPart.size
        }
        _uploadProgressLiveData.postValue(UploadingState.Processing)
        postCompleteResponse(vidID, uploadID, eTagList)
    }

    private suspend fun postCompleteResponse(
        vidID: String,
        uploadID: String,
        eTagList: ArrayList<ETag>
    ) {
        Timber.d("postCompleteResponse: 1111")
        val output = sdkApiService.postComplete(
            completeHealthUpdateBody(
                id = vidID,
                upload_id = uploadID,
                parts = eTagList
            )
        )
        Timber.d("onResponse: postComplete $output")
        val data = retrieveData(vidID)
        _rrHRAveragePairLiveData.postValue(data)
    }

    private fun completeHealthUpdateBody(
        id: String,
        upload_id: String,
        parts: ArrayList<ETag>
    ): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("upload_id", upload_id)
            put("parts", JSONArray().apply {
                parts.forEach {
                    put(JSONObject().apply {
                        put("ETag", it.ETag)
                        put("PartNumber", it.PartNumber)
                    })
                }
            })
        }
    }

    private suspend fun retrieveData(id: String): RetrievedData {
        val retryCount = 20
        val retryDelay = 512L
        delay(retryDelay * 2)  // initial delay
        for (attempt in 1..retryCount) {
            val output = sdkApiService.postRetrieveData(id)
            Timber.i("retriveData attempt#$attempt: $output")
            val json: JSONObject? = try {
                output?.let { JSONObject(it) }
            } catch (e: JSONException) {
                Timber.i("retrieveData #$attempt: failed to parse response")
                null
            }
            if (json != null && json.has("rr")) {
                return parseRetrieveDataResponse(json)
            } else {
                delay(retryDelay)
            }
        }
        throw IllegalStateException("newRetrieveData: failed to retrieve data. Timeout")
    }

    private fun generateUploadURLRequestBody(fileSize: Int): JSONObject =
        JSONObject().apply {
            put("file_size", fileSize)
            put("all", JSONObject().apply {
                put("to_process", true)
            })
        }

    private fun parseRetrieveDataResponse(response: JSONObject): RetrievedData {
        val rrObject = response.getJSONObject("rr")
        val rrIterator = rrObject.keys()
        var rrSum = 0.0
        var rrCount = 0
        while (rrIterator.hasNext()) {
            val key = rrIterator.next()
            val rrValue = rrObject.getJSONObject(key).getDouble("value")
            rrSum += rrValue
            rrCount++
        }
        val rrAverage = if (rrCount > 0) rrSum / rrCount else 0.0


        val hrObject = response.getJSONObject("hr")
        val hrIterator = hrObject.keys()
        var hrSum = 0.0
        var hrCount = 0

        while (hrIterator.hasNext()) {
            val key = hrIterator.next()
            val hrValue = hrObject.getJSONObject(key).getDouble("value")
            hrSum += hrValue
            hrCount++
        }
        val hrAverage = if (hrCount > 0) hrSum / hrCount else 0.0
        val result = RetrievedData(rrAverage, hrAverage)
        Timber.d("parseRetrieveDataResponse: $result")
        return result
    }

    data class RetrievedData(
        val rrAverage: Double,
        val hrAverage: Double,
    )
}
