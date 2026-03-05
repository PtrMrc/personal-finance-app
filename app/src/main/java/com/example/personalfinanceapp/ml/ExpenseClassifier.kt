package com.example.personalfinanceapp.ml

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

class ExpenseClassifier(private val context: Context) : TFLiteClassifierInterface {

    private var interpreter: Interpreter? = null
    private var vocab: Map<String, Int> = emptyMap()

    private val maxLength = 20
    // Confidence threshold: If model is less than 60% sure, we return null
    private val confidenceThreshold = 0.60f

    // TFLite model outputs ENGLISH categories (from training)
    private val englishCategories = listOf(
        "Food", "Transport", "Entertainment", "Bills", "Health", "Income"
    )

    // Map English → Hungarian
    private val categoryMapping = mapOf(
        "Food" to "Élelmiszer",
        "Transport" to "Utazás",
        "Entertainment" to "Szórakozás",
        "Bills" to "Számlák",
        "Health" to "Egészség",
        "Income" to "Bevétel"
    )

    init {
        try {
            loadModel()
            loadVocab()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModel() {
        val fileDescriptor = context.assets.openFd("expense_classifier.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val modelBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadVocab() {
        val jsonString = context.assets.open("vocab.json")
            .bufferedReader(Charset.forName("UTF-8"))
            .use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val map = mutableMapOf<String, Int>()

        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jsonObject.getInt(key)
        }
        vocab = map
    }

    override fun classify(text: String): String? {
        if (interpreter == null) return null

        val tokens = tokenize(text)

        val inputBuffer = Array(1) { FloatArray(maxLength) }
        for (i in tokens.indices) {
            if (i < maxLength) {
                inputBuffer[0][i] = tokens[i].toFloat()
            }
        }

        val outputBuffer = Array(1) { FloatArray(englishCategories.size) }
        interpreter?.run(inputBuffer, outputBuffer)

        val probabilities = outputBuffer[0]
        val maxScore = probabilities.maxOrNull() ?: 0f
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

        if (maxScore < confidenceThreshold) return null

        val englishCategory = englishCategories[maxIndex]
        return categoryMapping[englishCategory] ?: englishCategory
    }

    private fun tokenize(text: String): IntArray {
        val words = text.lowercase().split(" ", "\n", ".", ",")
        val tokenList = mutableListOf<Int>()

        for (word in words) {
            if (word.isBlank()) continue
            val id = vocab[word] ?: 1
            tokenList.add(id)
        }

        while (tokenList.size < maxLength) {
            tokenList.add(0)
        }
        return tokenList.toIntArray()
    }
}