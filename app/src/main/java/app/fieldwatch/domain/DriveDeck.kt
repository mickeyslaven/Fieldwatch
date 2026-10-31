package app.fieldwatch.domain

/** A watchlist contact prepared for the glanceable Drive alerts screen. */
data class DriveContact(
    val device: Sighting,
    val primaryClass: SignatureClass?,
    val signatureNames: List<String>,
    val active: Boolean,
    val alertedAt: Long?,
)

data class DriveSnapshot(
    val active: List<DriveContact>,
    val recent: List<DriveContact>,
    val totalActive: Int,
    val unmatchedActive: Int,
)

/**
 * Produces a quiet driving display from the same watchlist that drives beeps and voice.
 * It never invents direction or distance from RSSI.
 */
object DriveDeck {
    const val RECENT_MS = 15 * 60_000L

    private val classPriority = listOf(
        SignatureClass.DRONE,
        SignatureClass.SURVEILLANCE,
        SignatureClass.LAW_ENFORCEMENT,
        SignatureClass.CAMERA,
        SignatureClass.GLASSES,
        SignatureClass.WEARABLE,
        SignatureClass.HACKING,
        SignatureClass.FINDER,
        SignatureClass.VEHICLE,
        SignatureClass.MESH,
    ).withIndex().associate { it.value to it.index }

    fun snapshot(
        devices: List<Sighting>,
        fleets: List<Fleet>,
        watchlist: List<WatchTarget>,
        alertAt: Map<String, Long>,
        now: Long,
    ): DriveSnapshot {
        val fleetById = fleets.associateBy { it.id }
        val watchedFleets = RadioBookmarks.watchedFleetIds(watchlist)
        val watchedDevices = RadioBookmarks.alertDeviceKeys(watchlist)
        val mine = RadioBookmarks.mineKeys(watchlist)

        fun watched(device: Sighting): Boolean =
            device.key !in mine &&
                (device.key in watchedDevices || device.fleetIds.any { it in watchedFleets })

        fun contact(device: Sighting): DriveContact {
            val matched = device.fleetIds.mapNotNull(fleetById::get)
            val classes = matched.map { it.kind.folded() }.distinct()
            val primary = classes.minByOrNull { classPriority[it] ?: Int.MAX_VALUE }
            return DriveContact(
                device = device,
                primaryClass = primary,
                signatureNames = matched.map { it.name }.distinct().sorted(),
                active = !device.gone,
                alertedAt = alertAt[device.key],
            )
        }

        val contacts = devices.filter(::watched).map(::contact)
        val ordering = compareByDescending<DriveContact> { it.alertedAt ?: Long.MIN_VALUE }
            .thenBy { classPriority[it.primaryClass] ?: Int.MAX_VALUE }
            .thenByDescending { it.device.rssi }
        val active = contacts.filter { it.active }.sortedWith(ordering)
        val recent = contacts
            .filter { !it.active && it.alertedAt != null && now - it.alertedAt in 0..RECENT_MS }
            .sortedWith(ordering)
        val onAir = devices.filter { !it.gone }
        return DriveSnapshot(
            active = active,
            recent = recent,
            totalActive = onAir.size,
            unmatchedActive = onAir.count { it.fleetIds.isEmpty() },
        )
    }
}
