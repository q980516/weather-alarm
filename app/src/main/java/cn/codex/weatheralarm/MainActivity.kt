package cn.codex.weatheralarm

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.codex.weatheralarm.alarm.AlarmScheduler
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as WeatherAlarmApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAlarmTheme {
                val state by viewModel.uiState.collectAsState()
                PermissionRequest(onResult = viewModel::refreshPermissions)
                WeatherAlarmScreen(
                    state = state,
                    onRainTime = viewModel::updateRainTime,
                    onNormalTime = viewModel::updateNormalTime,
                    onDeadlineTime = viewModel::updateDeadlineTime,
                    onEnabled = viewModel::updateEnabled,
                    onDay = viewModel::updateDay,
                    onRefreshWeather = viewModel::refreshWeatherNow,
                    onTestAlarm = viewModel::scheduleTestAlarm,
                    onOpenExactAlarmSettings = {
                        startActivity(AlarmScheduler(this).exactAlarmSettingsIntent())
                    },
                    onOpenBatterySettings = {
                        openBatteryOptimizationSettings()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    private fun openBatteryOptimizationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        startActivity(intent)
    }
}

@Composable
private fun PermissionRequest(onResult: () -> Unit) {
    val permissions = buildList {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onResult()
    }
    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }
}

@Composable
private fun WeatherAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF48A078),
            secondary = Color(0xFF4D9BC4),
            tertiary = Color(0xFFFFC64A),
            surface = Color(0xFFFFFDF7),
            background = Color(0xFFFFFDF7),
            onSurface = Color(0xFF263238)
        ),
        content = content
    )
}

@Composable
private fun WeatherAlarmScreen(
    state: MainUiState,
    onRainTime: (LocalTime) -> Unit,
    onNormalTime: (LocalTime) -> Unit,
    onDeadlineTime: (LocalTime) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onDay: (Int, Boolean) -> Unit,
    onRefreshWeather: () -> Unit,
    onTestAlarm: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFFDF7)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header()
            StatusCard(state, onEnabled, onOpenExactAlarmSettings, onOpenBatterySettings, onTestAlarm)
            TimeCards(state, onRainTime, onNormalTime, onDeadlineTime)
            WeekdaySelector(state, onDay)
            LocationCard(state)
            DecisionCard(state, onRefreshWeather)
            CreatorSignature()
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        AppIconPreview()
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "雨晴闹钟",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "根据当前位置天气自动选择闹钟",
                color = Color(0xFF5C6F73),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppIconPreview() {
    Canvas(
        modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .background(Color(0xFF88D8B0))
    ) {
        drawCircle(Color.White, radius = size.minDimension * 0.27f, center = Offset(size.width * 0.48f, size.height * 0.47f))
        drawCircle(Color.White, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.34f, size.height * 0.51f))
        drawCircle(Color.White, radius = size.minDimension * 0.16f, center = Offset(size.width * 0.62f, size.height * 0.52f))
        drawCircle(Color(0xFFFFC64A), radius = size.minDimension * 0.13f, center = Offset(size.width * 0.70f, size.height * 0.28f))
        drawCircle(Color(0xFF263238), radius = size.minDimension * 0.23f, center = Offset(size.width * 0.50f, size.height * 0.64f), style = Stroke(width = 4.dp.toPx()))
        drawLine(Color(0xFF263238), Offset(size.width * 0.50f, size.height * 0.64f), Offset(size.width * 0.50f, size.height * 0.54f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF263238), Offset(size.width * 0.50f, size.height * 0.64f), Offset(size.width * 0.60f, size.height * 0.68f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF5BA7D1), Offset(size.width * 0.35f, size.height * 0.82f), Offset(size.width * 0.31f, size.height * 0.91f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF5BA7D1), Offset(size.width * 0.64f, size.height * 0.82f), Offset(size.width * 0.60f, size.height * 0.91f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun StatusCard(
    state: MainUiState,
    onEnabled: (Boolean) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onTestAlarm: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("总开关", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Switch(checked = state.profile.enabled, onCheckedChange = onEnabled)
            }
            InfoLine("下次自动检查", state.nextWeatherCheckAt.formatUi())
            InfoLine("兜底闹钟", state.fallbackAlarmAt.formatUi())
            InfoLine("精确闹钟", if (state.canScheduleExactAlarms) "已开启" else "未开启")
            InfoLine("定位权限", if (state.hasLocationPermission) "已开启" else "未开启")
            InfoLine("后台定位", if (state.hasBackgroundLocationPermission) "已开启" else "将用最近坐标")
            InfoLine("后台守护", if (state.isBatteryOptimizationIgnored) "已允许" else "建议开启")
            if (!state.canScheduleExactAlarms) {
                Button(onClick = onOpenExactAlarmSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("开启精确闹钟权限")
                }
            }
            if (!state.isBatteryOptimizationIgnored) {
                OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                    Text("允许后台守护")
                }
            }
            OutlinedButton(onClick = onTestAlarm, modifier = Modifier.fillMaxWidth()) {
                Text("测试闹钟 1 分钟后")
            }
            state.testAlarmStatus?.let { Text(it, color = Color(0xFF3F7D5A), fontSize = 13.sp) }
            if (!state.qWeatherConfigured) {
                Text(
                    "尚未配置 QWeather Key 或 API Host，会使用较晚时间兜底。",
                    color = Color(0xFFB36B00),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TimeCards(
    state: MainUiState,
    onRainTime: (LocalTime) -> Unit,
    onNormalTime: (LocalTime) -> Unit,
    onDeadlineTime: (LocalTime) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = this.maxWidth
        if (availableWidth < 360.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TimeCard(Modifier.fillMaxWidth(), "下雨", state.profile.rainTime, Color(0xFF5BA7D1), compact = true, onTime = onRainTime)
                TimeCard(Modifier.fillMaxWidth(), "不下雨", state.profile.normalTime, Color(0xFFFFC64A), compact = true, onTime = onNormalTime)
                TimeCard(Modifier.fillMaxWidth(), "最终死线", state.profile.deadlineTime, Color(0xFFFF6B6B), compact = true, onTime = onDeadlineTime)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    TimeCard(Modifier.weight(1f), "下雨", state.profile.rainTime, Color(0xFF5BA7D1), compact = availableWidth < 420.dp, onTime = onRainTime)
                    TimeCard(Modifier.weight(1f), "不下雨", state.profile.normalTime, Color(0xFFFFC64A), compact = availableWidth < 420.dp, onTime = onNormalTime)
                }
                TimeCard(Modifier.fillMaxWidth(), "最终死线", state.profile.deadlineTime, Color(0xFFFF6B6B), compact = true, onTime = onDeadlineTime)
            }
        }
    }
    Text(
        "普通闹钟后到最终死线前，会随机发 3 次起床确认通知；点任意一次就取消死线。",
        color = Color(0xFF7C8A8D),
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun TimeCard(
    modifier: Modifier,
    title: String,
    time: LocalTime,
    color: Color,
    compact: Boolean,
    onTime: (LocalTime) -> Unit
) {
    val context = LocalContext.current
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.18f)), shape = RoundedCornerShape(8.dp)) {
        Column(
            Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF263238))
            Text(
                time.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = if (compact) 26.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onTime(LocalTime.of(hour, minute)) },
                        time.hour,
                        time.minute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("修改")
            }
        }
    }
}

