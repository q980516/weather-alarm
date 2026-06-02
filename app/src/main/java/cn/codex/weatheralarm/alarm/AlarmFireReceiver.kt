package cn.codex.weatheralarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import cn.codex.weatheralarm.WeatherAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmFireReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 1L)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NORMAL
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as WeatherAlarmApp
                val profile = app.container.repository.getProfile(profileId)
                if (!profile.enabled && mode != MODE_TEST) {
                    app.container.scheduler.cancelAll(profileId)
                    return@launch
                }
                val serviceIntent = AlarmRingingService.intent(context, profileId, mode)
                ContextCompat.startForegroundService(context, serviceIntent)
                AlarmNotification.showRinging(context, profileId, mode)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val MODE_NORMAL = "normal"
        const val MODE_DEADLINE = "deadline"
        const val MODE_TEST = "test"
        private const val EXTRA_PROFILE_ID = "profileId"
        private const val EXTRA_MODE = "mode"

        fun intent(context: Context, profileId: Long, mode: String = MODE_NORMAL): Intent =
            Intent(context, AlarmFireReceiver::class.java)
                .putExtra(EXTRA_PROFILE_ID, profileId)
                .putExtra(EXTRA_MODE, mode)
    }
}
