package app.fieldwatch.domain

import kotlin.math.roundToLong

/**
 * Presentation-only motion for the Drive map. Detection and alerting continue to use the
 * newest accepted GPS fix; these helpers only keep the drawn vehicle from stepping once per fix.
 */
object DriveMapMotion {
    /** Ordinary parked-car GPS wander smaller than this does not move the map. */
    const val QUIET_M = 2.0

    /** A long reacquisition catches up immediately instead of flying through stale ground. */
    const val SNAP_M = 160.0

    /** About 15 redraws per second is smooth without overworking the 2015 Shield. */
    const val FRAME_MS = 66L
    const val MIN_GLIDE_MS = 240L
    const val MAX_GLIDE_MS = 850L

    fun shouldSnap(from: GpsSample?, to: GpsSample): Boolean {
        if (from == null) return true
        if (!from.lat.isFinite() || !from.lon.isFinite() || !to.lat.isFinite() || !to.lon.isFinite()) {
            return true
        }
        if (to.at < from.at) return true
        return Geo.meters(from.lat, from.lon, to.lat, to.lon) >= SNAP_M
    }

    /** Preserve a fresh timestamp while holding tiny coordinate noise in place. */
    fun quieted(from: GpsSample, to: GpsSample): GpsSample =
        if (Geo.meters(from.lat, from.lon, to.lat, to.lon) < QUIET_M) {
            from.copy(at = to.at, rssi = to.rssi)
        } else {
            to
        }

    /** Finish before the next usual one-second GPS fix, with a useful floor for fast updates. */
    fun glideMs(from: GpsSample, to: GpsSample): Long =
        ((to.at - from.at).coerceAtLeast(0L) * 82L / 100L)
            .coerceIn(MIN_GLIDE_MS, MAX_GLIDE_MS)

    /** Great-circle precision is unnecessary over one fix; the longitude still crosses ±180 safely. */
    fun interpolate(from: GpsSample, to: GpsSample, fraction: Float): GpsSample {
        val f = fraction.coerceIn(0f, 1f).toDouble()
        val lonDelta = shortestLonDelta(from.lon, to.lon)
        return GpsSample(
            at = (from.at + (to.at - from.at) * f).roundToLong(),
            lat = from.lat + (to.lat - from.lat) * f,
            lon = wrapLon(from.lon + lonDelta * f),
            rssi = if (fraction < 1f) from.rssi else to.rssi,
        )
    }

    private fun shortestLonDelta(from: Double, to: Double): Double {
        var delta = (to - from) % 360.0
        if (delta > 180.0) delta -= 360.0
        if (delta <= -180.0) delta += 360.0
        return delta
    }

    private fun wrapLon(lon: Double): Double {
        var wrapped = (lon + 180.0) % 360.0
        if (wrapped < 0.0) wrapped += 360.0
        return wrapped - 180.0
    }
}
