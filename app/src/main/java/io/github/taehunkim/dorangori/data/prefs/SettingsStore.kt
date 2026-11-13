package io.github.taehunkim.dorangori.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.taehunkim.ondolsaem.sdk.IdlePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** When a received recording is transcribed. */
enum class AsrMode {
    /** Right away (default). */
    IMMEDIATE,
    /** Once a day at a chosen time. */
    SCHEDULED,
    /** When charging and the screen has been off for a while. */
    IDLE,
    /** Only when the user asks ("transcribe now"). */
    MANUAL,
}

private val Context.dataStore by preferencesDataStore("dorangori_settings")

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val asrLanguage = store.data.map { io.github.taehunkim.dorangori.transcribe.AsrLanguage.of(it[ASR_LANGUAGE]) }
    val asrMode: Flow<AsrMode> = store.data.map { p -> AsrMode.entries.firstOrNull { it.name == p[ASR_MODE] } ?: AsrMode.IMMEDIATE }
    val scheduleMinutes: Flow<Int> = store.data.map { it[SCHEDULE_MINUTES] ?: DEFAULT_SCHEDULE_MINUTES }
    val scheduleChargingOnly: Flow<Boolean> = store.data.map { it[SCHEDULE_CHARGING_ONLY] ?: true }
    val idlePreset: Flow<IdlePreset> = store.data.map { IdlePreset.of(it[IDLE_PRESET]) }
    /** Recordings older than this many days are deleted with their transcript; 0 keeps them. */
    val retentionDays: Flow<Int> = store.data.map { it[RETENTION_DAYS] ?: 0 }

    suspend fun asrModeNow(): AsrMode = asrMode.first()

    suspend fun setAsrLanguage(language: io.github.taehunkim.dorangori.transcribe.AsrLanguage) = edit { it[ASR_LANGUAGE] = language.name }

    suspend fun setAsrMode(mode: AsrMode) = edit { it[ASR_MODE] = mode.name }
    suspend fun setScheduleMinutes(minutes: Int) = edit { it[SCHEDULE_MINUTES] = minutes }
    suspend fun setScheduleChargingOnly(value: Boolean) = edit { it[SCHEDULE_CHARGING_ONLY] = value }
    suspend fun setIdlePreset(preset: IdlePreset) = edit { it[IDLE_PRESET] = preset.name }
    suspend fun setRetentionDays(days: Int) = edit { it[RETENTION_DAYS] = days }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) { store.edit { block(it) } }

    companion object {
        const val DEFAULT_SCHEDULE_MINUTES = 3 * 60
        val RETENTION_CHOICES = listOf(0, 7, 30, 90)
        private val ASR_LANGUAGE = stringPreferencesKey("asr_language")
        private val ASR_MODE = stringPreferencesKey("asr_mode")
        private val SCHEDULE_MINUTES = intPreferencesKey("schedule_minutes")
        private val SCHEDULE_CHARGING_ONLY = booleanPreferencesKey("schedule_charging_only")
        private val IDLE_PRESET = stringPreferencesKey("idle_preset")
        private val RETENTION_DAYS = intPreferencesKey("retention_days")
    }
}
