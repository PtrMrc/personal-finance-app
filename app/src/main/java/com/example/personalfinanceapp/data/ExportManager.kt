package com.example.personalfinanceapp.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a plain-text ML research report from the database and exposes it
 * via Android's share sheet using FileProvider.
 *
 * Usage (from a coroutine / LaunchedEffect):
 *   ExportManager(context, db).exportAndShare()
 */
class ExportManager(
    private val context: Context,
    private val db: AppDatabase
) {

    companion object {
        private const val AUTHORITY = "com.example.personalfinanceapp.fileprovider"
        private const val EXPORT_FILENAME = "ml_research_export.txt"
    }

    /**
     * Generates the report, writes it to the app's cache directory,
     * and opens the system share sheet so the user can save or email it.
     *
     * Must be called from a coroutine (suspend).
     */
    suspend fun exportAndShare() {
        val report = buildReport()
        val file = writeToCache(report)
        shareFile(file)
    }

    // -------------------------------------------------------------------------
    // Report builder
    // -------------------------------------------------------------------------

    private suspend fun buildReport(): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        sb.appendLine("=================================================")
        sb.appendLine("  ML RESEARCH EXPORT — Personal Finance App")
        sb.appendLine("=================================================")
        sb.appendLine("Export timestamp: $timestamp")
        sb.appendLine()

        // --- 1. Model weights & accuracy ---
        sb.appendLine("-------------------------------------------------")
        sb.appendLine("MODEL PERFORMANCE")
        sb.appendLine("-------------------------------------------------")

        val performances = db.modelPerformanceDao().getAllPerformances()
        if (performances.isEmpty()) {
            sb.appendLine("  No model performance data recorded yet.")
        } else {
            for (perf in performances) {
                val accuracy = if (perf.totalCount > 0)
                    "%.1f%%".format(perf.correctCount.toDouble() / perf.totalCount * 100)
                else
                    "N/A (no predictions yet)"

                sb.appendLine("  Model       : ${perf.modelName}")
                sb.appendLine("  Weight      : ${"%.4f".format(perf.currentWeight)}")
                sb.appendLine("  Correct     : ${perf.correctCount}")
                sb.appendLine("  Total       : ${perf.totalCount}")
                sb.appendLine("  Accuracy    : $accuracy")
                sb.appendLine()
            }
        }

        // --- 2. Top 10 most frequent words ---
        sb.appendLine("-------------------------------------------------")
        sb.appendLine("TOP 10 WORDS (by frequency across all categories)")
        sb.appendLine("-------------------------------------------------")

        val topWords = db.wordCategoryCountDao().getTopWords(limit = 10)
        if (topWords.isEmpty()) {
            sb.appendLine("  No word data recorded yet.")
        } else {
            topWords.forEachIndexed { index, wcc ->
                sb.appendLine("  ${index + 1}. \"${wcc.word}\" → ${wcc.category}  (count: ${wcc.count})")
            }
        }
        sb.appendLine()

        // --- 3. Correction rate per category ---
        sb.appendLine("-------------------------------------------------")
        sb.appendLine("CORRECTION RATE PER CATEGORY")
        sb.appendLine("-------------------------------------------------")

        val corrections = tryGetCorrectionStats()
        if (corrections == null) {
            sb.appendLine("  CategoryCorrectionStat table not available.")
        } else if (corrections.isEmpty()) {
            sb.appendLine("  No correction data recorded yet.")
        } else {
            for (stat in corrections) {
                val rate = if (stat.totalSuggestions > 0)
                    "%.1f%%".format(stat.totalCorrections.toDouble() / stat.totalSuggestions * 100)
                else
                    "N/A"
                sb.appendLine(
                    "  ${stat.category.padEnd(20)} " +
                            "${stat.totalCorrections}/${stat.totalSuggestions} corrected ($rate)"
                )
            }
        }
        sb.appendLine()

        sb.appendLine("=================================================")
        sb.appendLine("End of report")
        sb.appendLine("=================================================")

        return sb.toString()
    }

    /**
     * Tries to load CategoryCorrectionStat rows.
     * Returns null if the DAO/table is unavailable (e.g. DB version < 6).
     */
    private suspend fun tryGetCorrectionStats(): List<CategoryCorrectionStat>? {
        return try {
            db.categoryCorrectionStatDao().getAll()
        } catch (e: Exception) {
            null
        }
    }

    // -------------------------------------------------------------------------
    // File I/O
    // -------------------------------------------------------------------------

    private fun writeToCache(content: String): File {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, EXPORT_FILENAME)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, AUTHORITY, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ML Research Export — Personal Finance App")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export kutatási adatok")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(chooser)
    }
}