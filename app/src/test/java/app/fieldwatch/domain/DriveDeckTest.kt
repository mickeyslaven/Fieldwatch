package app.fieldwatch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveDeckTest {
    private val drone = Fleet("drone", "Remote ID", kind = SignatureClass.DRONE)
    private val camera = Fleet("camera", "Roadside camera", kind = SignatureClass.SURVEILLANCE)
    private val home = Fleet("home", "Home device", kind = SignatureClass.HOME)

    @Test
    fun showsOnlyWatchlistContactsAndKeepsRecentDepartures() {
        val now = 1_000_000L
        val activeDrone = radio("drone", setOf(drone.id), gone = false, rssi = -54)
        val departedCamera = radio("camera", setOf(camera.id), gone = true, rssi = -75)
        val ordinary = radio("home", setOf(home.id), gone = false, rssi = -35)
        val unmatched = radio("unknown", emptySet(), gone = false, rssi = -45)
        val snap = DriveDeck.snapshot(
            devices = listOf(activeDrone, departedCamera, ordinary, unmatched),
            fleets = listOf(drone, camera, home),
            watchlist = listOf(
                WatchTarget("watch-drone", fleetId = drone.id, label = drone.name),
                WatchTarget("watch-camera", fleetId = camera.id, label = camera.name),
            ),
            alertAt = mapOf(
                activeDrone.key to now - 1_000L,
                departedCamera.key to now - 60_000L,
            ),
            now = now,
        )

        assertEquals(listOf(activeDrone.key), snap.active.map { it.device.key })
        assertEquals(listOf(departedCamera.key), snap.recent.map { it.device.key })
        assertEquals(SignatureClass.DRONE, snap.active.single().primaryClass)
        assertEquals(3, snap.totalActive)
        assertEquals(1, snap.unmatchedActive)
    }

    @Test
    fun directRadioAlertWorksAndMineIsSuppressed() {
        val direct = radio("direct", emptySet(), gone = false, rssi = -50)
        val mine = radio("mine", setOf(drone.id), gone = false, rssi = -40)
        val snap = DriveDeck.snapshot(
            devices = listOf(direct, mine),
            fleets = listOf(drone),
            watchlist = listOf(
                WatchTarget("direct-row", deviceKey = direct.key, label = "Pinned", alert = true),
                WatchTarget("fleet-row", fleetId = drone.id, label = drone.name),
                WatchTarget("mine-row", deviceKey = mine.key, label = "Mine", mine = true),
            ),
            alertAt = emptyMap(),
            now = 2_000_000L,
        )

        assertEquals(listOf(direct.key), snap.active.map { it.device.key })
        assertTrue(snap.recent.isEmpty())
    }

    private fun radio(
        key: String,
        fleets: Set<String>,
        gone: Boolean,
        rssi: Int,
    ) = Sighting(
        key = "BLE:$key",
        kind = RadioKind.BLE,
        mac = "AA:BB:CC:DD:EE:${key.hashCode().and(0xFF).toString(16).padStart(2, '0')}",
        name = key,
        rssi = rssi,
        rssiMin = rssi,
        rssiMax = rssi,
        channel = 0,
        frequencyMhz = 0,
        vendor = null,
        randomized = false,
        hiddenSsid = false,
        serviceUuids = emptyList(),
        manufacturerId = null,
        manufacturerDataHex = "",
        rawHex = "",
        extras = "",
        firstSeen = 1L,
        lastSeen = 2L,
        hitCount = 1,
        fleetIds = fleets,
        rssiHistory = emptyList(),
        presence = emptyList(),
        gone = gone,
    )
}
