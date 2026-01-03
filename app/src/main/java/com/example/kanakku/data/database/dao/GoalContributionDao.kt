package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kanakku.data.database.entity.GoalContributionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for goal contribution operations.
 * Provides CRUD operations for persisting and querying manual contributions to savings goals.
 */
@Dao
interface GoalContributionDao {

    /**
     * Inserts a single contribution into the database.
     * If a contribution with the same ID exists, it will be replaced.
     *
     * @param contribution The contribution entity to insert
     * @return The row ID of the inserted contribution
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contribution: GoalContributionEntity): Long

    /**
     * Inserts multiple contributions into the database in a single transaction.
     * If contributions with the same ID exist, they will be replaced.
     *
     * @param contributions List of contribution entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contributions: List<GoalContributionEntity>)

    /**
     * Retrieves all contributions for a specific savings goal.
     * Returns a Flow for reactive updates, sorted by date in descending order (newest first).
     *
     * @param goalId The ID of the goal to retrieve contributions for
     * @return Flow emitting list of contributions for the specified goal
     */
    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    fun getContributionsByGoal(goalId: Long): Flow<List<GoalContributionEntity>>

    /**
     * Retrieves all contributions for a specific savings goal as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @param goalId The ID of the goal to retrieve contributions for
     * @return List of contributions for the specified goal
     */
    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    suspend fun getContributionsByGoalSnapshot(goalId: Long): List<GoalContributionEntity>

    /**
     * Retrieves a single contribution by its ID.
     *
     * @param id The contribution ID to retrieve
     * @return The contribution entity, or null if not found
     */
    @Query("SELECT * FROM goal_contributions WHERE id = :id")
    suspend fun getContributionById(id: Long): GoalContributionEntity?

    /**
     * Calculates the total amount of all contributions for a specific goal.
     * This is useful for tracking the progress toward the goal.
     *
     * @param goalId The ID of the goal to calculate total contributions for
     * @return Total amount of contributions, or 0.0 if no contributions exist
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun getTotalContributionsForGoal(goalId: Long): Double

    /**
     * Calculates the total amount of all contributions for a specific goal as a Flow.
     * Allows reactive observation of contribution total changes.
     *
     * @param goalId The ID of the goal to calculate total contributions for
     * @return Flow emitting total amount of contributions
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM goal_contributions WHERE goalId = :goalId")
    fun getTotalContributionsForGoalFlow(goalId: Long): Flow<Double>

    /**
     * Deletes a contribution by its ID.
     *
     * @param id The ID of the contribution to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM goal_contributions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Deletes all contributions for a specific goal.
     * Note: This is automatically handled by CASCADE when a goal is deleted,
     * but can be called explicitly if needed.
     *
     * @param goalId The ID of the goal whose contributions should be deleted
     * @return Number of rows deleted
     */
    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    suspend fun deleteByGoalId(goalId: Long): Int

    /**
     * Deletes all contributions from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM goal_contributions")
    suspend fun deleteAll(): Int

    /**
     * Checks if a contribution with the given ID exists.
     *
     * @param id The contribution ID to check
     * @return True if contribution exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM goal_contributions WHERE id = :id LIMIT 1)")
    suspend fun exists(id: Long): Boolean

    /**
     * Gets the count of contributions for a specific goal.
     *
     * @param goalId The ID of the goal to count contributions for
     * @return Number of contributions for the goal
     */
    @Query("SELECT COUNT(*) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun getContributionCountForGoal(goalId: Long): Int

    /**
     * Gets the total count of all contributions in the database.
     *
     * @return Total number of contributions across all goals
     */
    @Query("SELECT COUNT(*) FROM goal_contributions")
    suspend fun getTotalContributionCount(): Int

    /**
     * Gets the most recent contribution date for a specific goal.
     * Useful for showing last contribution activity.
     *
     * @param goalId The ID of the goal
     * @return The timestamp of the most recent contribution, or null if no contributions exist
     */
    @Query("SELECT MAX(date) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun getLatestContributionDate(goalId: Long): Long?
}
