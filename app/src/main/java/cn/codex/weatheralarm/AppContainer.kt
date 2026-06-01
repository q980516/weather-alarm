package cn.codex.weatheralarm

import android.content.Context
import cn.codex.weatheralarm.alarm.AlarmScheduler
import cn.codex.weatheralarm.data.AppDatabase
import cn.codex.weatheralarm.data.AlarmRepository
import cn.codex.weatheralarm.location.LocationResolver
import cn.codex.weatheralarm.weather.QWeatherProvider

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database = AppDatabase.create(appContext)
    val weatherProvider = QWeatherProvider(
        apiKey = BuildConfig.QWEATHER_API_KEY,
        apiHost = BuildConfig.QWEATHER_API_HOST
    )
    val locationResolver = LocationResolver(appContext)
    val repository = AlarmRepository(database.alarmDao(), database.weatherDecisionDao())
    val scheduler = AlarmScheduler(appContext)
}
