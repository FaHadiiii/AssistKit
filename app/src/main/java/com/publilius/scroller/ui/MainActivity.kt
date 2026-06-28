package com.publilius.scroller.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.publilius.scroller.R
import com.publilius.scroller.data.AppContainer
import com.publilius.scroller.model.ScrollSpeed
import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.service.OverlayService
import com.publilius.scroller.ui.theme.ScrollerTheme
import com.publilius.scroller.util.PermissionStatus

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(AppContainer.settingsRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ScrollerTheme {
                MainScreen(
                    uiState = uiState,
                    onRefreshPermissions = { viewModel.refreshPermissions(this) },
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onSetSpeed = viewModel::setScrollSpeed,
                    onLaunchOverlay = { OverlayService.start(this) },
                    onStopOverlay = { OverlayService.stop(this) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions(this)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }
}

@Composable
private fun MainScreen(
    uiState: MainUiState,
    onRefreshPermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onSetSpeed: (ScrollSpeed) -> Unit,
    onLaunchOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .safeDrawingPadding()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                HeaderBlock()
                Spacer(modifier = Modifier.height(20.dp))
                if (!uiState.permissions.allGranted) {
                    SetupScreen(
                        permissionStatus = uiState.permissions,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onRefreshPermissions = onRefreshPermissions,
                    )
                } else {
                    SettingsScreen(
                        uiState = uiState,
                        onSetSpeed = onSetSpeed,
                        onLaunchOverlay = onLaunchOverlay,
                        onStopOverlay = onStopOverlay,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "AssistKit",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "User-initiated scrolling, volume, and lock controls with visible actions at all times.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    }
}

@Composable
private fun SetupScreen(
    permissionStatus: PermissionStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
) {
    Text(
        text = "Finish setup",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Grant the two required permissions before launching the floating controller.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(18.dp))
    PermissionRow(
        icon = R.drawable.ic_overlay_pause,
        title = "Accessibility access",
        body = "Required for user-initiated swipe gestures and the explicit lock-screen action from the floating controller.",
        granted = permissionStatus.accessibilityEnabled,
        actionIcon = R.drawable.ic_overlay_start,
        onAction = onOpenAccessibilitySettings,
    )
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    )
    PermissionRow(
        icon = R.drawable.ic_overlay_assist,
        title = "Draw over other apps",
        body = "Required to keep the floating pill and stop controls on top of the current app.",
        granted = permissionStatus.overlayEnabled,
        actionIcon = R.drawable.ic_overlay_start,
        onAction = onOpenOverlaySettings,
    )
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    )
    MinimalActionButton(
        icon = R.drawable.ic_overlay_close,
        label = "Refresh permission status",
        onClick = onRefreshPermissions,
    )
}

@Composable
private fun PermissionRow(
    icon: Int,
    title: String,
    body: String,
    granted: Boolean,
    actionIcon: Int,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 2.dp, end = 12.dp)
                .size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onAction,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(actionIcon),
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatusLine(granted = granted)
        }
    }
}

@Composable
private fun StatusLine(granted: Boolean) {
    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (granted) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                },
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (granted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(99.dp),
                ),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = if (granted) "Granted" else "Needs attention",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SettingsScreen(
    uiState: MainUiState,
    onSetSpeed: (ScrollSpeed) -> Unit,
    onLaunchOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    Text(
        text = "Scroll settings",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Choose a preset, then launch the floating controller. Auto-scroll, volume changes, and lock screen only run when you press them from the overlay.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(18.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Runtime state",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when (uiState.scrollState) {
                    ScrollState.Idle -> "Idle"
                    ScrollState.Running -> "Running"
                    ScrollState.Paused -> "Paused"
                    ScrollState.StoppedDueToEnd -> "Stopped at end of content"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (uiState.overlayVisible) {
                "Floating overlay active."
            } else {
                "Floating overlay stopped."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        )
        SpeedPicker(
            selected = uiState.scrollSpeed,
            onSetSpeed = onSetSpeed,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MinimalActionButton(
                icon = R.drawable.ic_overlay_start,
                label = "Launch overlay",
                onClick = onLaunchOverlay,
                modifier = Modifier.weight(1f),
            )
            MinimalActionButton(
                icon = R.drawable.ic_overlay_stop,
                label = "Stop overlay",
                onClick = onStopOverlay,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SpeedPicker(
    selected: ScrollSpeed,
    onSetSpeed: (ScrollSpeed) -> Unit,
) {
    Text(
        text = "Scroll speed",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Level ${selected.level} of 10",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
        value = selected.level.toFloat(),
        onValueChange = { value ->
            onSetSpeed(ScrollSpeed.fromLevel(value.toInt()))
        },
        valueRange = ScrollSpeed.MIN_LEVEL.toFloat()..ScrollSpeed.MAX_LEVEL.toFloat(),
        steps = ScrollSpeed.MAX_LEVEL - ScrollSpeed.MIN_LEVEL - 1,
    )
    Text(
        text = "Lower values scroll more gently. Higher values scroll faster.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MinimalActionButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = label)
    }
}
