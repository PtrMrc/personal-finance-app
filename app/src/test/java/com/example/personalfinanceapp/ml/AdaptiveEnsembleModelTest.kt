package com.example.personalfinanceapp.ml

import com.example.personalfinanceapp.data.ModelPerformance
import com.example.personalfinanceapp.data.ModelPerformanceDao
import com.example.personalfinanceapp.data.repository.NaiveBayesSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AdaptiveEnsembleModel weight adjustment logic.
 * Uses in-memory fakes implementing interfaces — no Android context, no Room, no TFLite.
 */
class AdaptiveEnsembleModelTest {

    private lateinit var fakeModelPerformanceDao: FakeModelPerformanceDao
    private lateinit var fakeNaiveBayesSource: FakeNaiveBayesSource
    private lateinit var fakeTfliteClassifier: FakeTFLiteClassifier
    private lateinit var ensemble: AdaptiveEnsembleModel

    @Before
    fun setup() {
        fakeModelPerformanceDao = FakeModelPerformanceDao()
        fakeNaiveBayesSource = FakeNaiveBayesSource()
        fakeTfliteClassifier = FakeTFLiteClassifier()

        ensemble = AdaptiveEnsembleModel(
            tfliteClassifier = fakeTfliteClassifier,
            naiveBayesRepository = fakeNaiveBayesSource,
            modelPerformanceDao = fakeModelPerformanceDao
        )
    }

    // ─── Weight direction tests ───────────────────────────────────────────────

    @Test
    fun `when TFLite correct and NaiveBayes wrong, TFLite weight increases`() = runBlocking {
        val initialWeight = fakeModelPerformanceDao.getPerformance("TFLite")!!.currentWeight

        fakeTfliteClassifier.nextPrediction = "Élelmiszer"
        fakeNaiveBayesSource.nextPrediction = "Szórakozás"

        val prediction = ensemble.predict("kenyér tej")
        ensemble.recordUserChoice("kenyér tej", prediction, "Élelmiszer")

        val newWeight = fakeModelPerformanceDao.getPerformance("TFLite")!!.currentWeight
        assertTrue("TFLite weight should increase when correct (was $initialWeight, now $newWeight)",
            newWeight > initialWeight)
    }

    @Test
    fun `when NaiveBayes correct and TFLite wrong, NaiveBayes weight increases`() = runBlocking {
        val initialWeight = fakeModelPerformanceDao.getPerformance("NaiveBayes")!!.currentWeight

        fakeTfliteClassifier.nextPrediction = "Szórakozás"
        fakeNaiveBayesSource.nextPrediction = "Élelmiszer"

        val prediction = ensemble.predict("kenyér")
        ensemble.recordUserChoice("kenyér", prediction, "Élelmiszer")

        val newWeight = fakeModelPerformanceDao.getPerformance("NaiveBayes")!!.currentWeight
        assertTrue("NaiveBayes weight should increase when correct (was $initialWeight, now $newWeight)",
            newWeight > initialWeight)
    }

    @Test
    fun `when TFLite wrong and NaiveBayes correct, TFLite weight decreases`() = runBlocking {
        val initialWeight = fakeModelPerformanceDao.getPerformance("TFLite")!!.currentWeight

        // NaiveBayes correct, TFLite wrong — this creates asymmetry so weights actually shift
        // Note: when BOTH models are wrong, EMA reduces both proportionally and
        // normalization restores the same ratio — so weights only shift when models disagree
        fakeTfliteClassifier.nextPrediction = "WrongCategory"
        fakeNaiveBayesSource.nextPrediction = "Élelmiszer"

        val prediction = ensemble.predict("valami")
        ensemble.recordUserChoice("valami", prediction, "Élelmiszer")

        val newWeight = fakeModelPerformanceDao.getPerformance("TFLite")!!.currentWeight
        assertTrue("TFLite weight should decrease when wrong (was $initialWeight, now $newWeight)",
            newWeight < initialWeight)
    }

    // ─── Bounds tests ─────────────────────────────────────────────────────────

    @Test
    fun `TFLite weight never exceeds 0_85 even after many correct predictions`() = runBlocking {
        fakeTfliteClassifier.nextPrediction = "Élelmiszer"
        fakeNaiveBayesSource.nextPrediction = "Szórakozás" // always wrong

        repeat(50) {
            val prediction = ensemble.predict("kenyér tej")
            ensemble.recordUserChoice("kenyér tej", prediction, "Élelmiszer")
        }

        val finalWeight = fakeModelPerformanceDao.getPerformance("TFLite")!!.currentWeight
        assertTrue("TFLite weight should never exceed 0.85, was $finalWeight",
            finalWeight <= 0.85)
    }

