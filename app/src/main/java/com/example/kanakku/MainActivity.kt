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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.notification.NotificationChannelManager
import com.example.kanakku.ui.MainViewModel
import com.example.kanakku.ui.components.InitialSyncProgress
import com.example.kanakku.ui.components.PrivacyInfoDialog
import com.example.kanakku.ui.navigation.KanakkuNavHost
import com.example.kanakku.ui.savings.SavingsGoalsViewModel
import com.example.kanakku.ui.theme.KanakkuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AppPreferences
        AppPreferences.getInstance(this)

        // Register notification channels (Android O+ only, safe to call on all versions)
        NotificationChannelManager.createNotificationChannels(this)

        setContent {
            KanakkuAppWithTheme()
        }
    }
}

/**
 * Root composable that applies theme preferences and wraps KanakkuApp.
 *
 * This composable reads theme preferences from AppPreferences and applies them
 * to the entire app. It reactively updates when preferences change, ensuring
 * theme changes take effect immediately.
 */
@Composable
fun KanakkuAppWithTheme() {
    val context = LocalContext.current
    val appPrefs = remember { AppPreferences.getInstance(context) }

    // Read theme preferences and observe changes
    var darkModePreference by remember { mutableStateOf(appPrefs.isDarkModeEnabled()) }
    var dynamicColorsPreference by remember { mutableStateOf(appPrefs.isDynamicColorsEnabled()) }

    // Listen for preference changes
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "dark_mode_enabled" -> {
                    darkModePreference = appPrefs.isDarkModeEnabled()
                }
                "use_dynamic_colors" -> {
                    dynamicColorsPreference = appPrefs.isDynamicColorsEnabled()
                }
            }
        }

        appPrefs.registerChangeListener(listener)

        onDispose {
            appPrefs.unregisterChangeListener(listener)
        }
    }

    // Determine dark theme setting
    // null = system default, true = dark mode, false = light mode
    val darkTheme = when (darkModePreference) {
        null -> isSystemInDarkTheme()
        else -> darkModePreference == true
    }

    KanakkuTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColorsPreference
    ) {
        KanakkuApp()
    }
}

@Composable
fun KanakkuApp(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val categoryMap by viewModel.categoryMap.collectAsState()
    val searchFilterState by viewModel.searchFilterState.collectAsState()
    val appPrefs = remember { AppPreferences.getInstance(context) }

    // Initialize ThemeViewModel
    LaunchedEffect(Unit) {
        themeViewModel.initialize(context)
    }

    // Observe theme mode for reactive theme changes
    val themeMode by themeViewModel.themeMode.collectAsState()

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
            viewModel.loadSmsData()
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

        if (hasPermission) {
            viewModel.loadSmsData()
        }

        // Initialize SavingsGoalsViewModel
        savingsViewModel.initialize(context)
    }

    // Apply theme with observed theme mode for immediate updates
    KanakkuTheme(themeMode = themeMode) {
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
            uiState.isInitialSync -> {
                // Show initial sync progress during first-time full history sync
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    InitialSyncProgress(
                        progress = uiState.syncProgress,
                        total = uiState.syncTotal,
                        statusMessage = uiState.syncStatusMessage,
                        onCancel = { viewModel.cancelSync() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
            uiState.isLoading -> {
                LoadingScreen(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                KanakkuNavHost(
                    uiState = uiState,
                    categoryMap = categoryMap,
                    onRefresh = { viewModel.loadSmsData() },
                    onCategoryChange = { smsId, category ->
                        viewModel.updateTransactionCategory(smsId, category)
                    },
                    onResetLearnedMappings = {
                        viewModel.resetLearnedMappings()
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
