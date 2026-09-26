package com.jh.alwaysonglyph

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jh.alwaysonglyph.receiver.BatteryStateReceiver
import com.jh.alwaysonglyph.renderer.MatrixCanvasRenderer
import com.jh.alwaysonglyph.renderer.StatusData
import com.jh.alwaysonglyph.renderer.StatusWidgetModule
import com.jh.alwaysonglyph.service.UnreadNotificationListenerService
import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.prefs.ClockStyle
import com.jh.alwaysonglyph.ui.theme.AlwaysOnGlyphTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class SavedSettings(
    val use24Hour: Boolean,
    val brightness: Int,
    val disableEnabled: Boolean,
    val disableStartMinutes: Int,
    val disableEndMinutes: Int,
    val clockStyle: ClockStyle,
)

@Composable
private fun StatusBarProtection(
    color: Color = MaterialTheme.colorScheme.background,
) {
    val density = LocalDensity.current
    val gradientHeight = WindowInsets.statusBars.getTop(density).times(1.2f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 1f),
                    color.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = gradientHeight
            ),
            size = Size(size.width, gradientHeight)
        )
    }
}

@Composable
private fun ClockStyleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        setContent {
            AlwaysOnGlyphTheme {
                GlyphClockHomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlyphClockHomeScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isNotifGranted by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableStateOf(100) }
    var temperatureCelsius by remember { mutableStateOf(0) }
    var unreadCount by remember { mutableStateOf(0) }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var statusWidget by remember { mutableStateOf(ClockPreferences.getStatusWidget(context)) }

    var saved by remember {
        mutableStateOf(
            SavedSettings(
                use24Hour = ClockPreferences.use24HourFormat(context),
                brightness = ClockPreferences.getBrightness(context),
                disableEnabled = ClockPreferences.isAodDisabledEnabled(context),
                disableStartMinutes = ClockPreferences.getAodDisabledStartMinutes(context),
                disableEndMinutes = ClockPreferences.getAodDisabledEndMinutes(context),
                clockStyle = ClockPreferences.getClockStyle(context),
            )
        )
    }

    var use24Hour by remember { mutableStateOf(saved.use24Hour) }
    var brightness by remember { mutableStateOf(saved.brightness.toFloat()) }
    var disableEnabled by remember { mutableStateOf(saved.disableEnabled) }
    var disableStartMinutes by remember { mutableStateOf(saved.disableStartMinutes) }
    var disableEndMinutes by remember { mutableStateOf(saved.disableEndMinutes) }
    var clockStyle by remember { mutableStateOf(saved.clockStyle) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var isDirty by remember { mutableStateOf(false) }

    fun applyChanges() {
        ClockPreferences.setUse24HourFormat(context, use24Hour)
        ClockPreferences.setBrightness(context, brightness.toInt())
        ClockPreferences.setAodDisabledEnabled(context, disableEnabled)
        ClockPreferences.setAodDisabledStartMinutes(context, disableStartMinutes)
        ClockPreferences.setAodDisabledEndMinutes(context, disableEndMinutes)
        ClockPreferences.setClockStyle(context, clockStyle)

        saved = SavedSettings(
            use24Hour = use24Hour,
            brightness = brightness.toInt(),
            disableEnabled = disableEnabled,
            disableStartMinutes = disableStartMinutes,
            disableEndMinutes = disableEndMinutes,
            clockStyle = clockStyle,
        )
        isDirty = false
    }

    fun cancelChanges() {
        use24Hour = saved.use24Hour
        brightness = saved.brightness.toFloat()
        disableEnabled = saved.disableEnabled
        disableStartMinutes = saved.disableStartMinutes
        disableEndMinutes = saved.disableEndMinutes
        clockStyle = saved.clockStyle
        isDirty = false
    }

    fun formatTimeLabel(time: LocalTime): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun refreshLiveData() {
        isNotifGranted = UnreadNotificationListenerService.isNotificationAccessGranted(context)
        batteryLevel = BatteryStateReceiver.getBatteryPercentage(context)
        temperatureCelsius = BatteryStateReceiver.getTemperatureCelsius(context)
        unreadCount = UnreadNotificationListenerService.getUnreadCount()
        currentTime = LocalTime.now()
        statusWidget = ClockPreferences.getStatusWidget(context)
    }

    LaunchedEffect(Unit) {
        refreshLiveData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            if (isDirty) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { cancelChanges() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { applyChanges() },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding)
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Glyph Matrix Status Toy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Flip to Glyph Clock & Status Display",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Clock Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "24시간제 사용",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = use24Hour,
                    onCheckedChange = { checked ->
                        use24Hour = checked
                        isDirty = true
                        currentTime = LocalTime.now()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clock Style Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "시계 스타일",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockStyleOption(
                        label = "디지털",
                        selected = clockStyle == ClockStyle.DIGITAL,
                        onClick = {
                            clockStyle = ClockStyle.DIGITAL
                            isDirty = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ClockStyleOption(
                        label = "아날로그",
                        selected = clockStyle == ClockStyle.ANALOG,
                        onClick = {
                            clockStyle = ClockStyle.ANALOG
                            isDirty = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Brightness Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Glyph 밝기",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = brightness.toInt().toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = brightness,
                    onValueChange = { value ->
                        brightness = value
                        isDirty = true
                    },
                    valueRange = 0f..255f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AOD Disable Schedule Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "특정 시간에 Always-on Glyph 끄기",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Switch(
                        checked = disableEnabled,
                        onCheckedChange = { checked ->
                            disableEnabled = checked
                            isDirty = true
                        }
                    )
                }

                if (disableEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("시작 ${ClockPreferences.minutesToTimeString(disableStartMinutes)}")
                        }

                        OutlinedButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("종료 ${ClockPreferences.minutesToTimeString(disableEndMinutes)}")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "이 시간대에는 Flip to Glyph(Always-on)가 꺼집니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Matrix Live Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Matrix Display Live Preview (25x25)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Render 25x25 matrix bitmap preview
                val previewBitmap = remember(currentTime, batteryLevel, temperatureCelsius, unreadCount, use24Hour, clockStyle, statusWidget) {
                    val data = StatusData(
                        batteryLevel = batteryLevel,
                        temperatureCelsius = temperatureCelsius,
                        unreadNotifications = unreadCount
                    )
                    when (clockStyle) {
                        ClockStyle.DIGITAL -> MatrixCanvasRenderer.renderFrame(
                            time = currentTime,
                            use24Hour = use24Hour,
                            widget = statusWidget,
                            data = data
                        )
                        ClockStyle.ANALOG -> MatrixCanvasRenderer.renderAnalogFrame(
                            time = currentTime,
                            use24Hour = use24Hour,
                            widget = statusWidget,
                            data = data
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Matrix Preview",
                        modifier = Modifier.size(180.dp),
                        filterQuality = FilterQuality.None // Pixel art nearest neighbor rendering
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Time: ${formatTimeLabel(currentTime)} | Battery: $batteryLevel% | Temp: ${temperatureCelsius}° | Unread: $unreadCount | Widget: ${statusWidget.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { refreshLiveData() }) {
                        Text("Refresh Preview")
                    }
                    OutlinedButton(
                        onClick = {
                            statusWidget = StatusWidgetModule.nextWidget(statusWidget, unreadCount)
                            ClockPreferences.setStatusWidget(context, statusWidget)
                        }
                    ) {
                        Text("Widget: ${statusWidget.name}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nothing OS Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Nothing OS Flip to Glyph Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To enable this clock when phone is locked and flipped, select 'Clock & Status Matrix' in Nothing OS Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent().apply {
                                component = ComponentName(
                                    "com.nothing.thirdparty",
                                    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Opening Nothing OS Toys Manager (Go to Settings > Glyph Interface)",
                                Toast.LENGTH_LONG
                            ).show()
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Glyph Toys in Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Access Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Unread Notification Count Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isNotifGranted) "Granted" else "Not Granted",
                        color = if (isNotifGranted) Color(0xFF4CAF50) else Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Notification Access is required to show the unread count (· N) on the matrix instead of battery %.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isNotifGranted) "Change Notification Settings" else "Grant Notification Access")
                }
            }
        }
    }
        }
        StatusBarProtection()
    }

    if (showStartPicker) {
        val state = rememberTimePickerState(
            initialHour = disableStartMinutes / 60,
            initialMinute = disableStartMinutes % 60,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showStartPicker = false },
            title = { Text("시작 시간") },
            confirmButton = {
                TextButton(
                    onClick = {
                        disableStartMinutes = state.hour * 60 + state.minute
                        isDirty = true
                        showStartPicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("취소")
                }
            },
        ) {
            TimePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberTimePickerState(
            initialHour = disableEndMinutes / 60,
            initialMinute = disableEndMinutes % 60,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showEndPicker = false },
            title = { Text("종료 시간") },
            confirmButton = {
                TextButton(
                    onClick = {
                        disableEndMinutes = state.hour * 60 + state.minute
                        isDirty = true
                        showEndPicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("취소")
                }
            },
        ) {
            TimePicker(state = state)
        }
    }
}
