package app.fieldwatch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveMapMotionTest {
    @Test
    fun smallParkedJitterKeepsTheDrawnCoordinate() {
        val from = GpsSample(1_000L, 42.000000, -83.000000)
        val noisy = GpsSample(2_000L, 42.000008, -83.000008)

        val held = DriveMapMotion.quieted(from, noisy)

        assertEquals(from.lat, held.lat, 0.0)
        assertEquals(from.lon, held.lon, 0.0)
        assertEquals(noisy.at, held.at)
    }

    @Test
    fun ordinaryDrivingFixGlidesAndFinishesBeforeTheNextFix() {
        val from = GpsSample(1_000L, 42.0, -83.0)
        val to = GpsSample(2_000L, 42.0001, -83.0)

        assertFalse(DriveMapMotion.shouldSnap(from, to))
        assertEquals(820L, DriveMapMotion.glideMs(from, to))
        val halfway = DriveMapMotion.interpolate(from, to, 0.5f)
        assertEquals(42.00005, halfway.lat, 1e-9)
        assertEquals(1_500L, halfway.at)
    }

    @Test
    fun aLongReacquisitionSnapsInsteadOfFlyingAcrossTheMap() {
        val from = GpsSample(1_000L, 42.0, -83.0)
        val far = GpsSample(20_000L, 42.002, -83.0)

        assertTrue(DriveMapMotion.shouldSnap(from, far))
    }

    @Test
    fun interpolationCrossesTheDateLineTheShortWay() {
        val from = GpsSample(0L, 0.0, 179.9)
        val to = GpsSample(1_000L, 0.0, -179.9)

        val halfway = DriveMapMotion.interpolate(from, to, 0.5f)

        assertEquals(-180.0, halfway.lon, 1e-9)
    }
}
