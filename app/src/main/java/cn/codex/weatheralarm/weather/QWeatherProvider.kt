package cn.codex.weatheralarm.weather

import cn.codex.weatheralarm.domain.HourlyWeather
import cn.codex.weatheralarm.domain.LocationQuery
import cn.codex.weatheralarm.domain.WeatherForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.Locale
import java.util.zip.GZIPInputStream

class QWeatherProvider(
    private val apiKey: String,
    private val apiHost: String
) : WeatherProvider {
    override suspend fun getHourlyForecast(
        location: LocationQuery,
        from: java.time.LocalDateTime,
        to: java.time.LocalDateTime
    ): WeatherForecast = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Missing QWEATHER_API_KEY" }
        require(apiHost.isNotBlank()) { "Missing QWEATHER_API_HOST" }

        val locationParam = location.toQWeatherLocation()
        val url = URL("https://$apiHost/v7/weather/24h?location=$locationParam&lang=zh")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("X-QW-Api-Key", apiKey)
            setRequestProperty("Accept-Encoding", "gzip")
            connectTimeout = 8000
            readTimeout = 8000
        }

        val responseCode = connection.responseCode
        val body = try {
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            stream.decode(connection.contentEncoding).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val json = runCatching { JSONObject(body) }.getOrElse {
            error("QWeather HTTP $responseCode: ${body.take(120)}")
        }
        val code = json.optString("code")
        if (code != "200") {
            val error = json.optJSONObject("error")
            val detail = error?.optString("detail").orEmpty()
            val title = error?.optString("title").orEmpty()
            val message = title.ifBlank { detail }.ifBlank { body.take(120) }
            error("QWeather HTTP $responseCode code=$code: $message")
        }

        val hourly = json.getJSONArray("hourly")
        val items = buildList {
            for (index in 0 until hourly.length()) {
                val item = hourly.getJSONObject(index)
                val time = OffsetDateTime.parse(item.getString("fxTime")).toLocalDateTime()
                if (!time.isBefore(from) && !time.isAfter(to)) {
                    add(
                        HourlyWeather(
                            time = time,
                            text = item.optString("text"),
                            iconCode = item.optString("icon"),
                            precipMm = item.optString("precip", "0").toDoubleOrNull() ?: 0.0,
                            probabilityOfPrecipitation = item.optString("pop").toIntOrNull()
                        )
                    )
                }
            }
        }
        WeatherForecast(source = "QWeather", hourly = items)
    }

    private fun LocationQuery.toQWeatherLocation(): String {
        val hasValidCoordinates = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
        if (hasValidCoordinates) {
            return String.format(Locale.US, "%.2f,%.2f", longitude, latitude)
        }
        if (cityId.isNotBlank()) {
            return URLEncoder.encode(cityId, Charsets.UTF_8.name())
        }
        error("No usable location: invalid coordinates and missing LocationID")
    }

    private fun InputStream.decode(contentEncoding: String?): InputStream =
        if (contentEncoding.equals("gzip", ignoreCase = true)) GZIPInputStream(this) else this
}
