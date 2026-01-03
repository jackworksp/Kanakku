package com.example.kanakku.widget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kanakku.R
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.kanakku.ui.theme.KanakkuTheme
import com.example.kanakku.widget.BudgetProgressWidget
import com.example.kanakku.widget.data.BudgetPreferences
import kotlinx.coroutines.launch

/**
 * Configuration activity for the Budget Progress widget.
 *
 * This activity allows users to set their weekly budget amount when adding
 * the Budget Progress widget to their home screen. It provides a simple form
 * with:
 * - Input field for budget amount (numeric only)
 * - Save button to confirm and apply the budget
 * - Cancel button to dismiss without changes
 *
 * The activity follows the Android widget configuration protocol:
 * 1. Extracts the appWidgetId from the intent extras
 * 2. Sets RESULT_CANCELED as default result
 * 3. Returns RESULT_OK with the appWidgetId when successfully configured
 * 4. Updates the widget to reflect the new budget immediately
 *
 * Widget configuration flow:
 * - User adds Budget Progress widget from launcher
 * - Android launches this activity automatically
 * - User enters weekly budget amount
 * - User clicks Save → budget saved, widget updated, result returned
 * - User clicks Cancel or back → widget not added, activity dismissed
 *
 * The budget value is stored in SharedPreferences via BudgetPreferences
 * and persists across app launches and device reboots.
 *
 * @see com.example.kanakku.widget.BudgetProgressWidget
 * @see com.example.kanakku.widget.data.BudgetPreferences
 *
 * Usage in AndroidManifest.xml:
 * ```xml
 * <activity
 *     android:name=".widget.config.BudgetWidgetConfigActivity"
 *     android:exported="true"
 *     android:theme="@style/Theme.Kanakku">
 *     <intent-filter>
 *         <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
 *     </intent-filter>
 * </activity>
 * ```
 */
class BudgetWidgetConfigActivity : ComponentActivity() {

    /**
     * The widget ID for the widget being configured.
     * Extracted from intent extras and used in the result intent.
     */
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set RESULT_CANCELED as default result
        // If the user backs out without saving, the widget will not be added
        setResult(RESULT_CANCELED)

        // Extract the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If no valid widget ID, finish immediately
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Load existing budget (if any) for pre-filling the input
        val existingBudget = BudgetPreferences.getWeeklyBudget(this)

        setContent {
            KanakkuTheme {
                BudgetConfigScreen(
                    initialBudget = if (existingBudget > 0.0) existingBudget else null,
                    onSave = { budget ->
                        saveBudgetAndFinish(budget)
                    },
                    onCancel = {
                        // User cancelled, finish with RESULT_CANCELED
                        finish()
                    }
                )
            }
        }
    }

    /**
     * Saves the budget to preferences, updates the widget, and finishes with success result.
     *
     * This method:
     * 1. Saves the budget amount to SharedPreferences
     * 2. Updates the Budget Progress widget to show the new budget
     * 3. Returns RESULT_OK with the widget ID to confirm widget addition
     *
     * @param budget The weekly budget amount to save
     */
    private fun saveBudgetAndFinish(budget: Double) {
        // Save budget to preferences
        BudgetPreferences.setWeeklyBudget(this, budget)

        // Update the widget to reflect the new budget
        // This triggers a refresh of all Budget Progress widget instances
        val coroutineScope = kotlinx.coroutines.MainScope()
        coroutineScope.launch {
            try {
                val glanceManager = GlanceAppWidgetManager(this@BudgetWidgetConfigActivity)
                BudgetProgressWidget().updateAll(this@BudgetWidgetConfigActivity)
            } catch (e: Exception) {
                // Log error but still finish successfully
                // Widget will update on next scheduled refresh
                e.printStackTrace()
            }
        }

        // Return RESULT_OK with the widget ID
        // This confirms to the launcher that the widget was successfully configured
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)

        // Close the configuration activity
        finish()
    }
}

/**
 * Composable screen for configuring the weekly budget.
 *
 * Displays a simple form with:
 * - Title and description
 * - Input field for budget amount (numeric keyboard)
 * - Save button (enabled only when valid amount entered)
 * - Cancel button
 *
 * Input validation:
 * - Must be a valid positive number
 * - Must be greater than 0
 * - Trailing/leading spaces are trimmed
 *
 * @param initialBudget The existing budget value to pre-fill (if any)
 * @param onSave Callback invoked when Save button is clicked with the budget amount
 * @param onCancel Callback invoked when Cancel button is clicked
 */
@Composable
private fun BudgetConfigScreen(
    initialBudget: Double? = null,
    onSave: (Double) -> Unit,
    onCancel: () -> Unit
) {
    var budgetInput by remember { mutableStateOf(initialBudget?.toInt()?.toString() ?: "") }
    var isError by remember { mutableStateOf(false) }

    // Validate input to enable/disable Save button
    val parsedBudget = budgetInput.trim().toDoubleOrNull()
    val isValidBudget = parsedBudget != null && parsedBudget > 0.0

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = stringResource(R.string.budget_config_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = stringResource(R.string.budget_config_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Budget input field
            OutlinedTextField(
                value = budgetInput,
                onValueChange = {
                    budgetInput = it
                    isError = false
                },
                label = { Text(stringResource(R.string.budget_config_amount_label)) },
                placeholder = { Text(stringResource(R.string.budget_config_amount_placeholder)) },
                leadingIcon = { Text(stringResource(R.string.currency_symbol), style = MaterialTheme.typography.titleMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(R.string.budget_config_error)) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    if (isValidBudget) {
                        onSave(parsedBudget!!)
                    } else {
                        isError = true
                    }
                },
                enabled = budgetInput.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.budget_config_save))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.budget_config_cancel))
            }
        }
    }
}
