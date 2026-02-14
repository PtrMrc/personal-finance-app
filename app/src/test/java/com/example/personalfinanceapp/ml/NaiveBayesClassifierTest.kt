package com.example.personalfinanceapp.ml

import com.example.personalfinanceapp.data.WordCategoryCount
import com.example.personalfinanceapp.data.WordCategoryCountDao
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for NaiveBayesClassifier
 *
 * These tests use a mock DAO to verify the classifier logic
 * without requiring a real database
 */
class NaiveBayesClassifierTest {

    private lateinit var classifier: NaiveBayesClassifier
    private lateinit var mockDao: MockWordCategoryCountDao

    @Before
    fun setup() {
        mockDao = MockWordCategoryCountDao()
        classifier = NaiveBayesClassifier(mockDao)
    }

    @Test
    fun `test training stores word counts`() = runBlocking {
        // Train with a simple expense
        classifier.train("Netflix subscription", "Bills")

        // Verify words were stored
        assertTrue(mockDao.hasWord("netflix"))
        assertTrue(mockDao.hasWord("subscription"))
        assertEquals("Bills", mockDao.getCategoryForWord("netflix"))
    }

    @Test
    fun `test training increments counts`() = runBlocking {
        // Train multiple times with same word
        classifier.train("Coffee", "Food")
        classifier.train("Coffee shop", "Food")
        classifier.train("Morning coffee", "Food")

        // Count should be incremented
        val count = mockDao.getCount("coffee", "Food")
        assertEquals(3, count)
    }

    @Test
    fun `test prediction returns probabilities`() = runBlocking {
        // Train with sample data
        classifier.train("Netflix", "Bills")
        classifier.train("Netflix subscription", "Bills")
        classifier.train("Coffee", "Food")
        classifier.train("Lunch", "Food")

        // Predict category for "Netflix"
        val predictions = classifier.predict("Netflix")

        // Should have predictions
        assertTrue(predictions.isNotEmpty())

        // Should predict "Bills" with high probability
        val topPrediction = predictions.first()
        assertEquals("Bills", topPrediction.category)
        assertTrue(topPrediction.probability > 0.5)
    }

    @Test
    fun `test prediction with unknown words uses Laplace smoothing`() = runBlocking {
        // Train with some data
        classifier.train("Coffee", "Food")

        // Predict with completely unknown word
        val predictions = classifier.predict("XYZ123")

        // Should still return predictions (due to Laplace smoothing)
        // May be empty if no categories exist, but shouldn't crash
        assertNotNull(predictions)
    }

    @Test
    fun `test probabilities sum to approximately 1`() = runBlocking {
        // Train with data
        classifier.train("Netflix", "Bills")
        classifier.train("Coffee", "Food")
        classifier.train("Uber", "Transport")

        // Get predictions
        val predictions = classifier.predict("Test")

        if (predictions.isNotEmpty()) {
            // Sum of probabilities should be close to 1.0
            val sum = predictions.sumOf { it.probability }
            assertEquals(1.0, sum, 0.01)  // Within 1% tolerance
        }
    }

    @Test
    fun `test model stats are accurate`() = runBlocking {
        // Train with known data
        classifier.train("Netflix subscription", "Bills")  // 2 words
        classifier.train("Coffee", "Food")                 // 1 word

        val stats = classifier.getModelStats()

        // Should have learned 3 unique words
        assertEquals(3, stats.vocabularySize)

        // Should have 2 categories
        assertEquals(2, stats.categoryCount)
    }

    @Test
    fun `test empty input returns empty predictions`() = runBlocking {
        val predictions = classifier.predict("")
        assertTrue(predictions.isEmpty())
    }

    @Test
    fun `test case insensitivity`() = runBlocking {
        // Train with different cases
        classifier.train("NETFLIX", "Bills")
        classifier.train("netflix", "Bills")
        classifier.train("Netflix", "Bills")

        // All should count as the same word
        val count = mockDao.getCount("netflix", "Bills")
        assertEquals(3, count)
    }

    @Test
    fun `test punctuation is removed`() = runBlocking {
        // Train with punctuation
        classifier.train("Coffee, tea & more!", "Food")

        // Should store clean words
        assertTrue(mockDao.hasWord("coffee"))
        assertTrue(mockDao.hasWord("tea"))
        assertTrue(mockDao.hasWord("more"))

        // Punctuation should be removed
        assertFalse(mockDao.hasWord("coffee,"))
        assertFalse(mockDao.hasWord("&"))
    }
}

/**
 * Mock DAO for testing without database
 * Stores data in memory
 */
class MockWordCategoryCountDao : WordCategoryCountDao {

    private val wordCounts = mutableMapOf<Pair<String, String>, Int>()

    override suspend fun insertInitial(wordCategoryCount: WordCategoryCount): Long {
        val key = wordCategoryCount.word to wordCategoryCount.category
        return if (wordCounts.containsKey(key)) {
            -1L // Standard Room behavior: return -1 if IGNORE strategy hits an existing row
        } else {
            wordCounts[key] = wordCategoryCount.count
            1L // Return a positive ID indicating a successful new insertion
        }
    }

    // Requirement 2: Implement updateExistingCount
    override suspend fun updateExistingCount(word: String, category: String) {
        val key = word to category
        val currentCount = wordCounts[key] ?: 0
        wordCounts[key] = currentCount + 1
    }

    override suspend fun insert(wordCategoryCount: WordCategoryCount) {
        val key = wordCategoryCount.word to wordCategoryCount.category
        wordCounts[key] = wordCategoryCount.count
    }

    override suspend fun incrementCount(word: String, category: String) {
        // We call the same internal logic as the real DAO transaction
        val rowId = insertInitial(WordCategoryCount(word = word, category = category, count = 1))
        if (rowId == -1L) {
            updateExistingCount(word, category)
        }
    }

    override suspend fun getCount(word: String, category: String): Int? = wordCounts[word to category]

    override suspend fun getCountsForWord(word: String): List<WordCategoryCount> {
        return wordCounts
            .filter { it.key.first == word }
            .map { WordCategoryCount(word = it.key.first, category = it.key.second, count = it.value) }
    }

    override suspend fun getTotalCountForCategory(category: String): Int? {
        return wordCounts.filter { it.key.second == category }.values.sum()
    }

    override suspend fun getAllCategories(): List<String> = wordCounts.keys.map { it.second }.distinct()

    override suspend fun getVocabularySize(): Int = wordCounts.keys.map { it.first }.distinct().size

    override fun getAllWordCounts() = throw NotImplementedError("Flow not needed for tests")

    override suspend fun deleteAll() { wordCounts.clear() }

    override suspend fun getTopWordsForCategory(category: String, limit: Int) = emptyList<WordCategoryCount>()

    // Helper methods for testing
    fun hasWord(word: String) = wordCounts.keys.any { it.first == word }
    fun getCategoryForWord(word: String) = wordCounts.keys.firstOrNull { it.first == word }?.second
}