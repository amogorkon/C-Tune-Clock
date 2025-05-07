package com.kiefner.c_tune_clock.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LocationUtils(
    private val context: Context,
    private val locationRequest: LocationRequest = defaultLocationRequest(),
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context),
    private val fallbackPrefs: FallbackPrefs = FallbackPrefs()
) : DefaultLifecycleObserver {
    private val lifecycleOwner = context as? LifecycleOwner ?: error("Context must be a LifecycleOwner")

    companion object {
        private const val TAG = "LocationUtils"
        private const val GPS_TIMEOUT_MS = 15_000L

        private fun defaultLocationRequest(): LocationRequest =
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500)
                .setWaitForAccurateLocation(true)
                .build()
    }

    private val locationLock = Any()
    private var locationCallback: LocationCallback? = null
    private var isInitialLocationReceived = false
    private var isUsingGpsLocation = false

    private var currentLongitude: Float = fallbackPrefs.defaultLongitude
    private var currentLatitude: Float = fallbackPrefs.defaultLatitude

    var onError: ((Exception) -> Unit)? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        startLocationUpdates()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopLocationUpdates()
    }

    fun startLocationUpdates() {
        if (!hasFineOrCoarseLocationPermission()) {
            logFallbackState()
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // Filter out stale or inaccurate results
                if (location.accuracy > 100 || location.time < System.currentTimeMillis() - 30_000) return

                synchronized(locationLock) {
                    currentLongitude = location.longitude.toFloat()
                    currentLatitude = location.latitude.toFloat()
                    isInitialLocationReceived = true
                    isUsingGpsLocation = true
                    logLocationState()
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLongitude = location.longitude.toFloat()
                    currentLatitude = location.latitude.toFloat()
                    isInitialLocationReceived = true
                    isUsingGpsLocation = true
                    logLocationState()
                }
            }

            scheduleGpsTimeout()

        } catch (e: SecurityException) {
            handleLocationError(e)
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d(TAG, "Location updates stopped")
        }
        locationCallback = null
    }

    fun getCurrentLongitude(): Float =
        if (isInitialLocationReceived && isUsingGpsLocation) currentLongitude
        else getTimezoneLongitude()

    fun getCurrentLatitude(): Float =
        if (isInitialLocationReceived && isUsingGpsLocation) currentLatitude
        else estimateLatitudeFromTimezone()

    private fun scheduleGpsTimeout() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isInitialLocationReceived) {
                Log.w(TAG, "GPS timeout - using fallback")
                isUsingGpsLocation = false
            }
        }, GPS_TIMEOUT_MS)
    }

    private fun handleLocationError(e: Exception) {
        Log.e(TAG, "Location error: ${e.message}", e)
        onError?.invoke(e)
        isUsingGpsLocation = false
        currentLongitude = getTimezoneLongitude()
        currentLatitude = estimateLatitudeFromTimezone()
    }

    private fun estimateLatitudeFromTimezone(): Float =
        fallbackPrefs.timezoneToLat[TimeZone.getDefault().id] ?: fallbackPrefs.defaultLatitude

    private fun getTimezoneLongitude(): Float = try {
        val systemZone = ZoneId.systemDefault()
        val offsetHours = ZoneId.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds / 3600f
        ((15f * offsetHours + 180) % 360 - 180) // Normalize to [-180, 180]
    } catch (e: Exception) {
        Log.e(TAG, "Timezone longitude error: ${e.message}", e)
        fallbackPrefs.defaultLongitude
    }

    private fun hasFineOrCoarseLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun logLocationState() {
        Log.d(TAG, """
            |Location Update:
            |Latitude: ${"%.6f".format(currentLatitude)}
            |Longitude: ${"%.6f".format(currentLongitude)}
            |Source: ${if (isUsingGpsLocation) "GPS" else "Timezone"}
        """.trimMargin())
    }

    private fun logFallbackState() {
        Log.w(TAG, """
            |Falling back to timezone-based location:
            |Latitude: ${estimateLatitudeFromTimezone()}
            |Longitude: ${getTimezoneLongitude()}
        """.trimMargin())
    }
}

/**
 * Stores fallback location values. Can later be injected from user preferences.
 */
data class FallbackPrefs(
    val defaultLatitude: Float = 48.8589f,
    val defaultLongitude: Float = 9.1829f,
    val timezoneToLat: Map<String, Float> = mapOf(
        "Europe/Berlin" to 51.0f,
        "America/New_York" to 40.7f,
        "Asia/Tokyo" to 35.68f
    )
)
