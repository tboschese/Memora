package com.memora.core.location.fake

import com.memora.core.location.GeocodingProvider
import com.memora.core.location.LatLng
import com.memora.core.location.NamedPlace
import com.memora.core.location.PlaceLabel
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Impl offline de referência (também usada como fake em testes): casa a coordenada com o
 * lugar nomeado mais próximo dentro do raio, via distância de Haversine. Zero rede.
 */
class NamedPlaceGeocodingProvider(
    private val places: List<NamedPlace>,
) : GeocodingProvider {

    override suspend fun resolve(point: LatLng): PlaceLabel? {
        val match = places
            .map { it to haversineMeters(point, it.center) }
            .filter { (place, dist) -> dist <= place.radiusMeters }
            .minByOrNull { (_, dist) -> dist }
            ?.first
        return match?.let { PlaceLabel(name = it.name, isNamedPlace = true) }
    }

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
        return 2 * r * asin(sqrt(h))
    }
}
