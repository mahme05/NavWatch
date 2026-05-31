package com.watchnav.com

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.watchnav.com.ui.theme.MyApplicationTheme

enum class Screen {
    SOURCE_SELECT, API_KEY_SETUP, API_NAV, DASHBOARD, ACCESSIBILITY_GUIDE
}

fun resolveStartScreen(context: Context): Screen {
    val prefs = context.getSharedPreferences("watchnav_prefs", Context.MODE_PRIVATE)
    return when (prefs.getString("nav_source", null)) {
        "notification", "accessibility" -> Screen.DASHBOARD
        "api" -> if (prefs.getString("maps_api_key", null) != null) Screen.API_NAV
                 else Screen.API_KEY_SETUP
        else -> Screen.SOURCE_SELECT
    }
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission required for live visual updates", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                val prefs = getSharedPreferences("watchnav_prefs", Context.MODE_PRIVATE)
                var currentScreen by remember { mutableStateOf(resolveStartScreen(this)) }

                Scaffold(
                    modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
                    containerColor = Color(0xFF1C1B1F)
                ) { innerPadding ->
                    when (currentScreen) {
                        Screen.SOURCE_SELECT -> SourceSelectionScreen(
                            modifier = Modifier.padding(innerPadding),
                            onSelect = { source ->
                                prefs.edit().putString("nav_source", source).apply()
                                currentScreen = when (source) {
                                    "notification" -> Screen.DASHBOARD
                                    "api" -> Screen.API_KEY_SETUP
                                    "accessibility" -> {
                                        val shown = prefs.getBoolean("accessibility_guide_shown", false)
                                        if (shown) Screen.DASHBOARD else Screen.ACCESSIBILITY_GUIDE
                                    }
                                    else -> Screen.DASHBOARD
                                }
                            }
                        )

                        Screen.API_KEY_SETUP -> ApiKeySetupScreen(
                            modifier = Modifier.padding(innerPadding),
                            onKeySaved = { currentScreen = Screen.API_NAV },
                            onBack = { currentScreen = Screen.SOURCE_SELECT }
                        )

                        Screen.API_NAV -> ApiNavigationScreen(
                            modifier = Modifier.padding(innerPadding),
                            onChangeMethod = {
                                prefs.edit().remove("nav_source").apply()
                                currentScreen = Screen.SOURCE_SELECT
                            }
                        )

                        Screen.DASHBOARD -> DashboardScreen(
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            onChangeMethod = {
                                prefs.edit().remove("nav_source").apply()
                                currentScreen = Screen.SOURCE_SELECT
                            }
                        )

                        Screen.ACCESSIBILITY_GUIDE -> AccessibilityGuideScreen(
                            modifier = Modifier.padding(innerPadding),
                            onContinue = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onChangeMethod: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isNotificationEnabled by remember { mutableStateOf(false) }
    var isMapsInstalled by remember { mutableStateOf(false) }
    var isNothingXInstalled by remember { mutableStateOf(false) }
    val isServiceConnected by NavListenerService.isServiceConnected.collectAsState()
    val activeDirection by NavNotificationHelper.currentDirection.collectAsState()

    val checkStatus = {
        isNotificationEnabled = isNotificationServiceEnabled(context)
        isMapsInstalled = isPackageInstalled(context, "com.google.android.apps.maps") ||
                          isPackageInstalled(context, "com.waze")
        isNothingXInstalled = isPackageInstalled(context, "com.nothing.x") ||
                              isPackageInstalled(context, "com.nothing.smartwatch") ||
                              isPackageInstalled(context, "com.nothing.smartwatch.cmf") ||
                              isPackageInstalled(context, "com.nothing.smartcenter")
    }

    LaunchedEffect(Unit) {
        checkStatus()
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader(onChangeMethod = onChangeMethod)

        GlobalStatusCard(
            isNotificationEnabled = isNotificationEnabled,
            isServiceConnected = isServiceConnected
        )

        CustomActionPanel(
            context = context,
            isNotificationEnabled = isNotificationEnabled,
            onRefresh = { checkStatus() }
        )

        SystemChecklistCard(
            isNotificationEnabled = isNotificationEnabled,
            isMapsInstalled = isMapsInstalled,
            isNothingXInstalled = isNothingXInstalled
        )

        ActiveNavigationCard(
            activeDirection = activeDirection,
            onTriggerTest = {
                val dummyDirection = NavDirection(
                    instruction = "Turn Right onto Grand Avenue",
                    distance = "250 m",
                    street = "Grand Ave",
                    action = "Right"
                )
                NavNotificationHelper.post(context, dummyDirection)
                Toast.makeText(context, "Simulated navigation notification sent!", Toast.LENGTH_SHORT).show()
                checkStatus()
            },
            onClearTest = {
                NavNotificationHelper.cancel(context)
                Toast.makeText(context, "Cleared navigation notifications", Toast.LENGTH_SHORT).show()
                checkStatus()
            }
        )

        SetupInstructionsCard()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AppHeader(modifier: Modifier = Modifier, onChangeMethod: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.watchnav_logo),
                contentDescription = "WatchNav Logo",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "WatchNav",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE6E1E5),
                letterSpacing = (-0.5).sp
            )
        }

        if (onChangeMethod != null) {
            IconButton(
                onClick = onChangeMethod,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Change navigation method",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF49454F), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFD0BCFF), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun GlobalStatusCard(
    isNotificationEnabled: Boolean,
    isServiceConnected: Boolean
) {
    val isSystemReady = isNotificationEnabled && isServiceConnected

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A4458)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SERVICE STATUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFEADDFF)
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (isSystemReady) Color(0xFFD0BCFF) else Color(0xFFF2B8B5),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSystemReady) "ACTIVE" else "REQUIRED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemReady) Color(0xFF381E72) else Color(0xFF601410)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isSystemReady) "Listening" else "Inactive",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                if (isSystemReady) {
                    Row(
                        modifier = Modifier.padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFFD0BCFF), RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFFD0BCFF).copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFFD0BCFF).copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isSystemReady) {
                    "Ready to bridge navigation notifications to your CMF Watch 3 Pro."
                } else {
                    "Access setup requested. Read notifications permission missing or listener offline."
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFEADDFF).copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SystemChecklistCard(
    isNotificationEnabled: Boolean,
    isMapsInstalled: Boolean,
    isNothingXInstalled: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "System Integrations",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFFE6E1E5)
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF49454F))
            Spacer(modifier = Modifier.height(12.dp))

            ChecklistItem(
                label = "Notification Reader Access",
                isOk = isNotificationEnabled,
                description = "Required to intercept Google Maps/Waze turn lists"
            )
            ChecklistItem(
                label = "Google Maps install detected",
                isOk = isMapsInstalled,
                description = "Required as primary navigation engine source"
            )
            ChecklistItem(
                label = "Nothing X watch app detected",
                isOk = isNothingXInstalled,
                description = "Required companion app to deliver alerts over Bluetooth"
            )
        }
    }
}

