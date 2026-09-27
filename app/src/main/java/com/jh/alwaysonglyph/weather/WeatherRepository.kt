package com.jh.alwaysonglyph.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Fetches the current outdoor temperature from the free, key-less Open-Meteo
 * API and caches the result so the status widget can render it without
 * blocking. Location is resolved from the device (coarse/fine) and falls back
 * to an IP-based geolocation lookup when permission is missing or the device
 * has no recent fix.
 */
object WeatherRepository {

    private const val TAG = "WeatherRepository"
    private const val PREFS_NAME = "weather_cache"
    private const val KEY_TEMP = "temperature"
    private const val KEY_TIME = "last_fetch_time"

    private const val OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast"
    private const val IP_GEOLOCATION_URL = "https://ipwho.is/"

    private const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L

    private var cachedTemperature: Int? = null
    private var lastFetchTime: Long = 0L

    fun currentTemperatureCelsius(): Int? = cachedTemperature

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    fun isStale(): Boolean =
        System.currentTimeMillis() - lastFetchTime > REFRESH_INTERVAL_MS

    /** Restores the last known temperature from disk (call once at startup). */
    fun loadCache(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cachedTemperature = prefs.getInt(KEY_TEMP, -1).takeIf { it >= 0 }
        lastFetchTime = prefs.getLong(KEY_TIME, 0L)
    }

    /** Fetches on a background thread and invokes [onResult] when finished. */
    fun refresh(context: Context, onResult: ((Int?) -> Unit)? = null) {
        Thread {
            val temperature = fetchCurrentTemperature(context)
            if (temperature != null) {
                cachedTemperature = temperature
                lastFetchTime = System.currentTimeMillis()
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_TEMP, temperature)
                    .putLong(KEY_TIME, lastFetchTime)
                    .apply()
            }
            onResult?.invoke(cachedTemperature)
        }.start()
    }

    private fun fetchCurrentTemperature(context: Context): Int? {
        val location = resolveLocation(context) ?: return null
        val url = "$OPEN_METEO_URL?latitude=${location.first}&longitude=${location.second}&current=temperature_2m"
        val body = httpGet(url) ?: return null
        return try {
            val current = JSONObject(body).optJSONObject("current") ?: return null
            val temp = current.optDouble("temperature_2m", Double.NaN)
            if (temp.isNaN()) null else temp.roundToInt()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse weather response", e)
            null
        }
    }

    private fun resolveLocation(context: Context): Pair<Double, Double>? =
        deviceLocation(context) ?: ipLocation()

    @SuppressLint("MissingPermission")
    private fun deviceLocation(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                val location = manager.getLastKnownLocation(provider) ?: continue
                return location.latitude to location.longitude
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read $provider location", e)
            }
        }
        return null
    }

    private fun ipLocation(): Pair<Double, Double>? {
        val body = httpGet(IP_GEOLOCATION_URL) ?: return null
        return try {
            val root = JSONObject(body)
            if (!root.optBoolean("success", true)) return null
            val lat = root.optDouble("latitude", Double.NaN)
            val lon = root.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) null else lat to lon
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse IP geolocation response", e)
            null
        }
    }

    private fun httpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP GET failed: $urlString", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
