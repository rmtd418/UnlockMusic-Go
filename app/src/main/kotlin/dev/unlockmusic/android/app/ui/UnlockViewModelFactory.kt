package dev.unlockmusic.android.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.unlockmusic.android.app.execution.UnlockExecutionPreflightChecker
import dev.unlockmusic.android.app.execution.UnlockForegroundServiceStarter
import dev.unlockmusic.android.data.settings.LastSessionSettingsStore
import dev.unlockmusic.android.domain.usecase.RetryUnfinishedUnlockTasksUseCase

class UnlockViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UnlockViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        return UnlockViewModel(
            serviceStarter = UnlockForegroundServiceStarter(context),
            preflightChecker = UnlockExecutionPreflightChecker(context),
            lastSessionSettingsStore = LastSessionSettingsStore(context),
            retryUnfinishedUnlockTasks = RetryUnfinishedUnlockTasksUseCase(),
        ) as T
    }
}
