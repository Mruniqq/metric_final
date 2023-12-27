package com.metricrun.app.network


import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NetworkService {

    fun sendReadingsToServer(readings: List<JSONObject>) {
        val thread = Thread {
            try {
                val url = URL("http://54.221.216.132/metricrun/update.php")
                val readingsArray = JSONArray(readings)
                Log.d("SendReadings", "Sending readings: ${readingsArray.toString()}")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")

                    outputStream.use { os ->
                        val input = readingsArray.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    // Handle server response
                    handleServerResponse(this)
                }
            } catch (e: Exception) {
                Log.e("SendReadings", "Exception when sending readings: ${e.message}")
                e.printStackTrace()
            }
        }
        thread.start()
    }

    fun getAndDisplayAverages(macAddress: String, callback: (AverageData?) -> Unit) {
        val thread = Thread {
            try {
                val url = URL("http://54.221.216.132/metricrun/get_data.php?macAddress=$macAddress")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"

                    val responseCode = responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        inputStream.bufferedReader().use {
                            val response = it.readText()
                            val jsonResponse = JSONObject(response)
                            val averageData = parseAverageData(jsonResponse)
                            callback(averageData)
                        }
                    } else {
                        Log.e("GetAverages", "Server responded with code: $responseCode")
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("GetAverages", "Exception when getting averages: ${e.message}")
                e.printStackTrace()
                callback(null)
            }
        }
        thread.start()
    }

    private fun handleServerResponse(connection: HttpURLConnection) {
        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage

        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.e("SendReadings", "Error in server response. Code: $responseCode, Message: $responseMessage")
            connection.errorStream?.bufferedReader()?.use {
                val errorResponse = it.readText()
                Log.e("SendReadings", "Server error detail: $errorResponse")
            }
        } else {
            connection.inputStream.bufferedReader().use {
                val response = it.readText()
                Log.d("SendReadings", "Server response: $response")
            }
        }
    }

    private fun parseAverageData(jsonResponse: JSONObject): AverageData {
        return AverageData(
            averageApint = jsonResponse.getDouble("average_apint"),
            averageApext = jsonResponse.getDouble("average_apext"),
            // Parse other data as needed
        )
    }

    data class AverageData(
        val averageApint: Double,
        val averageApext: Double,
        // Other data fields
    )
}
