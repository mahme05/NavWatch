package com.watchnav.com

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DirectionsResponse(
    val status: String,
    val routes: List<Route>
)

@JsonClass(generateAdapter = true)
data class Route(val legs: List<Leg>)

@JsonClass(generateAdapter = true)
data class Leg(
    val steps: List<Step>,
    val duration: Duration,
    val distance: DirectionsDistance
)

@JsonClass(generateAdapter = true)
data class Step(
    val html_instructions: String,
    val distance: DirectionsDistance,
    val duration: Duration,
    val maneuver: String? = null,
    val end_location: LatLngPoint
)

@JsonClass(generateAdapter = true)
data class DirectionsDistance(val text: String, val value: Int)

@JsonClass(generateAdapter = true)
data class Duration(val text: String, val value: Int)

@JsonClass(generateAdapter = true)
data class LatLngPoint(val lat: Double, val lng: Double)
