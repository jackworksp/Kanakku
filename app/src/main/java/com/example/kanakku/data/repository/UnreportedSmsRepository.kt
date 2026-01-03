package com.example.kanakku.data.repository

import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.UnreportedSmsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing unreported SMS persistence and retrieval.
 *
 * This repository acts as a bridge between the UI layer and the data layer,
 * handling all database operations for user-reported undetected SMS messages.
 *
 * Key responsibilities:
 * - Save and retrieve unreported SMS reports
 * - Update report status and notes
 * - Filter reports by status, sender, and date range
 * - Provide reactive data streams via Flow
 * - Track report statistics
 *
 * @param database The Room database instance
 */
class UnreportedSmsRepository(private val database: KanakkuDatabase) {

    // DAO for database access
    private val unreportedSmsDao = database.unreportedSmsDao()

    // Status constants
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_REVIEWED = "reviewed"
        const val STATUS_PROCESSED = "processed"
        const val STATUS_DISMISSED = "dismissed"
    }

    // ==================== Unreported SMS Operations ====================

    /**
     * Reports a single SMS as undetected by saving it to the database.
     * If an SMS with the same smsId exists, it will be replaced.
     *
     * @param unreportedSms The unreported SMS entity to save
     */
    suspend fun reportSms(unreportedSms: UnreportedSmsEntity) {
        unreportedSmsDao.insert(unreportedSms)
    }

    /**
     * Reports multiple SMS as undetected in a single operation.
     * More efficient than saving individually for bulk reports.
     *
     * @param unreportedSmsList List of unreported SMS entities to save
     */
    suspend fun reportSmsBulk(unreportedSmsList: List<UnreportedSmsEntity>) {
        unreportedSmsDao.insertAll(unreportedSmsList)
    }

    /**
     * Updates an existing unreported SMS report.
     * Useful for updating status or adding/modifying notes.
     *
     * @param unreportedSms The unreported SMS entity to update
     * @return True if the update was successful, false otherwise
     */
    suspend fun updateReport(unreportedSms: UnreportedSmsEntity): Boolean {
        return unreportedSmsDao.update(unreportedSms) > 0
    }

    /**
     * Updates the status of a specific unreported SMS.
     *
     * @param smsId The SMS ID to update
     * @param status The new status value (use STATUS_* constants)
     * @return True if the status was updated, false if SMS not found
     */
    suspend fun updateReportStatus(smsId: Long, status: String): Boolean {
        return unreportedSmsDao.updateStatus(smsId, status) > 0
    }

    /**
     * Retrieves all unreported SMS as a reactive Flow.
     * Reports are sorted by report date (newest first).
     *
     * @return Flow emitting list of all unreported SMS
     */
    fun getAllReports(): Flow<List<UnreportedSmsEntity>> {
        return unreportedSmsDao.getAllUnreportedSms()
    }

    /**
     * Retrieves all unreported SMS as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of all unreported SMS, sorted by report date (newest first)
     */
    suspend fun getAllReportsSnapshot(): List<UnreportedSmsEntity> {
        return unreportedSmsDao.getAllUnreportedSmsSnapshot()
    }

    /**
     * Retrieves unreported SMS filtered by status.
     * Returns a Flow for reactive updates.
     *
     * @param status The status to filter by (use STATUS_* constants)
     * @return Flow emitting list of unreported SMS matching the status
     */
    fun getReportsByStatus(status: String): Flow<List<UnreportedSmsEntity>> {
        return unreportedSmsDao.getUnreportedSmsByStatus(status)
    }

    /**
     * Retrieves pending unreported SMS reports.
     * Convenience method for filtering by STATUS_PENDING.
     *
     * @return Flow emitting list of pending unreported SMS
     */
    fun getPendingReports(): Flow<List<UnreportedSmsEntity>> {
        return getReportsByStatus(STATUS_PENDING)
    }

    /**
     * Retrieves unreported SMS from a specific sender address.
     * Useful for analyzing patterns from a particular bank.
     *
     * @param senderAddress The sender address to filter by
     * @return Flow emitting list of unreported SMS from the sender
     */
    fun getReportsBySender(senderAddress: String): Flow<List<UnreportedSmsEntity>> {
        return unreportedSmsDao.getUnreportedSmsBySender(senderAddress)
    }

    /**
     * Retrieves unreported SMS within a specific date range.
     * Both start and end timestamps are inclusive.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow emitting list of unreported SMS within the date range
     */
    fun getReportsByDateRange(startDate: Long, endDate: Long): Flow<List<UnreportedSmsEntity>> {
        return unreportedSmsDao.getUnreportedSmsByDateRange(startDate, endDate)
    }

    /**
     * Retrieves a specific unreported SMS by its ID.
     *
     * @param smsId The SMS ID to retrieve
     * @return The unreported SMS entity, or null if not found
     */
    suspend fun getReportById(smsId: Long): UnreportedSmsEntity? {
        return unreportedSmsDao.getUnreportedSmsById(smsId)
    }

    /**
     * Checks if an unreported SMS with the given SMS ID exists.
     * Useful for preventing duplicate reports.
     *
     * @param smsId The SMS ID to check
     * @return True if unreported SMS exists, false otherwise
     */
    suspend fun reportExists(smsId: Long): Boolean {
        return unreportedSmsDao.exists(smsId)
    }

    /**
     * Deletes an unreported SMS report by its SMS ID.
     *
     * @param smsId The SMS ID of the unreported SMS to delete
     * @return True if report was deleted, false if not found
     */
    suspend fun deleteReport(smsId: Long): Boolean {
        return unreportedSmsDao.deleteById(smsId) > 0
    }

    /**
     * Deletes all unreported SMS with a specific status.
     * Useful for clearing processed or dismissed reports.
     *
     * @param status The status of reports to delete (use STATUS_* constants)
     * @return Number of reports deleted
     */
    suspend fun deleteReportsByStatus(status: String): Int {
        return unreportedSmsDao.deleteByStatus(status)
    }

    /**
     * Deletes all processed reports.
     * Convenience method for clearing STATUS_PROCESSED reports.
     *
     * @return Number of reports deleted
     */
    suspend fun deleteProcessedReports(): Int {
        return deleteReportsByStatus(STATUS_PROCESSED)
    }

    /**
     * Deletes all dismissed reports.
     * Convenience method for clearing STATUS_DISMISSED reports.
     *
     * @return Number of reports deleted
     */
    suspend fun deleteDismissedReports(): Int {
        return deleteReportsByStatus(STATUS_DISMISSED)
    }

    /**
     * Deletes all unreported SMS from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of reports deleted
     */
    suspend fun deleteAllReports(): Int {
        return unreportedSmsDao.deleteAll()
    }

    // ==================== Statistics Operations ====================

    /**
     * Gets the total count of unreported SMS in the database.
     *
     * @return Total number of unreported SMS reports
     */
    suspend fun getReportCount(): Int {
        return unreportedSmsDao.getUnreportedSmsCount()
    }

    /**
     * Gets the count of unreported SMS by status.
     * Useful for displaying pending reports count in UI.
     *
     * @param status The status to count (use STATUS_* constants)
     * @return Number of unreported SMS with the specified status
     */
    suspend fun getCountByStatus(status: String): Int {
        return unreportedSmsDao.getCountByStatus(status)
    }

    /**
     * Gets the count of pending reports.
     * Convenience method for counting STATUS_PENDING reports.
     *
     * @return Number of pending unreported SMS reports
     */
    suspend fun getPendingReportCount(): Int {
        return getCountByStatus(STATUS_PENDING)
    }

    /**
     * Gets the most recent report date.
     * Useful for determining last user activity.
     *
     * @return The timestamp of the most recent report, or null if no reports exist
     */
    suspend fun getLatestReportDate(): Long? {
        return unreportedSmsDao.getLatestReportDate()
    }
}
