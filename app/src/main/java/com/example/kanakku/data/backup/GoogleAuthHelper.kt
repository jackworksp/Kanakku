package com.example.kanakku.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for managing Google Sign-In flow with Drive scope.
 *
 * This helper simplifies the Google Sign-In process for Drive access by:
 * - Providing ActivityResultContract for modern Activity Result API
 * - Managing GoogleDriveService initialization
 * - Handling sign-in callbacks with proper error handling
 * - Supporting both Compose (rememberLauncherForActivityResult) and traditional Activity patterns
 *
 * Usage with Compose:
 * ```
 * val googleAuthHelper = remember { GoogleAuthHelper(context, driveService) }
 * val signInLauncher = rememberLauncherForActivityResult(
 *     contract = googleAuthHelper.signInContract()
 * ) { result ->
 *     result.onSuccess { account ->
 *         // Handle successful sign-in
 *     }.onFailure { error ->
 *         // Handle error
 *     }
 * }
 *
 * // Launch sign-in
 * googleAuthHelper.launchSignIn(signInLauncher)
 * ```
 *
 * Usage with Activity:
 * ```
 * val googleAuthHelper = GoogleAuthHelper(context, driveService)
 * val signInLauncher = registerForActivityResult(googleAuthHelper.signInContract()) { result ->
 *     result.onSuccess { account ->
 *         // Handle successful sign-in
 *     }.onFailure { error ->
 *         // Handle error
 *     }
 * }
 *
 * // Launch sign-in
 * googleAuthHelper.launchSignIn(signInLauncher)
 * ```
 */
class GoogleAuthHelper(
    private val context: Context,
    private val driveService: GoogleDriveService
) {

    /**
     * Check if user is currently signed in with Drive access.
     *
     * @return GoogleSignInAccount if signed in, null otherwise
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return driveService.getSignedInAccount()
    }

    /**
     * Check if user is signed in and Drive service is ready.
     *
     * @return true if signed in and Drive service is initialized
     */
    fun isSignedIn(): Boolean {
        return getSignedInAccount() != null && driveService.isDriveServiceReady()
    }

    /**
     * Sign out from Google account and clear Drive service.
     */
    suspend fun signOut() {
        driveService.signOut()
    }

    /**
     * Get ActivityResultContract for Google Sign-In flow.
     * Use this with registerForActivityResult or rememberLauncherForActivityResult.
     *
     * @return ActivityResultContract that returns Result<GoogleSignInAccount>
     */
    fun signInContract(): ActivityResultContract<Unit, Result<GoogleSignInAccount>> {
        return GoogleSignInContract(context, driveService)
    }

    /**
     * Launch the Google Sign-In flow.
     * Call this to start the sign-in process.
     *
     * @param launcher ActivityResultLauncher obtained from registerForActivityResult
     */
    fun launchSignIn(launcher: ActivityResultLauncher<Unit>) {
        launcher.launch(Unit)
    }

    /**
     * Handle sign-in result and initialize Drive service.
     * This is automatically called by GoogleSignInContract, but can be used
     * for manual handling if needed.
     *
     * @param account GoogleSignInAccount from successful sign-in
     * @return Result with GoogleSignInAccount on success
     */
    suspend fun handleSignInResult(account: GoogleSignInAccount): Result<GoogleSignInAccount> {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize Drive service with account
                driveService.handleSignInResult(account)
                Result.success(account)
            } catch (e: DriveAuthException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(DriveAuthException("Failed to handle sign-in result", e))
            }
        }
    }
}

/**
 * ActivityResultContract for Google Sign-In with Drive scope.
 *
 * This contract handles the complete sign-in flow:
 * 1. Creates sign-in intent from GoogleDriveService
 * 2. Launches the intent
 * 3. Processes the result and initializes Drive service
 * 4. Returns Result<GoogleSignInAccount> with success or error
 */
private class GoogleSignInContract(
    private val context: Context,
    private val driveService: GoogleDriveService
) : ActivityResultContract<Unit, Result<GoogleSignInAccount>>() {

    /**
     * Create sign-in intent.
     * This initializes the sign-in client and returns the intent.
     */
    override fun createIntent(context: Context, input: Unit): Intent {
        try {
            driveService.initializeSignInClient()
            return driveService.getSignInIntent()
        } catch (e: DriveAuthException) {
            // Return empty intent if initialization fails
            // Error will be handled in parseResult
            return Intent()
        }
    }

    /**
     * Parse the sign-in result.
     * Extracts GoogleSignInAccount from result and initializes Drive service.
     */
    override fun parseResult(resultCode: Int, intent: Intent?): Result<GoogleSignInAccount> {
        // Check if user cancelled
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return Result.failure(
                DriveAuthException("Sign-in cancelled or failed")
            )
        }

        return try {
            // Get sign-in result from intent
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)

            if (account == null) {
                Result.failure(DriveAuthException("Sign-in failed - no account returned"))
            } else {
                // Note: Drive service initialization will be handled by the caller
                // using GoogleAuthHelper.handleSignInResult() in a coroutine
                Result.success(account)
            }
        } catch (e: ApiException) {
            Result.failure(
                DriveAuthException("Sign-in failed with code ${e.statusCode}", e)
            )
        } catch (e: Exception) {
            Result.failure(
                DriveAuthException("Sign-in failed", e)
            )
        }
    }
}

/**
 * Extension function to suspend and wait for GoogleDriveService initialization
 * after successful sign-in.
 *
 * Usage:
 * ```
 * val signInLauncher = rememberLauncherForActivityResult(
 *     contract = googleAuthHelper.signInContract()
 * ) { result ->
 *     lifecycleScope.launch {
 *         result.initializeDriveService(googleAuthHelper)
 *             .onSuccess { account ->
 *                 // Drive service is ready
 *             }
 *             .onFailure { error ->
 *                 // Handle error
 *             }
 *     }
 * }
 * ```
 */
suspend fun Result<GoogleSignInAccount>.initializeDriveService(
    helper: GoogleAuthHelper
): Result<GoogleSignInAccount> = withContext(Dispatchers.IO) {
    if (isSuccess) {
        val account = getOrThrow()
        helper.handleSignInResult(account)
    } else {
        this@initializeDriveService
    }
}
