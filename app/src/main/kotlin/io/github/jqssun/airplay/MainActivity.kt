package io.github.jqssun.airplay

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.jqssun.airplay.service.AirPlayService
import io.github.jqssun.airplay.ui.HomeScreen
import io.github.jqssun.airplay.ui.theme.AirPlayTheme
import io.github.jqssun.airplay.viewmodel.LocalNetworkAccess
import io.github.jqssun.airplay.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var service: AirPlayService? = null
    private val logCallback: (String) -> Unit = { viewModel.addLog(it) }
    private val pinCallback: (String?) -> Unit = { viewModel.showPin(it) }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as? AirPlayService.LocalBinder)?.service ?: return
            service = svc
            svc.logCallback = logCallback
            svc.pinCallback = pinCallback
            viewModel.bindService(svc)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) _refreshForeground()
            _autoStart()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            viewModel.unbindService()
        }
    }

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { if (it.isNotEmpty()) _syncLocalNetwork(requested = true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val missing = buildList {
            if (!hasLocalNetwork()) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
        if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray())

        bindService(Intent(this, AirPlayService::class.java), connection, BIND_AUTO_CREATE)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    viewModel.updateFromService()
                    delay(200)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // held whenever started, so ambient isn't replaced by the system screensaver (which stops the server)
                viewModel.pref(Prefs.KEEP_SCREEN_ON).collect { on ->
                    if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            AirPlayTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onSurfaceAvailable = { viewModel.onSurfaceAvailable(it) },
                    onSurfaceDestroyed = { viewModel.onSurfaceDestroyed(it) },
                    onLocalNetwork = ::_requestLocalNetwork
                )
            }
        }
    }

    private fun _autoStart() {
        val svc = service ?: return
        val state = svc.serverState.value
        // ERROR too: a failed start (e.g. port taken) gets another try on the next onStart
        if (viewModel.pref(Prefs.AUTO_START).value && (state == AirPlayService.ServerState.STOPPED || state == AirPlayService.ServerState.ERROR) && hasLocalNetwork()) {
            viewModel.startServer()
        }
    }
    
    private fun _refreshForeground() {
        if (service?.serverState?.value == AirPlayService.ServerState.RUNNING) {
            startService(Intent(this, AirPlayService::class.java))
        }
    }

    private fun _syncLocalNetwork(requested: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
        val p = Manifest.permission.ACCESS_LOCAL_NETWORK
        val prev = viewModel.localNetwork.value
        val now = when {
            hasLocalNetwork() -> LocalNetworkAccess.GRANTED
            shouldShowRequestPermissionRationale(p) -> LocalNetworkAccess.ASK
            requested || prev == LocalNetworkAccess.SETTINGS -> LocalNetworkAccess.SETTINGS
            else -> LocalNetworkAccess.ASK
        }
        viewModel.setLocalNetwork(now)
        if (now == LocalNetworkAccess.GRANTED && prev != LocalNetworkAccess.GRANTED) _autoStart()
    }

    private fun _requestLocalNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
        if (viewModel.localNetwork.value == LocalNetworkAccess.SETTINGS) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } else {
            permissions.launch(arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK))
        }
    }

    override fun onStart() {
        super.onStart()
        _syncLocalNetwork()
        _refreshForeground()
        _autoStart() // re-advertise after returning to the foreground
    }

    override fun onStop() {
        super.onStop()
        // foreground only: leaving the app closes sessions, even mid-session
        if (!isChangingConfigurations) viewModel.stopServer()
    }

    override fun onDestroy() {
        service?.let {
            if (it.logCallback === logCallback) it.logCallback = null
            if (it.pinCallback === pinCallback) it.pinCallback = null
        }
        unbindService(connection)
        super.onDestroy()
    }
}