    @Test
    fun `NaiveBayes weight never drops below 0_15 even after many wrong predictions`() = runBlocking {
        fakeTfliteClassifier.nextPrediction = "Élelmiszer" // always correct
        fakeNaiveBayesSource.nextPrediction = "Szórakozás" // always wrong

        repeat(50) {
            val prediction = ensemble.predict("kenyér")
            ensemble.recordUserChoice("kenyér", prediction, "Élelmiszer")
        }

        val finalWeight = fakeModelPerformanceDao.getPerformance("NaiveBayes")!!.currentWeight
        assertTrue("NaiveBayes weight should never drop below 0.15, was $finalWeight",
            finalWeight >= 0.15)
    }

    @Test
    fun `weights always sum to 1_0 after each adjustment`() = runBlocking {
        fakeTfliteClassifier.nextPrediction = "Élelmiszer"
        fakeNaiveBayesSource.nextPrediction = "Szórakozás"

        repeat(10) { i ->
            val prediction = ensemble.predict("kenyér tej sajt")
            ensemble.recordUserChoice("kenyér tej sajt", prediction, "Élelmiszer")

            val t = fakeModelPerformanceDao.getPerformance("TFLite")!!.currentWeight
            val nb = fakeModelPerformanceDao.getPerformance("NaiveBayes")!!.currentWeight
            assertEquals("Weights should sum to 1.0 at iteration $i, got ${t + nb}",
                1.0, t + nb, 0.001)
        }
    }

    // ─── Performance tracking tests ───────────────────────────────────────────

    @Test
    fun `correct prediction increments both correctCount and totalCount`() = runBlocking {
        fakeTfliteClassifier.nextPrediction = "Élelmiszer"
        fakeNaiveBayesSource.nextPrediction = "Élelmiszer"

        val prediction = ensemble.predict("kenyér")
        ensemble.recordUserChoice("kenyér", prediction, "Élelmiszer")

        val perf = fakeModelPerformanceDao.getPerformance("TFLite")!!
        assertTrue("correctCount should increase", perf.correctCount > 0)
        assertTrue("totalCount should increase", perf.totalCount > 0)
    }

    @Test
    fun `wrong prediction increments only totalCount, not correctCount`() = runBlocking {
        fakeTfliteClassifier.nextPrediction = "WrongCategory"
        fakeNaiveBayesSource.nextPrediction = "WrongCategory"

        val prediction = ensemble.predict("valami")
        ensemble.recordUserChoice("valami", prediction, "Élelmiszer")

        val perf = fakeModelPerformanceDao.getPerformance("TFLite")!!
        assertEquals("correctCount should stay 0", 0, perf.correctCount)
        assertTrue("totalCount should increase", perf.totalCount > 0)
    }
}

// ─── Fakes ────────────────────────────────────────────────────────────────────

/** Implements TFLiteClassifierInterface directly — no Android context needed */
class FakeTFLiteClassifier : TFLiteClassifierInterface {
    var nextPrediction: String = "Egyéb"
    override fun classify(text: String): String = nextPrediction
}

/** Implements NaiveBayesSource directly — no DAO or DB needed */
class FakeNaiveBayesSource : NaiveBayesSource {
    var nextPrediction: String = "Egyéb"

    override suspend fun getTopPrediction(title: String): Result<CategoryPrediction?> =
        Result.success(CategoryPrediction(nextPrediction, 0.8))

    override suspend fun trainModel(title: String, category: String): Result<Unit> =
        Result.success(Unit)
}

/** In-memory fake DAO — starts with TFLite=0.6, NaiveBayes=0.4 */
class FakeModelPerformanceDao : ModelPerformanceDao {

    private val store = mutableMapOf(
        "TFLite" to ModelPerformance("TFLite", 0, 0, 0.6),
        "NaiveBayes" to ModelPerformance("NaiveBayes", 0, 0, 0.4)
    )

    override suspend fun insert(modelPerformance: ModelPerformance) {
        store[modelPerformance.modelName] = modelPerformance
    }
    override suspend fun getPerformance(modelName: String) = store[modelName]
    override suspend fun getAllPerformances() = store.values.toList()
    override fun getAllPerformancesFlow(): Flow<List<ModelPerformance>> = flowOf(store.values.toList())
    override suspend fun incrementCorrect(modelName: String) {
        store[modelName]?.let { store[modelName] = it.copy(correctCount = it.correctCount + 1, totalCount = it.totalCount + 1) }
    }
    override suspend fun incrementTotal(modelName: String) {
        store[modelName]?.let { store[modelName] = it.copy(totalCount = it.totalCount + 1) }
    }
    override suspend fun updateWeight(modelName: String, weight: Double) {
        store[modelName]?.let { store[modelName] = it.copy(currentWeight = weight) }
    }
    override suspend fun getAccuracy(modelName: String): Double? {
        val p = store[modelName] ?: return null
        if (p.totalCount == 0) return null
        return p.correctCount.toDouble() / p.totalCount.toDouble()
    }
    override suspend fun deleteAll() = store.clear()
    override suspend fun initializeDefaultModels() {
        if (store["TFLite"] == null) store["TFLite"] = ModelPerformance("TFLite", 0, 0, 0.6)
        if (store["NaiveBayes"] == null) store["NaiveBayes"] = ModelPerformance("NaiveBayes", 0, 0, 0.4)
    }
}