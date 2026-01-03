package com.example.kanakku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.widget.data.WidgetDataRepository
import com.example.kanakku.widget.model.WidgetTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Large widget (4x3) displaying recent transactions list.
 *
 * This widget provides a quick overview of the user's most recent transactions.
 * It shows:
 * - "Recent Transactions" header
 * - List of up to 5 recent transactions
 * - Each transaction displays: merchant/description, amount, date
 * - Color coding: Debit amounts in red, Credit amounts in green
 * - Empty state message when no transactions exist
 *
 * The widget supports both light and dark themes and updates periodically
 * to show the latest transaction data.
 *
 * Size: 4x3 cells (approximately 250dp x 180dp)
 * Update frequency: Managed by WorkManager (hourly)
 *
 * @see com.example.kanakku.widget.RecentTransactionsWidgetReceiver
 * @see com.example.kanakku.widget.data.WidgetDataRepository
 */
class RecentTransactionsWidget : GlanceAppWidget() {

    /**
     * Provides the widget content using Jetpack Glance composables.
     *
     * This method is called when the widget needs to be rendered.
     * It fetches the latest transactions from the repository and composes the UI.
     *
     * @param context The context for the widget
     * @param glanceId Unique identifier for this widget instance
     */
    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        // Fetch recent transactions from repository
        val repository = WidgetDataRepository(context)
        val transactionsData = repository.getRecentTransactions(limit = 5)

        provideContent {
            GlanceTheme {
                RecentTransactionsContent(
                    transactions = transactionsData.transactions
                )
            }
        }
    }

    /**
     * Composable function that defines the widget's UI layout.
     *
     * Displays recent transactions in a scrollable list format suitable for
     * a large 4x3 widget size. The layout adapts to both light and dark themes
     * with color-coded transaction amounts.
     *
     * @param transactions The list of recent transactions to display
     */
    @Composable
    private fun RecentTransactionsContent(
        transactions: List<WidgetTransaction>
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Widget header
            Text(
                text = "Recent Transactions",
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.size(12.dp))

            if (transactions.isEmpty()) {
                // Show empty state when no transactions
                EmptyTransactionsView()
            } else {
                // Show transaction list
                TransactionList(transactions = transactions)
            }
        }
    }

    /**
     * Composable displaying the list of transactions.
     *
     * Each transaction shows merchant name, amount with color coding,
     * and formatted date. Debit amounts appear in red and credit amounts
     * in green for quick visual distinction.
     *
     * @param transactions The list of transactions to display
     */
    @Composable
    private fun TransactionList(transactions: List<WidgetTransaction>) {
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            transactions.forEach { transaction ->
                TransactionItem(transaction = transaction)
                Spacer(modifier = GlanceModifier.size(8.dp))
            }
        }
    }

    /**
     * Composable displaying a single transaction item.
     *
     * Shows merchant name on the left and amount on the right,
     * with the date displayed below the merchant name.
     *
     * @param transaction The transaction to display
     */
    @Composable
    private fun TransactionItem(transaction: WidgetTransaction) {
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            // Merchant and amount row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Merchant name (left side)
                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = transaction.merchant,
                        style = TextStyle(
                            color = GlanceTheme.colors.onBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = GlanceModifier.size(8.dp))

                // Amount (right side) with prefix for debit/credit
                val amountPrefix = when (transaction.type) {
                    TransactionType.DEBIT -> "-"
                    TransactionType.CREDIT -> "+"
                    TransactionType.UNKNOWN -> ""
                }
                Text(
                    text = "$amountPrefix${formatAmount(transaction.amount)}",
                    style = TextStyle(
                        color = getAmountColor(transaction.type),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Date below merchant
            Text(
                text = formatDate(transaction.date),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }

    /**
     * Composable displaying an empty state message.
     *
     * Shown when the user has no transactions in the database.
     * Provides a helpful message to guide the user.
     */
    @Composable
    private fun EmptyTransactionsView() {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No transactions yet",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.size(4.dp))

            Text(
                text = "Transactions will appear here",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Determines the color for the transaction amount based on type.
     *
     * Color coding:
     * - DEBIT: Red (money going out)
     * - CREDIT: Green (money coming in)
     * - UNKNOWN: Gray (indeterminate)
     *
     * @param type The transaction type
     * @return ColorProvider for the amount text
     */
    private fun getAmountColor(type: TransactionType): ColorProvider {
        return when (type) {
            TransactionType.DEBIT -> ColorProvider(Color(0xFFE53935)) // Red for debits
            TransactionType.CREDIT -> ColorProvider(Color(0xFF43A047)) // Green for credits
            TransactionType.UNKNOWN -> ColorProvider(Color(0xFF757575)) // Gray for unknown
        }
    }

    /**
     * Formats the amount with currency symbol and proper number formatting.
     *
     * Examples:
     * - formatAmount(1234.50) -> "₹1,234.50"
     * - formatAmount(500.0) -> "₹500.00"
     * - formatAmount(0.0) -> "₹0.00"
     *
     * @param amount The amount to format
     * @return Formatted amount string with currency symbol
     */
    private fun formatAmount(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "₹${formatter.format(amount)}"
    }

    /**
     * Formats the timestamp to a short date string.
     *
     * Examples:
     * - "Jan 15"
     * - "Dec 31"
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted date string
     */
    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
