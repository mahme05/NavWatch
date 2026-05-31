package com.watchnav.com

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ApiKeySetupScreen(
    modifier: Modifier = Modifier,
    onKeySaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("watchnav_prefs", Context.MODE_PRIVATE)
    val provider = prefs.getString("nav_source", "api_google") ?: "api_google"
    val isOrs = provider == "api_ors"

    LaunchedEffect(Unit) {
        apiKey = if (isOrs) prefs.getString("ors_api_key", "") ?: ""
                 else prefs.getString("maps_api_key", "") ?: ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFD0BCFF)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOrs) "Set Up ORS API Key" else "Set Up API Key",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE6E1E5)
            )
        }

        Text(
            text = if (isOrs) "Required once — free tier: 2,000 requests/day"
                   else "Required once — free tier covers ~10,000 trips/month",
            fontSize = 13.sp,
            color = Color(0xFFCAC4D0)
        )

        // How to get API key card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "How to get your API key",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(12.dp))

                if (isOrs) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF4A4458), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFEADDFF)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouteservice.org/dev/#/signup"))
                                )
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Go to openrouteservice.org and sign up",
                                fontSize = 14.sp,
                                color = Color(0xFFD0BCFF),
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                    InstructionStep(order = "2", text = "Verify your email and log in to the developer dashboard")
                    InstructionStep(order = "3", text = "Click 'Request a Token' under the free plan")
                    InstructionStep(order = "4", text = "Give the token any name and click 'Create Token'")
                    InstructionStep(order = "5", text = "Copy the generated token and paste it below")
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF4A4458), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFEADDFF)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://console.cloud.google.com"))
                                )
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Go to console.cloud.google.com",
                                fontSize = 14.sp,
                                color = Color(0xFFD0BCFF),
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                    InstructionStep(order = "2", text = "Sign in with your Google account")
                    InstructionStep(order = "3", text = "Click 'Select a project' → 'New Project', give it any name")
                    InstructionStep(order = "4", text = "Go to 'APIs & Services' → 'Enable APIs and Services'")
                    InstructionStep(order = "5", text = "Search for 'Directions API' and click Enable")
                    InstructionStep(order = "6", text = "Go to 'APIs & Services' → 'Credentials'")
                    InstructionStep(order = "7", text = "Click 'Create Credentials' → 'API Key'")
                    InstructionStep(order = "8", text = "Copy the generated key and paste it below")
                }
            }
        }

        // API Key input card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Your API Key",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFFE6E1E5)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        hasError = false
                    },
                    label = { Text("Paste your API Key here") },
                    visualTransformation = if (showKey) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.Visibility
                                              else Icons.Default.VisibilityOff,
                                contentDescription = if (showKey) "Hide key" else "Show key",
                                tint = Color(0xFFCAC4D0)
                            )
                        }
                    },
                    isError = hasError,
                    supportingText = if (hasError) {
                        {
                            Text(
                                if (isOrs) "Key must be at least 20 characters"
                                else "Key must start with 'AIza' and be 39 characters",
                                color = Color(0xFFF2B8B5)
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedLabelColor = Color(0xFFD0BCFF),
                        unfocusedLabelColor = Color(0xFFCAC4D0),
                        cursorColor = Color(0xFFD0BCFF),
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        errorBorderColor = Color(0xFFF2B8B5),
                        errorLabelColor = Color(0xFFF2B8B5)
                    )
                )

                Button(
                    onClick = {
                        val trimmed = apiKey.trim()
                        val valid = if (isOrs) trimmed.length >= 20
                                    else trimmed.startsWith("AIza") && trimmed.length == 39
                        if (valid) {
                            val prefKey = if (isOrs) "ors_api_key" else "maps_api_key"
                            prefs.edit().putString(prefKey, trimmed).apply()
                            onKeySaved()
                        } else {
                            hasError = true
                        }
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
                    Text(text = "Save & Continue", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = if (isOrs) "Your key is stored only on this device and never sent anywhere except OpenRouteService servers."
                           else "Your key is stored only on this device and never sent anywhere except Google's servers.",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
