package com.mtc.termosonda.ui

import android.icu.text.DecimalFormat
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mtc.termosonda.R
import com.mtc.termosonda.data.model.NvsState
import com.mtc.termosonda.ui.permissions.PermissionsUtils
import com.mtc.termosonda.ui.theme.SondaTheme
import kotlin.math.roundToInt

// ─── Main entry ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: SondaViewModel = hiltViewModel()) {
    val sondaUI by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current
    val permissionState =
        rememberMultiplePermissionsState(permissions = PermissionsUtils.permissions)

    NoPermissionDialog(
        show      = !permissionState.allPermissionsGranted,
        onConfirm = { activity?.finish() }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                bleConnected = sondaUI.bleConnected,
                onBleClick   = { viewModel.showList() }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ControlCard(
                    state               = sondaUI.state,
                    progress            = sondaUI.recordLevel,
                    preShow             = sondaUI.preShow,
                    bleSet              = sondaUI.bleSet,
                    showList            = sondaUI.showList,
                    onCapture           = { interval -> viewModel.startCapture(interval) },
                    onDownload          = { viewModel.startDownload() },
                    onDeviceClick       = { device -> viewModel.tryConnect(device) },
                )
            }
            item {
                SectionLabel(text = stringResource(R.string.temperature) + " readings")
            }
            itemsIndexed(sondaUI.tempList) { index, item ->
                TemperatureRow(index = index, rawValue = item)
            }
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun AppTopBar(bleConnected: Boolean, onBleClick: () -> Unit) {
    val ext = SondaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                color = ext.outlineSubtle,
                shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text  = "Termosonda",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
            )
            Text(
                text  = "Sistema de captura térmica",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
        BleBadge(connected = bleConnected, onClick = onBleClick)
    }
}

// ─── BLE badge ────────────────────────────────────────────────────────────────

@Composable
private fun BleBadge(connected: Boolean, onClick: () -> Unit) {
    val ext   = SondaTheme.colors
    val color = if (connected) ext.connected else ext.disconnected
    val bg    = if (connected) ext.connectedMuted else ext.disconnectedMuted
    val label = if (connected) "Conectado" else "Desconectado"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = color,
                fontWeight = FontWeight.Medium,
            )
        )
    }
}

// ─── Control card ─────────────────────────────────────────────────────────────

@Composable
private fun ControlCard(
    state         : NvsState,
    progress      : Float,
    preShow       : Boolean,
    bleSet        : Set<String>,
    showList      : Boolean,
    onCapture     : (Int) -> Unit,
    onDownload    : () -> Unit,
    onDeviceClick : (String) -> Unit,
) {
    val ext = SondaTheme.colors
    var selectedInterval by remember { mutableIntStateOf(10) }
    val intervals = remember { listOf(5, 10, 30, 60) }

    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── Action buttons ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    label    = stringResource(R.string.capture),
                    enabled  = state == NvsState.EMPTY,
                    modifier = Modifier.weight(1f),
                    onClick  = { onCapture(selectedInterval) }
                )
                SecondaryButton(
                    label    = stringResource(R.string.download),
                    enabled  = state == NvsState.FULL
                            || state == NvsState.PARTIAL
                            || state == NvsState.LOADING,
                    modifier = Modifier.weight(1f),
                    onClick  = onDownload,
                )
            }

            // ── Interval picker (only when EMPTY) ─────────────────────────────
            AnimatedVisibility(
                visible = state == NvsState.EMPTY,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = stringResource(R.string.capture_interval),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(intervals) { interval ->
                            IntervalPill(
                                label    = "$interval s",
                                selected = selectedInterval == interval,
                                onClick  = { selectedInterval = interval },
                            )
                        }
                    }
                }
            }

            // ── NVS status row ────────────────────────────────────────────────
            NvsStatusRow(state = state, progress = progress)

            // ── Loading spinner ───────────────────────────────────────────────
            AnimatedVisibility(visible = preShow) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(32.dp),
                        color     = ext.warning,
                        trackColor = ext.warningMuted,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            // ── Device list ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showList && bleSet.isNotEmpty(),
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically(),
            ) {
                DeviceList(devices = bleSet, onDeviceClick = onDeviceClick)
            }
        }
    }
}

// ─── NVS status ───────────────────────────────────────────────────────────────

