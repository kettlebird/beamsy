package io.github.jqssun.airplay.ui

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.jqssun.airplay.BoolPref
import io.github.jqssun.airplay.FREE_FPS
import io.github.jqssun.airplay.FreeTime
import io.github.jqssun.airplay.FullHd
import io.github.jqssun.airplay.Plan
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.isUnlocked
import io.github.jqssun.airplay.service.AirPlayService.ServerState
import io.github.jqssun.airplay.tvName
import io.github.jqssun.airplay.viewmodel.LocalNetworkAccess
import io.github.jqssun.airplay.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

// Concept C (design/beamsy-ui.html): sizes are dp on the TV's 960×540 dp; names match the HTML's :root tokens
private val Canvas0 = Color(0xFF0C0C0D)
private val CanvasTop = Color(0xFF18181B)
private val Material = Color(0xFF1E1E20) // the HTML's blurred surface, solid: dialogs can't blur what's behind them
private val Hairline = Color.White.copy(alpha = 0.09f)
private val Fill = Color.White.copy(alpha = 0.09f)
private val Text1 = Color(0xFFF5F5F7)
private val Text2 = Text1.copy(alpha = 0.62f)
private val Text3 = Text1.copy(alpha = 0.38f)
private val OnFocus = Canvas0
private val OnFocus2 = Canvas0.copy(alpha = 0.58f)
private val Ready = Color(0xFF30D158)
private val Attention = Color(0xFFFF9F0A)
private val Failure = Color(0xFFFF453A)
private val Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private const val FULL_FPS = 60

