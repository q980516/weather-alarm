package cn.codex.weatheralarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

class AwakeCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 1L)
        val attempt = intent.getIntExtra(EXTRA_ATTEMPT, 1)
        val deadlineMillis = intent.getLongExtra(EXTRA_DEADLINE_MILLIS, 0L)
        val nowMillis = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (deadlineMillis == 0L || nowMillis < deadlineMillis) {
            AlarmNotification.showAwakeCheck(context, profileId, attempt)
        }
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profileId"
        const val EXTRA_ATTEMPT = "attempt"
        const val EXTRA_DEADLINE_MILLIS = "deadlineMillis"

        fun intent(context: Context, profileId: Long, attempt: Int, deadlineMillis: Long): Intent =
            Intent(context, AwakeCheckReceiver::class.java)
                .putExtra(EXTRA_PROFILE_ID, profileId)
                .putExtra(EXTRA_ATTEMPT, attempt)
                .putExtra(EXTRA_DEADLINE_MILLIS, deadlineMillis)
    }
}
