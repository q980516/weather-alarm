package cn.codex.weatheralarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.codex.weatheralarm.domain.WeatherDecision
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "weather_decisions")
data class WeatherDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val checkedAt: String,
    val targetDate: String,
    val isRainExpected: Boolean,
    val source: String,
    val confidence: Float,
    val rawWeatherCode: String,
    val message: String
)

fun WeatherDecisionEntity.toDomain(): WeatherDecision = WeatherDecision(
    profileId = profileId,
    checkedAt = LocalDateTime.parse(checkedAt),
    targetDate = LocalDate.parse(targetDate),
    isRainExpected = isRainExpected,
    source = source,
    confidence = confidence,
    rawWeatherCode = rawWeatherCode,
    message = message
)

fun WeatherDecision.toEntity(): WeatherDecisionEntity = WeatherDecisionEntity(
    profileId = profileId,
    checkedAt = checkedAt.toString(),
    targetDate = targetDate.toString(),
    isRainExpected = isRainExpected,
    source = source,
    confidence = confidence,
    rawWeatherCode = rawWeatherCode,
    message = message
)