// system "Remove animations" (animator scale 0): no lift on focus, no cycling demo
private val LocalReducedMotion = staticCompositionLocalOf { false }

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
    val purchased by viewModel.pref(Prefs.FULL_HD).collectAsState()
    val trialUsed by viewModel.pref(Prefs.TRIAL_USED).collectAsState()
    val sessionSeconds by viewModel.sessionSeconds.collectAsState()
    val price by FullHd.price.collectAsState()
    val prefs = remember { context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE) }
    val name = remember(savedName) { context.tvName(prefs) }
    val timeFormat = remember { DateFormat.getTimeFormat(context) }
    val reduced = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastInteraction by remember { mutableLongStateOf(now) }
    var lastDisconnect by remember { mutableLongStateOf(now) }
    var settingsOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var unlockApp by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var hadConnection by remember { mutableStateOf(false) }
    val trialLeft = FreeTime.trialLeft(trialUsed)
    val plan = FreeTime.plan(purchased || unlockApp, trialLeft)

    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1_000) }
    }
    LaunchedEffect(connections) {
        if (connections > 0) { hadConnection = true; ended = false; return@LaunchedEffect }
        lastDisconnect = System.currentTimeMillis()
        // the service cut a free session at 15 minutes (it keeps the count until the next connection)
        if (hadConnection && FreeTime.plan(context, prefs) == Plan.FREE && viewModel.sessionSeconds.value >= FreeTime.SESSION_SECONDS) ended = true
        hadConnection = false
    }
    // back from the Play purchase sheet (or anywhere): fresh unlock state, steps again
    LifecycleResumeEffect(Unit) {
        FullHd.refresh()
        unlockApp = isUnlocked(context)
        lastInteraction = System.currentTimeMillis()
        onPauseOrDispose { }
    }

    val idle = if (connections > 0 || renaming || settingsOpen || ended) 0L else now - maxOf(lastInteraction, lastDisconnect)
    val mode = homeMode(mirroring, audioOnly, idle)
    val clock = timeFormat.format(Date(now))
    // the phone mockup's status bar, iPhone style: "8:52", no leading zero or AM/PM
    val phoneClock = DateFormat.format(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm", now).toString()
    val link = remember(now / 5_000) { context.linkBars() }
    val online = remember(now / 5_000) { context.isOnline() }
    val weakLink = remember(now) { wifiWarnings && mode == HomeMode.MIRROR && viewModel.weakLink() }
    val from = sender.ifEmpty { "your iPhone" }
    // the service clears the name on disconnect, which is exactly when the paused dialog needs it
    var lastSender by remember { mutableStateOf("") }
    LaunchedEffect(sender) { if (sender.isNotEmpty()) lastSender = sender }
    val status = when {
        localNetwork != LocalNetworkAccess.GRANTED -> Status.PERMISSION
        !online -> Status.OFFLINE
        serverState == ServerState.ERROR -> Status.FAILED
        connections > 0 -> Status.CONNECTING
        else -> Status.READY
    }
    val buy = { FullHd.buy(context as Activity) }

    // plan toasts while mirroring, 8 s each: 2 minutes before the trial or a free session ends, and when the trial is over
    var trialOver by remember { mutableStateOf(false) }
    var lastPlan by remember { mutableStateOf(plan) }
    LaunchedEffect(plan) {
        if (lastPlan == Plan.TRIAL && plan == Plan.FREE && mirroring) trialOver = true
        lastPlan = plan
    }
    val sessionLeft = (FreeTime.SESSION_SECONDS - sessionSeconds).coerceAtLeast(0)
    val notice = when {
        trialOver -> Notice("trial-over", "Your free hour of Full HD is over", "You’re on Free now: 720p, ${FreeTime.SESSION_MINUTES} minutes at a time", neutral = true)
        plan == Plan.TRIAL && trialLeft <= FreeTime.WARN_SECONDS ->
            Notice("trial-ends", "Your free hour of Full HD ends in ${minutes(trialLeft)}", "Then 720p, ${FreeTime.SESSION_MINUTES} minutes at a time", neutral = true)
        plan == Plan.FREE && sessionLeft in 1..FreeTime.WARN_SECONDS ->
            Notice("session-ends", "${minutes(sessionLeft)} left in this free session", "Press OK to go unlimited" + (price?.let { " · $it once" } ?: ""))
        else -> null
    }
    var noticeShown by remember { mutableStateOf(false) }
    LaunchedEffect(notice?.key, mode == HomeMode.MIRROR) {
        if (notice == null || mode != HomeMode.MIRROR) return@LaunchedEffect
        noticeShown = true; delay(8_000); noticeShown = false
        trialOver = false
    }
    // the session warning is the only toast that takes OK: it opens the purchase
    val okBuys = noticeShown && notice?.key == "session-ends" && mode == HomeMode.MIRROR

    // nothing else is focusable outside the steps screen; the root catches the remote there
    val rootFocus = remember { FocusRequester() }
    var swallowUp by remember { mutableStateOf(false) }
    LaunchedEffect(mode) { if (mode != HomeMode.STEPS) rootFocus.requestFocusUntilLanded() }

    // with predictive back (targetSdk 37) Back never reaches onPreviewKeyEvent; on ambient it just wakes
    BackHandler(enabled = mode == HomeMode.AMBIENT) { lastInteraction = System.currentTimeMillis() }

    CompositionLocalProvider(LocalReducedMotion provides reduced) {
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
                    if (okBuys && (it.key == Key.DirectionCenter || it.key == Key.Enter)) { buy(); return@onPreviewKeyEvent true }
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
                    HomeMode.STEPS -> Home(
                        name = name, clock = phoneClock, status = status, from = from,
                        plan = plan, trialLeft = trialLeft,
                        link = link, online = online, wifiWarnings = wifiWarnings, localNetwork = localNetwork,
                        animate = !reduced && status == Status.READY && !settingsOpen && !renaming && pin == null,
                        onLocalNetwork = onLocalNetwork, onRetry = viewModel::startServer,
                        onSettings = { settingsOpen = true }
                    )
                    HomeMode.AUDIO -> Screensaver(name, now, clock, audioFrom = from)
                    else -> Screensaver(name, now, clock, audioFrom = null)
                }
            }
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 32.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(visible = noticeShown && notice != null && mode == HomeMode.MIRROR, enter = fadeIn(), exit = fadeOut()) {
                    notice?.let { n ->
                        Toast({ StrokeIcon(Icons.clock, 15f, 15f, 15.dp, if (n.neutral) Text1 else Attention, 1.7f) }, n.title, n.text, n.neutral)
                    }
                }
                AnimatedVisibility(visible = weakLink, enter = fadeIn(), exit = fadeOut()) {
                    Toast({ Bars(1, Attention) }, "Weak Wi-Fi", "The picture may stutter")
                }
            }
        }

        pin?.let { PinCode(it, from, onDismiss = viewModel::dismissPin) }

        if (ended && plan == Plan.FREE) {
            Ended(lastSender.ifEmpty { "your iPhone" }, price, onBuy = { ended = false; buy() }, onDismiss = { ended = false; lastInteraction = System.currentTimeMillis() })
        }

        if (settingsOpen) {
            SettingsPanel(
                viewModel = viewModel,
                name = name,
                plan = plan,
                trialLeft = trialLeft,
                price = price,
                onRename = { settingsOpen = false; renaming = true },
                onUnlock = buy,
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
}

private enum class Status { READY, CONNECTING, PERMISSION, OFFLINE, FAILED }

private class Notice(val key: String, val title: String, val text: String, val neutral: Boolean = false)

private fun minutes(seconds: Int) = FreeTime.minutes(seconds).let { if (it == 1) "1 minute" else "$it minutes" }

// ---- shared pieces ----

@Composable
private fun T(
    text: String,
    color: Color,
    size: TextUnit,
    weight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier,
    spacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = 1.3.em,
    tabular: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null
) = T(AnnotatedString(text), color, size, weight, modifier, spacing, lineHeight, tabular, maxLines, align)

// LocalTextStyle carries the Open Sauce family from the theme; leading defaults to the HTML's 1.3, not the theme's 24 sp
@Composable
private fun T(
    text: AnnotatedString,
    color: Color,
    size: TextUnit,
    weight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier,
    spacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = 1.3.em,
    tabular: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null
) = Text(
    text, modifier, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    style = LocalTextStyle.current.merge(
        TextStyle(
            fontSize = size, fontWeight = weight, letterSpacing = spacing, lineHeight = lineHeight,
            fontFeatureSettings = if (tabular) "tnum" else null, textAlign = align ?: TextAlign.Unspecified
        )
    )
)

private fun upgradeSub(price: String?) = price?.let { "$it once" }

// the Beamsy mark, monochrome: the phone shows through the TV's screen
@Composable
private fun BeamsyMark(size: Dp, color: Color = Text1) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 100f
        fun Path.rr(x: Float, y: Float, w: Float, h: Float, r: Float) =
            addRoundRect(RoundRect(x * u, y * u, (x + w) * u, (y + h) * u, CornerRadius(r * u)))
        drawRoundRect(color.copy(alpha = color.alpha * 0.42f), Offset(58 * u, 10 * u), Size(30 * u, 52 * u), CornerRadius(8 * u))
        drawPath(Path().apply { fillType = PathFillType.EvenOdd; rr(10f, 34f, 64f, 42f, 9f); rr(18f, 42f, 48f, 26f, 4f) }, color)
        drawRoundRect(color, Offset(29 * u, 82 * u), Size(26 * u, 6 * u), CornerRadius(3 * u))
    }
}

