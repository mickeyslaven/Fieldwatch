package app.fieldwatch

import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import app.fieldwatch.alert.Alerter
import app.fieldwatch.radio.RadioPermissions
import app.fieldwatch.ui.FieldwatchRoot
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.screen.CrashNoteScreen
import app.fieldwatch.ui.theme.FieldwatchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: FieldwatchViewModel by viewModels()
    @Suppress("DEPRECATION")
    private var carKeyguardLock: KeyguardManager.KeyguardLock? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        vm.refreshPermissions()
        if (RadioPermissions.granted(this)) {
            (application as FieldwatchApp).refreshFix()
            vm.startScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 7 on the Shield can leave a swipe-only keyguard above an activity
        // launched by BOOT_COMPLETED even with the window flags below. This is a
        // dedicated car display, so use the legacy API that this OS actually honors.
        @Suppress("DEPRECATION")
        carKeyguardLock =
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .newKeyguardLock("fieldwatch:car-display")
                .also { it.disableKeyguard() }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )
        enableEdgeToEdge()
        val app = application as FieldwatchApp
        if (app.pendingCrash != null) {
            showCrashNote(app)
            return
        }
        showApp()
    }

    override fun onResume() {
        super.onResume()
        val app = application as FieldwatchApp
        if (app.pendingCrash != null) return
        vm.refreshPermissions()
        if (RadioPermissions.granted(this) && !app.devices.stats.value.scanning) {
            vm.startScan()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if ((application as FieldwatchApp).pendingCrash != null) return
        intent.getStringExtra(Alerter.EXTRA_DEVICE_KEY)?.let { vm.select(it) }
    }

    private fun showCrashNote(app: FieldwatchApp) {
        val pending = app.pendingCrash
        if (pending == null) {
            showApp()
            return
        }
        setContent {
            FieldwatchTheme {
                CrashNoteScreen(
                    note = pending.text,
                    onCopy = { copyNote(pending.text) },
                    onContinue = {
                        app.dismissCrashNote()
                        showApp()
                    },
                )
            }
        }
    }

    private fun showApp() {
        setContent {
            FieldwatchRoot(vm) {
                permissionLauncher.launch(RadioPermissions.required())
            }
        }
        if (RadioPermissions.granted(this)) {
            lifecycleScope.launch { vm.startScan() }
        }
        intent?.getStringExtra(Alerter.EXTRA_DEVICE_KEY)?.let { vm.select(it) }
    }

    private fun copyNote(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Fieldwatch crash", text))
    }
}
