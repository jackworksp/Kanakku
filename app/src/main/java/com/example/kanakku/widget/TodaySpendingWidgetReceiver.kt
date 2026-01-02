package com.example.kanakku.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Widget receiver for the Today's Spending widget.
 *
 * This receiver handles the lifecycle events for the small widget (2x1) that displays
 * today's total spending. It serves as the entry point for the Android system to
 * interact with the widget.
 *
 * The receiver is registered in AndroidManifest.xml with the APPWIDGET_UPDATE action
 * and linked to the widget configuration in XML resources.
 *
 * Lifecycle events handled:
 * - onUpdate: Called when the widget needs to be updated (periodic or manual)
 * - onEnabled: Called when the first instance of the widget is added
 * - onDisabled: Called when the last instance of the widget is removed
 * - onDeleted: Called when a widget instance is removed
 *
 * The actual widget UI and data fetching is handled by [TodaySpendingWidget].
 *
 * @see TodaySpendingWidget
 * @see com.example.kanakku.widget.data.WidgetDataRepository
 *
 * Usage in AndroidManifest.xml:
 * ```xml
 * <receiver
 *     android:name=".widget.TodaySpendingWidgetReceiver"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
 *     </intent-filter>
 *     <meta-data
 *         android:name="android.appwidget.provider"
 *         android:resource="@xml/today_spending_widget_info" />
 * </receiver>
 * ```
 */
class TodaySpendingWidgetReceiver : GlanceAppWidgetReceiver() {

    /**
     * Returns the GlanceAppWidget implementation for this receiver.
     *
     * This property is required by GlanceAppWidgetReceiver and must return
     * the widget instance that will handle the UI rendering and data display.
     *
     * @return Instance of TodaySpendingWidget
     */
    override val glanceAppWidget: GlanceAppWidget = TodaySpendingWidget()
}
