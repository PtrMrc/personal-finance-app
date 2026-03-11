package com.example.personalfinanceapp.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.personalfinanceapp.ml.EnsemblePrediction
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import com.example.personalfinanceapp.utils.Validation
import com.example.personalfinanceapp.utils.ValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    initialTitle: String,
    initialAmount: String,
    initialCategory: String,
    initialDescription: String,
    isEditing: Boolean = false,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var amount by remember { mutableStateOf(initialAmount) }
    var category by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf(initialDescription) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Élelmiszer", "Utazás", "Szórakozás", "Számlák", "Egészség", "Bevétel", "Egyéb")

    var currentPrediction by remember { mutableStateOf<EnsemblePrediction?>(null) }
    // Tracks the last category the AI set, so we can tell if the user manually overrode it
    var lastAiSuggestedCategory by remember { mutableStateOf<String?>(null) }
    var showLearning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Trigger AI prediction after the user pauses typing (debounce 500ms)
    LaunchedEffect(title) {
        if (title.length >= 3) {
            delay(500)
            val prediction = viewModel.predictCategoryEnsemble(title)
            currentPrediction = prediction

            // Only follow the new prediction if the user hasn't manually
            // changed the category away from the AI's previous suggestion.
            val userHasOverridden = lastAiSuggestedCategory != null
                    && category != lastAiSuggestedCategory
            if (!userHasOverridden) {
                category = prediction.finalCategory
                lastAiSuggestedCategory = prediction.finalCategory
            }
        } else {
            currentPrediction = null
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .padding(28.dp)
            .imePadding()
            .clip(RoundedCornerShape(28.dp)),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isEditing) "Tétel szerkesztése" else "Új Tétel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = null
                        },
                        label = { Text("Megnevezés") },
                        singleLine = true,
                        isError = titleError != null,
                        supportingText = titleError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            amountError = null
                        },
                        label = { Text("Összeg (Ft)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = amountError != null,
                        supportingText = amountError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // AI prediction card — only shown when a prediction is available.
                    // Uses the shared AIPredictionCard from AIPredictionComponents.kt.
                    AnimatedVisibility(
                        visible = currentPrediction != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        currentPrediction?.let { prediction ->
                            AIPredictionCard(prediction = prediction)
                        }
                    }

                    Box {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            label = { Text("Kategória") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Kategória választó",
                                    modifier = Modifier.clickable { expanded = true }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        // If the user picks a different category than the AI
                                        // suggested, record it so the model can learn.
                                        if (currentPrediction != null && cat != category) {
                                            viewModel.recordCategoryChoice(
                                                title = title,
                                                ensemblePrediction = currentPrediction!!,
                                                userChoice = cat
                                            )
                                            scope.launch {
                                                showLearning = true
                                                delay(2500)
                                                showLearning = false
                                            }
                                        }
                                        category = cat
                                        // Mark that the user has manually chosen — AI won't
                                        // override the dropdown on future predictions anymore.
                                        lastAiSuggestedCategory = null
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // "AI is learning" feedback — uses shared LearningIndicator from
                    // AIPredictionComponents.kt.
                    LearningIndicator(show = showLearning)

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Leírás (Opcionális)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mégse", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                val titleValidation = Validation.validateTitle(title)
                                val amountValidation = Validation.validateAmount(amount)

                                if (titleValidation is ValidationResult.Error) titleError = titleValidation.message
                                if (amountValidation is ValidationResult.Error) amountError = amountValidation.message

                                if (titleValidation is ValidationResult.Success && amountValidation is ValidationResult.Success) {
                                    val amt = amount.toDouble()

                                    // Always record the final choice on save, so the model
                                    // learns even if the user didn't change the dropdown.
                                    if (currentPrediction != null) {
                                        viewModel.recordCategoryChoice(
                                            title = title,
                                            ensemblePrediction = currentPrediction!!,
                                            userChoice = category
                                        )
                                    }

                                    onConfirm(title, amt, category, description)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mentés", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}