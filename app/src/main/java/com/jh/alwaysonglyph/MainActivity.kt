package com.jh.alwaysonglyph

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jh.alwaysonglyph.receiver.BatteryStateReceiver
import com.jh.alwaysonglyph.renderer.MatrixCanvasRenderer
import com.jh.alwaysonglyph.service.UnreadNotificationListenerService
import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.ui.theme.AlwaysOnGlyphTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlwaysOnGlyphTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GlyphClockHomeScreen()
                }
            }
        }
    }
}

@Composable
fun GlyphClockHomeScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isNotifGranted by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableStateOf(100) }
    var unreadCount by remember { mutableStateOf(0) }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var use24Hour by remember { mutableStateOf(ClockPreferences.use24HourFormat(context)) }

    fun formatTimeLabel(time: LocalTime): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun refreshData() {
        isNotifGranted = UnreadNotificationListenerService.isNotificationAccessGranted(context)
        batteryLevel = BatteryStateReceiver.getBatteryPercentage(context)
        unreadCount = UnreadNotificationListenerService.getUnreadCount()
        currentTime = LocalTime.now()
        use24Hour = ClockPreferences.use24HourFormat(context)
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .verticalScroll(scrollState),
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
                        ClockPreferences.setUse24HourFormat(context, checked)
                        currentTime = LocalTime.now()
                    }
                )
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
                val previewBitmap = remember(currentTime, batteryLevel, unreadCount, use24Hour) {
                    MatrixCanvasRenderer.renderFrame(
                        time = currentTime,
                        batteryLevel = batteryLevel,
                        unreadNotifications = unreadCount,
                        use24Hour = use24Hour
                    )
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
                    text = "Time: ${formatTimeLabel(currentTime)} | Battery: $batteryLevel% | Unread: $unreadCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { refreshData() }) {
                    Text("Refresh Preview")
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
