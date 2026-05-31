package com.watchnav.com

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.pow
import kotlin.math.sqrt

object DirectionsApi {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: DirectionsApiService = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()
        )
        .build()
        .create(DirectionsApiService::class.java)
}

interface DirectionsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String,
        @Query("mode") mode: String = "driving"
    ): DirectionsResponse
}

fun parseInstruction(html: String): String =
    android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT)
        .toString()
        .trim()

fun maneuverToAction(maneuver: String?): String = when (maneuver) {
    "turn-left" -> "Left"
    "turn-right" -> "Right"
    "turn-sharp-left" -> "Sharp Left"
    "turn-slight-left" -> "Slight Left"
    "turn-sharp-right" -> "Sharp Right"
    "turn-slight-right" -> "Slight Right"
    "roundabout-left", "roundabout-right" -> "Roundabout"
    "merge" -> "Merge"
    "ramp-left" -> "Ramp Left"
    "ramp-right" -> "Ramp Right"
    "fork-left" -> "Keep Left"
    "fork-right" -> "Keep Right"
    "ferry" -> "Ferry"
    "straight" -> ""
    else -> ""
}

fun haversineMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).pow(2) +
            Math.cos(Math.toRadians(lat1)) *
            Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).pow(2)
    return R * 2 * Math.atan2(sqrt(a), sqrt(1 - a))
}