// the HTML's inline SVG icons, same path data
private object Icons {
    const val wifi = "M1.5 4.2a9.5 9.5 0 0 1 13 0M4 7a6 6 0 0 1 8 0"
    const val wifiDot = "M6.8 10a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0Z"
    const val mirror = "M2.5 1h6a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1 -1.5 1.5h-6a1.5 1.5 0 0 1 -1.5 -1.5v-4a1.5 1.5 0 0 1 1.5 -1.5ZM5 11h8.5a0.5 0.5 0 0 0 0.5 -0.5V5"
    const val tv = "M2.2 0.7h8.6a1.5 1.5 0 0 1 1.5 1.5v4.6a1.5 1.5 0 0 1 -1.5 1.5H2.2a1.5 1.5 0 0 1 -1.5 -1.5V2.2a1.5 1.5 0 0 1 1.5 -1.5ZM4 10.3h5"
    const val tick = "M1 4.6L4 7.6L10 1.4"
    const val chev = "M1 1l5 5l-5 5"
    const val clock = "M1.3 7.5a6.2 6.2 0 1 0 12.4 0a6.2 6.2 0 1 0 -12.4 0ZM7.5 4.2v3.5l2.3 1.4"
    const val warn = "M6.6 1.6L1.2 11a1 1 0 0 0 0.9 1.5h10.8a1 1 0 0 0 0.9 -1.5L8.4 1.6a1 1 0 0 0 -1.8 0ZM7.5 5.3v3M7.5 10.3v0.01"
}

@Composable
private fun StrokeIcon(d: String, vw: Float, vh: Float, width: Dp, color: Color, stroke: Float, fill: String? = null) {
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    val dot = remember(fill) { fill?.let { PathParser().parsePathString(it).toPath() } }
    Canvas(Modifier.size(width, width * (vh / vw))) {
        scale(size.width / vw, pivot = Offset.Zero) {
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            dot?.let { drawPath(it, color) }
        }
    }
}

@Composable
private fun Bars(lit: Int, color: Color) {
    Row(Modifier.height(10.dp), horizontalArrangement = Arrangement.spacedBy(1.5.dp), verticalAlignment = Alignment.Bottom) {
        listOf(4, 6, 8, 10).forEachIndexed { i, h ->
            Box(Modifier.width(2.5.dp).height(h.dp).background(color.copy(alpha = color.alpha * if (i < lit) 1f else 0.25f), RoundedCornerShape(0.75.dp)))
        }
    }
}

@Composable
private fun Spinner(size: Dp, color: Color = Text1, stroke: Dp = 2.dp) =
    CircularProgressIndicator(Modifier.size(size), color = color, strokeWidth = stroke)

// quiet at rest; on focus white with dark text and a slight lift
@Composable
private fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    large: Boolean = false,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val reduced = LocalReducedMotion.current
    val lift by animateFloatAsState(if (focused) 1f else 0f, tween(220, easing = Ease), label = "lift")
    val bg by animateColorAsState(if (focused) Text1 else Fill, tween(120, easing = LinearEasing), label = "bg")
    val fg by animateColorAsState(if (focused) OnFocus else Text1.copy(alpha = 0.92f), tween(120, easing = LinearEasing), label = "fg")
    val shape = RoundedCornerShape(if (large) 12.dp else 10.dp)
    Row(
        modifier
            .graphicsLayer {
                val scale = if (reduced) 1f else 1f + 0.06f * lift
                scaleX = scale; scaleY = scale
                shadowElevation = 14.dp.toPx() * lift
                this.shape = shape
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(bg)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f)
            .height(if (large) 42.dp else 34.dp)
            .padding(horizontal = if (large) 22.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val size = if (large) 16.sp else 14.sp
        T(text, fg, size, FontWeight.SemiBold, spacing = 0.005.em, maxLines = 1)
        sub?.let { T(it, fg.copy(alpha = fg.alpha * 0.62f), size, maxLines = 1) }
    }
}

// ---- home ----

private const val STEP_END = 4_000f
private val STEP_MS = longArrayOf(0, 2_600, 2_600, 3_800)

