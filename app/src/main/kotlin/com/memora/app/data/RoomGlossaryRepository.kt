package com.memora.app.data

import com.memora.core.db.dao.GlossaryDao
import com.memora.core.db.entity.GlossaryEntity
import com.memora.core.glossary.GlossaryEntry
import com.memora.core.glossary.GlossaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistência do glossário sobre o `GlossaryDao`. Fica em `:app` — `:core:glossary` não depende de
 * `:core:db`. Traduz as variantes entre a `List<String>` do domínio e a String newline-separated da
 * entidade (variantes podem ser multi-palavra, mas nunca contêm `\n`).
 */
class RoomGlossaryRepository(private val dao: GlossaryDao) : GlossaryRepository {

    override fun observeAll(): Flow<List<GlossaryEntry>> =
        dao.observeAll().map { rows -> rows.map(GlossaryEntity::toEntry) }

    override suspend fun save(entry: GlossaryEntry) = dao.upsert(entry.toEntity())

    override suspend fun delete(id: String) = dao.deleteById(id)
}

internal fun GlossaryEntity.toEntry(): GlossaryEntry = GlossaryEntry(
    id = id,
    canonical = canonical,
    variants = variants.toVariantList(),
    description = description,
)

internal fun GlossaryEntry.toEntity(): GlossaryEntity = GlossaryEntity(
    id = id,
    canonical = canonical,
    variants = variants.joinToString("\n"),
    description = description,
)

private fun String.toVariantList(): List<String> =
    if (isEmpty()) emptyList() else split("\n").filter { it.isNotEmpty() }
