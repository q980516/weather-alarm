package cn.codex.weatheralarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import cn.codex.weatheralarm.MainActivity
import cn.codex.weatheralarm.R
import cn.codex.weatheralarm.domain.AlarmProfile
import cn.codex.weatheralarm.domain.WeatherDecision
import java.time.format.DateTimeFormatter

object AlarmNotification {
    const val RINGING_NOTIFICATION_ID = 5100
    const val GUARD_NOTIFICATION_ID = 5102
    private const val WEATHER_NOTIFICATION_ID = 5101
    private const val AWAKE_CHECK_NOTIFICATION_BASE = 5200
    private const val ALARM_CHANNEL_ID = "alarm_ringing"
    private const val WEATHER_CHANNEL_ID = "weather_decisions"
    private const val GUARD_CHANNEL_ID = "alarm_guard"
    private const val AWAKE_CHANNEL_ID = "awake_checks"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(ALARM_CHANNEL_ID, "闹钟响铃", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(WEATHER_CHANNEL_ID, "天气判断", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel(GUARD_CHANNEL_ID, "闹钟守护", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(AWAKE_CHANNEL_ID, "起床确认", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    fun ringingNotification(context: Context, mode: String = AlarmFireReceiver.MODE_NORMAL): Notification {
        val isDeadline = mode == AlarmFireReceiver.MODE_DEADLINE
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            6200,
            Intent(context, AlarmRingingActivity::class.java).putExtra("mode", mode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            context,
            6202,
            AlarmRingingService.stopIntent(context, confirmAwake = false),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val awakeIntent = PendingIntent.getService(
            context,
            6204,
            AlarmRingingService.stopIntent(context, confirmAwake = true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather_alarm)
            .setContentTitle(if (isDeadline) "最终死线闹钟" else "雨晴闹钟")
            .setContentText(if (isDeadline) "现在必须起床了" else "该起床啦")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, "停止", stopIntent)
            .addAction(0, "我已起床", awakeIntent)
            .build()
    }

    fun showRinging(context: Context, profileId: Long, mode: String = AlarmFireReceiver.MODE_NORMAL) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(RINGING_NOTIFICATION_ID, ringingNotification(context, mode))
    }

    fun guardNotification(context: Context, profile: AlarmProfile? = null): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            6203,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (profile == null) {
            "正在守护天气闹钟"
        } else {
            val checkAt = AlarmTimes.nextWeatherCheckAt(profile).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            val fallbackAt = AlarmTimes.fallbackAlarmAt(profile).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            "天气检查 $checkAt，兜底 $fallbackAt"
        }
        return NotificationCompat.Builder(context, GUARD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather_alarm)
            .setContentTitle("雨晴闹钟守护中")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    fun showGuard(context: Context, profile: AlarmProfile) {
        context.getSystemService(NotificationManager::class.java)
            .notify(GUARD_NOTIFICATION_ID, guardNotification(context, profile))
    }

    fun showWeatherDecision(context: Context, decision: WeatherDecision, fallback: Boolean) {
        val title = if (fallback) "天气未更新" else "天气闹钟已调整"
        showWeatherMessage(context, title, decision.message)
    }

    fun showFallback(context: Context, detail: String) {
        val message = if (detail.isBlank()) "已使用较晚时间兜底" else "已使用较晚时间兜底：$detail"
        showWeatherMessage(context, "天气未更新", message)
    }

    fun showAwakeCheck(context: Context, profileId: Long, attempt: Int) {
        val confirmIntent = PendingIntent.getBroadcast(
            context,
            6300 + attempt,
            AwakeConfirmReceiver.intent(context, profileId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, AWAKE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather_alarm)
            .setContentTitle("已经起床了吗？")
            .setContentText("第 $attempt 次确认。点这里表示你已经起来了。")
            .setContentIntent(confirmIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .addAction(0, "我已起床", confirmIntent)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(AWAKE_CHECK_NOTIFICATION_BASE + attempt, notification)
    }

    fun clearAwakeChecks(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        (1..3).forEach { attempt ->
            manager.cancel(AWAKE_CHECK_NOTIFICATION_BASE + attempt)
        }
    }

    private fun showWeatherMessage(context: Context, title: String, message: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            6201,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, WEATHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(WEATHER_NOTIFICATION_ID, notification)
    }
}
