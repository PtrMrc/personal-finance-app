package com.example.personalfinanceapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.data.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val currentTheme by settingsManager.themeFlow.collectAsState(initial = com.example.personalfinanceapp.data.AppTheme.SIMPLE)
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Unified Custom Header (Matches Home/Stats/Recurring) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 0.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Vissza",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Beállítások",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // --- Scrollable Content ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance Section
                Text(
                    text = "Megjelenés",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Theme selector card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = "Téma",
                                tint = MaterialTheme.colorScheme.primary)
                            Text("Téma", style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                        }

                        // 3-way segmented button: Világos / Sötét / OLED
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = currentTheme == com.example.personalfinanceapp.data.AppTheme.LIGHT,
                                onClick = { scope.launch { settingsManager.setTheme(com.example.personalfinanceapp.data.AppTheme.LIGHT) } },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                icon = {
                                    SegmentedButtonDefaults.ActiveIcon()
                                    if (currentTheme != com.example.personalfinanceapp.data.AppTheme.LIGHT)
                                        Icon(Icons.Default.LightMode, null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
                                }
                            ) { Text("Világos") }

                            SegmentedButton(
                                selected = currentTheme == com.example.personalfinanceapp.data.AppTheme.SIMPLE,
                                onClick = { scope.launch { settingsManager.setTheme(com.example.personalfinanceapp.data.AppTheme.SIMPLE) } },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                icon = {
                                    SegmentedButtonDefaults.ActiveIcon()
                                    if (currentTheme != com.example.personalfinanceapp.data.AppTheme.SIMPLE)
                                        Icon(Icons.Default.Nightlight, null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
                                }
                            ) { Text("Simple") }

                            SegmentedButton(
                                selected = currentTheme == com.example.personalfinanceapp.data.AppTheme.OLED,
                                onClick = { scope.launch { settingsManager.setTheme(com.example.personalfinanceapp.data.AppTheme.OLED) } },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                icon = {
                                    SegmentedButtonDefaults.ActiveIcon()
                                    if (currentTheme != com.example.personalfinanceapp.data.AppTheme.OLED)
                                        Icon(Icons.Default.DarkMode, null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
                                }
                            ) { Text("OLED") }
                        }

                        // Description of the currently selected theme
                        Text(
                            text = when (currentTheme) {
                                com.example.personalfinanceapp.data.AppTheme.LIGHT -> "Világos mód — fehér háttér"
                                com.example.personalfinanceapp.data.AppTheme.SIMPLE  -> "Simple stílus — sötét teal háttér"
                                com.example.personalfinanceapp.data.AppTheme.OLED  -> "Igazi fekete — OLED akkumulátor kímélés"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About Section
                Text(
                    text = "Információ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Pénzügyi Nyomkövető",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Verzió: 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        Text(
                            text = "Adaptív gépi tanulással működő személyes pénzügy kezelő alkalmazás.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Az AI két modellt kombinál:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "• TFLite: Általános tudás\n• Naive Bayes: Személyes szokásaid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Minél többet használod, annál pontosabb lesz! 🚀",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}