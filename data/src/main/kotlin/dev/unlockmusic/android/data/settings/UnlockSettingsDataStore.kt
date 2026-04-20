package dev.unlockmusic.android.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val DATASTORE_NAME = "unlock_music_android_datastore"

internal val Context.unlockMusicDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
)

internal object UnlockSettingsKeys {
    val outputDirectoryUri = stringPreferencesKey("output_directory_uri")
    val executionSnapshot = stringPreferencesKey("execution_snapshot")
}

internal object UnlockLegacySettings {
    const val preferencesName = "unlock_music_android"
}

internal fun Context.unlockLegacySharedPreferences(): SharedPreferences {
    return getSharedPreferences(UnlockLegacySettings.preferencesName, Context.MODE_PRIVATE)
}