@Composable
private fun Home(
    name: String,
    clock: String,
    status: Status,
    from: String,
    plan: Plan,
    trialLeft: Int,
    link: Int?,
    online: Boolean,
    wifiWarnings: Boolean,
    localNetwork: LocalNetworkAccess,
    animate: Boolean,
    onLocalNetwork: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit
) {
    val settingsFocus = remember { FocusRequester() }
    val actionFocus = remember { FocusRequester() }
    val hasAction = status == Status.PERMISSION || status == Status.FAILED
    LaunchedEffect(hasAction) { (if (hasAction) actionFocus else settingsFocus).requestFocusUntilLanded() }

    // the three steps take turns on the phone; a problem state never shows the "done" tick
    var step by remember { mutableIntStateOf(1) }
    var prevStep by remember { mutableIntStateOf(0) }
    var t by remember { mutableFloatStateOf(STEP_END) } // ms into the current step
    LaunchedEffect(animate, status) {
        prevStep = step
        if (!animate) { step = if (status == Status.READY || status == Status.CONNECTING) 3 else 1; t = STEP_END; return@LaunchedEffect }
        // step and its timeline change in the same frame: a separate clock lagged one frame and flashed the old state
        step = 1; t = 0f
        var start = withFrameMillis { it }
        while (true) withFrameMillis { now ->
            val elapsed = now - start
            if (elapsed >= STEP_MS[step]) { prevStep = step; step = step % 3 + 1; start = now; t = 0f } else t = elapsed.toFloat()
        }
    }
    val dimmed = status != Status.READY

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to CanvasTop, 0.62f to Canvas0))) {
        Row(Modifier.padding(start = 48.dp, end = 48.dp, top = 28.dp).fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            BeamsyMark(22.dp)
            Spacer(Modifier.width(9.dp))
            T("Beamsy", Text1, 16.sp, FontWeight.SemiBold, spacing = (-0.01).em)
        }

        Column(Modifier.padding(start = 48.dp, bottom = 8.dp).width(540.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            T(if (status == Status.OFFLINE) "Waiting for a network" else "Mirror your iPhone to", Text2, 16.sp, lineHeight = 1.4.em)
            T(
                name, Text1, when { name.length <= 12 -> 76.sp; name.length <= 20 -> 54.sp; else -> 38.sp }, FontWeight.SemiBold,
                Modifier.padding(top = 4.dp), spacing = (-0.035).em, lineHeight = 1.02.em, maxLines = 1
            )
            StatusLine(status, from)
            if (status == Status.OFFLINE) T("Connect it to Wi-Fi or Ethernet in the TV’s settings.", Text3, 13.sp, modifier = Modifier.padding(top = 6.dp, start = 18.dp))
            if (hasAction) Btn(
                when {
                    status == Status.FAILED -> "Try again"
                    localNetwork == LocalNetworkAccess.SETTINGS -> "Open settings"
                    else -> "Allow access"
                },
                onClick = if (status == Status.FAILED) onRetry else onLocalNetwork,
                modifier = Modifier.padding(top = 18.dp).focusRequester(actionFocus),
                large = true
            )
            StepsList(name, step, animate, Modifier.padding(top = 38.dp).alpha(if (dimmed) 0.3f else 1f))
        }

        Device(step, prevStep, t, animate, name, clock, status == Status.CONNECTING,
            Modifier.offset(676.dp, 74.dp).alpha(if (dimmed && status != Status.CONNECTING) 0.3f else 1f))

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 48.dp, end = 48.dp, bottom = 28.dp).fillMaxWidth().height(34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // quiet on purpose: people buy at the end of a session, not from the home screen (Settings has it too)
            when (plan) {
                Plan.UNLIMITED -> T("Unlimited · Full HD · $FULL_FPS fps", Text3, 13.sp)
                Plan.TRIAL -> { T("Free trial · Full HD · $FULL_FPS fps", Text3, 13.sp); TrialLeft(trialLeft) }
                Plan.FREE -> T("Free · 720p · $FREE_FPS fps · ${FreeTime.SESSION_MINUTES} min at a time", Text3, 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Net(link, online, wifiWarnings)
            Btn("Settings", onSettings, Modifier.padding(start = 10.dp).focusRequester(settingsFocus))
        }
    }
}

@Composable
private fun StatusLine(status: Status, from: String) {
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = Text1)
    val (dot, text) = when (status) {
        Status.READY -> Ready to buildAnnotatedString { withStyle(bold) { append("Ready") }; append(" for iPhones on this network") }
        Status.CONNECTING -> null to buildAnnotatedString { append("Connecting to "); withStyle(bold) { append(from) }; append("…") }
        Status.PERMISSION -> Attention to AnnotatedString("Beamsy needs permission to find iPhones on your network")
        Status.OFFLINE -> Attention to AnnotatedString("This TV isn’t connected to a network")
        Status.FAILED -> Failure to AnnotatedString("iPhones can’t find this TV right now")
    }
    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (dot == null) Spinner(12.dp) else Box(Modifier.size(8.dp).background(dot, CircleShape))
        T(text, Text2, 15.sp, lineHeight = 1.35.em)
    }
}

@Composable
private fun StepsList(name: String, step: Int, animate: Boolean, modifier: Modifier) {
    val steps = listOf(
        AnnotatedString("Connect your iPhone to the same Wi-Fi as this TV"),
        AnnotatedString("Swipe down from the top-right to open Control Center"),
        buildAnnotatedString { append("Tap Screen Mirroring, then choose "); withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("“$name”") } }
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        steps.forEachIndexed { i, text ->
            // not cycling: every step reads at full strength
            val current = animate && step == i + 1
            val color by animateColorAsState(if (!animate || current) Text1 else Text3, tween(300, easing = LinearEasing), label = "step")
            val circle by animateColorAsState(if (current) Text1 else Color.Transparent, tween(300, easing = LinearEasing), label = "n")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(24.dp).background(circle, CircleShape)
                        .border(1.25.dp, if (current) Color.Transparent else if (animate) color else Text3, CircleShape),
                    contentAlignment = Alignment.Center
                ) { T("${i + 1}", if (current) Canvas0 else if (animate) color else Text2, 12.sp, FontWeight.Bold) }
                T(text, color, 17.sp, lineHeight = 24.sp)
            }
        }
    }
}

// the trial's Full HD minutes left
@Composable
private fun TrialLeft(seconds: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.14f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(seconds / FreeTime.TRIAL_SECONDS.toFloat()).background(Text2, RoundedCornerShape(2.dp)))
        }
        T("${FreeTime.minutes(seconds)} min left", Text2, 13.sp, tabular = true)
    }
}

