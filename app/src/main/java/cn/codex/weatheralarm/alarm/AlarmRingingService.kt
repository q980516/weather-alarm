package cn.codex.weatheralarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import cn.codex.weatheralarm.WeatherAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var currentMode: String = AlarmFireReceiver.MODE_NORMAL

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val confirmAwake = intent.getBooleanExtra(EXTRA_CONFIRM_AWAKE, false)
            stopAlarm()
            scheduleNext(confirmAwake)
            stopSelf()
            return START_NOT_STICKY
        }
        currentMode = intent?.getStringExtra(EXTRA_MODE) ?: AlarmFireReceiver.MODE_NORMAL
        startForeground(AlarmNotification.RINGING_NOTIFICATION_ID, AlarmNotification.ringingNotification(this, currentMode))
        playAlarm()
        return START_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun playAlarm() {
        if (ringtone?.isPlaying == true) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri).apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        val pattern = if (currentMode == AlarmFireReceiver.MODE_DEADLINE) {
            longArrayOf(0, 900, 120, 900, 120, 1200)
        } else {
            longArrayOf(0, 600, 400, 600)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopAlarm() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun scheduleNext(confirmAwake: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val app = applicationContext as WeatherAlarmApp
            app.container.repository.ensureDefaultProfile()
            val profile = app.container.repository.getProfile()
            if (profile.enabled) {
                app.container.scheduler.scheduleWeatherCheck(profile)
                val fallbackAt = app.container.scheduler.scheduleFallbackAlarm(profile)
                if (confirmAwake || currentMode == AlarmFireReceiver.MODE_DEADLINE) {
                    app.container.scheduler.cancelDeadlineAlarm(profile.id)
                    app.container.scheduler.cancelAwakeChecks(profile.id)
                    AlarmNotification.clearAwakeChecks(applicationContext)
                } else {
                    app.container.scheduler.scheduleDeadlineAlarm(profile)
                    app.container.scheduler.scheduleAwakeChecksAfterStop(profile)
                }
                AlarmNotification.showGuard(applicationContext, profile)
                AlarmGuardService.start(applicationContext)
            }
        }
    }

    companion object {
        private const val ACTION_STOP = "cn.codex.weatheralarm.action.STOP_ALARM"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_CONFIRM_AWAKE = "confirmAwake"

        fun intent(context: Context, profileId: Long, mode: String = AlarmFireReceiver.MODE_NORMAL): Intent =
            Intent(context, AlarmRingingService::class.java)
                .putExtra("profileId", profileId)
                .putExtra(EXTRA_MODE, mode)

        fun stopIntent(context: Context, confirmAwake: Boolean = false): Intent =
            Intent(context, AlarmRingingService::class.java)
                .setAction(ACTION_STOP)
                .putExtra(EXTRA_CONFIRM_AWAKE, confirmAwake)
    }
}
