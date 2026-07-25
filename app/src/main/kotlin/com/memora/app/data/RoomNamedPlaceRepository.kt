package com.memora.app.data

import com.memora.core.db.dao.NamedPlaceDao
import com.memora.core.db.entity.NamedPlaceEntity
import com.memora.core.location.LatLng
import com.memora.core.location.NamedPlace
import com.memora.core.location.NamedPlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistência dos lugares nomeados sobre o `NamedPlaceDao`. Fica em `:app` — `:core:location` não
 * depende de `:core:db`. Os lugares salvos alimentam o `NamedPlaceGeocodingProvider` (matching por
 * proximidade, offline).
 */
class RoomNamedPlaceRepository(private val dao: NamedPlaceDao) : NamedPlaceRepository {

    override fun observeAll(): Flow<List<NamedPlace>> =
        dao.observeAll().map { rows -> rows.map(NamedPlaceEntity::toNamedPlace) }

    override suspend fun save(id: String, place: NamedPlace) = dao.upsert(place.toEntity(id))

    override suspend fun delete(id: String) = dao.deleteById(id)
}

internal fun NamedPlaceEntity.toNamedPlace(): NamedPlace = NamedPlace(
    name = name,
    center = LatLng(lat, lng),
    radiusMeters = radiusMeters,
)

internal fun NamedPlace.toEntity(id: String): NamedPlaceEntity = NamedPlaceEntity(
    id = id,
    name = name,
    lat = center.lat,
    lng = center.lng,
    radiusMeters = radiusMeters,
)
