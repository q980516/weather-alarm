package cn.codex.weatheralarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import cn.codex.weatheralarm.MainActivity
import cn.codex.weatheralarm.domain.AlarmProfile
import java.time.LocalDateTime
import kotlin.random.Random

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
    }

    fun scheduleWeatherCheck(profile: AlarmProfile) {
        if (!profile.enabled) return
        val checkAt = AlarmTimes.nextWeatherCheckAt(profile)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WEATHER_CHECK_REQUEST_CODE,
            WeatherCheckReceiver.intent(context, profile.id),
            pendingIntentFlags()
        )
        scheduleExact(AlarmTimes.toEpochMillis(checkAt), pendingIntent, asAlarmClock = false)
    }

    fun scheduleActualAlarm(profile: AlarmProfile, useRainTime: Boolean): LocalDateTime {
        val alarmAt = AlarmTimes.nextActualAlarmAt(profile, useRainTime)
        scheduleAlarmAt(alarmAt, profile.id, AlarmFireReceiver.MODE_NORMAL, ACTUAL_ALARM_REQUEST_CODE)
        return alarmAt
    }

    fun scheduleFallbackAlarm(profile: AlarmProfile): LocalDateTime {
        val alarmAt = AlarmTimes.fallbackAlarmAt(profile)
        scheduleAlarmAt(alarmAt, profile.id, AlarmFireReceiver.MODE_NORMAL, ACTUAL_ALARM_REQUEST_CODE)
        return alarmAt
    }

    fun scheduleTestAlarm(profileId: Long = AlarmProfile.DEFAULT_ID): LocalDateTime {
        val alarmAt = LocalDateTime.now().plusMinutes(1)
        scheduleAlarmAt(alarmAt, profileId, AlarmFireReceiver.MODE_TEST, ACTUAL_ALARM_REQUEST_CODE)
        return alarmAt
    }

    fun scheduleDeadlineAlarm(profile: AlarmProfile): LocalDateTime {
        val alarmAt = AlarmTimes.nextDeadlineAlarmAt(profile)
        scheduleAlarmAt(alarmAt, profile.id, AlarmFireReceiver.MODE_DEADLINE, DEADLINE_ALARM_REQUEST_CODE)
        return alarmAt
    }

    fun scheduleAwakeChecks(profile: AlarmProfile, actualAlarmAt: LocalDateTime, deadlineAt: LocalDateTime) {
        cancelAwakeChecks(profile.id)
        val startMillis = AlarmTimes.toEpochMillis(actualAlarmAt.plusSeconds(30))
        val endMillis = AlarmTimes.toEpochMillis(deadlineAt.minusSeconds(30))
        if (endMillis <= startMillis) return

        val times = (1..AWAKE_CHECK_COUNT)
            .map { Random.nextLong(startMillis, endMillis + 1) }
            .sorted()
        val deadlineMillis = AlarmTimes.toEpochMillis(deadlineAt)
        times.forEachIndexed { index, triggerAtMillis ->
            val attempt = index + 1
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                AWAKE_CHECK_REQUEST_BASE + attempt,
                AwakeCheckReceiver.intent(context, profile.id, attempt, deadlineMillis),
                pendingIntentFlags()
            )
            scheduleExact(triggerAtMillis, pendingIntent, asAlarmClock = false)
        }
    }

    fun scheduleAwakeChecksAfterStop(profile: AlarmProfile) {
        val now = LocalDateTime.now()
        val deadlineAt = AlarmTimes.nextDeadlineAlarmAt(profile, now)
        scheduleAwakeChecks(profile, now, deadlineAt)
    }

    fun cancelDeadlineAlarm(profileId: Long = AlarmProfile.DEFAULT_ID) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                DEADLINE_ALARM_REQUEST_CODE,
                AlarmFireReceiver.intent(context, profileId, AlarmFireReceiver.MODE_DEADLINE),
                pendingIntentFlags()
            )
        )
    }

    fun cancelAwakeChecks(profileId: Long = AlarmProfile.DEFAULT_ID) {
        (1..AWAKE_CHECK_COUNT).forEach { attempt ->
            alarmManager.cancel(
                PendingIntent.getBroadcast(
                    context,
                    AWAKE_CHECK_REQUEST_BASE + attempt,
                    AwakeCheckReceiver.intent(context, profileId, attempt, 0L),
                    pendingIntentFlags()
                )
            )
        }
    }

    private fun scheduleAlarmAt(alarmAt: LocalDateTime, profileId: Long, mode: String, requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            AlarmFireReceiver.intent(context, profileId, mode),
            pendingIntentFlags()
        )
        scheduleExact(AlarmTimes.toEpochMillis(alarmAt), pendingIntent, asAlarmClock = true)
    }

    fun cancelAll(profileId: Long = AlarmProfile.DEFAULT_ID) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                WEATHER_CHECK_REQUEST_CODE,
                WeatherCheckReceiver.intent(context, profileId),
                pendingIntentFlags()
            )
        )
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                ACTUAL_ALARM_REQUEST_CODE,
                AlarmFireReceiver.intent(context, profileId, AlarmFireReceiver.MODE_NORMAL),
                pendingIntentFlags()
            )
        )
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                DEADLINE_ALARM_REQUEST_CODE,
                AlarmFireReceiver.intent(context, profileId, AlarmFireReceiver.MODE_DEADLINE),
                pendingIntentFlags()
            )
        )
        cancelAwakeChecks(profileId)
        AlarmNotification.clearAwakeChecks(context)
    }

    private fun scheduleExact(triggerAtMillis: Long, pendingIntent: PendingIntent, asAlarmClock: Boolean) {
        if (asAlarmClock && canScheduleExactAlarms()) {
            val showIntent = PendingIntent.getActivity(
                context,
                SHOW_ALARM_REQUEST_CODE,
                Intent(context, MainActivity::class.java),
                pendingIntentFlags()
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                pendingIntent
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    companion object {
        private const val WEATHER_CHECK_REQUEST_CODE = 2001
        private const val ACTUAL_ALARM_REQUEST_CODE = 2002
        private const val SHOW_ALARM_REQUEST_CODE = 2003
        private const val DEADLINE_ALARM_REQUEST_CODE = 2004
        private const val AWAKE_CHECK_REQUEST_BASE = 2100
        private const val AWAKE_CHECK_COUNT = 3
    }
}
