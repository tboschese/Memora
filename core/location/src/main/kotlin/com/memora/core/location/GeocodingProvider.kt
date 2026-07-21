package com.memora.core.location

/**
 * Resolve uma coordenada para um lugar legível, SEM geocoding online: casa por proximidade a
 * "lugares nomeados" que o usuário cadastrou (casa, trabalho, academia…). Se não houver match,
 * retorna null (a UI mostra só a coordenada bruta ou nada).
 */
interface GeocodingProvider {
    suspend fun resolve(point: LatLng): PlaceLabel?
}

data class LatLng(val lat: Double, val lng: Double)

data class PlaceLabel(
    val name: String,
    /** true se veio de um lugar nomeado pelo usuário; false para rótulos genéricos. */
    val isNamedPlace: Boolean,
)

/** Lugar nomeado pelo usuário: um ponto + raio de match, em metros. */
data class NamedPlace(
    val name: String,
    val center: LatLng,
    val radiusMeters: Double = 120.0,
)
