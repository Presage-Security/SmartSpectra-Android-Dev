package com.presagetech.smartspectra.ui.viewmodel

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.presagetech.smartspectra.network.SDKApiService
import com.presagetech.smartspectra.network.model.ETag
import com.presagetech.smartspectra.ui.summary.UploadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Integer.min
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream

class ScreeningViewModel(
    private val sdkApiService: SDKApiService
) : ViewModel() {
    private val MAX_UPLOAD_SIZE = 5 * 1024 * 1024

    private var jsonData: String? = null

    private val _uploadProgressLiveData = MutableLiveData<UploadingState>()
    val uploadProgressLiveData: LiveData<UploadingState> = _uploadProgressLiveData

    private val _rrstrictPulseRatePairLiveData = MutableLiveData<RetrievedData>()
    val rrstrictPulseRatePairLiveData: LiveData<RetrievedData> = _rrstrictPulseRatePairLiveData

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
        _rrstrictPulseRatePairLiveData.postValue(data)
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
            if (json != null && json.has("pressure")) {
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


    /*
        Parses json like:
        [ "2.036": { "value": -0.0042 }, ... ]
     */
    private fun parseFloatToValueArray(json: JSONObject): List<Pair<Float, Float>> {
        val result = ArrayList<Pair<Float, Float>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.getJSONObject(key).getDouble("value").toFloat()
            result.add(Pair(key.toFloat(), value))
        }
        return result
    }

    private fun parseFloatToConfidenceArray(json: JSONObject): List<Pair<Float, Float>> {
        val result = ArrayList<Pair<Float, Float>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.getJSONObject(key).getDouble("confidence").toFloat()
            result.add(Pair(key.toFloat(), value))
        }
        return result
    }
    private fun parseBoolToValueArray(json: JSONObject): List<Pair<Float, Boolean>> {
        val result = ArrayList<Pair<Float, Boolean>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.getJSONObject(key).getBoolean("value")
            result.add(Pair(key.toFloat(), value))
        }
        return result
    }

    private fun parseRetrieveDataResponse(response: JSONObject): RetrievedData {
        val version = response.getString("version")
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd, HH:mm:ss")
        val raw_datetime = LocalDateTime.parse(response.getString("upload_date"), formatter)
        val utcZone = ZoneId.of("UTC")
        val localZone = ZoneId.systemDefault()
        val utcZonedDateTime = ZonedDateTime.of(raw_datetime, utcZone)
        val upload_date = utcZonedDateTime.withZoneSameInstant(localZone)
        val hrObject = response.getJSONObject("pulse").getJSONObject("hr")
        val strictPulseRate = parseFloatToValueArray(hrObject)
            .map { it.second }.average()
            .let {
                if (it.isFinite()) it else 0.0
            }
        val pulsePleth: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("pulse")
                .getJSONObject("hr_trace").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val rrObject = response.getJSONObject("breath").getJSONObject("rr")

        val strictBreathingRate = parseFloatToValueArray(rrObject)
            .map { it.second }.average()
            .let {
                if (it.isFinite()) it else 0.0
            }
        val breathingPleth: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("rr_trace").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val rrValues: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("rr").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val rrConfidence: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("rr").let {
                    parseFloatToConfidenceArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val hrValues: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("pulse")
                .getJSONObject("hr").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val hrConfidence: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("pulse")
                .getJSONObject("hr").let {
                    parseFloatToConfidenceArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val amplitude: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("amplitude").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val baseline: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("baseline").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val ie: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("ie").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val rrl: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("rrl").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val phasic: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("pressure")
                .getJSONObject("phasic").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val hrv: List<Pair<Float, Float>>? = try {
            response
                .getJSONObject("pulse")
                .getJSONObject("hrv").let {
                    parseFloatToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val apnea: List<Pair<Float, Boolean>>? = try {
            response
                .getJSONObject("breath")
                .getJSONObject("apnea").let {
                    parseBoolToValueArray(it)
                }.sortedBy { it.first }
                .ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        val result = RetrievedData(
            strictPulseRate,
            pulsePleth,
            strictBreathingRate,
            breathingPleth,
            hrValues,
            hrConfidence,
            rrValues,
            rrConfidence,
            amplitude,
            apnea,
            baseline,
            ie,
            rrl,
            phasic,
            hrv,
            version,
            upload_date
        )
        Timber.i("UTC=${utcZonedDateTime}")
        Timber.i("Local=${upload_date}")
        return result
    }

    @Parcelize
    data class RetrievedData(
        val strictPulseRate: Double,
        val pulsePleth: List<Pair<Float, Float>>?,
        val strictBreathingRate: Double,
        val breathingPleth: List<Pair<Float, Float>>?,
        val pulseValues: List<Pair<Float, Float>>?,
        val pulseConfidence: List<Pair<Float, Float>>?,
        val breathingValues: List<Pair<Float, Float>>?,
        val breathingConfidence: List<Pair<Float, Float>>?,
        val breathingAmplitude: List<Pair<Float, Float>>?,
        val apnea: List<Pair<Float,Boolean>>?,
        val breathingBaseline: List<Pair<Float, Float>>?,
        val ie: List<Pair<Float, Float>>?,
        val rrl: List<Pair<Float, Float>>?,
        val phasic: List<Pair<Float, Float>>?,
        val hrv: List<Pair<Float, Float>>?,
        val version: String,
        val upload_date: ZonedDateTime
        ): Parcelable
}
