package dev.unlockmusic.android.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class LastSessionSettingsStore(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    fun loadOutputDirectoryUri(): String? {
        return runBlocking(Dispatchers.IO) {
            migrateLegacyValueIfNeeded()
            applicationContext.unlockMusicDataStore.data.first()[UnlockSettingsKeys.outputDirectoryUri]
        }
    }

    fun saveOutputDirectoryUri(uriString: String) {
        runBlocking(Dispatchers.IO) {
            migrateLegacyValueIfNeeded()
            applicationContext.unlockMusicDataStore.edit { preferences ->
                preferences[UnlockSettingsKeys.outputDirectoryUri] = uriString
            }
        }
    }

    fun clear() {
        runBlocking(Dispatchers.IO) {
            applicationContext.unlockMusicDataStore.edit { preferences ->
                preferences.remove(UnlockSettingsKeys.outputDirectoryUri)
            }
            applicationContext
                .unlockLegacySharedPreferences()
                .edit()
                .remove(KEY_OUTPUT_DIRECTORY_URI)
                .apply()
        }
    }

    private suspend fun migrateLegacyValueIfNeeded() {
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
    }

    private companion object {
        const val KEY_OUTPUT_DIRECTORY_URI = "output_directory_uri"
    }
}
