package com.example.kanakku

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.kanakku.ui.MainViewModel
import com.example.kanakku.ui.components.PrivacyInfoDialog
import com.example.kanakku.ui.navigation.KanakkuNavHost
import com.example.kanakku.ui.theme.KanakkuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AppPreferences
        AppPreferences.getInstance(this)

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
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    val appPrefs = remember { AppPreferences.getInstance(context) }

    // Track whether to show the privacy dialog
    var showPrivacyDialog by remember {
        mutableStateOf(!appPrefs.isPrivacyDialogShown())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
        if (isGranted) {
            viewModel.loadSmsData(context)
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.updatePermissionStatus(hasPermission)

        if (hasPermission) {
            viewModel.loadSmsData(context)
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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when {
            !uiState.hasPermission -> {
                PermissionScreen(
                    modifier = Modifier.padding(innerPadding),
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_SMS)
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
                    selectedDateRange = selectedDateRange,
                    onRefresh = { viewModel.loadSmsData(context) },
                    onCategoryChange = { smsId, category ->
                        viewModel.updateTransactionCategory(smsId, category)
                    },
                    onDateRangeChange = { dateRange ->
                        viewModel.updateDateRange(dateRange)
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
            text = "This app needs SMS permission to read your bank transaction messages and help you track spending.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermission) {
            Text("Grant SMS Permission")
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