@Composable
private fun NvsStatusRow(state: NvsState, progress: Float) {
    val ext        = SondaTheme.colors
    val animProg   by animateFloatAsState(
        targetValue  = progress,
        animationSpec = tween(400),
        label        = "nvs_progress"
    )
    val (badgeColor, badgeBg, badgeLabel) = when (state) {
        NvsState.EMPTY   -> Triple(MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant, "VACÍO")
        NvsState.WAIT    -> Triple(ext.warning, ext.warningMuted, "CAPTURANDO")
        NvsState.PARTIAL -> Triple(ext.warning, ext.warningMuted, "PARCIAL")
        NvsState.FULL    -> Triple(ext.connected, ext.connectedMuted, "COMPLETO")
        NvsState.LOADING -> Triple(MaterialTheme.colorScheme.primary,
            ext.outlineSubtle, "DESCARGANDO")
        else             -> Triple(MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant, state.name)
    }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = "Estado NVS",
            style = MaterialTheme.typography.labelSmall.copy(
                color       = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.06.sp,
            )
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text  = badgeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = badgeColor,
                    fontWeight = FontWeight.Medium,
                )
            )
        }
    }

    if (state == NvsState.WAIT || state == NvsState.LOADING) {
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress     = { animProg },
            modifier     = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(99.dp)),
            color        = MaterialTheme.colorScheme.primary,
            trackColor   = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap    = StrokeCap.Round,
        )
        Text(
            text     = "${(animProg * 100).roundToInt()}%",
            style    = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            //modifier = Modifier.align(Alignment.End),
        )
    }
}
// Make Column's Alignment.End available inside NvsStatusRow scope
//private fun Modifier.align(alignment: Alignment.Horizontal) = this

// ─── Device list ──────────────────────────────────────────────────────────────

@Composable
private fun DeviceList(devices: Set<String>, onDeviceClick: (String) -> Unit) {
    val ext = SondaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(0.5.dp, ext.outlineSubtle, RoundedCornerShape(10.dp))
    ) {
        devices.forEach { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeviceClick(device) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ext.connected)
                )
                Text(
                    text  = device,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                )
            }
        }
    }
}

// ─── Temperature row ─────────────────────────────────────────────────────────

@Composable
fun TemperatureRow(index: Int, rawValue: Int) {
    val decimal = remember { DecimalFormat("0.0") }
    val ext     = SondaTheme.colors
    val celsius = rawValue / 100.0
    // Normalize 0–45°C range to 0..1 for the bar
    val fraction = (celsius / 45.0).coerceIn(0.0, 1.0).toFloat()
    val animFraction by animateFloatAsState(
        targetValue   = fraction,
        animationSpec = tween(600),
        label         = "temp_bar_$index"
    )
    val barColor = when {
        celsius >= 30 -> ext.tempHot
        celsius >= 20 -> ext.tempWarm
        celsius >= 10 -> ext.tempMid
        else          -> ext.tempCold
    }

    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Index label
            Text(
                text     = "S${index + 1}",
                style    = MaterialTheme.typography.labelMedium.copy(
                    color  = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.width(28.dp),
            )

            // Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animFraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.6f), barColor))
                        )
                )
            }

            // Value
            Text(
                text  = "${decimal.format(celsius)}°C",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

// ─── Shared composables ───────────────────────────────────────────────────────

@Composable
private fun SurfaceCard(
    modifier: Modifier = Modifier,
    content : @Composable () -> Unit,
) {
    val ext = SondaTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, ext.outlineSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) { content() }
}

@Composable
private fun PrimaryButton(
    label   : String,
    enabled : Boolean,
    modifier: Modifier = Modifier,
    onClick : () -> Unit,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(44.dp),
        shape    = RoundedCornerShape(10.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = MaterialTheme.colorScheme.primary,
            contentColor           = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun SecondaryButton(
    label   : String,
    enabled : Boolean,
    modifier: Modifier = Modifier,
    onClick : () -> Unit,
) {
    val ext = SondaTheme.colors
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(44.dp),
        shape    = RoundedCornerShape(10.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = MaterialTheme.colorScheme.surfaceVariant,
            contentColor           = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (enabled) ext.outlineSubtle else Color.Transparent,
        ),
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun IntervalPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val ext = SondaTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                0.5.dp,
                if (selected) Color.Transparent else ext.outlineSubtle,
                RoundedCornerShape(99.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color      = if (selected) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.08.sp,
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

// Make Column's Alignment.End available inside NvsStatusRow scope
//private fun Modifier.align(alignment: Alignment.Horizontal) = this