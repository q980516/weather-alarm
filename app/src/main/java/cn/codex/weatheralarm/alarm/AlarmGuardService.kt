package cn.codex.weatheralarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import cn.codex.weatheralarm.WeatherAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmGuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(AlarmNotification.GUARD_NOTIFICATION_ID, AlarmNotification.guardNotification(this))
        refreshSchedulesAndNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun refreshSchedulesAndNotification() {
        scope.launch {
            val app = applicationContext as WeatherAlarmApp
            app.container.repository.ensureDefaultProfile()
            val profile = app.container.repository.getProfile()
            if (!profile.enabled) {
                stopSelf()
                return@launch
            }
            app.container.scheduler.scheduleWeatherCheck(profile)
            val fallbackAt = app.container.scheduler.scheduleFallbackAlarm(profile)
            val deadlineAt = app.container.scheduler.scheduleDeadlineAlarm(profile)
            app.container.scheduler.scheduleAwakeChecks(profile, fallbackAt, deadlineAt)
            AlarmNotification.showGuard(applicationContext, profile)
        }
    }

    companion object {
        private const val ACTION_STOP = "cn.codex.weatheralarm.action.STOP_GUARD"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, AlarmGuardService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AlarmGuardService::class.java).setAction(ACTION_STOP))
        }
    }
}
