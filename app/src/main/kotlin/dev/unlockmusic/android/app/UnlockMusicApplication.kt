package dev.unlockmusic.android.app

import android.app.Application
import dev.unlockmusic.android.app.execution.UnlockExecutionPersistenceCoordinator
import dev.unlockmusic.android.data.settings.UnlockExecutionSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class UnlockMusicApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        UnlockExecutionPersistenceCoordinator(
            snapshotStore = UnlockExecutionSnapshotStore(this),
        ).initialize(applicationScope)
    }
}
