package cn.codex.weatheralarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.codex.weatheralarm.WeatherAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as WeatherAlarmApp
                app.container.repository.ensureDefaultProfile()
                app.container.repository.getEnabledProfiles().forEach {
                    app.container.scheduler.scheduleWeatherCheck(it)
                    val fallbackAt = app.container.scheduler.scheduleFallbackAlarm(it)
                    val deadlineAt = app.container.scheduler.scheduleDeadlineAlarm(it)
                    app.container.scheduler.scheduleAwakeChecks(it, fallbackAt, deadlineAt)
                    AlarmGuardService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
