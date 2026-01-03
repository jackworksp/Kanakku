package com.example.kanakku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.GoalContributionDao
import com.example.kanakku.data.database.dao.SavingsGoalDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.GoalContributionEntity
import com.example.kanakku.data.database.entity.SavingsGoalEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity

/**
 * Main Room database for the Kanakku application.
 * Manages all entities and provides access to DAOs for database operations.
 *
 * This database stores:
 * - Parsed transactions from SMS messages
 * - User's manual category assignments
 * - Synchronization metadata for incremental updates
 * - Savings goals and progress tracking
 * - Manual contributions to savings goals
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryOverrideEntity::class,
        SyncMetadataEntity::class,
        SavingsGoalEntity::class,
        GoalContributionEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KanakkuDatabase : RoomDatabase() {

    /**
     * Provides access to transaction data operations.
     * @return TransactionDao instance for CRUD operations on transactions
     */
    abstract fun transactionDao(): TransactionDao

    /**
     * Provides access to category override operations.
     * @return CategoryOverrideDao instance for managing manual category assignments
     */
    abstract fun categoryOverrideDao(): CategoryOverrideDao

    /**
     * Provides access to sync metadata operations.
     * @return SyncMetadataDao instance for tracking synchronization state
     */
    abstract fun syncMetadataDao(): SyncMetadataDao

    /**
     * Provides access to savings goal operations.
     * @return SavingsGoalDao instance for CRUD operations on savings goals
     */
    abstract fun savingsGoalDao(): SavingsGoalDao

    /**
     * Provides access to goal contribution operations.
     * @return GoalContributionDao instance for managing contributions to savings goals
     */
    abstract fun goalContributionDao(): GoalContributionDao

    companion object {
        /**
         * Migration from version 1 to version 2.
         * Adds savings_goals and goal_contributions tables for the savings goals feature.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create savings_goals table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL,
                        `deadline` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `icon` TEXT,
                        `color` TEXT
                    )
                    """.trimIndent()
                )

                // Create indices for savings_goals
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_savings_goals_deadline` ON `savings_goals` (`deadline`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_savings_goals_isCompleted` ON `savings_goals` (`isCompleted`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_savings_goals_createdAt` ON `savings_goals` (`createdAt`)"
                )

                // Create goal_contributions table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goal_contributions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `note` TEXT,
                        FOREIGN KEY(`goalId`) REFERENCES `savings_goals`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Create indices for goal_contributions
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_goal_contributions_goalId` ON `goal_contributions` (`goalId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_goal_contributions_date` ON `goal_contributions` (`date`)"
                )
            }
        }
    }
}
