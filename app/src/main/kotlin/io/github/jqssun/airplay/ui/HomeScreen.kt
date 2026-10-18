package io.github.jqssun.airplay.ui

import android.content.Context
import android.text.format.DateFormat
import android.view.Surface
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.isUnlocked
import io.github.jqssun.airplay.openUnlockListing
import io.github.jqssun.airplay.tvName
import io.github.jqssun.airplay.viewmodel.LocalNetworkAccess
import io.github.jqssun.airplay.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.util.Date

private val Wallpaper = Brush.linearGradient(listOf(Color(0xFF0C1220), Color(0xFF14213A), Color(0xFF1A3140)))
private val Ink = Color(0xFFF2F5FA)
private val InkSoft = Color(0xFFA3B0C2)
private val Accent = Color(0xFF7CC4FF)

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
    val aspect by viewModel.videoAspect.collectAsState()
    val localNetwork by viewModel.localNetwork.collectAsState()
    val savedName by viewModel.pref(Prefs.SERVER_NAME).collectAsState()
    val prefs = remember { context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE) }
    val name = remember(savedName) { context.tvName(prefs) }
    val timeFormat = remember { DateFormat.getTimeFormat(context) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastInteraction by remember { mutableLongStateOf(now) }
    var lastDisconnect by remember { mutableLongStateOf(now) }
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

    val idle = if (connections > 0 || renaming) 0L else now - maxOf(lastInteraction, lastDisconnect)
    val mode = homeMode(mirroring, audioOnly, idle)
    val clock = timeFormat.format(Date(now))

    // nothing else is focusable outside the steps screen; the root catches the remote there
    val rootFocus = remember { FocusRequester() }
    var swallowUp by remember { mutableStateOf(false) }
    LaunchedEffect(mode) { if (mode != HomeMode.STEPS) rootFocus.requestFocusUntilLanded() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    // the wake press's key-up would otherwise click the newly focused "Rename TV"
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
            aspectRatio = aspect,
            modifier = Modifier.align(Alignment.Center).alpha(if (mode == HomeMode.MIRROR) 1f else 0f)
        )
        Crossfade(targetState = mode, label = "home") { m ->
            if (m == HomeMode.MIRROR) return@Crossfade
            // wallpaper is drawn here, over the SurfaceView's hole, so idle modes never show video
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Wallpaper)
                    .padding(horizontal = 48.dp, vertical = 27.dp) // ~5% TV overscan safe area
            ) {
                when (m) {
                    HomeMode.STEPS -> Steps(
                        name = name,
                        clock = clock,
                        unlocked = unlocked,
                        localNetwork = localNetwork,
                        onLocalNetwork = onLocalNetwork,
                        onUnlock = { runCatching { openUnlockListing(context) } }, // no store or browser on some TVs
                        onRename = { renaming = true }
                    )
                    HomeMode.AUDIO -> Ambient(name, clock, "Playing audio from ${sender.ifEmpty { "your iPhone" }}")
                    else -> Ambient(name, clock, "Screen Mirroring on your iPhone to connect")
                }
            }
        }
    }

    if (renaming) {
        RenameDialog(
            current = name,
            onSave = { viewModel.set(Prefs.SERVER_NAME, it) },
            onDismiss = { renaming = false; lastInteraction = System.currentTimeMillis() }
        )
    }
}

@Composable
private fun Steps(
    name: String,
    clock: String,
    unlocked: Boolean,
    localNetwork: LocalNetworkAccess,
    onLocalNetwork: () -> Unit,
    onUnlock: () -> Unit,
    onRename: () -> Unit
) {
    val renameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { renameFocus.requestFocusUntilLanded() }

    Box(Modifier.fillMaxSize()) {
        Text(clock, Modifier.align(Alignment.TopEnd), color = InkSoft, fontSize = 22.sp)

        Row(
            Modifier.fillMaxSize().padding(top = 24.dp, bottom = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 32.dp)) {
                Text("This TV is called", color = InkSoft, fontSize = 20.sp)
                Text(
                    name, color = Ink, fontSize = 52.sp, fontWeight = FontWeight.Light,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(36.dp))
                listOf(
                    "Connect your iPhone to the same Wi-Fi as this TV",
                    "Open Control Center",
                    "Tap Screen Mirroring → $name"
                ).forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).border(1.5.dp, Accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("${i + 1}", color = Accent, fontSize = 18.sp) }
                        Spacer(Modifier.width(18.dp))
                        Text(step, color = Ink, fontSize = 22.sp)
                    }
                }
                if (localNetwork != LocalNetworkAccess.GRANTED) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.local_network_label), color = InkSoft, fontSize = 18.sp)
                        Spacer(Modifier.width(12.dp))
                        HomeButton(
                            stringResource(
                                if (localNetwork == LocalNetworkAccess.SETTINGS) R.string.btn_open_settings
                                else R.string.btn_allow
                            ),
                            onClick = onLocalNetwork,
                            color = Accent
                        )
                    }
                }
            }
            Image(painterResource(R.drawable.control_center), contentDescription = null, Modifier.height(300.dp))
        }

        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (unlocked) Text("Full HD · unlocked", color = InkSoft, fontSize = 16.sp)
            else HomeButton("Free · 720p · Full HD: get the Unlock app, \$6.99 once", onClick = onUnlock)
            HomeButton("Rename TV", onClick = onRename, modifier = Modifier.focusRequester(renameFocus))
        }
    }
}

@Composable
private fun HomeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = InkSoft) {
    TextButton(
        onClick = onClick,
        modifier = modifier.dpadFocus(),
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) { Text(text, fontSize = 16.sp) }
}

@Composable
private fun Ambient(name: String, clock: String, hint: String) {
    Box(Modifier.fillMaxSize()) {
        Text(clock, Modifier.align(Alignment.TopStart), color = Ink, fontSize = 96.sp, fontWeight = FontWeight.Light)
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(name, color = Ink, fontSize = 34.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hint, color = InkSoft, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RenameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue(current, TextRange(current.length))) }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { fieldFocus.requestFocusUntilLanded() }
    val valid = isValidTvName(field.text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename TV") },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                isError = !valid,
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus).dpadFocus()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(field.text.trim()); onDismiss() },
                enabled = valid,
                modifier = Modifier.dpadFocus()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.dpadFocus()) { Text("Cancel") }
        }
    )
}
