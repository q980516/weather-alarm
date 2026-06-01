package cn.codex.weatheralarm.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.codex.weatheralarm.WeatherAlarmApp
import cn.codex.weatheralarm.domain.WeatherDecision
import cn.codex.weatheralarm.weather.RainClassifier
import java.time.LocalDateTime

class WeatherCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val profileId = inputData.getLong(WeatherCheckReceiver.EXTRA_PROFILE_ID, 1L)
        val app = applicationContext as WeatherAlarmApp
        val repository = app.container.repository
        val scheduler = app.container.scheduler
        val profile = repository.getProfile(profileId)
        if (!profile.enabled) return Result.success()

        val now = LocalDateTime.now()
        val checkWindowEnd = maxOf(profile.rainTime, profile.normalTime)
        val targetDateTime = AlarmTimes.fallbackAlarmAt(profile, now)
        val from = now.minusMinutes(5)
        val to = targetDateTime.toLocalDate().atTime(checkWindowEnd).plusMinutes(45)

        return try {
            val resolvedLocation = app.container.locationResolver.currentOrSaved(profile)
            if (
                resolvedLocation.latitude != profile.latitude ||
                resolvedLocation.longitude != profile.longitude ||
                profile.cityName != resolvedLocation.sourceLabel
            ) {
                repository.saveProfile(
                    profile.copy(
                        cityName = resolvedLocation.sourceLabel,
                        latitude = resolvedLocation.latitude,
                        longitude = resolvedLocation.longitude
                    )
                )
            }
            val forecast = app.container.weatherProvider.getHourlyForecast(
                location = resolvedLocation.query,
                from = from,
                to = to
            )
            val decision = RainClassifier.decide(profile.id, forecast, now, targetDateTime.toLocalDate())
            repository.saveDecision(decision)
            val actualAt = scheduler.scheduleActualAlarm(profile, decision.isRainExpected)
            val deadlineAt = scheduler.scheduleDeadlineAlarm(profile)
            scheduler.scheduleAwakeChecks(profile, actualAt, deadlineAt)
            scheduler.scheduleWeatherCheck(profile)
            AlarmNotification.showGuard(applicationContext, profile)
            AlarmNotification.showWeatherDecision(applicationContext, decision, fallback = false)
            Result.success()
        } catch (error: Exception) {
            val fallbackAt = scheduler.scheduleFallbackAlarm(profile)
            val deadlineAt = scheduler.scheduleDeadlineAlarm(profile)
            scheduler.scheduleAwakeChecks(profile, fallbackAt, deadlineAt)
            val detail = error.message.orEmpty().ifBlank { error::class.java.simpleName }
            repository.saveDecision(
                WeatherDecision(
                    profileId = profile.id,
                    checkedAt = now,
                    targetDate = fallbackAt.toLocalDate(),
                    isRainExpected = true,
                    source = "fallback",
                    confidence = 0.1f,
                    rawWeatherCode = "",
                    message = "天气未更新，已使用兜底闹钟：$detail"
                )
            )
            AlarmNotification.showFallback(applicationContext, detail)
            AlarmNotification.showGuard(applicationContext, profile)
            Result.success()
        }
    }
}
