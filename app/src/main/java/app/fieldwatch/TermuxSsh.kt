package app.fieldwatch

import android.content.Context
import android.content.Intent
import android.util.Log

/** Starts the key-only Termux SSH daemon through Termux's background service. */
object TermuxSsh {
    private const val TAG = "FieldwatchSsh"
    private const val ACTION_RUN = "com.termux.RUN_COMMAND"
    private const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val COMMAND =
        "/data/data/com.termux/files/home/.local/bin/fieldwatch-ensure-sshd"
    private const val HOME = "/data/data/com.termux/files/home"

    fun ensureRunning(context: Context) {
        val intent = Intent(ACTION_RUN)
            .setClassName("com.termux", "com.termux.app.RunCommandService")
            .putExtra(EXTRA_PATH, COMMAND)
            .putExtra(EXTRA_WORKDIR, HOME)
            .putExtra(EXTRA_BACKGROUND, true)
        runCatching { context.startService(intent) }
            .onFailure { Log.w(TAG, "Termux SSH service unavailable", it) }
    }
}
