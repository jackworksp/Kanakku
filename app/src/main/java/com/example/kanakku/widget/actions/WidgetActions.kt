package com.example.kanakku.widget.actions

import android.content.Context
import android.content.Intent
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * Widget action callbacks for handling tap events on widgets.
 *
 * These action callbacks create deep link intents that navigate to specific
 * screens in the app when a widget is tapped. The MainActivity will handle
 * these intents and route to the appropriate navigation destination.
 *
 * Usage:
 * - TodaySpendingWidget: OpenTransactionsAction (view all transactions)
 * - BudgetProgressWidget: OpenAnalyticsAction (view budget analytics)
 * - RecentTransactionsWidget: OpenTransactionsAction (view transaction details)
 *
 * @see com.example.kanakku.MainActivity
 * @see com.example.kanakku.ui.navigation.KanakkuNavigation
 */

/**
 * Action constants for widget navigation.
 *
 * These constants define the action strings and extra keys used for
 * deep linking from widgets to app screens.
 */
object WidgetActions {
    /**
     * Action to open the Transactions screen.
     * Widget: TodaySpendingWidget, RecentTransactionsWidget
     * Destination: Transactions tab
     */
    const val ACTION_OPEN_TRANSACTIONS = "com.example.kanakku.OPEN_TRANSACTIONS"

    /**
     * Action to open the Analytics screen.
     * Widget: BudgetProgressWidget
     * Destination: Analytics tab
     */
    const val ACTION_OPEN_ANALYTICS = "com.example.kanakku.OPEN_ANALYTICS"

    /**
     * Action to open the Categories screen.
     * Widget: (Future use)
     * Destination: Categories tab
     */
    const val ACTION_OPEN_CATEGORIES = "com.example.kanakku.OPEN_CATEGORIES"

    /**
     * Intent extra key for target destination route.
     * Value should match one of the BottomNavItem routes:
     * - "transactions"
     * - "analytics"
     * - "categories"
     */
    const val EXTRA_DESTINATION = "destination"
}

/**
 * ActionCallback that opens the Transactions screen.
 *
 * When the widget is tapped, this action creates an intent that launches
 * MainActivity with the action ACTION_OPEN_TRANSACTIONS and navigates to
 * the Transactions tab.
 *
 * Used by:
 * - TodaySpendingWidget (to view detailed spending breakdown)
 * - RecentTransactionsWidget (to view full transaction list)
 *
 * Example usage in widget:
 * ```kotlin
 * Column(
 *     modifier = GlanceModifier
 *         .clickable(actionStartActivity<OpenTransactionsAction>())
 * ) {
 *     // Widget content
 * }
 * ```
 */
class OpenTransactionsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters
    ) {
        // Create intent to launch MainActivity with transactions destination
        val intent = Intent(context, com.example.kanakku.MainActivity::class.java).apply {
            action = WidgetActions.ACTION_OPEN_TRANSACTIONS
            putExtra(WidgetActions.EXTRA_DESTINATION, "transactions")
            // Clear top ensures we don't create multiple MainActivity instances
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        context.startActivity(intent)
    }
}

/**
 * ActionCallback that opens the Analytics screen.
 *
 * When the widget is tapped, this action creates an intent that launches
 * MainActivity with the action ACTION_OPEN_ANALYTICS and navigates to
 * the Analytics tab.
 *
 * Used by:
 * - BudgetProgressWidget (to view detailed budget analytics and spending trends)
 *
 * Example usage in widget:
 * ```kotlin
 * Column(
 *     modifier = GlanceModifier
 *         .clickable(actionStartActivity<OpenAnalyticsAction>())
 * ) {
 *     // Widget content
 * }
 * ```
 */
class OpenAnalyticsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters
    ) {
        // Create intent to launch MainActivity with analytics destination
        val intent = Intent(context, com.example.kanakku.MainActivity::class.java).apply {
            action = WidgetActions.ACTION_OPEN_ANALYTICS
            putExtra(WidgetActions.EXTRA_DESTINATION, "analytics")
            // Clear top ensures we don't create multiple MainActivity instances
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        context.startActivity(intent)
    }
}

/**
 * ActionCallback that opens the Categories screen.
 *
 * When the widget is tapped, this action creates an intent that launches
 * MainActivity with the action ACTION_OPEN_CATEGORIES and navigates to
 * the Categories tab.
 *
 * Used by:
 * - (Reserved for future category-based widgets)
 *
 * Example usage in widget:
 * ```kotlin
 * Column(
 *     modifier = GlanceModifier
 *         .clickable(actionStartActivity<OpenCategoriesAction>())
 * ) {
 *     // Widget content
 * }
 * ```
 */
class OpenCategoriesAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters
    ) {
        // Create intent to launch MainActivity with categories destination
        val intent = Intent(context, com.example.kanakku.MainActivity::class.java).apply {
            action = WidgetActions.ACTION_OPEN_CATEGORIES
            putExtra(WidgetActions.EXTRA_DESTINATION, "categories")
            // Clear top ensures we don't create multiple MainActivity instances
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        context.startActivity(intent)
    }
}
