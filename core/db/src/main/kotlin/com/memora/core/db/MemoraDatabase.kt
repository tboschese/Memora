package com.memora.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.memora.core.db.dao.NoteDao
import com.memora.core.db.dao.SegmentDao
import com.memora.core.db.dao.SessionDao
import com.memora.core.db.dao.TimelineGapDao
import com.memora.core.db.entity.NoteEntity
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.db.entity.SessionEntity
import com.memora.core.db.entity.TimelineGapEntity

/**
 * Banco local do Memora (Room sobre SQLCipher). A construção da instância — com a chave derivada
 * no :core:security e o `SupportFactory` do SQLCipher — é feita na camada de DI (:app) na Fase 1;
 * aqui fica só o contrato do schema e os DAOs.
 *
 * FTS (busca full-text, Fase 3) e as migrations reais entram nas fases correspondentes; por isso
 * `exportSchema = false` por ora (habilitar com `room.schemaLocation` quando as migrations existirem).
 *
 * ⚠️ Processado pelo Room (KSP); não compilado no ambiente de desenvolvimento atual.
 */
@Database(
    entities = [
        SessionEntity::class,
        SegmentEntity::class,
        TimelineGapEntity::class,
        NoteEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MemoraDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun segmentDao(): SegmentDao
    abstract fun timelineGapDao(): TimelineGapDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val NAME = "memora.db"
    }
}
