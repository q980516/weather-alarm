package cn.codex.weatheralarm.alarm

import cn.codex.weatheralarm.domain.AlarmProfile
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AlarmTimesTest {
    @Test
    fun weatherCheckIsTenMinutesBeforeEarlierAlarm() {
        val monday = LocalDate.of(2026, 6, 1)
        val profile = AlarmProfile(
            rainTime = LocalTime.of(7, 0),
            normalTime = LocalTime.of(7, 30),
            enabledDays = setOf(1)
        )

        val checkAt = AlarmTimes.nextWeatherCheckAt(
            profile,
            now = LocalDateTime.of(monday, LocalTime.of(1, 0))
        )

        assertEquals(LocalDateTime.of(monday, LocalTime.of(6, 50)), checkAt)
    }

    @Test
    fun fallbackUsesLaterOfRainAndNormalTimes() {
        val monday = LocalDate.of(2026, 6, 1)
        val profile = AlarmProfile(
            rainTime = LocalTime.of(7, 30),
            normalTime = LocalTime.of(7, 0),
            enabledDays = setOf(1)
        )

        val fallbackAt = AlarmTimes.fallbackAlarmAt(
            profile,
            now = LocalDateTime.of(monday, LocalTime.of(1, 0))
        )

        assertEquals(LocalDateTime.of(monday, LocalTime.of(7, 30)), fallbackAt)
    }

    @Test
    fun pastAlarmRollsToNextEnabledDay() {
        val monday = LocalDate.of(2026, 6, 1)
        val profile = AlarmProfile(
            rainTime = LocalTime.of(7, 0),
            normalTime = LocalTime.of(7, 30),
            enabledDays = setOf(1, 2)
        )

        val alarmAt = AlarmTimes.nextActualAlarmAt(
            profile,
            useRainTime = true,
            now = LocalDateTime.of(monday, LocalTime.of(8, 0))
        )

        assertEquals(LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)), alarmAt)
    }
}
