package cn.codex.weatheralarm.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import cn.codex.weatheralarm.domain.AlarmProfile
import cn.codex.weatheralarm.domain.LocationQuery
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationResolver(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)

    fun hasForegroundPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasBackgroundPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    suspend fun currentOrSaved(profile: AlarmProfile): ResolvedLocation {
        val current = currentLocationOrNull()
        return if (current != null) {
            ResolvedLocation(
                query = LocationQuery(
                    cityId = "",
                    latitude = current.latitude,
                    longitude = current.longitude
                ),
                latitude = current.latitude,
                longitude = current.longitude,
                sourceLabel = "当前位置"
            )
        } else {
            ResolvedLocation(
                query = LocationQuery(
                    cityId = profile.cityId,
                    latitude = profile.latitude,
                    longitude = profile.longitude
                ),
                latitude = profile.latitude,
                longitude = profile.longitude,
                sourceLabel = "最近一次定位"
            )
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLocationOrNull(): Location? {
        if (!hasForegroundPermission()) return null
        val bestLastKnown = bestLastKnownLocation()
        if (bestLastKnown != null && System.currentTimeMillis() - bestLastKnown.time < FRESH_LOCATION_MS) {
            return bestLastKnown
        }

        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { locationManager.isProviderEnabled(it) }
        for (provider in providers) {
            val current = requestSingleLocation(provider)
            if (current != null) return current
        }
        return bestLastKnown
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(): Location? {
        if (!hasForegroundPermission()) return null
        return locationManager.getProviders(true)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocation(provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                locationManager.getCurrentLocation(
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                continuation.invokeOnCancellation { signal.cancel() }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    override fun onProviderDisabled(provider: String) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                locationManager.requestSingleUpdate(provider, listener, context.mainLooper)
                continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            }
        }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val FRESH_LOCATION_MS = 30 * 60 * 1000L
    }
}

data class ResolvedLocation(
    val query: LocationQuery,
    val latitude: Double,
    val longitude: Double,
    val sourceLabel: String
)
