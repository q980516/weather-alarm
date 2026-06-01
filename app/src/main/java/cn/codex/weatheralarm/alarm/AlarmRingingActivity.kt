package cn.codex.weatheralarm.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val mode = intent?.getStringExtra("mode") ?: AlarmFireReceiver.MODE_NORMAL
        setContent {
            RingingScreen(mode = mode, onStop = {
                startService(AlarmRingingService.stopIntent(this, confirmAwake = false))
                finish()
            }, onAwake = {
                startService(AlarmRingingService.stopIntent(this, confirmAwake = true))
                finish()
            })
        }
    }
}

@Composable
private fun RingingScreen(mode: String, onStop: () -> Unit, onAwake: () -> Unit) {
    val isDeadline = mode == AlarmFireReceiver.MODE_DEADLINE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF7))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (isDeadline) "最终死线" else "早安", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
        Text(
            if (isDeadline) "现在必须起床了" else "雨晴闹钟正在响铃",
            modifier = Modifier.padding(top = 12.dp),
            color = Color(0xFF4F6367)
        )
        Button(modifier = Modifier.padding(top = 40.dp), onClick = onStop) {
            Text("停止", style = MaterialTheme.typography.titleMedium)
        }
        Button(modifier = Modifier.padding(top = 12.dp), onClick = onAwake) {
            Text("我已起床", style = MaterialTheme.typography.titleMedium)
        }
    }
}
