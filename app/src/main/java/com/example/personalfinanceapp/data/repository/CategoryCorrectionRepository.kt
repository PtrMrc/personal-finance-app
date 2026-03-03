package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.CategoryCorrectionStat
import com.example.personalfinanceapp.data.CategoryCorrectionStatDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for AI category correction rate tracking.
 * Follows the same pattern as the other repositories in this project.
 */
class CategoryCorrectionRepository(
    private val dao: CategoryCorrectionStatDao
) {
    /** Live stream of all correction stats, worst-performing categories first. */
    val allStats: Flow<List<CategoryCorrectionStat>> = dao.getAllStatsFlow()

    /**
     * Record one AI prediction event.
     *
     * @param aiSuggestedCategory  The category the ensemble predicted.
     * @param userChosenCategory   The category the user actually saved.
     *
     * Logic:
     *  - Always increments [totalSuggestions] for the AI-suggested category.
     *  - If the user chose differently, also increments [totalCorrections].
     */
    suspend fun recordPrediction(aiSuggestedCategory: String, userChosenCategory: String) {
        // Ensure a row exists for the AI-suggested category before updating it
        dao.insertIfAbsent(CategoryCorrectionStat(category = aiSuggestedCategory))
        dao.incrementSuggestions(aiSuggestedCategory)

        val wasCorrected = aiSuggestedCategory.trim() != userChosenCategory.trim()
        if (wasCorrected) {
            dao.incrementCorrections(aiSuggestedCategory)
        }
    }

    /** One-shot snapshot, useful for data export. */
    suspend fun getAllStatsSnapshot(): List<CategoryCorrectionStat> =
        dao.getAllStatsSnapshot()
}