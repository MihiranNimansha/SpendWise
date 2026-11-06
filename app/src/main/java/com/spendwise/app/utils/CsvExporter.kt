package com.spendwise.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.spendwise.app.data.local.entity.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportExpenses(context: Context, expenses: List<Expense>, currency: String): Uri? {
        return try {
            val fileName = "SpendWise_Export_${
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }.csv"

            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(buildCsv(expenses, currency), Charsets.UTF_8)

            // Return URI via FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Builds the whole CSV document (header, one row per expense, summary) as a string.
     * Pure function, so it can be unit-tested without a device.
     */
    fun buildCsv(
        expenses: List<Expense>,
        currency: String,
        exportedOn: Date = Date(),
        locale: Locale = Locale.getDefault()
    ): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", locale)
        val csv = StringBuilder()

        csv.append("ID,Title,Amount ($currency),Category,Date,Note\n")
        expenses.forEach { expense ->
            csv.append("${expense.id},")
            csv.append(quote(expense.title)).append(',')
            csv.append("${expense.amount},")
            csv.append("${expense.category},")
            csv.append("${sdf.format(Date(expense.date))},")
            csv.append(quote(expense.note)).append('\n')
        }

        csv.append("\n")
        csv.append("SUMMARY\n")
        csv.append("Total Expenses,${expenses.size}\n")
        csv.append("Total Amount,$currency ${expenses.sumOf { it.amount }}\n")
        csv.append("Export Date,${sdf.format(exportedOn)}\n")
        csv.append("Exported By,SpendWise App v1.0\n")
        return csv.toString()
    }

    /**
     * Wraps free text in quotes for CSV (a quote inside becomes two quotes). Text that starts with
     * = + - @ is prefixed with an apostrophe so spreadsheet apps show it as text instead of
     * running it as a formula ("CSV injection").
     */
    internal fun quote(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    fun shareCSV(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SpendWise Expense Export")
            putExtra(Intent.EXTRA_TEXT, "Please find my expense report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export via..."))
    }
}
