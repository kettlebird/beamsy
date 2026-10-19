package io.github.jqssun.airplay.ui

import android.content.Context
import android.text.format.DateFormat
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.jqssun.airplay.BoolPref
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.isUnlocked
import io.github.jqssun.airplay.openUnlockListing
import io.github.jqssun.airplay.service.AirPlayService.ServerState
import io.github.jqssun.airplay.tvName
import io.github.jqssun.airplay.viewmodel.LocalNetworkAccess
import io.github.jqssun.airplay.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

// Concept B (Figma "Beamsy · Concept B"): sizes are the 1920×1080 frames halved, the TV runs 960×540 dp
private val Night = Color(0xFF0A0A18)
private val Ink = Color.White
private val Violet = Color(0xFF8B5CF6)
private val Lilac = Color(0xFFA78BFA)
private val LilacSoft = Color(0xFFC4B5FD)
private val Plum = Color(0xFF2E1065)
private val Panel = Color(0xFF15122B)
private val Green = Color(0xFF34D399)
private val Amber = Color(0xFFFFC27A)
private val Red = Color(0xFFF87171)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onSurfaceAvailable: (Surface) -> Unit,
    onSurfaceDestroyed: (Surface) -> Unit,
    onLocalNetwork: () -> Unit
) {
    val context = LocalContext.current
    val mirroring by viewModel.mirroringActive.collectAsState()
    val audioOnly by viewModel.audioOnly.collectAsState()
    val connections by viewModel.connectionCount.collectAsState()
    val sender by viewModel.senderName.collectAsState()
    val localNetwork by viewModel.localNetwork.collectAsState()
    val serverState by viewModel.serverState.collectAsState()
    val savedName by viewModel.pref(Prefs.SERVER_NAME).collectAsState()
    val pin by viewModel.pinCode.collectAsState()
    val wifiWarnings by viewModel.pref(Prefs.WIFI_WARNINGS).collectAsState()
    val prefs = remember { context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE) }
    val name = remember(savedName) { context.tvName(prefs) }
    val timeFormat = remember { DateFormat.getTimeFormat(context) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastInteraction by remember { mutableLongStateOf(now) }
    var lastDisconnect by remember { mutableLongStateOf(now) }
    var settingsOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1_000) }
    }
    LaunchedEffect(connections) {
        if (connections == 0) lastDisconnect = System.currentTimeMillis()
    }
    // back from the Play Store (or anywhere): fresh unlock state, steps again
    LifecycleResumeEffect(Unit) {
        unlocked = isUnlocked(context)
        lastInteraction = System.currentTimeMillis()
        onPauseOrDispose { }
    }

    val idle = if (connections > 0 || renaming || settingsOpen) 0L else now - maxOf(lastInteraction, lastDisconnect)
    val mode = homeMode(mirroring, audioOnly, idle)
    val clock = timeFormat.format(Date(now))
    val link = remember(now / 5_000) { context.linkBars() }
    val online = remember(now / 5_000) { context.isOnline() }
    val weakLink = remember(now) { wifiWarnings && mode == HomeMode.MIRROR && viewModel.weakLink() }
    val from = sender.ifEmpty { "your iPhone" }
    val status = when {
        localNetwork != LocalNetworkAccess.GRANTED -> Status.PERMISSION
        !online -> Status.OFFLINE
        serverState == ServerState.ERROR -> Status.FAILED
        connections > 0 -> Status.CONNECTING
        else -> Status.READY
    }

    // nothing else is focusable outside the steps screen; the root catches the remote there
    val rootFocus = remember { FocusRequester() }
    var swallowUp by remember { mutableStateOf(false) }
    LaunchedEffect(mode) { if (mode != HomeMode.STEPS) rootFocus.requestFocusUntilLanded() }

    // with predictive back (targetSdk 37) Back never reaches onPreviewKeyEvent; on ambient it just wakes
    BackHandler(enabled = mode == HomeMode.AMBIENT) { lastInteraction = System.currentTimeMillis() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    // the wake press's key-up would otherwise click the newly focused "Settings"
                    return@onPreviewKeyEvent swallowUp.also { swallowUp = false }
                }
                lastInteraction = System.currentTimeMillis()
                // a press only wakes the ambient screen, e.g. Back must not exit
                val wake = mode == HomeMode.AMBIENT
                if (wake) swallowUp = true
                wake
            }
            .focusRequester(rootFocus)
            .focusable()
    ) {
        // always composed: re-adding the AndroidView recreates the surface (and reparenting crashes)
        VideoSurfaceView(
            onSurfaceAvailable = onSurfaceAvailable,
            onSurfaceDestroyed = onSurfaceDestroyed,
            // full screen: VideoPipeline fits the video and fills the bars with a blurred copy
            applyAspectRatio = false,
            modifier = Modifier.fillMaxSize().alpha(if (mode == HomeMode.MIRROR) 1f else 0f)
        )
        Crossfade(targetState = mode, label = "home") { m ->
            if (m == HomeMode.MIRROR) return@Crossfade
            // drawn over the SurfaceView's hole, so idle modes never show video
            when (m) {
                HomeMode.STEPS -> Steps(
                    name = name,
                    now = now,
                    clock = clock,
                    status = status,
                    from = from,
                    unlocked = unlocked,
                    link = link,
                    online = online,
                    wifiWarnings = wifiWarnings,
                    localNetwork = localNetwork,
                    onLocalNetwork = onLocalNetwork,
                    onRetry = viewModel::startServer,
                    onUnlock = { runCatching { openUnlockListing(context) } }, // no store or browser on some TVs
                    onSettings = { settingsOpen = true }
                )
                HomeMode.AUDIO -> Screensaver(name, now, clock, "Playing audio from $from")
                else -> Screensaver(name, now, clock, "Ready for Screen Mirroring on your iPhone")
            }
        }
        AnimatedVisibility(
            visible = weakLink,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 28.dp, vertical = 24.dp)
        ) { WeakWifiNote() }
    }

    pin?.let { PinCode(it, from, onDismiss = viewModel::dismissPin) }

    if (settingsOpen) {
        SettingsPanel(
            viewModel = viewModel,
            name = name,
            unlocked = unlocked,
            onRename = { settingsOpen = false; renaming = true },
            onUnlock = { runCatching { openUnlockListing(context) } },
            onDismiss = { settingsOpen = false; lastInteraction = System.currentTimeMillis() }
        )
    }

    if (renaming) {
        RenameCard(
            current = name,
            onSave = { viewModel.set(Prefs.SERVER_NAME, it) },
            onDismiss = { renaming = false; lastInteraction = System.currentTimeMillis() }
        )
    }
}

