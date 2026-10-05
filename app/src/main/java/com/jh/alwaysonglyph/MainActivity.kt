package com.jh.alwaysonglyph

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jh.alwaysonglyph.receiver.BatteryStateReceiver
import com.jh.alwaysonglyph.renderer.MatrixCanvasRenderer
import com.jh.alwaysonglyph.renderer.StatusData
import com.jh.alwaysonglyph.renderer.StatusWidgetModule
import com.jh.alwaysonglyph.service.NotificationAccess
import com.jh.alwaysonglyph.R
import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.prefs.ClockStyle
import com.jh.alwaysonglyph.renderer.displayName
import com.jh.alwaysonglyph.ui.theme.AlwaysOnGlyphTheme
import com.jh.alwaysonglyph.weather.WeatherRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val MatrixPreviewBackground = Color.Black

private data class SavedSettings(
    val use24Hour: Boolean,
    val brightness: Int,
    val disableEnabled: Boolean,
    val disableStartMinutes: Int,
    val disableEndMinutes: Int,
    val clockStyle: ClockStyle,
    val turnOffOnWake: Boolean,
)

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        content = content
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        window.decorView.setImportantForContentCapture(
            View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        )
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

    var batteryLevel by remember { mutableStateOf(100) }
    var temperatureCelsius by remember { mutableStateOf(0) }
    var weatherCelsius by remember { mutableStateOf(WeatherRepository.currentTemperatureCelsius()) }
    var unreadCount by remember { mutableStateOf(0) }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var statusWidget by remember { mutableStateOf(ClockPreferences.getStatusWidget(context)) }
    var activeWidgets by remember { mutableStateOf(ClockPreferences.getActiveWidgets(context)) }

    var saved by remember {
        mutableStateOf(
            SavedSettings(
                use24Hour = ClockPreferences.use24HourFormat(context),
                brightness = ClockPreferences.getBrightness(context),
                disableEnabled = ClockPreferences.isAodDisabledEnabled(context),
                disableStartMinutes = ClockPreferences.getAodDisabledStartMinutes(context),
                disableEndMinutes = ClockPreferences.getAodDisabledEndMinutes(context),
                clockStyle = ClockPreferences.getClockStyle(context),
                turnOffOnWake = ClockPreferences.isTurnOffOnWakeEnabled(context),
            )
        )
    }

    var use24Hour by remember { mutableStateOf(saved.use24Hour) }
    var brightness by remember { mutableStateOf(saved.brightness.toFloat()) }
    var disableEnabled by remember { mutableStateOf(saved.disableEnabled) }
    var disableStartMinutes by remember { mutableStateOf(saved.disableStartMinutes) }
    var disableEndMinutes by remember { mutableStateOf(saved.disableEndMinutes) }
    var clockStyle by remember { mutableStateOf(saved.clockStyle) }
    var turnOffOnWake by remember { mutableStateOf(saved.turnOffOnWake) }
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
        ClockPreferences.setTurnOffOnWakeEnabled(context, turnOffOnWake)

        saved = SavedSettings(
            use24Hour = use24Hour,
            brightness = brightness.toInt(),
            disableEnabled = disableEnabled,
            disableStartMinutes = disableStartMinutes,
            disableEndMinutes = disableEndMinutes,
            clockStyle = clockStyle,
            turnOffOnWake = turnOffOnWake,
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
        turnOffOnWake = saved.turnOffOnWake
        isDirty = false
    }

    fun formatTimeLabel(time: LocalTime): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun refreshLiveData() {
        batteryLevel = BatteryStateReceiver.getBatteryPercentage(context)
        temperatureCelsius = BatteryStateReceiver.getTemperatureCelsius(context)
        unreadCount = NotificationAccess.getUnreadCount()
        currentTime = LocalTime.now()
        statusWidget = ClockPreferences.getStatusWidget(context)
        activeWidgets = ClockPreferences.getActiveWidgets(context)
    }

    fun refreshWeather() {
        WeatherRepository.refresh(context) { temperature ->
            weatherCelsius = temperature
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshWeather()
    }

    fun ensureLocationPermission() {
        val needed = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            refreshWeather()
        }
    }

    LaunchedEffect(Unit) {
        WeatherRepository.loadCache(context)
        weatherCelsius = WeatherRepository.currentTemperatureCelsius()
        refreshLiveData()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshLiveData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val previewBitmap = remember(
        currentTime, batteryLevel, temperatureCelsius, weatherCelsius, unreadCount,
        use24Hour, clockStyle, statusWidget, activeWidgets
    ) {
        val data = StatusData(
            batteryLevel = batteryLevel,
            temperatureCelsius = temperatureCelsius,
            weatherCelsius = weatherCelsius,
            unreadNotifications = unreadCount
        )
        when (clockStyle) {
            ClockStyle.DIGITAL -> MatrixCanvasRenderer.renderFrame(
                time = currentTime,
                use24Hour = use24Hour,
                widget = statusWidget,
                data = data,
                widgets = activeWidgets
            )
            ClockStyle.ANALOG -> MatrixCanvasRenderer.renderAnalogFrame(
                time = currentTime,
                use24Hour = use24Hour,
                widget = statusWidget,
                data = data,
                widgets = activeWidgets
            )
        }
    }
    val previewImage = remember(previewBitmap) { previewBitmap.asImageBitmap() }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (isDirty) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        TextButton(onClick = { cancelChanges() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        Button(
                            onClick = { applyChanges() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.apply))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + bottomInset
                )
            ) {
            item {
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            item { SectionHeader(stringResource(R.string.section_behavior)) }

            item {
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.brightness_label),
                                style = MaterialTheme.typography.titleMedium
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

                    CardDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.disable_aod_label),
                                style = MaterialTheme.typography.titleMedium
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
                                    Text(stringResource(R.string.disable_start, ClockPreferences.minutesToTimeString(disableStartMinutes)))
                                }

                                OutlinedButton(
                                    onClick = { showEndPicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.disable_end, ClockPreferences.minutesToTimeString(disableEndMinutes)))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(R.string.disable_aod_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    CardDivider()

                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.turn_off_on_wake_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = turnOffOnWake,
                                onCheckedChange = { checked ->
                                    turnOffOnWake = checked
                                    isDirty = true
                                }
                            )
                        }
                    )

                    CardDivider()

                    Text(
                        text = stringResource(R.string.enable_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.manage_toys),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
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
                                    context.getString(R.string.toast_open_toys_manager),
                                    Toast.LENGTH_LONG
                                ).show()
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.section_appearance)) }

            item {
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.use_24_hour),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = use24Hour,
                                onCheckedChange = { checked ->
                                    use24Hour = checked
                                    isDirty = true
                                    currentTime = LocalTime.now()
                                }
                            )
                        }
                    )

                    CardDivider()

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        SegmentedButton(
                            selected = clockStyle == ClockStyle.DIGITAL,
                            onClick = {
                                clockStyle = ClockStyle.DIGITAL
                                isDirty = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text(stringResource(R.string.clock_digital)) }
                        )
                        SegmentedButton(
                            selected = clockStyle == ClockStyle.ANALOG,
                            onClick = {
                                clockStyle = ClockStyle.ANALOG
                                isDirty = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text(stringResource(R.string.clock_analog)) }
                        )
                    }

                    CardDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(MatrixPreviewBackground, MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = previewImage,
                                contentDescription = stringResource(R.string.matrix_preview_desc),
                                modifier = Modifier.size(180.dp),
                                filterQuality = FilterQuality.None // Pixel art nearest neighbor rendering
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(
                                R.string.preview_details,
                                formatTimeLabel(currentTime),
                                batteryLevel,
                                temperatureCelsius,
                                weatherCelsius?.let { "${it}°" } ?: "--",
                                unreadCount,
                                statusWidget.displayName(context)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = {
                                refreshLiveData()
                                ensureLocationPermission()
                            }) {
                                Text(stringResource(R.string.refresh_preview))
                            }
                            OutlinedButton(
                                onClick = {
                                    statusWidget = StatusWidgetModule.nextWidget(statusWidget, unreadCount, activeWidgets)
                                    ClockPreferences.setStatusWidget(context, statusWidget)
                                }
                            ) {
                                Text(stringResource(R.string.widget_label_format, statusWidget.displayName(context)))
                            }
                        }
                    }

                    CardDivider()

                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.edit_widgets),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(context, EditWidgetsActivity::class.java))
                        }
                    )
                }
            }
        }
        }
    }

    if (showStartPicker) {
        val state = rememberTimePickerState(
            initialHour = disableStartMinutes / 60,
            initialMinute = disableStartMinutes % 60,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showStartPicker = false },
            title = { Text(stringResource(R.string.start_time)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        disableStartMinutes = state.hour * 60 + state.minute
                        isDirty = true
                        showStartPicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(R.string.cancel))
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
            title = { Text(stringResource(R.string.end_time)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        disableEndMinutes = state.hour * 60 + state.minute
                        isDirty = true
                        showEndPicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            TimePicker(state = state)
        }
    }
}