@Composable
private fun Net(link: Int?, online: Boolean, warn: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            !online -> { StrokeIcon(Icons.warn, 15f, 14f, 15.dp, Attention, 1.7f); T("No network", Attention, 13.sp) }
            link == 0 -> T("Connected by Ethernet", Text2, 13.sp)
            link == 1 && warn -> { Bars(1, Attention); T("Weak Wi-Fi · mirroring may stutter", Attention, 13.sp) }
            link != null -> { Bars(link, Text2); T("Wi-Fi · " + listOf("Weak", "Fair", "Good", "Strong")[link - 1], Text2, 13.sp) }
            else -> {} // the driver didn't report a signal
        }
    }
}

// ---- the iPhone illustration: one timeline per step, the HTML's keyframes as time ranges ----

private fun seg(t: Float, start: Float, dur: Float, easing: Easing = LinearEasing) = easing.transform(((t - start) / dur).coerceIn(0f, 1f))

private fun mix(a: Float, b: Float, f: Float) = a + (b - a) * f

// CSS eases each segment between keyframes, not the whole animation: progress through [a, b], eased
private fun key(p: Float, a: Float, b: Float, easing: Easing) = easing.transform(((p - a) / (b - a)).coerceIn(0f, 1f))

private val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f) // the CSS default "ease"

// touch dot: (alpha, scale, dy); @keyframes swipe with var(--ease)
private fun swipe(t: Float): Triple<Float, Float, Float> {
    val p = (t - 200f) / 1250f
    return when {
        p < 0f -> Triple(0f, 0.6f, 0f)
        p < 0.15f -> key(p, 0f, 0.15f, Ease).let { Triple(it, mix(0.6f, 1f, it), 0f) }
        p < 0.7f -> Triple(1f, 1f, 104f * key(p, 0.15f, 0.7f, Ease))
        p < 1f -> key(p, 0.7f, 1f, Ease).let { Triple(1f - it, 1f, mix(104f, 118f, it)) }
        else -> Triple(0f, 1f, 118f)
    }
}

// touch dot: (alpha, scale); @keyframes tap with the default "ease". Scale keyframes 0/25/45 %, opacity 0/25/75/100 %
private fun tap(t: Float, start: Float): Pair<Float, Float> {
    val p = (t - start) / 800f
    if (p < 0f) return 0f to 1.4f
    if (p >= 1f) return 0f to 0.82f
    val alpha = when {
        p < 0.25f -> key(p, 0f, 0.25f, CssEase)
        p < 0.75f -> 1f
        else -> 1f - key(p, 0.75f, 1f, CssEase)
    }
    val scale = when {
        p < 0.25f -> mix(1.4f, 1f, key(p, 0f, 0.25f, CssEase))
        p < 0.45f -> mix(1f, 0.82f, key(p, 0.25f, 0.45f, CssEase))
        else -> 0.82f
    }
    return alpha to scale
}

