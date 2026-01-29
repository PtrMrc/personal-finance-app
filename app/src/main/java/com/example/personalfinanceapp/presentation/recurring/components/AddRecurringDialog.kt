package com.example.personalfinanceapp.presentation.recurring.components

import com.example.personalfinanceapp.utils.Validation
import com.example.personalfinanceapp.utils.ValidationResult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.data.Frequency
import com.example.personalfinanceapp.utils.mapFrequency


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Boolean, Frequency, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf("1") }

    var freqExpanded by remember { mutableStateOf(false) }
    var selectedFreq by remember { mutableStateOf(Frequency.MONTHLY) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var dayError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új Ismétlődő Tétel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    label = { Text("Megnevezés") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        amountError = null
                    },
                    label = { Text("Összeg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } }
                )

                OutlinedTextField(
                    value = day,
                    onValueChange = {
                        if (it.length <= 2) {
                            day = it
                            dayError = null
                        }
                    },
                    label = { Text("Nap (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = dayError != null,
                    supportingText = dayError?.let { { Text(it) } }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isIncome, onCheckedChange = { isIncome = it })
                    Text("Bevétel")
                }

                Box {
                    OutlinedTextField(
                        value = mapFrequency(selectedFreq),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gyakoriság") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { freqExpanded = true }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                        Frequency.entries.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(mapFrequency(f)) },
                                onClick = { selectedFreq = f; freqExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val titleValidation = Validation.validateTitle(title)
                val amountValidation = Validation.validateAmount(amount)
                val dayValidation = Validation.validateDay(day)

                if (titleValidation is ValidationResult.Error) {
                    titleError = titleValidation.message
                }
                if (amountValidation is ValidationResult.Error) {
                    amountError = amountValidation.message
                }
                if (dayValidation is ValidationResult.Error) {
                    dayError = dayValidation.message
                }

                if (titleValidation is ValidationResult.Success && amountValidation is ValidationResult.Success && dayValidation is ValidationResult.Success) {
                    val amt = amount.toDouble()
                    val dayInt = day.toInt()
                    onConfirm(title, amt, isIncome, selectedFreq, dayInt)
                }
            }) { Text("Mentés") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse") } }
    )
}