package cn.codex.weatheralarm.weather

import cn.codex.weatheralarm.domain.LocationQuery
import cn.codex.weatheralarm.domain.WeatherForecast
import java.time.LocalDateTime

interface WeatherProvider {
    suspend fun getHourlyForecast(
        location: LocationQuery,
        from: LocalDateTime,
        to: LocalDateTime
    ): WeatherForecast
}