private enum class Status { READY, CONNECTING, PERMISSION, OFFLINE, FAILED }

// ---- shared pieces ----

private fun DrawScope.glow(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) {
    val c = Offset(size.width * cx, size.height * cy)
    val radius = size.minDimension * r
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), c, radius), radius, c)
}

// the Figma glows: violet behind the TV name, teal bottom right, pink bottom left
private fun Modifier.wallpaper(dim: Float = 1f) = drawBehind {
    drawRect(Night)
    glow(Color(0xFF6D28D9), 0.55f * dim, 0.5f, 0.42f, 0.95f)
    glow(Color(0xFF0EA5A4), 0.32f * dim, 0.86f, 0.78f, 0.6f)
    glow(Color(0xFFDB2777), 0.22f * dim, 0.08f, 0.82f, 0.5f)
}

// the Beamsy mark on a 100-unit grid: a see-through phone beaming onto a TV on its stand
@Composable
private fun BeamsyMark(size: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size.dp)) {
        val u = this.size.width / 100f
        fun rr(x: Float, y: Float, w: Float, h: Float, r: Float, brush: Brush) =
            drawRoundRect(brush, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(r * u))
        rr(58f, 10f, 30f, 52f, 8f, Brush.linearGradient(listOf(Ink.copy(alpha = 0.42f), Ink.copy(alpha = 0.42f))))
        rr(10f, 34f, 64f, 42f, 9f, Brush.linearGradient(listOf(Ink, Ink)))
        rr(29f, 82f, 26f, 6f, 3f, Brush.linearGradient(listOf(Ink, Ink)))
        rr(20f, 43f, 44f, 24f, 5f, Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Lilac), 20f * u, 64f * u))
    }
}

