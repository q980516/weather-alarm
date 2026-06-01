package cn.codex.weatheralarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class WeatherCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 1L)
        val request = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
            .setInputData(workDataOf(EXTRA_PROFILE_ID to profileId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "weather-check-$profileId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profileId"

        fun intent(context: Context, profileId: Long): Intent =
            Intent(context, WeatherCheckReceiver::class.java).putExtra(EXTRA_PROFILE_ID, profileId)
    }
}
