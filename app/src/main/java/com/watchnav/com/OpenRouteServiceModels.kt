package com.watchnav.com

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrsDirectionsRequest(
    val coordinates: List<List<Double>>,
    val language: String = "en",
    val geometry_format: String = "geojson"
)

@JsonClass(generateAdapter = true)
data class OrsResponse(
    val routes: List<OrsRoute>
)

@JsonClass(generateAdapter = true)
data class OrsRoute(
    val summary: OrsSummary,
    val segments: List<OrsSegment>,
    val geometry: OrsGeometry
)

@JsonClass(generateAdapter = true)
data class OrsSummary(val distance: Double, val duration: Double)

@JsonClass(generateAdapter = true)
data class OrsSegment(
    val distance: Double,
    val duration: Double,
    val steps: List<OrsStep>
)

@JsonClass(generateAdapter = true)
data class OrsStep(
    val distance: Double,
    val duration: Double,
    val type: Int,
    val instruction: String,
    val name: String,
    val way_points: List<Int>
)

@JsonClass(generateAdapter = true)
data class OrsGeometry(
    val type: String,
    val coordinates: List<List<Double>>
)

@JsonClass(generateAdapter = true)
data class OrsGeocodingResponse(
    val features: List<OrsGeocodingFeature>
)

@JsonClass(generateAdapter = true)
data class OrsGeocodingFeature(
    val geometry: OrsGeocodingGeometry
)

@JsonClass(generateAdapter = true)
data class OrsGeocodingGeometry(
    val coordinates: List<Double>
)