@Composable
private fun WeekdaySelector(state: MainUiState, onDay: (Int, Boolean) -> Unit) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 340.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.chunked(4).forEachIndexed { rowIndex, rowLabels ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowLabels.forEachIndexed { index, label ->
                            val day = rowIndex * 4 + index + 1
                            DayChip(label, day, state, onDay, Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                labels.forEachIndexed { index, label ->
                    val day = index + 1
                    DayChip(label, day, state, onDay, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    day: Int,
    state: MainUiState,
    onDay: (Int, Boolean) -> Unit,
    modifier: Modifier
) {
    FilterChip(
        selected = day in state.profile.enabledDays,
        onClick = { onDay(day, day !in state.profile.enabledDays) },
        label = { Text(label, maxLines = 1) },
        modifier = modifier
    )
}

@Composable
private fun LocationCard(state: MainUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("天气位置", fontWeight = FontWeight.SemiBold)
            Text(
                if (state.hasLocationPermission) "已授权定位，天气会优先使用当前位置。" else "请允许定位权限，天气才能按当前位置判断。",
                color = if (state.hasLocationPermission) Color(0xFF3F7D5A) else Color(0xFFB36B00),
                fontSize = 13.sp
            )
            Text(
                "最近坐标 ${state.profile.latitude.formatCoord()}, ${state.profile.longitude.formatCoord()}",
                color = Color(0xFF5C6F73),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!state.hasBackgroundLocationPermission) {
                Text(
                    "后台检查天气时，若系统不给后台定位，会使用最近一次成功定位坐标兜底。",
                    color = Color(0xFF7C8A8D),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DecisionCard(state: MainUiState, onRefreshWeather: () -> Unit) {
    val decision = state.latestDecision
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("今日状态", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onRefreshWeather) {
                    Text("马上更新")
                }
            }
            state.refreshStatus?.let { Text(it, color = Color(0xFF3F7D5A), fontSize = 13.sp) }
            if (decision == null) {
                Text("等待第一次天气检查", color = Color(0xFF5C6F73))
            } else {
                val status = when {
                    decision.source == "fallback" -> decision.message
                    decision.isRainExpected -> "预计有雨：启用雨天闹钟"
                    else -> "未见降雨：启用非雨天闹钟"
                }
                Text(status)
                InfoLine("来源", decision.source)
                InfoLine("置信度", "${(decision.confidence * 100).toInt()}%")
                InfoLine("检查时间", decision.checkedAt.formatUiWithSeconds())
            }
        }
    }
}

@Composable
private fun CreatorSignature() {
    Text(
        text = stringResource(R.string.creator_signature),
        color = Color(0xFF9AA6A8),
        fontSize = 11.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF5C6F73), modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines = 1)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun LocalDateTime.formatUi(): String =
    format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

private fun LocalDateTime.formatUiWithSeconds(): String =
    format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))

private fun Double.formatCoord(): String =
    String.format(Locale.US, "%.4f", this)
