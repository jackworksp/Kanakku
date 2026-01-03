package com.example.kanakku.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Service for handling Google Drive operations including authentication and file management.
 *
 * This service manages:
 * - Google Sign-In with Drive API scope
 * - Drive API authentication and credentials
 * - File operations in app-specific folder (appDataFolder)
 *
 * Security features:
 * - Uses appDataFolder for privacy (files not visible to user in Drive)
 * - Requires explicit user authentication
 * - Scoped access to only app-created files
 *
 * Note: This service handles authentication and low-level Drive operations.
 * Use DriveBackupRepository for backup-specific operations.
 */
class GoogleDriveService(private val context: Context) {

    companion object {
        private const val APP_NAME = "Kanakku"
        const val BACKUP_FOLDER_NAME = "appDataFolder"
        const val BACKUP_MIME_TYPE = "application/octet-stream"
        const val BACKUP_FILE_EXTENSION = ".kbak"
    }

    private var driveService: Drive? = null
    private var signInClient: GoogleSignInClient? = null

    /**
     * Initialize Google Sign-In client with Drive scope.
     * Call this before requesting sign-in.
     */
    fun initializeSignInClient() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

        signInClient = GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Get the sign-in intent for user authentication.
     * Launch this intent with startActivityForResult to get sign-in result.
     *
     * @return Intent for Google Sign-In flow
     * @throws DriveAuthException if sign-in client not initialized
     */
    fun getSignInIntent(): Intent {
        val client = signInClient
            ?: throw DriveAuthException("Sign-in client not initialized. Call initializeSignInClient() first.")
        return client.signInIntent
    }

    /**
     * Check if user is currently signed in with Drive access.
     *
     * @return GoogleSignInAccount if signed in, null otherwise
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && hasRequiredScopes(account)) {
            account
        } else {
            null
        }
    }

    /**
     * Check if account has required Drive scopes.
     */
    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
        return account.grantedScopes.contains(requiredScope)
    }

    /**
     * Handle sign-in result and initialize Drive service.
     * Call this from onActivityResult after sign-in flow.
     *
     * @param account GoogleSignInAccount from sign-in result
     * @throws DriveAuthException if account doesn't have required scopes
     */
    suspend fun handleSignInResult(account: GoogleSignInAccount) = withContext(Dispatchers.IO) {
        if (!hasRequiredScopes(account)) {
            throw DriveAuthException("Account does not have required Drive scopes")
        }

        try {
            // Create credential from account
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = account.account

            // Initialize Drive service
            driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(APP_NAME)
                .build()
        } catch (e: Exception) {
            throw DriveAuthException("Failed to initialize Drive service", e)
        }
    }

    /**
     * Sign out from Google account and clear Drive service.
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            signInClient?.signOut()?.await()
            driveService = null
        } catch (e: Exception) {
            throw DriveException("Failed to sign out", e)
        }
    }

    /**
     * Check if Drive service is initialized and ready for operations.
     *
     * @return true if Drive service is ready
     */
    fun isDriveServiceReady(): Boolean {
        return driveService != null
    }

    /**
     * Upload a file to Google Drive app folder.
     *
     * @param fileName Name of the file to create
     * @param data Data to upload
     * @return File ID of created file
     * @throws DriveException if upload fails
     */
    suspend fun uploadFile(fileName: String, data: ByteArray): String = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: throw DriveAuthException("Drive service not initialized. Sign in first.")

        try {
            // Create file metadata
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(BACKUP_FOLDER_NAME)
                mimeType = BACKUP_MIME_TYPE
            }

            // Upload file
            val content = data.inputStream()
            val file = drive.files().create(fileMetadata, com.google.api.client.http.InputStreamContent(BACKUP_MIME_TYPE, content))
                .setFields("id, name, createdTime, size")
                .execute()

            file.id ?: throw DriveException("Failed to create file - no ID returned")
        } catch (e: Exception) {
            when (e) {
                is DriveException -> throw e
                else -> throw DriveException("Failed to upload file to Drive", e)
            }
        }
    }

    /**
     * Download a file from Google Drive by ID.
     *
     * @param fileId ID of the file to download
     * @return ByteArray containing file data
     * @throws DriveException if download fails
     */
    suspend fun downloadFile(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: throw DriveAuthException("Drive service not initialized. Sign in first.")

        try {
            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            throw DriveException("Failed to download file from Drive", e)
        }
    }

    /**
     * Download a file from Google Drive by ID to an output stream.
     *
     * @param fileId ID of the file to download
     * @param outputStream Output stream to write file data
     * @throws DriveException if download fails
     */
    suspend fun downloadFileToStream(fileId: String, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: throw DriveAuthException("Drive service not initialized. Sign in first.")

        try {
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        } catch (e: Exception) {
            throw DriveException("Failed to download file from Drive", e)
        }
    }

    /**
     * List all backup files in the app folder.
     *
     * @return List of DriveFileInfo for all backup files
     * @throws DriveException if listing fails
     */
    suspend fun listBackupFiles(): List<DriveFileInfo> = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: throw DriveAuthException("Drive service not initialized. Sign in first.")

        try {
            val result: FileList = drive.files().list()
                .setSpaces(BACKUP_FOLDER_NAME)
                .setQ("name contains '$BACKUP_FILE_EXTENSION'")
                .setFields("files(id, name, createdTime, modifiedTime, size)")
                .setOrderBy("modifiedTime desc")
                .execute()

            result.files?.map { file ->
                DriveFileInfo(
                    id = file.id,
                    name = file.name,
                    createdTime = file.createdTime?.value ?: 0L,
                    modifiedTime = file.modifiedTime?.value ?: 0L,
                    size = file.size ?: 0L
                )
            } ?: emptyList()
        } catch (e: Exception) {
            throw DriveException("Failed to list files from Drive", e)
        }
    }

    /**
     * Delete a file from Google Drive.
     *
     * @param fileId ID of the file to delete
     * @throws DriveException if deletion fails
     */
    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: throw DriveAuthException("Drive service not initialized. Sign in first.")

        try {
            drive.files().delete(fileId).execute()
        } catch (e: Exception) {
            throw DriveException("Failed to delete file from Drive", e)
        }
    }

    /**
     * Get file metadata without downloading content.
     *
     * @param fileId ID of the file
     * @return DriveFileInfo containing file metadata
     * @throws DriveException if retrieval fails
     */
    suspend fun getFileInfo(fileId: String): DriveFileInfo = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: throw DriveAuthException("Drive service not initialized. Sign in first.")

        try {
            val file = drive.files().get(fileId)
                .setFields("id, name, createdTime, modifiedTime, size")
                .execute()

            DriveFileInfo(
                id = file.id,
                name = file.name,
                createdTime = file.createdTime?.value ?: 0L,
                modifiedTime = file.modifiedTime?.value ?: 0L,
                size = file.size ?: 0L
            )
        } catch (e: Exception) {
            throw DriveException("Failed to get file info from Drive", e)
        }
    }
}

/**
 * Information about a file stored in Google Drive
 */
data class DriveFileInfo(
    val id: String,
    val name: String,
    val createdTime: Long,
    val modifiedTime: Long,
    val size: Long
)

/**
 * Exception thrown when Drive authentication fails
 */
class DriveAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when Drive operations fail
 */
class DriveException(message: String, cause: Throwable? = null) : Exception(message, cause)
