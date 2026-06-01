package cn.codex.weatheralarm

import android.app.Application
import cn.codex.weatheralarm.alarm.AlarmNotification

class WeatherAlarmApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        AlarmNotification.createChannels(this)
    }
}
