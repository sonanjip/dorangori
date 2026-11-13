package io.github.taehunkim.dorangori.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromState(s: RecState): String = s.name
    @TypeConverter fun toState(s: String): RecState = RecState.entries.firstOrNull { it.name == s } ?: RecState.FAILED
}

/** Recordings and their transcripts are user data: every change needs a migration (see AGENTS.md), never a destructive fallback. */
@Database(entities = [RecordingEntity::class], version = AppDatabase.VERSION, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordings(): RecordingDao

    companion object {
        const val VERSION = 1
    }
}
