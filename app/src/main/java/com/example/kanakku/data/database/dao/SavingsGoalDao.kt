package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kanakku.data.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for savings goal operations.
 * Provides CRUD operations and queries for persisting and retrieving savings goals.
 */
@Dao
interface SavingsGoalDao {

    /**
     * Inserts a single savings goal into the database.
     * If a goal with the same ID exists, it will be replaced.
     *
     * @param goal The savings goal entity to insert
     * @return The row ID of the inserted goal
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    /**
     * Inserts multiple savings goals into the database in a single transaction.
     * If goals with the same ID exist, they will be replaced.
     *
     * @param goals List of savings goal entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    /**
     * Updates an existing savings goal in the database.
     *
     * @param goal The savings goal entity to update
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Update
    suspend fun update(goal: SavingsGoalEntity): Int

    /**
     * Retrieves all savings goals sorted by creation date in descending order (newest first).
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all savings goals
     */
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    /**
     * Retrieves all savings goals as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of all savings goals
     */
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    suspend fun getAllGoalsSnapshot(): List<SavingsGoalEntity>

    /**
     * Retrieves only active (not completed) savings goals.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of active savings goals
     */
    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY deadline ASC")
    fun getActiveGoals(): Flow<List<SavingsGoalEntity>>

    /**
     * Retrieves only completed savings goals.
     * Returns a Flow for reactive updates sorted by completion date.
     *
     * @return Flow emitting list of completed savings goals
     */
    @Query("SELECT * FROM savings_goals WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedGoals(): Flow<List<SavingsGoalEntity>>

    /**
     * Retrieves a single savings goal by its ID.
     *
     * @param id The goal ID to retrieve
     * @return The savings goal entity, or null if not found
     */
    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): SavingsGoalEntity?

    /**
     * Retrieves a single savings goal by its ID as a Flow.
     * Useful for observing changes to a specific goal.
     *
     * @param id The goal ID to retrieve
     * @return Flow emitting the savings goal entity, or null if not found
     */
    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getGoalByIdFlow(id: Long): Flow<SavingsGoalEntity?>

    /**
     * Updates the current progress amount for a specific goal.
     *
     * @param goalId The ID of the goal to update
     * @param currentAmount The new current amount
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Query("UPDATE savings_goals SET currentAmount = :currentAmount WHERE id = :goalId")
    suspend fun updateProgress(goalId: Long, currentAmount: Double): Int

    /**
     * Marks a goal as completed with the completion timestamp.
     *
     * @param goalId The ID of the goal to mark as completed
     * @param completedAt The timestamp when the goal was completed
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Query("UPDATE savings_goals SET isCompleted = 1, completedAt = :completedAt WHERE id = :goalId")
    suspend fun markAsCompleted(goalId: Long, completedAt: Long): Int

    /**
     * Deletes a savings goal by its ID.
     * Note: Related contributions will be automatically deleted due to CASCADE constraint.
     *
     * @param id The ID of the goal to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Deletes all savings goals from the database.
     * Use with caution - this cannot be undone.
     * Note: Related contributions will be automatically deleted due to CASCADE constraint.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll(): Int

    /**
     * Checks if a savings goal with the given ID exists.
     *
     * @param id The goal ID to check
     * @return True if goal exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM savings_goals WHERE id = :id LIMIT 1)")
    suspend fun exists(id: Long): Boolean

    /**
     * Gets the total count of savings goals in the database.
     *
     * @return Total number of goals
     */
    @Query("SELECT COUNT(*) FROM savings_goals")
    suspend fun getGoalCount(): Int

    /**
     * Gets the count of active savings goals.
     *
     * @return Number of active goals
     */
    @Query("SELECT COUNT(*) FROM savings_goals WHERE isCompleted = 0")
    suspend fun getActiveGoalCount(): Int

    /**
     * Gets the count of completed savings goals.
     *
     * @return Number of completed goals
     */
    @Query("SELECT COUNT(*) FROM savings_goals WHERE isCompleted = 1")
    suspend fun getCompletedGoalCount(): Int
}
