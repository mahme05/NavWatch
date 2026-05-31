package com.watchnav.com

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun ApiNavigationScreen(
    modifier: Modifier = Modifier,
    onChangeMethod: () -> Unit
) {
    val context = LocalContext.current
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Stable state objects captured by locationCallback
    val stepsState = remember { mutableStateOf<List<NavDirection>>(emptyList()) }
    val endLocationsState = remember { mutableStateOf<List<LatLngPoint>>(emptyList()) }
    val currentStepState = remember { mutableStateOf(0) }
    val isArrivedState = remember { mutableStateOf(false) }

    // UI-facing delegates
    var steps by stepsState
    var stepEndLocations by endLocationsState
    var currentStepIndex by currentStepState
    var isArrived by isArrivedState

    var destination by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("driving") }
    var isLoading by remember { mutableStateOf(false) }
    var totalDistance by remember { mutableStateOf("") }
    var totalDuration by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val currentSteps = stepsState.value
                val endLocs = endLocationsState.value
                if (currentSteps.isEmpty() || endLocs.isEmpty() || isArrivedState.value) return

                val idx = currentStepState.value
                val endLoc = endLocs.getOrNull(idx) ?: return
                val dist = haversineMetres(loc.latitude, loc.longitude, endLoc.lat, endLoc.lng)

                if (dist < 30.0) {
                    if (idx < currentSteps.size - 1) {
                        val newIdx = idx + 1
                        currentStepState.value = newIdx
                        NavNotificationHelper.post(context, currentSteps[newIdx])
                    } else {
                        isArrivedState.value = true
                        NavNotificationHelper.cancel(context)
                    }
                }
            }
        }
    }

    // Start/stop location updates based on whether route is loaded
    DisposableEffect(steps.isEmpty()) {
        if (steps.isNotEmpty() && locationPermission.status.isGranted) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    fun clearNavigation() {
        stepsState.value = emptyList()
        endLocationsState.value = emptyList()
        currentStepState.value = 0
        isArrivedState.value = false
        totalDistance = ""
        totalDuration = ""
        NavNotificationHelper.cancel(context)
    }

    fun fetchRoute() {
        if (destination.isBlank()) return
        coroutineScope.launch {
            isLoading = true
            try {
                val location = getLastKnownLocation(fusedLocationClient)
                if (location == null) {
                    errorMessage = "Could not get current location"
                    return@launch
                }
                val apiKey = context.getSharedPreferences("watchnav_prefs", Context.MODE_PRIVATE)
                    .getString("maps_api_key", "") ?: ""

                val response = DirectionsApi.service.getDirections(
                    origin = "${location.latitude},${location.longitude}",
                    destination = destination,
                    apiKey = apiKey,
                    mode = selectedMode
                )
                if (response.status != "OK") {
                    errorMessage = "API Error: ${response.status}"
                    return@launch
                }
                val leg = response.routes.firstOrNull()?.legs?.firstOrNull()
                if (leg == null) {
                    errorMessage = "No route found"
                    return@launch
                }
                val parsedSteps = leg.steps.map { step ->
                    NavDirection(
                        instruction = parseInstruction(step.html_instructions),
                        distance = step.distance.text,
                        street = parseInstruction(step.html_instructions),
                        action = maneuverToAction(step.maneuver)
                    )
                }
                stepsState.value = parsedSteps
                endLocationsState.value = leg.steps.map { it.end_location }
                currentStepState.value = 0
                isArrivedState.value = false
                totalDistance = leg.distance.text
                totalDuration = leg.duration.text
                NavNotificationHelper.post(context, parsedSteps.first())
            } catch (e: java.io.IOException) {
                errorMessage = "No internet connection"
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF1C1B1F),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (steps.isNotEmpty()) {
                CurrentStepBottomCard(
                    step = steps.getOrNull(currentStepIndex),
                    isArrived = isArrived
                )
            }
        }
    ) { scaffoldPadding ->
        if (!locationPermission.status.isGranted) {
            LocationPermissionCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(20.dp),
                onRequest = { locationPermission.launchPermissionRequest() }
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = if (steps.isNotEmpty()) 180.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "API Navigation",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE6E1E5)
                        )
                        TextButton(onClick = onChangeMethod) {
                            Text(text = "Change Method", color = Color(0xFFD0BCFF), fontSize = 12.sp)
                        }
                    }
                }

                // Section A — Destination input
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = destination,
                                    onValueChange = { destination = it },
                                    label = { Text("Enter destination") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFD0BCFF),
                                        unfocusedBorderColor = Color(0xFF49454F),
                                        focusedLabelColor = Color(0xFFD0BCFF),
                                        unfocusedLabelColor = Color(0xFFCAC4D0),
                                        cursorColor = Color(0xFFD0BCFF),
                                        focusedTextColor = Color(0xFFE6E1E5),
                                        unfocusedTextColor = Color(0xFFE6E1E5)
                                    )
                                )
                                Button(
                                    onClick = { fetchRoute() },
                                    modifier = Modifier.width(72.dp).height(56.dp),
                                    enabled = !isLoading && destination.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD0BCFF),
                                        contentColor = Color(0xFF381E72)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF381E72),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(text = "Go", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            // Mode chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("driving" to "Driving", "walking" to "Walking", "bicycling" to "Cycling")
                                    .forEach { (mode, label) ->
                                        FilterChip(
                                            selected = selectedMode == mode,
                                            onClick = { selectedMode = mode },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFD0BCFF),
                                                selectedLabelColor = Color(0xFF381E72),
                                                containerColor = Color(0xFF4A4458),
                                                labelColor = Color(0xFFCAC4D0)
                                            )
                                        )
                                    }
                            }
                        }
                    }
                }

                // Section B — Status row
                if (steps.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = totalDistance,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE6E1E5),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = totalDuration,
                                    fontSize = 13.sp,
                                    color = Color(0xFFCAC4D0)
                                )
                            }
                            TextButton(onClick = { clearNavigation() }) {
                                Text(text = "Clear", color = Color(0xFFF2B8B5), fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section C — Steps list
                itemsIndexed(steps) { index, step ->
                    val isCurrent = index == currentStepIndex
                    val isPast = index < currentStepIndex
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) Color(0xFF4A4458) else Color(0xFF211F24)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isPast) Modifier.then(Modifier) else Modifier),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) Color(0xFFD0BCFF)
                            else Color(0xFF49454F).copy(alpha = if (isPast) 0.2f else 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .then(if (isPast) Modifier else Modifier),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (isCurrent) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color(0xFF381E72) else Color(0xFFCAC4D0)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.instruction,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isPast) Color(0xFFE6E1E5).copy(alpha = 0.4f)
                                            else Color(0xFFE6E1E5)
                                )
                                if (step.distance.isNotBlank()) {
                                    Text(
                                        text = step.distance,
                                        fontSize = 12.sp,
                                        color = if (isPast) Color(0xFFCAC4D0).copy(alpha = 0.4f)
                                                else Color(0xFFCAC4D0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentStepBottomCard(
    modifier: Modifier = Modifier,
    step: NavDirection?,
    isArrived: Boolean
) {
    Surface(
        color = Color(0xFF211F24),
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isArrived) {
                Text(
                    text = "📍 You have arrived!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBFCFAD),
                    textAlign = TextAlign.Center
                )
            } else if (step != null) {
                if (step.distance.isNotBlank()) {
                    Text(
                        text = "${step.distance} →",
                        fontSize = 12.sp,
                        color = Color(0xFFCAC4D0)
                    )
                }
                Text(
                    text = step.instruction,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (step.action.isNotBlank()) {
                    Text(
                        text = step.action,
                        fontSize = 13.sp,
                        color = Color(0xFFD0BCFF)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    modifier: Modifier = Modifier,
    onRequest: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFFF2B8B5).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFF2B8B5),
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Location permission required to use API navigation",
                    fontSize = 15.sp,
                    color = Color(0xFFE6E1E5),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(text = "Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun getLastKnownLocation(client: FusedLocationProviderClient): Location? =
    suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }
