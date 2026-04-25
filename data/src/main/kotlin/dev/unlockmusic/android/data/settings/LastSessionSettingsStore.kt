package dev.unlockmusic.android.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

class LastSessionSettingsStore(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    @Volatile
    private var migrationComplete = false

    suspend fun loadOutputDirectoryUri(): String? {
        migrateLegacyValueIfNeeded()
        return applicationContext.unlockMusicDataStore.data.first()[UnlockSettingsKeys.outputDirectoryUri]
    }

    suspend fun saveOutputDirectoryUri(uriString: String) {
        migrateLegacyValueIfNeeded()
        applicationContext.unlockMusicDataStore.edit { preferences ->
            preferences[UnlockSettingsKeys.outputDirectoryUri] = uriString
        }
    }

    suspend fun clear() {
        applicationContext.unlockMusicDataStore.edit { preferences ->
            preferences.remove(UnlockSettingsKeys.outputDirectoryUri)
        }
        applicationContext
            .unlockLegacySharedPreferences()
            .edit()
            .remove(KEY_OUTPUT_DIRECTORY_URI)
            .apply()
    }

    private suspend fun migrateLegacyValueIfNeeded() {
        if (migrationComplete) return

        val dataStore = applicationContext.unlockMusicDataStore
        val existing = dataStore.data.first()[UnlockSettingsKeys.outputDirectoryUri]
        val legacyPreferences = applicationContext.unlockLegacySharedPreferences()
        val legacyValue = legacyPreferences.getString(KEY_OUTPUT_DIRECTORY_URI, null)

        if (existing == null && legacyValue != null) {
            dataStore.edit { preferences ->
                preferences[UnlockSettingsKeys.outputDirectoryUri] = legacyValue
            }
        }

        if (legacyValue != null) {
            legacyPreferences.edit().remove(KEY_OUTPUT_DIRECTORY_URI).apply()
        }

        migrationComplete = true
    }

    private companion object {
        const val KEY_OUTPUT_DIRECTORY_URI = "output_directory_uri"
    }
}
