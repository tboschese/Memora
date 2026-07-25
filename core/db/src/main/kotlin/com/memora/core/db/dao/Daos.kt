package com.memora.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.memora.core.db.entity.GlossaryEntity
import com.memora.core.db.entity.NamedPlaceEntity
import com.memora.core.db.entity.NoteEntity
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.db.entity.SessionEntity
import com.memora.core.db.entity.TimelineGapEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAOs do banco local. Consultas por dia usam um intervalo `[fromMs, toMs)` em epoch-millis,
 * calculado na camada de cima (fuso do usuário) — o banco não conhece timezone.
 *
 * ⚠️ Gerado pelo Room (KSP); não compilado no ambiente atual.
 */
@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM session WHERE startedAtMs >= :fromMs AND startedAtMs < :toMs ORDER BY startedAtMs")
    fun observeInRange(fromMs: Long, toMs: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun byId(id: String): SessionEntity?
}

@Dao
interface SegmentDao {
    /** Insere os segmentos de um chunk transcrito. Idempotente por id (REPLACE). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<SegmentEntity>)

    @Query("SELECT * FROM segment WHERE startMs >= :fromMs AND startMs < :toMs ORDER BY startMs")
    fun observeInRange(fromMs: Long, toMs: Long): Flow<List<SegmentEntity>>

    /** Instantâneo de todos os segmentos (busca full-history; ordenar/filtrar na camada de cima). */
    @Query("SELECT * FROM segment")
    suspend fun snapshotAll(): List<SegmentEntity>

    @Query("UPDATE segment SET speaker = :speaker WHERE id = :segmentId")
    suspend fun setSpeaker(segmentId: String, speaker: String)

    @Query("SELECT COUNT(*) FROM segment WHERE chunkId = :chunkId")
    suspend fun countForChunk(chunkId: String): Int
}

@Dao
interface TimelineGapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gap: TimelineGapEntity)

    @Query("SELECT * FROM timeline_gap WHERE fromMs >= :fromMs AND fromMs < :toMs ORDER BY fromMs")
    fun observeInRange(fromMs: Long, toMs: Long): Flow<List<TimelineGapEntity>>
}

@Dao
interface NoteDao {
    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("SELECT * FROM note WHERE createdAtMs >= :fromMs AND createdAtMs < :toMs ORDER BY createdAtMs")
    fun observeInRange(fromMs: Long, toMs: Long): Flow<List<NoteEntity>>

    /** Instantâneo de todas as notas (busca full-history). */
    @Query("SELECT * FROM note")
    suspend fun snapshotAll(): List<NoteEntity>

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface GlossaryDao {
    @Upsert
    suspend fun upsert(entry: GlossaryEntity)

    @Query("SELECT * FROM glossary ORDER BY canonical")
    fun observeAll(): Flow<List<GlossaryEntity>>

    @Query("DELETE FROM glossary WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface NamedPlaceDao {
    @Upsert
    suspend fun upsert(place: NamedPlaceEntity)

    @Query("SELECT * FROM named_place ORDER BY name")
    fun observeAll(): Flow<List<NamedPlaceEntity>>

    @Query("DELETE FROM named_place WHERE id = :id")
    suspend fun deleteById(id: String)
}
