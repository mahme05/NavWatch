package com.watchnav.com

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

object OrsApi {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openrouteservice.org/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()

    val directions: OrsDirectionsService = retrofit.create(OrsDirectionsService::class.java)
    val geocoding: OrsGeocodingService = retrofit.create(OrsGeocodingService::class.java)
}

interface OrsDirectionsService {
    @POST("v2/directions/{profile}/json")
    suspend fun getDirections(
        @Path("profile") profile: String,
        @Header("Authorization") apiKey: String,
        @Body request: OrsDirectionsRequest
    ): OrsResponse
}

interface OrsGeocodingService {
    @GET("geocode/search")
    suspend fun geocode(
        @Query("text") text: String,
        @Query("api_key") apiKey: String,
        @Query("size") size: Int = 1
    ): OrsGeocodingResponse
}

fun orsStepTypeToAction(type: Int): String = when (type) {
    0 -> "Left"
    1 -> "Right"
    2 -> "Sharp Left"
    3 -> "Sharp Right"
    4 -> "Slight Left"
    5 -> "Slight Right"
    6 -> ""
    7 -> "Roundabout"
    8 -> "Exit Roundabout"
    9 -> "U-turn"
    10 -> "Arrived"
    11 -> ""
    12 -> "Keep Left"
    13 -> "Keep Right"
    else -> ""
}

fun formatOrsDistance(metres: Double): String = when {
    metres >= 1000.0 -> "${"%.1f".format(metres / 1000.0)} km"
    else -> "${metres.toInt()} m"
}

fun orsModeToProfile(mode: String): String = when (mode) {
    "walking" -> "foot-walking"
    "bicycling" -> "cycling-regular"
    else -> "driving-car"
}
