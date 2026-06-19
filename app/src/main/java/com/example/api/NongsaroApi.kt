package com.example.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

data class PlantCareData(
    val waterCycleSpring: String,
    val waterCycleSummer: String,
    val waterCycleAutumn: String,
    val waterCycleWinter: String,
    val lightDemand: String,
    val growthTemperature: String,
    val winterMinTemperature: String,
    val humidity: String,
    val manageLevel: String,
    val fertilizer: String
)

object NongsaroApi {
    private const val TAG = "NongsaroApi"
    private const val API_KEY = "20260522RJLLUKQ6JPF4HP57L3MCTQ"
    private const val BASE_URL = "http://api.nongsaro.go.kr/service/garden"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchPlantCare(plantName: String): PlantCareData? {
        val cntntsNo = searchCntntsNo(plantName) ?: return null
        return fetchDetail(cntntsNo)
    }

    private fun searchCntntsNo(plantName: String): String? {
        val url = "$BASE_URL/gardenList?apiKey=$API_KEY&sType=sCntntsSj&sText=$plantName"
        Log.d(TAG, "Searching: $url")

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(body))

        var insideItem = false
        var currentTag = ""
        var cntntsNo: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") insideItem = true
                }
                XmlPullParser.TEXT -> {
                    if (insideItem && currentTag == "cntntsNo" && cntntsNo == null) {
                        cntntsNo = parser.text?.trim()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") insideItem = false
                    currentTag = ""
                }
            }
            parser.next()
        }

        Log.d(TAG, "Found cntntsNo: $cntntsNo")
        return cntntsNo
    }

    private fun fetchDetail(cntntsNo: String): PlantCareData? {
        val url = "$BASE_URL/gardenDtl?apiKey=$API_KEY&cntntsNo=$cntntsNo"
        Log.d(TAG, "Fetching detail: $url")

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null

        val fields = mutableMapOf<String, String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(body))

        var currentTag = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> currentTag = parser.name
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (!text.isNullOrEmpty() && currentTag.isNotEmpty()) {
                        fields[currentTag] = text
                    }
                }
                XmlPullParser.END_TAG -> currentTag = ""
            }
            parser.next()
        }

        Log.d(TAG, "Detail fields: ${fields.keys}")

        return PlantCareData(
            waterCycleSpring = fields["watercycleSprngCodeNm"] ?: "",
            waterCycleSummer = fields["watercycleSummerCodeNm"] ?: "",
            waterCycleAutumn = fields["watercycleAutumnCodeNm"] ?: "",
            waterCycleWinter = fields["watercycleWinterCodeNm"] ?: "",
            lightDemand = fields["lighttdemanddoCodeNm"] ?: "",
            growthTemperature = fields["grwhTpCodeNm"] ?: "",
            winterMinTemperature = fields["winterLwetTpCodeNm"] ?: "",
            humidity = fields["hdCodeNm"] ?: "",
            manageLevel = fields["managelevelCodeNm"] ?: "",
            fertilizer = fields["frtlzrInfo"] ?: ""
        )
    }
}
