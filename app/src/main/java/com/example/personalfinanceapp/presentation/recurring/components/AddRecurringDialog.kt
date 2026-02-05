package com.example.personalfinanceapp.presentation.recurring.components

import androidx.compose.foundation.background
import com.example.personalfinanceapp.utils.Validation
import com.example.personalfinanceapp.utils.ValidationResult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
    var selectedFreq by remember { mutableStateOf(Frequency.MONTHLY) }
    var freqExpanded by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var dayError by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .padding(28.dp)
            .clip(RoundedCornerShape(28.dp)),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Új állandó tétel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; titleError = null },
                        label = { Text("Megnevezés (pl. Netflix)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = titleError != null,
                        supportingText = titleError?.let { { Text(it) } },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Amount Input
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it; amountError = null },
                            label = { Text("Összeg") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = amountError != null,
                            shape = RoundedCornerShape(12.dp)
                        )
                        // Day Input
                        OutlinedTextField(
                            value = day,
                            onValueChange = { if (it.length <= 2) day = it; dayError = null },
                            label = { Text("Nap (1-31)") },
                            modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = dayError != null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Frequency Dropdown
                    Box {
                        OutlinedTextField(
                            value = mapFrequency(selectedFreq),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gyakoriság") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, null,
                                    Modifier.clickable { freqExpanded = true })
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = freqExpanded,
                            onDismissRequest = { freqExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            Frequency.entries.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(mapFrequency(f)) },
                                    onClick = { selectedFreq = f; freqExpanded = false }
                                )
                            }
                        }
                    }

                    // Income Toggle Card
                    Surface(
                        color = if (isIncome) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isIncome = !isIncome }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isIncome,
                                onCheckedChange = { isIncome = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                            )
                            Text(
                                text = "Ez egy rendszeres bevétel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isIncome) Color(0xFF059669) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mégse", color = Color(0xFF64748B))
                        }
                        Button(
                            onClick = {
                                val tVal = Validation.validateTitle(title)
                                val aVal = Validation.validateAmount(amount)
                                val dVal = Validation.validateDay(day)

                                if (tVal is ValidationResult.Error) titleError = tVal.message
                                if (aVal is ValidationResult.Error) amountError = aVal.message
                                if (dVal is ValidationResult.Error) dayError = dVal.message

                                if (tVal is ValidationResult.Success && aVal is ValidationResult.Success && dVal is ValidationResult.Success) {
                                    onConfirm(title, amount.toDouble(), isIncome, selectedFreq, day.toInt())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mentés", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        })
}