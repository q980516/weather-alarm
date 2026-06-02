package cn.codex.weatheralarm.alarm

import cn.codex.weatheralarm.domain.AlarmProfile
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AlarmTimes {
    private const val WEATHER_CHECK_LEAD_MINUTES = 10L

    fun nextWeatherCheckAt(profile: AlarmProfile, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val earlier = minOf(profile.rainTime, profile.normalTime)
        return nextEnabledDateTime(profile, earlier, now).minusMinutes(WEATHER_CHECK_LEAD_MINUTES).let {
            if (it.isAfter(now)) it else nextEnabledDateTime(profile, earlier, now.plusDays(1)).minusMinutes(WEATHER_CHECK_LEAD_MINUTES)
        }
    }

    fun nextActualAlarmAt(
        profile: AlarmProfile,
        useRainTime: Boolean,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        val time = if (useRainTime) profile.rainTime else profile.normalTime
        return nextEnabledDateTime(profile, time, now)
    }

    fun fallbackAlarmAt(profile: AlarmProfile, now: LocalDateTime = LocalDateTime.now()): LocalDateTime =
        nextEnabledDateTime(profile, maxOf(profile.rainTime, profile.normalTime), now)

    fun nextDeadlineAlarmAt(profile: AlarmProfile, now: LocalDateTime = LocalDateTime.now()): LocalDateTime =
        nextEnabledDateTime(profile, profile.deadlineTime, now)

    fun toEpochMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun nextEnabledDateTime(
        profile: AlarmProfile,
        time: LocalTime,
        now: LocalDateTime
    ): LocalDateTime {
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val candidate = LocalDateTime.of(date, time)
            val day = date.dayOfWeek.toAppDay()
            if (day in profile.enabledDays && candidate.isAfter(now.plus(Duration.ofSeconds(1)))) {
                return candidate
            }
        }
        return LocalDateTime.of(now.toLocalDate().plusDays(1), time)
    }

    private fun DayOfWeek.toAppDay(): Int = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }
}
