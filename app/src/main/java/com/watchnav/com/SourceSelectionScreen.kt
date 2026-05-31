package com.watchnav.com

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SourceSelectionScreen(
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose Navigation Source",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE6E1E5)
        )
        Text(
            text = "Select how WatchNav gets directions to send to your watch",
            fontSize = 13.sp,
            color = Color(0xFFCAC4D0)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SourceCard(
            emoji = "🔔",
            title = "Notification Listener",
            description = "Intercepts Maps/Waze navigation notifications silently",
            pros = listOf(
                "No API key needed",
                "Works with Google Maps and Waze",
                "Zero extra battery usage"
            ),
            cons = listOf(
                "Notifications may be delayed or dropped",
                "No persistent watch screen between turns",
                "May break if Maps updates change notification format"
            ),
            warningText = null,
            onSelect = { onSelect("notification") }
        )

        SourceCard(
            emoji = "🗺️",
            title = "Google Maps Directions API",
            description = "Fetches your full route from Google and tracks it via GPS",
            pros = listOf(
                "Reliable — independent of notification delivery",
                "Gets all steps upfront with distance to next turn",
                "Works even when Maps is minimised or screen is off",
                "Step advances automatically based on GPS location"
            ),
            cons = listOf(
                "Requires a free Google API key (guided setup inside app)",
                "Uses internet data and GPS in background",
                "Free tier: 10,000 requests/month"
            ),
            warningText = null,
            onSelect = { onSelect("api_google") }
        )

        SourceCard(
            emoji = "🌐",
            title = "OpenRouteService API",
            description = "Open-source routing — fetches route via ORS and tracks via GPS",
            pros = listOf(
                "Free & open-source — no billing account needed",
                "2,000 requests/day on free tier",
                "Privacy-friendly alternative to Google",
                "Step advances automatically based on GPS location"
            ),
            cons = listOf(
                "Requires a free ORS API key (guided setup inside app)",
                "Geocodes addresses separately — slightly slower first load",
                "Coverage may be lower in some regions vs Google"
            ),
            warningText = null,
            onSelect = { onSelect("api_ors") }
        )

        SourceCard(
            emoji = "♿",
            title = "Accessibility Service",
            description = "Reads the Maps directions list directly from your screen",
            pros = listOf(
                "No API key needed",
                "Reads every step from the visible directions list",
                "Very accurate — reflects exactly what Maps shows"
            ),
            cons = listOf(
                "Requires Accessibility permission (system shows scary prompt)",
                "Only works when Maps directions panel is visible on screen",
                "May break if Maps UI layout changes"
            ),
            warningText = "Important: Before starting navigation, open Google Maps, " +
                    "tap the route bar at the bottom, then tap 'Directions' to " +
                    "show the step-by-step list. Accessibility only works when " +
                    "this panel is visible on screen.",
            onSelect = { onSelect("accessibility") }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SourceCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    description: String,
    pros: List<String>,
    cons: List<String>,
    warningText: String?,
    onSelect: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = emoji, fontSize = 28.sp)
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6E1E5)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color(0xFFCAC4D0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF49454F))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pros & Cons",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCAC4D0)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                  else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFFCAC4D0),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pros.forEach { pro ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFBFCFAD),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = pro, fontSize = 13.sp, color = Color(0xFFCAC4D0))
                        }
                    }
                    cons.forEach { con ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFF2B8B5),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = con, fontSize = 13.sp, color = Color(0xFFCAC4D0))
                        }
                    }
                }
            }

            if (warningText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2B8B5).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFF2B8B5).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF2B8B5),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = warningText,
                            fontSize = 12.sp,
                            color = Color(0xFFF2B8B5),
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(text = "Use This", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
