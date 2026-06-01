package cn.codex.weatheralarm.weather

import cn.codex.weatheralarm.domain.HourlyWeather
import cn.codex.weatheralarm.domain.WeatherForecast
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RainClassifierTest {
    @Test
    fun rainyIconChoosesRainAlarm() {
        val decision = RainClassifier.decide(
            profileId = 1,
            forecast = WeatherForecast(
                source = "test",
                hourly = listOf(
                    HourlyWeather(LocalDateTime.now(), "小雨", "305", 0.3, 70)
                )
            ),
            checkedAt = LocalDateTime.now(),
            targetDate = LocalDate.now()
        )
        assertTrue(decision.isRainExpected)
    }

    @Test
    fun clearForecastChoosesNormalAlarm() {
        val decision = RainClassifier.decide(
            profileId = 1,
            forecast = WeatherForecast(
                source = "test",
                hourly = listOf(
                    HourlyWeather(LocalDateTime.now(), "晴", "100", 0.0, 10)
                )
            ),
            checkedAt = LocalDateTime.now(),
            targetDate = LocalDate.now()
        )
        assertFalse(decision.isRainExpected)
    }
}
