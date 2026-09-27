package com.jh.alwaysonglyph

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.renderer.StatusWidget
import com.jh.alwaysonglyph.renderer.StatusWidgetModule
import com.jh.alwaysonglyph.ui.theme.AlwaysOnGlyphTheme

class EditWidgetsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlwaysOnGlyphTheme {
                EditWidgetsScreen()
            }
        }
    }
}

private fun StatusWidget.label(): String = when (this) {
    StatusWidget.NOTIFICATION -> "알림"
    StatusWidget.BATTERY -> "배터리"
    StatusWidget.TEMPERATURE -> "기기 온도"
    StatusWidget.WEATHER -> "날씨"
}

private fun StatusWidget.description(): String = when (this) {
    StatusWidget.NOTIFICATION -> "읽지 않은 알림 개수"
    StatusWidget.BATTERY -> "배터리 잔량"
    StatusWidget.TEMPERATURE -> "기기(배터리) 온도"
    StatusWidget.WEATHER -> "현재 기온"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWidgetsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    var enabled by remember { mutableStateOf(ClockPreferences.getEnabledWidgets(context)) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { activity?.finish() }) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Edit Widgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(64.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "표시할 위젯을 선택하세요. 알림이 있으면 알림 위젯이 가장 먼저 표시됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StatusWidgetModule.DEFAULT_ORDER, key = { it.name }) { widget ->
                    val included = widget in enabled

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = widget.label(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (included) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                                Text(
                                    text = widget.description(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = included,
                                onCheckedChange = { checked ->
                                    val newEnabled = if (checked) enabled + widget else enabled - widget
                                    if (newEnabled.isNotEmpty()) {
                                        enabled = newEnabled
                                        ClockPreferences.setEnabledWidgets(context, newEnabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
