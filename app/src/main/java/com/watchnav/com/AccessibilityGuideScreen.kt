package com.watchnav.com

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccessibilityGuideScreen(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "One Thing Before You Start",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE6E1E5)
        )
        Text(
            text = "Accessibility mode requires one extra step in Google Maps",
            fontSize = 13.sp,
            color = Color(0xFFCAC4D0)
        )

        // Steps card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Setup Steps",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(12.dp))

                InstructionStep(order = "1", text = "Open Google Maps and search for your destination")
                InstructionStep(order = "2", text = "Tap 'Start' to begin navigation")
                InstructionStep(order = "3", text = "Look for the grey bar at the bottom showing your ETA and distance")
                InstructionStep(order = "4", text = "Tap that bar — a panel slides up from the bottom")
                InstructionStep(order = "5", text = "Tap 'Directions' in that panel — this shows the full step list")
                InstructionStep(order = "6", text = "Now switch back to WatchNav — it will read the list automatically")
            }
        }

        // Warning card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2B8B5).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFF2B8B5).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF2B8B5),
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "This step is not optional",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFF2B8B5)
                    )
                    Text(
                        text = "If the Directions panel is not open in Google Maps, " +
                                "Accessibility Service cannot see the steps and your " +
                                "watch will not receive any directions. " +
                                "You must tap 'Directions' every time before navigating.",
                        fontSize = 13.sp,
                        color = Color(0xFFF2B8B5).copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Info box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD0BCFF).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "You only need to see this guide once. " +
                            "WatchNav will remember that you've read it.",
                    fontSize = 12.sp,
                    color = Color(0xFFCAC4D0),
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = {
                context.getSharedPreferences("watchnav_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("accessibility_guide_shown", true)
                    .apply()
                onContinue()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Got it, start navigating",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
