package com.example.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FinancialWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context.packageName, FinancialWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val txs = db.financialDao().getAllTransactionsListSync()
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                val currentMonthTxs = txs.filter { it.monthString == currentMonth }
                val income = currentMonthTxs.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
                val expense = currentMonthTxs.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }

                val format = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                    maximumFractionDigits = 0
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_financial)
                    views.setTextViewText(R.id.widget_income, format.format(income))
                    views.setTextViewText(R.id.widget_expense, format.format(expense))

                    // Pending intent to open main activity on click
                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_background, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
