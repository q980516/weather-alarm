package cn.codex.weatheralarm.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AlarmProfile(
    val id: Long = DEFAULT_ID,
    val label: String = "上班闹钟",
    val rainTime: LocalTime = LocalTime.of(7, 0),
    val normalTime: LocalTime = LocalTime.of(7, 30),
    val deadlineTime: LocalTime = LocalTime.of(8, 0),
    val enabledDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val cityName: String = "上海",
    val cityId: String = "101020100",
    val latitude: Double = 31.23,
    val longitude: Double = 121.47,
    val enabled: Boolean = true,
    val fallbackMode: FallbackMode = FallbackMode.EARLIEST_TIME
) {
    companion object {
        const val DEFAULT_ID = 1L
    }
}

enum class FallbackMode {
    EARLIEST_TIME
}

data class WeatherDecision(
    val profileId: Long,
    val checkedAt: LocalDateTime,
    val targetDate: LocalDate,
    val isRainExpected: Boolean,
    val source: String,
    val confidence: Float,
    val rawWeatherCode: String,
    val message: String
)

data class LocationQuery(
    val cityId: String,
    val latitude: Double,
    val longitude: Double
)

data class HourlyWeather(
    val time: LocalDateTime,
    val text: String,
    val iconCode: String,
    val precipMm: Double,
    val probabilityOfPrecipitation: Int?
)

data class WeatherForecast(
    val source: String,
    val hourly: List<HourlyWeather>
)
