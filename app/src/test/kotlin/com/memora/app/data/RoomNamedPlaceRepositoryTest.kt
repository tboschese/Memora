package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.db.MemoraDatabase
import com.memora.core.location.LatLng
import com.memora.core.location.NamedPlace
import com.memora.core.location.fake.NamedPlaceGeocodingProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Round-trip real dos lugares nomeados no Room e prova de que alimentam o geocoding offline: um
 * ponto perto do centro salvo casa com o nome do lugar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomNamedPlaceRepositoryTest {

    private lateinit var db: MemoraDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            MemoraDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `save and observe round-trips a named place`() = runBlocking {
        val repo = RoomNamedPlaceRepository(db.namedPlaceDao())
        repo.save("p1", NamedPlace("Casa", LatLng(-23.5, -46.6), radiusMeters = 150.0))

        val place = repo.observeAll().first().single()
        assertEquals("Casa", place.name)
        assertEquals(-23.5, place.center.lat, 0.0)
        assertEquals(150.0, place.radiusMeters, 0.0)
    }

    @Test
    fun `observed places are ordered by name`() = runBlocking {
        val repo = RoomNamedPlaceRepository(db.namedPlaceDao())
        repo.save("b", NamedPlace("Trabalho", LatLng(0.0, 0.0)))
        repo.save("a", NamedPlace("Academia", LatLng(1.0, 1.0)))

        assertEquals(listOf("Academia", "Trabalho"), repo.observeAll().first().map { it.name })
    }

    @Test
    fun `saved places feed the offline geocoding`() = runBlocking {
        val repo = RoomNamedPlaceRepository(db.namedPlaceDao())
        repo.save("home", NamedPlace("Casa", LatLng(-23.5000, -46.6000), radiusMeters = 200.0))

        val provider = NamedPlaceGeocodingProvider(repo.observeAll().first())
        // ~20m ao lado do centro → dentro do raio
        val label = provider.resolve(LatLng(-23.5001, -46.6001))

        assertEquals("Casa", label?.name)
        assertTrue(label?.isNamedPlace == true)
    }

    @Test
    fun `delete removes the place`() = runBlocking {
        val repo = RoomNamedPlaceRepository(db.namedPlaceDao())
        repo.save("p1", NamedPlace("Casa", LatLng(0.0, 0.0)))
        repo.delete("p1")

        assertTrue(repo.observeAll().first().isEmpty())
    }
}
