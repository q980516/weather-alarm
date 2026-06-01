package cn.codex.weatheralarm.weather

import cn.codex.weatheralarm.domain.HourlyWeather
import cn.codex.weatheralarm.domain.WeatherDecision
import cn.codex.weatheralarm.domain.WeatherForecast
import java.time.LocalDate
import java.time.LocalDateTime

object RainClassifier {
    private val rainIconCodes = (300..399).map { it.toString() }.toSet()

    fun decide(
        profileId: Long,
        forecast: WeatherForecast,
        checkedAt: LocalDateTime,
        targetDate: LocalDate
    ): WeatherDecision {
        val relevant = forecast.hourly
        val rainHour = relevant.firstOrNull { it.isRainy() }
        val fallback = relevant.firstOrNull()
        val isRain = rainHour != null
        val rawCode = (rainHour ?: fallback)?.iconCode.orEmpty()
        val confidence = when {
            rainHour?.precipMm ?: 0.0 > 0.0 -> 0.95f
            rainHour?.probabilityOfPrecipitation ?: 0 >= 60 -> 0.82f
            isRain -> 0.72f
            relevant.isNotEmpty() -> 0.68f
            else -> 0.2f
        }
        val message = if (isRain) {
            "预计有雨，已采用雨天闹钟"
        } else {
            "未发现降雨，已采用非雨天闹钟"
        }

        return WeatherDecision(
            profileId = profileId,
            checkedAt = checkedAt,
            targetDate = targetDate,
            isRainExpected = isRain,
            source = forecast.source,
            confidence = confidence,
            rawWeatherCode = rawCode,
            message = message
        )
    }

    private fun HourlyWeather.isRainy(): Boolean {
        val textLooksRainy = text.contains("雨") || text.contains("阵雨") || text.contains("雷")
        return iconCode in rainIconCodes ||
            precipMm > 0.0 ||
            (probabilityOfPrecipitation ?: 0) >= 50 ||
            textLooksRainy
    }
}