@Composable
private fun Touch(x: Int, y: Int, alpha: Float, scale: Float, dy: Float = 0f) = Box(
    Modifier.offset(x.dp, y.dp).size(22.dp)
        .graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale; translationY = dy.dp.toPx() }
        .background(Color.White.copy(alpha = 0.5f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
)

@Composable
private fun Device(step: Int, prevStep: Int, t: Float, animate: Boolean, name: String, clock: String, connecting: Boolean, modifier: Modifier) {
    val leaving = step == 1 && prevStep == 3 // the sheet and Control Center fade away
    fun shown(start: Float, dur: Float, easing: Easing = LinearEasing) = when {
        step == 3 -> seg(t, start, dur, easing)
        leaving -> 1f - seg(t, 0f, dur, easing)
        else -> 0f
    }
    val ccAlpha = when (step) { 2 -> seg(t, 500f, 260f); 3 -> 1f; else -> if (leaving) 1f - seg(t, 0f, 260f) else 0f }
    val ccShift = when (step) { 2 -> 1f - seg(t, 500f, 420f, Ease); 3 -> 0f; else -> if (leaving) seg(t, 0f, 420f, Ease) else 1f }
    val tileOn = shown(450f, 120f)
    val sheetAlpha = shown(1_000f, 180f)
    val sheetScale = mix(0.4f, 1f, shown(1_000f, 380f, Ease))
    val rowOn = shown(1_950f, 150f)
    val tickAlpha = shown(2_050f, 150f)
    val tilesAlpha = 1f - 0.65f * shown(1_000f, 300f) // Control Center dims behind the sheet, as on iOS
    val phone = RoundedCornerShape(34.dp)

    Box(
        modifier
            .size(184.dp, 392.dp)
            .graphicsLayer { shadowElevation = 28.dp.toPx(); shape = phone }
            .background(Color(0xFF1A1A1C), phone)
            .border(1.dp, Color.White.copy(alpha = 0.16f), phone)
            .padding(6.dp)
    ) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(0f to Color(0xFF34343A), 0.55f to Color(0xFF1B1B20), 1f to Color(0xFF121215), start = Offset.Zero, end = Offset(250f, 760f)))
        ) {
            // status bar
            T(clock, Text1, 9.sp, FontWeight.SemiBold, Modifier.offset(20.dp, 12.dp))
            Box(Modifier.align(Alignment.TopCenter).offset(y = 9.dp).size(52.dp, 16.dp).background(Color.Black, RoundedCornerShape(8.dp)))
            Row(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // two pulses around the Wi-Fi icon on step 1, drawn behind it: as a laid-out box it grew the row and shifted the icons
                val p = ((t - 300f) / 1_300f).takeIf { animate && step == 1 && it >= 0f && it < 2f }?.let { it % 1f }
                Box(Modifier.drawBehind {
                    if (p == null) return@drawBehind
                    // the CSS scales the whole 24 dp ring, stroke included
                    val s = mix(0.6f, 1.35f, Ease.transform(p))
                    val stroke = 1.5.dp.toPx() * s
                    // each keyframe segment eased like the CSS: a quick blink, not a lingering ring
                    val alpha = if (p < 0.25f) key(p, 0f, 0.25f, Ease) else 1f - key(p, 0.25f, 1f, Ease)
                    drawCircle(Color.White.copy(alpha = alpha), 12.dp.toPx() * s - stroke / 2, style = Stroke(stroke))
                }) {
                    StrokeIcon(Icons.wifi, 16f, 12f, 11.dp, Text1, 1.8f, Icons.wifiDot)
                }
                Box(Modifier.size(16.dp, 8.dp).border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.5.dp)).padding(1.5.dp)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(0.7f).background(Color.White, RoundedCornerShape(1.dp)))
                }
            }
            // home screen: 4×4 apps and the dock
            for (i in 0 until 16) Box(
                Modifier.offset((18 + (i % 4) * 37).dp, (48 + (i / 4) * 41).dp).size(25.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(9.dp))
            )
            Box(Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().height(54.dp).background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(22.dp))) {
                for (i in 0 until 4) Box(Modifier.align(Alignment.CenterStart).offset((10 + i * 37).dp).size(25.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(9.dp)))
            }

            // Control Center (denser than the HTML's 0.62: Android draws no blur behind it)
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = ccAlpha; translationY = -14.dp.toPx() * ccShift }.background(Color(0xFF08080A).copy(alpha = 0.88f))) {
              Box(Modifier.fillMaxSize().graphicsLayer { alpha = tilesAlpha }) {
                fun pos(col: Int, row: Int) = Modifier.offset((14 + col * 38).dp, (42 + row * 38).dp)
                fun span(n: Int) = (30 * n + 8 * (n - 1)).dp
                val tile = Color.White.copy(alpha = 0.14f)
                Box(pos(0, 0).size(span(2)).background(tile, RoundedCornerShape(16.dp)).padding(7.dp)) {
                    for (i in 0 until 4) Box(
                        Modifier.offset((i % 2 * 30 + 2).dp, (i / 2 * 30 + 2).dp).size(20.dp)
                            .background(if (i == 0) Color.White else Color.White.copy(alpha = 0.22f), CircleShape)
                    )
                }
                Box(pos(2, 0).size(span(2)).background(tile, RoundedCornerShape(16.dp)))
                Box(pos(0, 2).size(30.dp).background(tile, CircleShape))
                Box(pos(1, 2).size(30.dp).background(lerp(tile, Color.White, tileOn), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    StrokeIcon(Icons.mirror, 15f, 12f, 15.dp, lerp(Color.White, Color.Black, tileOn), 1.5f)
                }
                Box(pos(2, 2).size(30.dp, span(2)).background(Brush.verticalGradient(0.45f to tile, 0.45f to Color.White), RoundedCornerShape(12.dp)))
                Box(pos(3, 2).size(30.dp, span(2)).background(Brush.verticalGradient(0.65f to tile, 0.65f to Color.White), RoundedCornerShape(12.dp)))
                Box(pos(0, 3).size(span(2), 30.dp).background(tile, RoundedCornerShape(12.dp)))
                for (c in 0 until 4) Box(pos(c, 4).size(30.dp).background(tile, CircleShape))
              }
            }

            // Screen Mirroring sheet, scaled in from the tile that opened it
            Column(
                Modifier.padding(start = 12.dp, end = 12.dp, top = 112.dp).fillMaxWidth()
                    .graphicsLayer { alpha = sheetAlpha; scaleX = sheetScale; scaleY = sheetScale; transformOrigin = TransformOrigin(55f / 148f, 19f / 73f) }
                    .background(Color(0xFF2C2C30).copy(alpha = 0.97f), RoundedCornerShape(18.dp))
                    .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
            ) {
                T("Screen Mirroring", Text1, 10.sp, FontWeight.SemiBold, Modifier.fillMaxWidth().padding(bottom = 10.dp), align = TextAlign.Center)
                Row(
                    Modifier.fillMaxWidth().height(30.dp).background(Color.White.copy(alpha = mix(0.07f, 0.2f, rowOn)), RoundedCornerShape(10.dp)).padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StrokeIcon(Icons.tv, 13f, 11f, 13.dp, Text1, 1.4f)
                    T(name, Text1, 10.sp, FontWeight.SemiBold, Modifier.weight(1f), maxLines = 1)
                    if (connecting) Spinner(10.dp, stroke = 1.5.dp)
                    else Box(Modifier.alpha(tickAlpha)) { StrokeIcon(Icons.tick, 11f, 9f, 11.dp, Text1, 1.8f) }
                }
            }

            // touches
            if (animate && step == 2) swipe(t).let { (a, s, dy) -> Touch(140, 4, a, s, dy) }
            if (animate && step == 3) {
                tap(t, 100f).let { (a, s) -> Touch(56, 120, a, s) }
                tap(t, 1_600f).let { (a, s) -> Touch(75, 150, a, s) }
            }
        }
    }
}