@Composable
private fun Header(now: Long, clock: String) {
    val date = remember(now / 60_000) { DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM"), now).toString() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BeamsyMark(25)
        Spacer(Modifier.width(8.dp))
        Text("Beamsy", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(date, color = Ink.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(17.dp).background(Ink.copy(alpha = 0.3f)))
        Text(clock, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

// a focusable pill: soft when idle, white with plum text when the remote is on it
@Composable
private fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(
                when {
                    focused -> Ink
                    primary -> Violet
                    else -> Ink.copy(alpha = 0.1f)
                }
            )
            .border(if (focused) 1.5.dp else 0.75.dp, if (focused) Lilac else Ink.copy(alpha = 0.16f), shape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = if (focused) Plum else Ink, fontSize = fontSize, fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun Bars(lit: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        listOf(3, 5, 7, 9).forEachIndexed { i, h ->
            Box(
                Modifier.padding(end = 1.5.dp).width(2.5.dp).height(h.dp)
                    .background(if (i < lit) color else Ink.copy(alpha = 0.22f), RoundedCornerShape(1.dp))
            )
        }
    }
}

// ---- steps screen ----

@Composable
private fun Steps(
    name: String,
    now: Long,
    clock: String,
    status: Status,
    from: String,
    unlocked: Boolean,
    link: Int?,
    online: Boolean,
    wifiWarnings: Boolean,
    localNetwork: LocalNetworkAccess,
    onLocalNetwork: () -> Unit,
    onRetry: () -> Unit,
    onUnlock: () -> Unit,
    onSettings: () -> Unit
) {
    val settingsFocus = remember { FocusRequester() }
    val actionFocus = remember { FocusRequester() }
    val hasAction = status == Status.PERMISSION || status == Status.FAILED
    LaunchedEffect(hasAction) { (if (hasAction) actionFocus else settingsFocus).requestFocusUntilLanded() }

    Column(
        Modifier.fillMaxSize().wallpaper().padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Header(now, clock)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (status) {
                    Status.CONNECTING -> "Mirroring to"
                    Status.OFFLINE -> "Waiting for a network"
                    else -> "Mirror your iPhone to"
                },
                color = Ink.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.Medium
            )
            Text(
                name, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                // 40-character names must still fit on one line
                fontSize = when { name.length <= 12 -> 90.sp; name.length <= 20 -> 64.sp; else -> 44.sp },
                letterSpacing = (-0.03).em,
                style = TextStyle(lineHeight = 1.1.em)
            )
            Spacer(Modifier.height(14.dp))
            StatusPill(status, from)
            if (status == Status.PERMISSION) {
                Spacer(Modifier.height(12.dp))
                Text("Beamsy needs permission to find iPhones on your network.", color = Ink.copy(alpha = 0.75f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (hasAction) {
                Spacer(Modifier.height(12.dp))
                Pill(
                    when {
                        status == Status.FAILED -> "Try again"
                        localNetwork == LocalNetworkAccess.SETTINGS -> "Open settings"
                        else -> "Allow access"
                    },
                    onClick = if (status == Status.FAILED) onRetry else onLocalNetwork,
                    modifier = Modifier.focusRequester(actionFocus),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(40.dp))
            StepCards(name, Modifier.alpha(if (status == Status.READY) 1f else 0.3f))
        }

        Footer(unlocked, link, online, wifiWarnings, onUnlock, onSettings, Modifier.focusRequester(settingsFocus))
    }
}

@Composable
private fun StatusPill(status: Status, from: String) {
    val (color, text) = when (status) {
        Status.READY -> Green to "Ready · waiting for your iPhone"
        Status.CONNECTING -> Lilac to "Connecting to $from…"
        Status.PERMISSION -> Amber to "One more step before iPhones can see this TV"
        Status.OFFLINE -> Amber to "This TV isn’t connected to a network"
        Status.FAILED -> Red to "iPhones can’t find this TV right now"
    }
    val shape = RoundedCornerShape(50)
    val connecting = status == Status.CONNECTING
    Row(
        Modifier
            .clip(shape)
            .background(if (connecting) Violet.copy(alpha = 0.2f) else Ink.copy(alpha = 0.08f))
            .border(0.75.dp, if (status == Status.READY) Ink.copy(alpha = 0.14f) else color.copy(alpha = 0.5f), shape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (connecting) CircularProgressIndicator(Modifier.size(11.dp), color = Lilac, strokeWidth = 2.dp)
        else Box(
            Modifier.size(7.dp).drawBehind {
                if (status == Status.READY) drawCircle(color.copy(alpha = 0.35f), size.minDimension) // soft halo
            }.background(color, CircleShape)
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = Ink.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StepCards(name: String, modifier: Modifier) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf("Connect to the same Wi-Fi", "Open Control Center", "Tap Screen Mirroring → $name").forEachIndexed { i, step ->
            val last = i == 2 // the step where the TV is actually chosen
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .width(276.dp)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(if (last) Violet.copy(alpha = 0.22f) else Ink.copy(alpha = 0.06f))
                    .border(0.75.dp, if (last) Lilac.copy(alpha = 0.6f) else Ink.copy(alpha = 0.12f), shape)
                    .padding(start = 16.dp, end = 18.dp, top = 15.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(32.dp).background(if (last) Lilac else Ink.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("${i + 1}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Text(step, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Footer(
    unlocked: Boolean,
    link: Int?,
    online: Boolean,
    warn: Boolean,
    onUnlock: () -> Unit,
    onSettings: () -> Unit,
    settingsModifier: Modifier
) {
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            if (unlocked) Text("Full HD · Unlocked", color = Ink.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            else {
                Text("Free · 720p", color = Ink.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(9.dp))
                Pill("Get Full HD · \$6.99 once", onUnlock)
            }
        }
        // centred on the screen, not between the side groups
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            val (text, color, lit) = when {
                !online -> Triple("Not connected", Amber, 0)
                link == 0 -> Triple("Connected by Ethernet", Ink.copy(alpha = 0.7f), -1)
                link == 1 && warn -> Triple("Wi-Fi signal: Weak · mirroring may stutter", Amber, 1)
                link != null -> Triple("Wi-Fi signal: " + listOf("Weak", "Fair", "Good", "Strong")[link - 1], Ink.copy(alpha = 0.7f), link)
                else -> Triple("", Ink, -1) // the driver didn't report a signal
            }
            if (lit >= 0) { Bars(lit, if (color == Amber) Amber else Ink.copy(alpha = 0.9f)); Spacer(Modifier.width(7.dp)) }
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Pill("Settings", onSettings, settingsModifier.align(Alignment.CenterEnd))
    }
}

// ---- screensaver and audio ----

@Composable
private fun Screensaver(name: String, now: Long, clock: String, hint: String) {
    val date = remember(now / 60_000) { DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"), now).toString() }
    // a few dp of drift every minute so nothing burns into the panel
    val dx = ((now / 60_000) % 7 - 3).toInt() * 4
    val dy = ((now / 90_000) % 5 - 2).toInt() * 4
    Box(Modifier.fillMaxSize().wallpaper(dim = 0.5f).padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 36.dp)) {
        Column(Modifier.align(Alignment.Center).offset(dx.dp, dy.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(clock, color = Ink.copy(alpha = 0.92f), fontSize = 130.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em, style = TextStyle(lineHeight = 1.em))
            Text(date, color = Ink.copy(alpha = 0.55f), fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Row(Modifier.align(Alignment.BottomCenter).offset(dx.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
            BeamsyMark(21, Modifier.alpha(0.8f))
            Spacer(Modifier.width(8.dp))
            Text(name, color = Ink.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.padding(horizontal = 8.dp).size(5.dp).background(Green.copy(alpha = 0.8f), CircleShape))
            Text(hint, color = Ink.copy(alpha = 0.45f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun WeakWifiNote() {
    // sits inside the side bar beside upright video
    Row(
        Modifier
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(9.dp))
            .border(0.75.dp, Amber.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            .padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Bars(1, Amber)
        Spacer(Modifier.width(9.dp))
        Column {
            Text("Weak Wi-Fi connection", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("The picture may stutter", color = Ink.copy(alpha = 0.75f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ---- overlays: full-window dialogs keep the remote's focus inside them ----

@Composable
private fun Overlay(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // our own veil replaces the window dim
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize().background(Color(0xFF05040C).copy(alpha = 0.72f)), content = content)
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(Panel.copy(alpha = 0.97f))
            .border(0.75.dp, Ink.copy(alpha = 0.1f), shape)
            .padding(horizontal = 40.dp, vertical = 32.dp)
    ) { content() }
}

@Composable
private fun KeyHint(key: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            key, color = Ink.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.border(0.75.dp, Ink.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = Ink.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// the iPhone asks for this code when "Ask for a code before mirroring" is on
@Composable
private fun PinCode(pin: String, from: String, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        Card(Modifier.align(Alignment.Center)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enter this code on your iPhone", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pin.forEach { d ->
                        Box(
                            Modifier.width(75.dp).height(92.dp)
                                .background(Ink.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                                .border(0.75.dp, Ink.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("$d", color = Ink, fontSize = 60.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("${from.replaceFirstChar { it.uppercase() }} is asking to mirror", color = Ink.copy(alpha = 0.65f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))
                KeyHint("Back", "to hide the code")
            }
        }
    }
}

@Composable
private fun RenameCard(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue(current, TextRange(current.length))) }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { fieldFocus.requestFocusUntilLanded() }
    val valid = isValidTvName(field.text)
    val count = field.text.trim().let { it.codePointCount(0, it.length) }
    fun save() { onSave(field.text.trim()); onDismiss() }

    Overlay(onDismiss) {
        // high on screen: the TV's own keyboard covers the lower half
        Card(Modifier.align(Alignment.TopCenter).offset(y = 24.dp).width(540.dp)) {
            Text("Rename TV", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("This is the name iPhones show in Screen Mirroring.", color = Ink.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                isError = !valid,
                textStyle = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Ink),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Ink.copy(alpha = 0.08f),
                    unfocusedContainerColor = Ink.copy(alpha = 0.08f),
                    focusedBorderColor = Lilac,
                    unfocusedBorderColor = Ink.copy(alpha = 0.16f),
                    cursorColor = Lilac
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) save() }),
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus)
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$count / 40", Modifier.weight(1f), color = if (valid) Ink.copy(alpha = 0.5f) else Red,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
                Pill("Cancel", onDismiss, fontSize = 14.sp)
                Pill("Save", ::save, fontSize = 14.sp, primary = true, enabled = valid)
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    viewModel: MainViewModel,
    name: String,
    unlocked: Boolean,
    onRename: () -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocusUntilLanded() }

    Overlay(onDismiss) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .width(410.dp)
                .fillMaxHeight()
                .background(Panel)
                .drawBehind { drawRect(Ink.copy(alpha = 0.08f), size = Size(1.dp.toPx(), size.height)) }
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 18.dp)
        ) {
            Text("Settings", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Beamsy $version · $name", color = Ink.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                SettingsSection("This TV")
                SettingsRow("TV name", onRename, Modifier.focusRequester(firstFocus)) { Value(name) }
                PrefRow(viewModel, "Start Beamsy when the TV turns on", Prefs.BOOT_AUTO_START)
                PrefRow(viewModel, "Open when an iPhone connects", Prefs.LAUNCH_ON_CONNECT)
                SettingsSection("Picture")
                if (unlocked) SettingsRow("Picture quality", {}) { Value("Full HD", chevron = false) }
                else SettingsRow("Picture quality", onUnlock) { Value("720p · Get Full HD", accent = true) }
                PrefRow(viewModel, "Blurred sides for upright iPhone", Prefs.BLUR_BARS)
                PrefRow(viewModel, "Shrink picture to fit the screen", Prefs.OVERSCANNED)
                PrefRow(viewModel, "Weak Wi-Fi warnings", Prefs.WIFI_WARNINGS)
                SettingsSection("Privacy")
                PrefRow(viewModel, "Ask for a code before mirroring", Prefs.REQUIRE_PIN)
                PrefRow(viewModel, "Let another iPhone take over", Prefs.ALLOW_NEW_CONN)
            }
            Spacer(Modifier.height(12.dp))
            KeyHint("Back", "to close")
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title.uppercase(), color = Ink.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, control: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .padding(bottom = 4.dp)
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Ink.copy(alpha = if (focused) 0.14f else 0.04f))
            .border(1.25.dp, if (focused) Lilac else Color.Transparent, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), color = Ink.copy(alpha = if (focused) 1f else 0.92f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(12.dp))
        control()
    }
}

@Composable
private fun Value(text: String, accent: Boolean = false, chevron: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = if (accent) LilacSoft else Ink.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium)
        if (chevron) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = Ink.copy(alpha = 0.5f))
    }
}

@Composable
private fun PrefRow(viewModel: MainViewModel, label: String, pref: BoolPref) {
    val on by viewModel.pref(pref).collectAsState()
    SettingsRow(label, { viewModel.set(pref, !on) }) {
        Box(
            Modifier.width(36.dp).height(20.dp)
                .background(if (on) Violet else Ink.copy(alpha = 0.18f), RoundedCornerShape(50))
                .padding(2.dp),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
        ) { Box(Modifier.size(16.dp).background(Ink, CircleShape)) }
    }
}
