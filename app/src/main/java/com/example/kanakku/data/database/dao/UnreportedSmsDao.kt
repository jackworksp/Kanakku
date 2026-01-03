package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kanakku.data.database.entity.UnreportedSmsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for unreported SMS operations.
 * Provides operations for persisting and querying user-reported undetected SMS messages.
 */
@Dao
interface UnreportedSmsDao {

    /**
     * Inserts a single unreported SMS into the database.
     * If an SMS with the same smsId exists, it will be replaced.
     *
     * @param sms The unreported SMS entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: UnreportedSmsEntity)

    /**
     * Inserts multiple unreported SMS messages into the database in a single transaction.
     * If SMS with the same smsId exist, they will be replaced.
     *
     * @param smsList List of unreported SMS entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(smsList: List<UnreportedSmsEntity>)

    /**
     * Updates an existing unreported SMS entity.
     * Useful for updating status or adding notes.
     *
     * @param sms The unreported SMS entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(sms: UnreportedSmsEntity): Int

    /**
     * Retrieves all unreported SMS messages sorted by report date in descending order (newest first).
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all unreported SMS
     */
    @Query("SELECT * FROM unreported_sms ORDER BY reportedAt DESC")
    fun getAllUnreportedSms(): Flow<List<UnreportedSmsEntity>>

    /**
     * Retrieves all unreported SMS messages as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of all unreported SMS
     */
    @Query("SELECT * FROM unreported_sms ORDER BY reportedAt DESC")
    suspend fun getAllUnreportedSmsSnapshot(): List<UnreportedSmsEntity>

    /**
     * Retrieves unreported SMS filtered by status.
     * Returns a Flow for reactive updates.
     *
     * @param status The status to filter by (e.g., "pending", "reviewed", "processed", "dismissed")
     * @return Flow emitting list of unreported SMS matching the status
     */
    @Query("SELECT * FROM unreported_sms WHERE status = :status ORDER BY reportedAt DESC")
    fun getUnreportedSmsByStatus(status: String): Flow<List<UnreportedSmsEntity>>

    /**
     * Retrieves unreported SMS from a specific sender address.
     * Useful for analyzing patterns from a particular bank.
     *
     * @param senderAddress The sender address to filter by
     * @return Flow emitting list of unreported SMS from the sender
     */
    @Query("SELECT * FROM unreported_sms WHERE senderAddress = :senderAddress ORDER BY reportedAt DESC")
    fun getUnreportedSmsBySender(senderAddress: String): Flow<List<UnreportedSmsEntity>>

    /**
     * Retrieves unreported SMS within a specific date range.
     * Both start and end timestamps are inclusive.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow emitting list of unreported SMS within the date range
     */
    @Query("SELECT * FROM unreported_sms WHERE reportedAt BETWEEN :startDate AND :endDate ORDER BY reportedAt DESC")
    fun getUnreportedSmsByDateRange(startDate: Long, endDate: Long): Flow<List<UnreportedSmsEntity>>

    /**
     * Retrieves a specific unreported SMS by its ID.
     *
     * @param smsId The SMS ID to retrieve
     * @return The unreported SMS entity, or null if not found
     */
    @Query("SELECT * FROM unreported_sms WHERE smsId = :smsId")
    suspend fun getUnreportedSmsById(smsId: Long): UnreportedSmsEntity?

    /**
     * Updates the status of a specific unreported SMS.
     *
     * @param smsId The SMS ID to update
     * @param status The new status value
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Query("UPDATE unreported_sms SET status = :status WHERE smsId = :smsId")
    suspend fun updateStatus(smsId: Long, status: String): Int

    /**
     * Deletes an unreported SMS by its SMS ID.
     *
     * @param smsId The SMS ID of the unreported SMS to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM unreported_sms WHERE smsId = :smsId")
    suspend fun deleteById(smsId: Long): Int

    /**
     * Deletes all unreported SMS with a specific status.
     * Useful for clearing processed or dismissed reports.
     *
     * @param status The status of reports to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM unreported_sms WHERE status = :status")
    suspend fun deleteByStatus(status: String): Int

    /**
     * Deletes all unreported SMS from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM unreported_sms")
    suspend fun deleteAll(): Int

    /**
     * Checks if an unreported SMS with the given SMS ID exists.
     *
     * @param smsId The SMS ID to check
     * @return True if unreported SMS exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM unreported_sms WHERE smsId = :smsId LIMIT 1)")
    suspend fun exists(smsId: Long): Boolean

    /**
     * Gets the total count of unreported SMS in the database.
     *
     * @return Total number of unreported SMS
     */
    @Query("SELECT COUNT(*) FROM unreported_sms")
    suspend fun getUnreportedSmsCount(): Int

    /**
     * Gets the count of unreported SMS by status.
     * Useful for displaying pending reports count in UI.
     *
     * @param status The status to count
     * @return Number of unreported SMS with the specified status
     */
    @Query("SELECT COUNT(*) FROM unreported_sms WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    /**
     * Gets the most recent report date.
     * Useful for determining last user activity.
     *
     * @return The timestamp of the most recent report, or null if no reports exist
     */
    @Query("SELECT MAX(reportedAt) FROM unreported_sms")
    suspend fun getLatestReportDate(): Long?
}
