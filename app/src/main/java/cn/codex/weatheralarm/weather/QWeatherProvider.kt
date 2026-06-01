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
        require(apiKey.isNotBlank()) { "未配置 QWEATHER_API_KEY" }
        require(apiHost.isNotBlank()) { "未配置 QWEATHER_API_HOST，请在和风天气控制台设置页复制 API Host" }

        val locationParam = String.format(Locale.US, "%.2f,%.2f", location.longitude, location.latitude)
        val url = URL("https://$apiHost/v7/weather/24h?location=$locationParam&lang=zh")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("X-QW-Api-Key", apiKey)
            setRequestProperty("Accept-Encoding", "gzip")
            connectTimeout = 8000
            readTimeout = 8000
        }

        val body = try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            stream.decode(connection.contentEncoding).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val json = JSONObject(body)
        val code = json.optString("code")
        if (code != "200") {
            val error = json.optJSONObject("error")
            val detail = error?.optString("detail").orEmpty()
            val title = error?.optString("title").orEmpty()
            error("QWeather 返回异常：$code ${title.ifBlank { detail }}".trim())
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

    private fun InputStream.decode(contentEncoding: String?): InputStream =
        if (contentEncoding.equals("gzip", ignoreCase = true)) GZIPInputStream(this) else this
}
