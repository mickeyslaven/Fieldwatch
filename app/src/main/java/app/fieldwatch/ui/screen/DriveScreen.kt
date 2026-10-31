package app.fieldwatch.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fieldwatch.domain.DriveContact
import app.fieldwatch.domain.DriveDeck
import app.fieldwatch.domain.MacUtil
import app.fieldwatch.domain.Palette
import app.fieldwatch.domain.Sighting
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.RadioClassBadge
import app.fieldwatch.ui.component.RssiBar
import app.fieldwatch.ui.component.TrendMark
import app.fieldwatch.ui.component.rssiColor
import kotlinx.coroutines.delay

@Composable
fun DriveView(
    state: FieldwatchUi,
    vm: FieldwatchViewModel,
    onOpen: (Sighting) -> Unit,
    flashKeys: Set<String>,
) {
    val gnssNotice by vm.gnssNotice.collectAsStateWithLifecycle()
    val gnssStatus by vm.gnssStatus.collectAsStateWithLifecycle()
    val mappedNotices by vm.driveNotices.collectAsStateWithLifecycle()
    val fullscreen by vm.driveFullscreen.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val snapshot = remember(state.devices, state.fleets, state.watchlist, state.alertAt, now / 1_000) {
        DriveDeck.snapshot(state.devices, state.fleets, state.watchlist, state.alertAt, now)
    }
    val gnssActive = gnssNotice?.let { !it.ended && it.line.isNotBlank() } == true
    val currentNotices = mappedNotices.filter { now - it.at <= DriveDeck.RECENT_MS }
    val activeCount = snapshot.active.size + if (gnssActive) 1 else 0
    val mode = when {
        state.scanBlocked -> DriveHeroMode.BLOCKED
        !state.scanning -> DriveHeroMode.STARTING
        activeCount > 0 -> DriveHeroMode.ALERT
        else -> DriveHeroMode.CLEAR
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth >= 700.dp
        SignalSweepBackground(mode)
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DriveSummary(
                    state = state,
                    mode = mode,
                    activeCount = activeCount,
                    gnssStatus = gnssStatus,
                    gnssLine = gnssNotice?.line.orEmpty(),
                    totalActive = snapshot.totalActive,
                    unmatchedActive = snapshot.unmatchedActive,
                    modifier = Modifier.weight(0.40f).fillMaxHeight(),
                )
                DriveAlertFeed(
                    active = snapshot.active,
                    recent = snapshot.recent,
                    mapped = currentNotices,
                    gnssLine = gnssNotice?.line.orEmpty(),
                    gnssActive = gnssActive,
                    vm = vm,
                    now = now,
                    flashKeys = flashKeys,
                    onOpen = onOpen,
                    modifier = Modifier.weight(0.60f).fillMaxHeight(),
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DriveSummary(
                    state = state,
                    mode = mode,
                    activeCount = activeCount,
                    gnssStatus = gnssStatus,
                    gnssLine = gnssNotice?.line.orEmpty(),
                    totalActive = snapshot.totalActive,
                    unmatchedActive = snapshot.unmatchedActive,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                )
                DriveAlertFeed(
                    active = snapshot.active,
                    recent = snapshot.recent,
                    mapped = currentNotices,
                    gnssLine = gnssNotice?.line.orEmpty(),
                    gnssActive = gnssActive,
                    vm = vm,
                    now = now,
                    flashKeys = flashKeys,
                    onOpen = onOpen,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .size(58.dp)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    CircleShape,
                )
                .clip(CircleShape)
                .clickable { vm.setDriveFullscreen(!fullscreen) },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (fullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = if (fullscreen) "Exit full screen" else "Enter full screen",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private enum class DriveHeroMode { CLEAR, ALERT, STARTING, BLOCKED }

@Composable
private fun DriveSummary(
    state: FieldwatchUi,
    mode: DriveHeroMode,
    activeCount: Int,
    gnssStatus: String,
    gnssLine: String,
    totalActive: Int,
    unmatchedActive: Int,
    modifier: Modifier,
    compact: Boolean = false,
) {
    val accent = when (mode) {
        DriveHeroMode.CLEAR -> Color(0xFF42F5A7)
        DriveHeroMode.ALERT, DriveHeroMode.BLOCKED -> MaterialTheme.colorScheme.error
        DriveHeroMode.STARTING -> Color(0xFFFFB020)
    }
    val pulse = rememberInfiniteTransition(label = "drive-status")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mode == DriveHeroMode.ALERT) 620 else 1_600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drive-glow",
    )
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (mode == DriveHeroMode.ALERT) 1.025f else 1.008f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mode == DriveHeroMode.ALERT) 620 else 1_900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drive-scale",
    )
    val title = when (mode) {
        DriveHeroMode.CLEAR -> "ALL CLEAR"
        DriveHeroMode.ALERT -> if (activeCount == 1) "1 ACTIVE ALERT" else "$activeCount ACTIVE ALERTS"
        DriveHeroMode.STARTING -> "STARTING SCANNERS"
        DriveHeroMode.BLOCKED -> "SCANNER BLOCKED"
    }
    val subtitle = when (mode) {
        DriveHeroMode.CLEAR -> "Watchlist and GNSS monitor are quiet"
        DriveHeroMode.ALERT -> "Voice alert issued · details at right"
        DriveHeroMode.STARTING -> "Waiting for Wi-Fi and Bluetooth radios"
        DriveHeroMode.BLOCKED -> "Open Diagnostics from Settings"
    }

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(2.dp, accent.copy(alpha = glow), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(if (compact) 16.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
        ) {
            AnimatedContent(targetState = title, label = "drive-title") { shown ->
                Text(
                    shown,
                    color = accent,
                    fontSize = if (compact) 30.sp else 42.sp,
                    lineHeight = if (compact) 32.sp else 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                )
            }
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 16.sp else 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriveStatPill(Icons.Outlined.Wifi, "${state.wifiNow} Wi-Fi")
                DriveStatPill(Icons.Outlined.Bluetooth, "${state.bleNow} BLE")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriveStatPill(Icons.Outlined.Sensors, "$totalActive on air")
                DriveStatPill(Icons.Outlined.NotificationsActive, "$unmatchedActive unmatched")
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Outlined.GpsFixed,
                    contentDescription = null,
                    tint = if (gnssLine.isNotBlank()) MaterialTheme.colorScheme.error else accent,
                    modifier = Modifier.size(26.dp),
                )
                Column {
                    Text(
                        if (state.settings.gnssMonitor) "GNSS MONITOR" else "GNSS MONITOR OFF",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        gnssLine.ifBlank { gnssStatus.ifBlank { "No interference alert" } },
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        color = if (gnssLine.isNotBlank()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = if (compact) 2 else 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveStatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DriveAlertFeed(
    active: List<DriveContact>,
    recent: List<DriveContact>,
    mapped: List<app.fieldwatch.ui.DriveNotice>,
    gnssLine: String,
    gnssActive: Boolean,
    vm: FieldwatchViewModel,
    now: Long,
    flashKeys: Set<String>,
    onOpen: (Sighting) -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        tonalElevation = 3.dp,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    if (active.isEmpty() && !gnssActive) "RECENT EVENTS" else "LIVE ALERTS",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (gnssActive) {
                item("gnss-active") {
                    DriveTextAlertCard(
                        title = "GNSS ALERT",
                        body = gnssLine,
                        active = true,
                        icon = Icons.Outlined.GpsFixed,
                    )
                }
            }
            items(active, key = { "active-${it.device.key}" }) { contact ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 3 },
                    exit = fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 4 },
                ) {
                    DriveContactCard(
                        contact = contact,
                        vm = vm,
                        now = now,
                        highlighted = contact.device.key in flashKeys,
                        onOpen = onOpen,
                    )
                }
            }
            if (active.isNotEmpty() && (recent.isNotEmpty() || mapped.isNotEmpty())) {
                item {
                    Text(
                        "RECENT",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(mapped, key = { "mapped-${it.at}-${it.text}" }) { notice ->
                DriveTextAlertCard(
                    title = "MAPPED SENSOR",
                    body = notice.text,
                    age = ageLabel(now - notice.at),
                    active = false,
                    icon = Icons.Outlined.Videocam,
                )
            }
            items(recent, key = { "recent-${it.device.key}" }) { contact ->
                DriveContactCard(contact, vm, now, highlighted = false, onOpen = onOpen)
            }
            if (active.isEmpty() && recent.isEmpty() && mapped.isEmpty() && !gnssActive) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Sensors,
                            contentDescription = null,
                            tint = Color(0xFF42F5A7),
                            modifier = Modifier.size(54.dp),
                        )
                        Text(
                            "No watchlist alerts in the last 15 minutes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveContactCard(
    contact: DriveContact,
    vm: FieldwatchViewModel,
    now: Long,
    highlighted: Boolean,
    onOpen: (Sighting) -> Unit,
) {
    val device = contact.device
    val fleetId = device.fleetIds.firstOrNull()
    val accent = fleetId?.let { Color(Palette.color(vm.fleetColor(it))) } ?: rssiColor(device.rssi)
    val pulse = rememberInfiniteTransition(label = "contact-${device.key}")
    val glow by pulse.animateFloat(
        initialValue = if (highlighted) 0.35f else 0.50f,
        targetValue = if (highlighted) 1f else 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (highlighted) 420 else 1_400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "contact-glow",
    )
    val title = vm.watchLabelFor(device.key)
        ?: contact.signatureNames.firstOrNull()
        ?: device.listTitle(contact.signatureNames)
    val className = contact.primaryClass?.label() ?: "Watchlist"
    val ageAt = contact.alertedAt ?: device.lastSeen
    val line = buildString {
        append(className)
        append(" · ")
        append(if (device.kind.name == "WIFI") "Wi-Fi" else "BLE")
        append(" · ")
        append(ageLabel(now - ageAt))
    }
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (highlighted) 3.dp else 2.dp, accent.copy(alpha = glow), shape)
            .clip(shape)
            .clickable { onOpen(device) },
        shape = shape,
        color = accent.copy(alpha = if (highlighted) 0.20f else 0.10f),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RadioClassBadge(contact.primaryClass, accent, compact = false)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title,
                    fontSize = 23.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    line,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${device.rssi} dBm",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    TrendMark(device.rssiTrend())
                    Spacer(Modifier.width(10.dp))
                    RssiBar(device.rssi, accent, Modifier.weight(1f))
                }
                Text(
                    MacUtil.screenMac(device.mac, vm.ui.value.settings.demoMode),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DriveTextAlertCard(
    title: String,
    body: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    age: String = "NOW",
) {
    val accent = if (active) MaterialTheme.colorScheme.error else Color(0xFFFFB020)
    val pulse = rememberInfiniteTransition(label = "text-alert")
    val glow by pulse.animateFloat(
        initialValue = 0.38f,
        targetValue = if (active) 1f else 0.65f,
        animationSpec = infiniteRepeatable(tween(if (active) 600 else 1_500), RepeatMode.Reverse),
        label = "text-alert-glow",
    )
    Surface(
        modifier = Modifier.fillMaxWidth().border(2.dp, accent.copy(alpha = glow), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.11f),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Text(age, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    body,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SignalSweepBackground(mode: DriveHeroMode) {
    val transition = rememberInfiniteTransition(label = "signal-sweep")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mode == DriveHeroMode.ALERT) 1_300 else 3_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "signal-sweep-position",
    )
    val color = when (mode) {
        DriveHeroMode.CLEAR -> Color(0xFF42F5A7)
        DriveHeroMode.ALERT, DriveHeroMode.BLOCKED -> MaterialTheme.colorScheme.error
        DriveHeroMode.STARTING -> Color(0xFFFFB020)
    }
    Canvas(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val grid = color.copy(alpha = 0.045f)
        for (i in 1 until 12) {
            val x = size.width * i / 12f
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
        }
        for (i in 1 until 8) {
            val y = size.height * i / 8f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        }
        val y = size.height * progress
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = 0.12f), Color.Transparent),
                startY = (y - 90f).coerceAtLeast(0f),
                endY = (y + 90f).coerceAtMost(size.height),
            ),
            topLeft = Offset(0f, (y - 90f).coerceAtLeast(0f)),
            size = androidx.compose.ui.geometry.Size(size.width, 180f.coerceAtMost(size.height)),
        )
        drawLine(color.copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y), 2f)
    }
}

private fun ageLabel(ageMs: Long): String {
    val sec = (ageMs.coerceAtLeast(0L) / 1_000L)
    return when {
        sec < 5 -> "NOW"
        sec < 60 -> "${sec}s ago"
        sec < 3_600 -> "${sec / 60}m ago"
        else -> "${sec / 3_600}h ago"
    }
}
