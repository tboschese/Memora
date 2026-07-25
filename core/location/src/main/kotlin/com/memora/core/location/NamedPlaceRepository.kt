package com.memora.core.location

import kotlinx.coroutines.flow.Flow

/**
 * Lugares nomeados do usuário (casa, trabalho, academia…). Interface aqui; a impl real (sobre o
 * Room) fica em `:app`. Alimenta o `NamedPlaceGeocodingProvider`, que casa uma coordenada ao lugar
 * mais próximo dentro do raio — offline, sem geocoding online (RF-28).
 */
interface NamedPlaceRepository {
    fun observeAll(): Flow<List<NamedPlace>>

    suspend fun save(id: String, place: NamedPlace)

    suspend fun delete(id: String)
}
