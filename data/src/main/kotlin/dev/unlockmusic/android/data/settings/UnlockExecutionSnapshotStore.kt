package dev.unlockmusic.android.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class UnlockExecutionSnapshot(
    val outputDirectoryUri: String?,
    val tasks: List<UnlockTask>,
    val isExecuting: Boolean,
    val activeTaskId: String?,
    val lastMessage: String?,
    val cancelRequested: Boolean,
    val processedCount: Int,
    val totalCount: Int,
    val currentTaskName: String?,
    val currentTaskProgressPercent: Int,
)

class UnlockExecutionSnapshotStore(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    @Volatile
    private var migrationComplete = false

    suspend fun load(): UnlockExecutionSnapshot? {
        migrateLegacyValueIfNeeded()
        val rawSnapshot =
            applicationContext.unlockMusicDataStore.data.first()[UnlockSettingsKeys.executionSnapshot]
                ?: return null
        return runCatching { decode(rawSnapshot) }.getOrNull()
    }

    suspend fun save(snapshot: UnlockExecutionSnapshot) {
        migrateLegacyValueIfNeeded()
        applicationContext.unlockMusicDataStore.edit { preferences ->
            preferences[UnlockSettingsKeys.executionSnapshot] = encode(snapshot)
        }
    }

    suspend fun clear() {
        applicationContext.unlockMusicDataStore.edit { preferences ->
            preferences.remove(UnlockSettingsKeys.executionSnapshot)
        }
        applicationContext
            .unlockLegacySharedPreferences()
            .edit()
            .remove(KEY_EXECUTION_SNAPSHOT)
            .apply()
    }

    private suspend fun migrateLegacyValueIfNeeded() {
        if (migrationComplete) return

        val dataStore = applicationContext.unlockMusicDataStore
        val existing = dataStore.data.first()[UnlockSettingsKeys.executionSnapshot]
        val legacyPreferences = applicationContext.unlockLegacySharedPreferences()
        val legacyValue = legacyPreferences.getString(KEY_EXECUTION_SNAPSHOT, null)

        if (existing == null && legacyValue != null) {
            dataStore.edit { preferences ->
                preferences[UnlockSettingsKeys.executionSnapshot] = legacyValue
            }
        }

        if (legacyValue != null) {
            legacyPreferences.edit().remove(KEY_EXECUTION_SNAPSHOT).apply()
        }

        migrationComplete = true
    }

    private fun encode(snapshot: UnlockExecutionSnapshot): String {
        return JSONObject().apply {
            put("outputDirectoryUri", snapshot.outputDirectoryUri)
            put("isExecuting", snapshot.isExecuting)
            put("activeTaskId", snapshot.activeTaskId)
            put("lastMessage", snapshot.lastMessage)
            put("cancelRequested", snapshot.cancelRequested)
            put("processedCount", snapshot.processedCount)
            put("totalCount", snapshot.totalCount)
            put("currentTaskName", snapshot.currentTaskName)
            put("currentTaskProgressPercent", snapshot.currentTaskProgressPercent)
            put(
                "tasks",
                JSONArray().apply {
                    snapshot.tasks.forEach { task ->
                        put(
                            JSONObject().apply {
                                put("id", task.id)
                                put("queuedAtEpochMillis", task.queuedAtEpochMillis)
                                put(
                                    "source",
                                    JSONObject().apply {
                                        put("uriString", task.source.uriString)
                                        put("displayName", task.source.displayName)
                                        put("detectedFileType", task.source.detectedFileType.name)
                                    },
                                )
                                put("status", encodeStatus(task.status))
                            },
                        )
                    }
                },
            )
        }.toString()
    }

    private fun decode(rawSnapshot: String): UnlockExecutionSnapshot {
        val json = JSONObject(rawSnapshot)
        val tasksJson = json.optJSONArray("tasks") ?: JSONArray()

        return UnlockExecutionSnapshot(
            outputDirectoryUri = json.optNullableString("outputDirectoryUri"),
            tasks =
                buildList {
                    for (index in 0 until tasksJson.length()) {
                        val taskJson = tasksJson.optJSONObject(index) ?: continue
                        val sourceJson = taskJson.optJSONObject("source") ?: continue
                        val statusJson = taskJson.optJSONObject("status") ?: continue

                        add(
                            UnlockTask(
                                id = taskJson.optString("id"),
                                source =
                                    UnlockSource(
                                        uriString = sourceJson.optString("uriString"),
                                        displayName = sourceJson.optString("displayName"),
                                        detectedFileType = decodeFileType(sourceJson.optString("detectedFileType")),
                                    ),
                                status = decodeStatus(statusJson),
                                queuedAtEpochMillis = taskJson.optLong("queuedAtEpochMillis"),
                            ),
                        )
                    }
                },
            isExecuting = json.optBoolean("isExecuting", false),
            activeTaskId = json.optNullableString("activeTaskId"),
            lastMessage = json.optNullableString("lastMessage"),
            cancelRequested = json.optBoolean("cancelRequested", false),
            processedCount = json.optInt("processedCount", 0),
            totalCount = json.optInt("totalCount", 0),
            currentTaskName = json.optNullableString("currentTaskName"),
            currentTaskProgressPercent = json.optInt("currentTaskProgressPercent", 0),
        )
    }

    private fun encodeStatus(status: UnlockStatus): JSONObject {
        return JSONObject().apply {
            when (status) {
                UnlockStatus.Canceled -> {
                    put("type", "canceled")
                }
                is UnlockStatus.Failed -> {
                    put("type", "failed")
                    put("message", status.message)
                }
                UnlockStatus.Queued -> {
                    put("type", "queued")
                }
                is UnlockStatus.Running -> {
                    put("type", "running")
                    put("progressPercent", status.progressPercent)
                }
                is UnlockStatus.Success -> {
                    put("type", "success")
                    put("outputName", status.outputName)
                }
            }
        }
    }

    private fun decodeStatus(statusJson: JSONObject): UnlockStatus {
        return when (statusJson.optString("type")) {
            "canceled" -> UnlockStatus.Canceled
            "failed" -> UnlockStatus.Failed(statusJson.optString("message", "Unknown error"))
            "queued" -> UnlockStatus.Queued
            "running" -> UnlockStatus.Running(statusJson.optInt("progressPercent", 0))
            "success" -> UnlockStatus.Success(statusJson.optString("outputName", "output"))
            else -> UnlockStatus.Queued
        }
    }

    private fun decodeFileType(name: String): DetectedFileType {
        return runCatching { DetectedFileType.valueOf(name) }.getOrDefault(DetectedFileType.UNKNOWN)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (isNull(key)) null else optString(key)
    }

    private companion object {
        const val KEY_EXECUTION_SNAPSHOT = "execution_snapshot"
    }
}
