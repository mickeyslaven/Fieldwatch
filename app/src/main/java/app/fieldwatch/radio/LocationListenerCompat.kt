package app.fieldwatch.radio

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

/**
 * Android 7 declares every legacy LocationListener callback abstract. Newer
 * SDKs provide defaults, which makes a Kotlin SAM listener crash on API 24.
 */
internal fun compatLocationListener(
    onLocationChanged: (Location) -> Unit,
): LocationListener = object : LocationListener {
    override fun onLocationChanged(location: Location) = onLocationChanged(location)

    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit
}
