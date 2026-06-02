package cn.codex.weatheralarm

import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cn.codex.weatheralarm.alarm.AlarmScheduler
import cn.codex.weatheralarm.alarm.AlarmGuardService
import cn.codex.weatheralarm.alarm.AlarmTimes
import cn.codex.weatheralarm.alarm.WeatherCheckReceiver
import cn.codex.weatheralarm.alarm.WeatherCheckWorker
import cn.codex.weatheralarm.data.AlarmRepository
import cn.codex.weatheralarm.domain.AlarmProfile
import cn.codex.weatheralarm.domain.WeatherDecision
import cn.codex.weatheralarm.location.LocationResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

data class MainUiState(
    val profile: AlarmProfile = AlarmProfile(),
    val latestDecision: WeatherDecision? = null,
    val nextWeatherCheckAt: LocalDateTime = AlarmTimes.nextWeatherCheckAt(profile),
    val fallbackAlarmAt: LocalDateTime = AlarmTimes.fallbackAlarmAt(profile),
    val canScheduleExactAlarms: Boolean = true,
    val qWeatherConfigured: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasBackgroundLocationPermission: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val refreshStatus: String? = null,
    val testAlarmStatus: String? = null
)

class MainViewModel(
    private val container: AppContainer,
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val locationResolver: LocationResolver,
    private val qWeatherConfigured: Boolean
) : ViewModel() {
    private val permissionRefresh = MutableStateFlow(0)
    private val refreshStatus = MutableStateFlow<String?>(null)
    private val testAlarmStatus = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeProfile(),
        repository.observeLatestDecision(),
        permissionRefresh,
        refreshStatus,
        testAlarmStatus
    ) { profile, decision, _, refresh, testAlarm ->
        MainUiState(
            profile = profile,
            latestDecision = decision,
            nextWeatherCheckAt = AlarmTimes.nextWeatherCheckAt(profile),
            fallbackAlarmAt = AlarmTimes.fallbackAlarmAt(profile),
            canScheduleExactAlarms = scheduler.canScheduleExactAlarms(),
            qWeatherConfigured = qWeatherConfigured,
            hasLocationPermission = locationResolver.hasForegroundPermission(),
            hasBackgroundLocationPermission = locationResolver.hasBackgroundPermission(),
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(),
            refreshStatus = refresh,
            testAlarmStatus = testAlarm
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile()
            val profile = repository.getProfile()
            if (profile.enabled) {
                scheduler.scheduleWeatherCheck(profile)
                val fallbackAt = scheduler.scheduleFallbackAlarm(profile)
                val deadlineAt = scheduler.scheduleDeadlineAlarm(profile)
                scheduler.scheduleAwakeChecks(profile, fallbackAt, deadlineAt)
                AlarmGuardService.start(container.appContext)
            } else {
                scheduler.cancelAll(profile.id)
                AlarmGuardService.stop(container.appContext)
            }
        }
    }

    fun updateRainTime(time: LocalTime) = update { it.copy(rainTime = time) }

    fun updateNormalTime(time: LocalTime) = update { it.copy(normalTime = time) }

    fun updateDeadlineTime(time: LocalTime) = update { it.copy(deadlineTime = time) }

    fun updateEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun updateDay(day: Int, enabled: Boolean) = update {
        val days = if (enabled) it.enabledDays + day else it.enabledDays - day
        it.copy(enabledDays = days.ifEmpty { setOf(day) })
    }

    fun refreshPermissions() {
        permissionRefresh.update { it + 1 }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = container.appContext.getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(container.appContext.packageName)
    }

    fun refreshWeatherNow() {
        viewModelScope.launch {
            val profile = repository.getProfile()
            refreshStatus.value = "正在更新天气..."
            val request = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
                .setInputData(workDataOf(WeatherCheckReceiver.EXTRA_PROFILE_ID to profile.id))
                .build()
            WorkManager.getInstance(container.appContext).enqueueUniqueWork(
                "weather-check-now-${profile.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
            refreshStatus.value = "已开始更新，稍后查看今日状态"
        }
    }

    fun scheduleTestAlarm() {
        viewModelScope.launch {
            val alarmAt = scheduler.scheduleTestAlarm(repository.getProfile().id)
            testAlarmStatus.value = "测试闹钟已设置：${alarmAt.toLocalTime()}"
            AlarmGuardService.start(container.appContext)
        }
    }

    private fun update(block: (AlarmProfile) -> AlarmProfile) {
        viewModelScope.launch {
            val updated = block(repository.getProfile())
            repository.saveProfile(updated)
            if (updated.enabled) {
                scheduler.scheduleWeatherCheck(updated)
                val fallbackAt = scheduler.scheduleFallbackAlarm(updated)
                val deadlineAt = scheduler.scheduleDeadlineAlarm(updated)
                scheduler.scheduleAwakeChecks(updated, fallbackAt, deadlineAt)
                AlarmGuardService.start(container.appContext)
            } else {
                scheduler.cancelAll(updated.id)
                AlarmGuardService.stop(container.appContext)
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                container = container,
                repository = container.repository,
                scheduler = container.scheduler,
                locationResolver = container.locationResolver,
                qWeatherConfigured = BuildConfig.QWEATHER_API_KEY.isNotBlank() &&
                    BuildConfig.QWEATHER_API_HOST.isNotBlank()
            ) as T
        }
    }
}
