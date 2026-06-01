package cn.codex.weatheralarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.codex.weatheralarm.MainActivity

class AwakeConfirmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 1L)
        AlarmScheduler(context).cancelDeadlineAlarm(profileId)
        AlarmScheduler(context).cancelAwakeChecks(profileId)
        AlarmNotification.clearAwakeChecks(context)
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    companion object {
        private const val EXTRA_PROFILE_ID = "profileId"

        fun intent(context: Context, profileId: Long): Intent =
            Intent(context, AwakeConfirmReceiver::class.java).putExtra(EXTRA_PROFILE_ID, profileId)
    }
}
