package com.jh.alwaysonglyph

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jh.alwaysonglyph.R
import com.jh.alwaysonglyph.BuildConfig
import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.renderer.StatusWidget
import com.jh.alwaysonglyph.renderer.StatusWidgetModule
import com.jh.alwaysonglyph.renderer.description
import com.jh.alwaysonglyph.renderer.displayName
import com.jh.alwaysonglyph.service.NotificationAccess
import com.jh.alwaysonglyph.ui.theme.AlwaysOnGlyphTheme
import com.jh.alwaysonglyph.weather.WeatherRepository
import kotlinx.coroutines.launch

class EditWidgetsActivity : ComponentActivity() {
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
        window.decorView.setImportantForContentCapture(
            View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        )
        setContent {
            AlwaysOnGlyphTheme {
                EditWidgetsScreen()
            }
        }
    }
}

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
private fun PermissionAlertCard(
    title: String,
    message: String,
    buttonLabel: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun NotificationSubSettings(
    filterEnabled: Boolean,
    onFilterEnabledChange: (Boolean) -> Unit,
    selectedApps: Set<String>,
) {
    val context = LocalContext.current
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.notification_subsettings_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        context.startActivity(
                            Intent(context, NotificationAppsActivity::class.java)
                        )
                    }
                    .semantics { role = Role.Button }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_filter_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (filterEnabled) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(selectedApps.size.toString())
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.notification_filter_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            VerticalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
            )
            Switch(
                checked = filterEnabled,
                onCheckedChange = onFilterEnabledChange
            )
        }

        if (filterEnabled && selectedApps.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.notification_filter_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWidgetsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    var enabled by remember { mutableStateOf(ClockPreferences.getEnabledWidgets(context)) }
    var notificationFilterEnabled by remember { mutableStateOf(ClockPreferences.isNotificationFilterEnabled(context)) }
    var selectedNotificationApps by remember { mutableStateOf(ClockPreferences.getSelectedNotificationApps(context)) }
    var priorityEnabled by remember { mutableStateOf(ClockPreferences.isPriorityEnabled(context)) }
    var notificationGranted by remember {
        mutableStateOf(NotificationAccess.isAccessGranted(context))
    }
    var locationGranted by remember {
        mutableStateOf(WeatherRepository.hasLocationPermission(context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationGranted = WeatherRepository.hasLocationPermission(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = NotificationAccess.isAccessGranted(context)
                locationGranted = WeatherRepository.hasLocationPermission(context)
                selectedNotificationApps = ClockPreferences.getSelectedNotificationApps(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_widgets),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(innerPadding)
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.priority_behavior_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.priority_behavior_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = priorityEnabled,
                            onCheckedChange = { checked ->
                                priorityEnabled = checked
                                ClockPreferences.setPriorityEnabled(context, checked)
                            }
                        )
                    }
                }

                item {
                    SectionHeader(stringResource(R.string.section_widgets))
                    Text(
                        text = stringResource(R.string.edit_widgets_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(
                    StatusWidgetModule.DEFAULT_ORDER.filter {
                        BuildConfig.HAS_NOTIFICATION || it != StatusWidget.NOTIFICATION
                    },
                    key = { it.name }
                ) { widget ->
                    val included = widget in enabled
                    val showNotificationAlert =
                        widget == StatusWidget.NOTIFICATION && included && !notificationGranted
                    val showLocationAlert =
                        widget == StatusWidget.WEATHER && included && !locationGranted

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = widget.displayName(context),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (included) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = widget.description(context),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (widget == StatusWidget.WEATHER) {
                                    val tooltipState = rememberTooltipState(isPersistent = true)
                                    val scope = rememberCoroutineScope()
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = {
                                            PlainTooltip {
                                                Text(
                                                    stringResource(R.string.weather_tooltip)
                                                )
                                            }
                                        },
                                        state = tooltipState
                                    ) {
                                        IconButton(onClick = { scope.launch { tooltipState.show() } }) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = stringResource(R.string.weather_tooltip_desc),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
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

                            if (showNotificationAlert) {
                                PermissionAlertCard(
                                    title = stringResource(R.string.notification_permission_title),
                                    message = stringResource(R.string.notification_permission_message),
                                    buttonLabel = stringResource(R.string.grant_permission),
                                    onAction = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        )
                                    }
                                )
                            }

                            if (showLocationAlert) {
                                PermissionAlertCard(
                                    title = stringResource(R.string.location_permission_title),
                                    message = stringResource(R.string.location_permission_message),
                                    buttonLabel = stringResource(R.string.grant_permission),
                                    onAction = {
                                        val permissions = listOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ).filter {
                                            ContextCompat.checkSelfPermission(context, it) !=
                                                PackageManager.PERMISSION_GRANTED
                                        }
                                        if (permissions.isNotEmpty()) {
                                            locationPermissionLauncher.launch(permissions.toTypedArray())
                                        }
                                    }
                                )
                            }

                            if (widget == StatusWidget.NOTIFICATION && included) {
                                NotificationSubSettings(
                                    filterEnabled = notificationFilterEnabled,
                                    onFilterEnabledChange = { checked ->
                                        notificationFilterEnabled = checked
                                        ClockPreferences.setNotificationFilterEnabled(context, checked)
                                    },
                                    selectedApps = selectedNotificationApps
                                )
            }
        }
        }
    }
}
            }
        }
    }
}
