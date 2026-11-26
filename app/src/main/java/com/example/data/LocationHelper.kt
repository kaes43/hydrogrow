package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class DeviceLocationResult(
    val cityLabel: String,
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun fetchCoarseLocation(context: Context): Result<DeviceLocationResult> {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: client.lastLocation.await()
                ?: return Result.failure(IllegalStateException("Lokasi perangkat belum tersedia. Silakan pilih area secara manual."))

            val cityName = try {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale("in", "ID")).getFromLocation(loc.latitude, loc.longitude, 1)
                val addr = addresses?.firstOrNull()
                listOfNotNull(addr?.subAdminArea, addr?.locality, addr?.adminArea)
                    .firstOrNull { it.isNotBlank() } ?: "Lokasi Perangkat"
            } catch (_: Exception) {
                "Lokasi Perangkat (${String.format(Locale.US, "%.3f, %.3f", loc.latitude, loc.longitude)})"
            }

            Result.success(
                DeviceLocationResult(
                    cityLabel = cityName,
                    latitude = loc.latitude,
                    longitude = loc.longitude
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
