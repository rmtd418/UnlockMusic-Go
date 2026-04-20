package dev.unlockmusic.android.app.execution

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class UnlockForegroundServiceStarter(
    private val context: Context,
) {
    fun start() {
        val intent =
            Intent(context, UnlockForegroundService::class.java).apply {
                action = UnlockForegroundService.ACTION_RUN_QUEUE
            }
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancel() {
        context.startService(
            Intent(context, UnlockForegroundService::class.java).apply {
                action = UnlockForegroundService.ACTION_CANCEL_QUEUE
            },
        )
    }
}
