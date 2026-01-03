package com.example.kanakku

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.notification.TransactionNotificationManager
import com.example.kanakku.ui.MainViewModel
import com.example.kanakku.ui.components.NotificationPermissionDialog
import com.example.kanakku.ui.components.PrivacyInfoDialog
import com.example.kanakku.ui.navigation.KanakkuNavHost
import com.example.kanakku.ui.theme.KanakkuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AppPreferences
        AppPreferences.getInstance(this)

        // Initialize notification channel for transaction alerts
        // Required for Android 8.0 (API 26) and above
        val notificationManager = TransactionNotificationManager(this)
        notificationManager.createNotificationChannel()

        setContent {
            KanakkuTheme {
                KanakkuApp()
            }
        }
    }
}

@Composable
fun KanakkuApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val categoryMap by viewModel.categoryMap.collectAsState()
    val appPrefs = remember { AppPreferences.getInstance(context) }

    // Track whether to show the privacy dialog
    var showPrivacyDialog by remember {
        mutableStateOf(!appPrefs.isPrivacyDialogShown())
    }

    // Track whether to show the notification permission dialog (Android 13+)
    var showNotificationPermissionDialog by remember {
        mutableStateOf(false)
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+ only)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.i(
                "MainActivity",
                "POST_NOTIFICATIONS permission granted - transaction notifications enabled"
            )
        } else {
            android.util.Log.i(
                "MainActivity",
                "POST_NOTIFICATIONS permission denied - transaction notifications disabled"
            )
        }
        // App continues to work regardless of permission result
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if at least READ_SMS is granted (minimum required functionality)
        val hasReadSms = permissions[Manifest.permission.READ_SMS] == true
        val hasReceiveSms = permissions[Manifest.permission.RECEIVE_SMS] == true

        viewModel.updatePermissionStatus(hasReadSms)

        if (hasReadSms) {
            viewModel.loadSmsData(context)

            // Log warning if RECEIVE_SMS is not granted (real-time detection won't work)
            if (!hasReceiveSms) {
                android.util.Log.w(
                    "MainActivity",
                    "RECEIVE_SMS permission denied - real-time transaction detection disabled"
                )
            }

            // On Android 13+, prompt for notification permission after SMS permissions are granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotificationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                // Show explanation dialog if permission is not already granted
                if (!hasNotificationPermission) {
                    showNotificationPermissionDialog = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasReadSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasReceiveSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.updatePermissionStatus(hasReadSms)

        if (hasReadSms) {
            viewModel.loadSmsData(context)

            // Log info if RECEIVE_SMS is not granted
            if (!hasReceiveSms) {
                android.util.Log.i(
                    "MainActivity",
                    "RECEIVE_SMS permission not granted - real-time transaction detection disabled"
                )
            }
        }
    }

    // Show privacy dialog on first launch
    if (showPrivacyDialog) {
        PrivacyInfoDialog(
            onDismiss = {
                appPrefs.setPrivacyDialogShown()
                showPrivacyDialog = false
            }
        )
    }

    // Show notification permission dialog on Android 13+ (after SMS permissions granted)
    if (showNotificationPermissionDialog) {
        NotificationPermissionDialog(
            onDismiss = {
                // User chose "Not Now" - app continues to work without notifications
                showNotificationPermissionDialog = false
                android.util.Log.i(
                    "MainActivity",
                    "User declined notification permission prompt - notifications disabled"
                )
            },
            onGrantPermission = {
                // User chose "Enable Notifications" - request the permission
                showNotificationPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when {
            !uiState.hasPermission -> {
                PermissionScreen(
                    modifier = Modifier.padding(innerPadding),
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS
                            )
                        )
                    }
                )
            }
            uiState.isLoading -> {
                LoadingScreen(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                KanakkuNavHost(
                    uiState = uiState,
                    categoryMap = categoryMap,
                    onRefresh = { viewModel.loadSmsData(context) },
                    onCategoryChange = { smsId, category ->
                        viewModel.updateTransactionCategory(smsId, category)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun PermissionScreen(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Kanakku",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Track your bank transactions automatically",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "This app needs SMS permissions to:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "• Read your bank transaction messages\n• Detect new transactions in real-time\n• Help you track spending automatically",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "All data stays on your device. No information is sent to any server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermission) {
            Text("Grant SMS Permissions")
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Reading SMS messages...")
        }
    }
}
