package com.spendwise.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.spendwise.app.data.local.entity.Expense
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportExpenses(context: Context, expenses: List<Expense>, currency: String): Uri? {
        return try {
            val fileName = "SpendWise_Export_${
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }.csv"

            val file = File(context.getExternalFilesDir(null), fileName)
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            // use{} closes the file even if writing fails
            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("ID,Title,Amount ($currency),Category,Date,Note\n")

                // Write each expense row (a double quote inside a quoted CSV field is written as "")
                expenses.forEach { expense ->
                    writer.append("${expense.id},")
                    writer.append("\"${expense.title.replace("\"", "\"\"")}\",")
                    writer.append("${expense.amount},")
                    writer.append("${expense.category},")
                    writer.append("${sdf.format(Date(expense.date))},")
                    writer.append("\"${expense.note.replace("\"", "\"\"")}\"\n")
                }

                // Write summary at bottom
                writer.append("\n")
                writer.append("SUMMARY\n")
                writer.append("Total Expenses,${expenses.size}\n")
                writer.append("Total Amount,$currency ${expenses.sumOf { it.amount }}\n")
                writer.append("Export Date,${sdf.format(Date())}\n")
                writer.append("Exported By,SpendWise App v1.0\n")
            }

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