@Composable
fun ChecklistItem(label: String, isOk: Boolean, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (isOk) Modifier.background(Color(0xFFBFCFAD), RoundedCornerShape(12.dp))
                    else Modifier
                        .background(Color.Transparent, RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFFF2B8B5), RoundedCornerShape(12.dp))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isOk) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isOk) Color(0xFF1A1C16) else Color(0xFFF2B8B5),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOk) Color(0xFFE6E1E5) else Color(0xFFE6E1E5).copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 12.sp, color = Color(0xFFCAC4D0))
        }
    }
}

@Composable
fun CustomActionPanel(
    context: Context,
    isNotificationEnabled: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isNotificationEnabled) {
            Button(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open Notification Settings: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("grant_permission_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF381E72)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "GRANT PERMISSIONS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F)),
            shape = RoundedCornerShape(25.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "RE-VERIFY PERMISSIONS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun ActiveNavigationCard(
    activeDirection: NavDirection?,
    onTriggerTest: () -> Unit,
    onClearTest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Live Stream Monitor", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFFE6E1E5))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Real-time bridge parser preview from navigation notifications", fontSize = 12.sp, color = Color(0xFFCAC4D0))
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF49454F))
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFF1C1B1F),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (activeDirection != null) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFBFCFAD),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (activeDirection.distance.isNotBlank())
                                "${activeDirection.distance} → ${activeDirection.instruction}"
                            else activeDirection.instruction,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeDirection.action.isNotBlank())
                                "${activeDirection.action}: ${activeDirection.street}"
                            else activeDirection.street,
                            fontSize = 14.sp,
                            color = Color(0xFFBFCFAD),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No Routing Stream Received", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFFCAC4D0))
                        Text(text = "Awaiting active routing on your mobile device", fontSize = 12.sp, color = Color(0xFF49454F))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTriggerTest,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4458), contentColor = Color(0xFFEADDFF)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trigger Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClearTest,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF2B8B5)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SetupInstructionsCard() {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F24)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Required Setup Action Items", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFFE6E1E5))
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(12.dp))
                InstructionStep(order = "1", text = "Grant notification reader access permission by clicking 'Grant Notification Access' at the top.")
                InstructionStep(order = "2", text = "Open the Nothing X App on your phone. Go to Watch Settings -> Notification Options. Scroll to find and whitelist/enable 'WatchNav' in the companion forwarding list.")
                InstructionStep(order = "3", text = "Go to Phone Settings -> Apps -> Google Maps -> Battery. Choose 'Unrestricted' battery optimization so maps updates aren't frozen during background routing.")
                InstructionStep(order = "4", text = "Set WatchNav App's battery settings to 'Unrestricted' (optional, but highly recommended for uninterrupted trips).")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF49454F).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(20.dp))
                Text(
                    text = "Enable WatchNav in Nothing X app's notification settings to see directions on your wrist.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFCAC4D0),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InstructionStep(modifier: Modifier = Modifier, order: String, text: String) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFF4A4458), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = order, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFEADDFF))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFFCAC4D0), modifier = Modifier.weight(1f))
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabledListeners.contains(context.packageName)
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}