// ---- screensaver and audio ----

@Composable
private fun Screensaver(name: String, now: Long, clock: String, audioFrom: String?) {
    val date = remember(now / 60_000) { DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"), now).toString() }
    // keeps pixels moving: no burn-in
    val t = now / 60_000
    val dx by animateDpAsState(((t * 37) % 41 - 20).toInt().dp, tween(2_000, easing = Ease), label = "dx")
    val dy by animateDpAsState(((t * 53) % 31 - 15).toInt().dp, tween(2_000, easing = Ease), label = "dy")
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.align(Alignment.Center).offset(dx, dy), horizontalAlignment = Alignment.CenterHorizontally) {
            T(clock, Text1.copy(alpha = 0.88f), 128.sp, FontWeight.Normal, spacing = (-0.045).em, lineHeight = 1.em, tabular = true)
            T(date, Text3, 20.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            BeamsyMark(16.dp, Text2)
            T(name, Text2, 13.sp, FontWeight.SemiBold, maxLines = 1)
            Box(Modifier.size(3.dp).background(Text3, CircleShape))
            if (audioFrom != null) {
                Row(Modifier.height(11.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(5, 11, 8).forEach { Box(Modifier.width(2.5.dp).height(it.dp).background(Ready, RoundedCornerShape(1.dp))) }
                }
                T("Playing audio from $audioFrom", Text3, 13.sp)
            } else T("Ready for Screen Mirroring", Text3, 13.sp)
        }
    }
}

@Composable
private fun Toast(icon: @Composable () -> Unit, title: String, text: String, neutral: Boolean = false) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.graphicsLayer { shadowElevation = 12.dp.toPx(); this.shape = shape }
            .background(Material.copy(alpha = 0.94f), shape).border(1.dp, Hairline, shape)
            .padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(30.dp).background(if (neutral) Color.White.copy(alpha = 0.1f) else Attention.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { icon() }
        Column {
            T(title, Text1, 14.sp, FontWeight.SemiBold)
            T(text, Text2, 12.sp)
        }
    }
}

// ---- overlays: full-window dialogs keep the remote's focus inside them ----

@Composable
private fun Overlay(onDismiss: () -> Unit, scrim: Float = 0.55f, content: @Composable BoxScope.() -> Unit) {
    // a dialog is its own window, which resets the density: keep the stage's (scaled on phones)
    val stage = LocalDensity.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // our own scrim replaces the window dim
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            window?.let { WindowCompat.getInsetsController(it, it.decorView).hide(WindowInsetsCompat.Type.systemBars()) }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalDensity provides stage) { Box(Modifier.size(STAGE_WIDTH, STAGE_HEIGHT), content = content) }
        }
    }
}

