package cn.codex.weatheralarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.codex.weatheralarm.domain.AlarmProfile
import cn.codex.weatheralarm.domain.FallbackMode
import java.time.LocalTime

@Entity(tableName = "alarm_profiles")
data class AlarmProfileEntity(
    @PrimaryKey val id: Long,
    val label: String,
    val rainMinuteOfDay: Int,
    val normalMinuteOfDay: Int,
    val deadlineMinuteOfDay: Int,
    val enabledDaysMask: Int,
    val cityName: String,
    val cityId: String,
    val latitude: Double,
    val longitude: Double,
    val enabled: Boolean,
    val fallbackMode: String
)

fun AlarmProfileEntity.toDomain(): AlarmProfile = AlarmProfile(
    id = id,
    label = label,
    rainTime = rainMinuteOfDay.toLocalTime(),
    normalTime = normalMinuteOfDay.toLocalTime(),
    deadlineTime = deadlineMinuteOfDay.toLocalTime(),
    enabledDays = maskToDays(enabledDaysMask),
    cityName = cityName,
    cityId = cityId,
    latitude = latitude,
    longitude = longitude,
    enabled = enabled,
    fallbackMode = FallbackMode.valueOf(fallbackMode)
)

fun AlarmProfile.toEntity(): AlarmProfileEntity = AlarmProfileEntity(
    id = id,
    label = label,
    rainMinuteOfDay = rainTime.toMinuteOfDay(),
    normalMinuteOfDay = normalTime.toMinuteOfDay(),
    deadlineMinuteOfDay = deadlineTime.toMinuteOfDay(),
    enabledDaysMask = daysToMask(enabledDays),
    cityName = cityName,
    cityId = cityId,
    latitude = latitude,
    longitude = longitude,
    enabled = enabled,
    fallbackMode = fallbackMode.name
)

fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)

private fun daysToMask(days: Set<Int>): Int = days.fold(0) { mask, day -> mask or (1 shl day) }

private fun maskToDays(mask: Int): Set<Int> = (1..7).filter { day -> mask and (1 shl day) != 0 }.toSet()
