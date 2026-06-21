package com.trobatapp.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

object GeocodingUtil {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class NominatimResponse(val display_name: String? = null)

    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = URI("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&accept-language=es").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "TrobatApp/1.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val body = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<NominatimResponse>(body).display_name
        } catch (e: Exception) {
            null
        }
    }
}