@Composable
private fun DialogCard(modifier: Modifier, horizontal: Dp, top: Dp, bottom: Dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .graphicsLayer { shadowElevation = 30.dp.toPx(); this.shape = shape }
            .background(Material, shape).border(1.dp, Hairline, shape)
            .padding(start = horizontal, end = horizontal, top = top, bottom = bottom),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

// the iPhone asks for this code when "Require a code to mirror" is on
@Composable
private fun PinCode(pin: String, from: String, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        DialogCard(Modifier.align(Alignment.Center).width(600.dp), 40.dp, 40.dp, 32.dp) {
            T("${from.replaceFirstChar { it.uppercase() }} wants to mirror", Text2, 15.sp)
            T("Enter this code on your iPhone", Text1, 26.sp, FontWeight.SemiBold, Modifier.padding(top = 6.dp), spacing = (-0.015).em)
            Row(Modifier.padding(top = 30.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                pin.forEach { d ->
                    Box(
                        Modifier.width(72.dp)
                            .drawUnderline(Color.White.copy(alpha = 0.22f))
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { T("$d", Text1, 92.sp, FontWeight.SemiBold, spacing = (-0.02).em, lineHeight = 1.em, tabular = true) }
                }
            }
            T("Press Back to hide the code", Text3, 13.sp)
        }
    }
}

private fun Modifier.drawUnderline(color: Color) = drawBehind {
    drawRect(color, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
}

// a free session reached 15 minutes: the service already ended it
@Composable
private fun Ended(from: String, price: String?, onBuy: () -> Unit, onDismiss: () -> Unit) {
    val buyFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { buyFocus.requestFocusUntilLanded() }
    Overlay(onDismiss, scrim = 0.72f) {
        DialogCard(Modifier.align(Alignment.Center).width(580.dp), 40.dp, 36.dp, 30.dp) {
            T("Mirroring paused", Text2, 15.sp)
            T("Free sessions last ${FreeTime.SESSION_MINUTES} minutes", Text1, 26.sp, FontWeight.SemiBold, Modifier.padding(top = 6.dp), spacing = (-0.015).em)
            T(
                "Reconnect from $from to keep going, or go unlimited to watch without breaks, in Full HD at $FULL_FPS fps.",
                Text2, 15.sp, modifier = Modifier.padding(top = 12.dp).widthIn(max = 440.dp), lineHeight = 1.35.em, align = TextAlign.Center
            )
            Row(Modifier.padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Btn("Go unlimited", onBuy, Modifier.focusRequester(buyFocus), sub = upgradeSub(price), large = true)
                Btn("Not now", onDismiss, large = true)
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
        val shape = RoundedCornerShape(22.dp)
        Column(
            Modifier.align(Alignment.TopCenter).offset(y = 32.dp).width(560.dp)
                .graphicsLayer { shadowElevation = 30.dp.toPx(); this.shape = shape }
                .background(Material, shape).border(1.dp, Hairline, shape)
                .padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 24.dp)
        ) {
            T("Rename this TV", Text1, 26.sp, FontWeight.SemiBold, spacing = (-0.015).em)
            T("iPhones show this name in Screen Mirroring.", Text2, 15.sp, modifier = Modifier.padding(top = 6.dp))
            BasicTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em, color = Text1)),
                cursorBrush = SolidColor(Text1),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) save() }),
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(56.dp).focusRequester(fieldFocus)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(2.dp, if (valid) Text1 else Failure, RoundedCornerShape(12.dp)),
                decorationBox = { inner -> Box(Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) { inner() } }
            )
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                T(
                    when { valid -> "$count of 40"; count > 0 -> "$count of 40 · too long"; else -> "Enter a name" },
                    if (valid) Text3 else Failure, 13.sp, modifier = Modifier.weight(1f), tabular = true
                )
                Btn("Cancel", onDismiss, large = true)
                Btn("Save", ::save, large = true, enabled = valid)
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    viewModel: MainViewModel,
    name: String,
    plan: Plan,
    trialLeft: Int,
    price: String?,
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
                .width(420.dp)
                .fillMaxHeight()
                .background(Material)
                .drawBehind { drawRect(Hairline, size = Size(1.dp.toPx(), size.height)) }
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 22.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                T("Settings", Text1, 26.sp, FontWeight.SemiBold, spacing = (-0.015).em)
                T("$name · Beamsy $version", Text3, 13.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1)
            }
            Column(Modifier.weight(1f).padding(top = 14.dp).fadeEdges().verticalScroll(rememberScrollState()).padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 24.dp)) {
                Section("This TV", first = true)
                SettingRow("TV name", name, onRename, chevron = true, modifier = Modifier.focusRequester(firstFocus))
                PrefRow(viewModel, "Open when the TV turns on", Prefs.BOOT_AUTO_START)
                PrefRow(viewModel, "Open when an iPhone connects", Prefs.LAUNCH_ON_CONNECT)
                Section("Plan")
                when (plan) {
                    Plan.UNLIMITED -> SettingRow("Beamsy Unlimited", "Owned", {})
                    Plan.TRIAL -> SettingRow("Free trial", "${FreeTime.minutes(trialLeft)} min of Full HD left", {})
                    Plan.FREE -> SettingRow("Free plan", "${FreeTime.SESSION_MINUTES} min sessions", {})
                }
                if (plan != Plan.UNLIMITED) SettingRow("Go unlimited", price?.let { "Full HD · $it" } ?: "No limits, Full HD", onUnlock, chevron = true) // local prices run long (179,99 CZK)
                Section("Picture")
                SettingRow("Picture quality", if (plan == Plan.FREE) "720p · $FREE_FPS fps" else "Full HD · $FULL_FPS fps", {})
                PrefRow(viewModel, "Blur the sides of upright video", Prefs.BLUR_BARS)
                PrefRow(viewModel, "Fit the whole picture on screen", Prefs.OVERSCANNED)
                PrefRow(viewModel, "Warn about weak Wi-Fi", Prefs.WIFI_WARNINGS)
                Section("Privacy")
                PrefRow(viewModel, "Require a code to mirror", Prefs.REQUIRE_PIN)
                PrefRow(viewModel, "Let another iPhone take over", Prefs.ALLOW_NEW_CONN)
            }
            T("Press Back to close", Text3, 13.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp))
        }
    }
}

// rows scrolling out fade instead of being cut: 10 dp at the top, 28 dp at the bottom
private fun Modifier.fadeEdges() = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
    drawContent()
    val top = 10.dp.toPx() / size.height
    val bottom = 1f - 28.dp.toPx() / size.height
    drawRect(Brush.verticalGradient(0f to Color.Transparent, top to Color.Black, bottom to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
}

@Composable
private fun Section(title: String, first: Boolean = false) =
    T(title, Text3, 13.sp, FontWeight.SemiBold, Modifier.padding(start = 14.dp, end = 14.dp, top = if (first) 6.dp else 16.dp, bottom = 6.dp))

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit, chevron: Boolean = false, on: Boolean = false, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if (focused && !reduced) 1.025f else 1f, tween(220, easing = Ease), label = "lift")
    val bg by animateColorAsState(if (focused) Text1 else Color.Transparent, tween(120, easing = LinearEasing), label = "bg")
    val fg = if (focused) OnFocus else Text1.copy(alpha = 0.92f)
    val valueColor = when { on && focused -> OnFocus; on -> Text1; focused -> OnFocus2; else -> Text2 }
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier
            .padding(bottom = 2.dp)
            .fillMaxWidth()
            .height(42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        T(label, fg, 16.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            T(value, valueColor, 15.sp, tabular = true, maxLines = 1)
            if (chevron) StrokeIcon(Icons.chev, 7f, 12f, 7.dp, valueColor.copy(alpha = valueColor.alpha * 0.7f), 1.8f)
        }
    }
}

@Composable
private fun PrefRow(viewModel: MainViewModel, label: String, pref: BoolPref) {
    val on by viewModel.pref(pref).collectAsState()
    SettingRow(label, if (on) "On" else "Off", { viewModel.set(pref, !on) }, on = on)
}
