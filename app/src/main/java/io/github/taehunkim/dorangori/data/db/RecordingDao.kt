package io.github.taehunkim.dorangori.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observe(id: Long): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun get(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE fileName = :name")
    suspend fun getByName(name: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<RecordingEntity>

    /** -1 when a row with this file name exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: RecordingEntity): Long

    /** Waiting to be transcribed, oldest first. */
    @Query("SELECT id FROM recordings WHERE state IN ('RECEIVED', 'NO_MODEL') ORDER BY recordedAt")
    suspend fun pendingIds(): List<Long>

    @Query("UPDATE recordings SET state = :state, error = :error WHERE id = :id")
    suspend fun setState(id: Long, state: RecState, error: String?)

    @Query("UPDATE recordings SET durationMs = :ms WHERE id = :id")
    suspend fun setDuration(id: Long, ms: Long)

    @Query("UPDATE recordings SET state = 'DONE', text = :text, cuesJson = :cues, asrModelId = :model, asrModelVersion = :version, error = NULL WHERE id = :id")
    suspend fun setResult(id: Long, text: String, cues: String, model: String, version: Int)

    @Query("UPDATE recordings SET jogakboNoteId = :noteId WHERE id = :id")
    suspend fun setJogakboNote(id: Long, noteId: Long)

    @Query("DELETE FROM recordings WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT * FROM recordings WHERE recordedAt < :cutoff")
    suspend fun olderThan(cutoff: Long): List<RecordingEntity>

    /** After a crash or kill: a recording left "transcribing" is simply waiting again. */
    @Query("UPDATE recordings SET state = 'RECEIVED' WHERE state = 'TRANSCRIBING'")
    suspend fun resetStuck()
}
