package com.example.personalfinanceapp.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.AppTheme
import com.example.personalfinanceapp.data.ExportManager
import com.example.personalfinanceapp.data.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val currentTheme by settingsManager.themeFlow.collectAsState(initial = AppTheme.SIMPLE)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Export state
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Unified Custom Header ---
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
                // ── Appearance Section ────────────────────────────────────────
                Text(
                    text = "Megjelenés",
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
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = "Téma",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Téma",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 3-way segmented button: Világos / Sötét / OLED
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = currentTheme == AppTheme.LIGHT,
                                onClick = { scope.launch { settingsManager.setTheme(AppTheme.LIGHT) } },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                icon = {
                                    SegmentedButtonDefaults.ActiveIcon()
                                    if (currentTheme != AppTheme.LIGHT)
                                        Icon(
                                            Icons.Default.LightMode, null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                        )
                                }
                            ) { Text("Világos") }

                            SegmentedButton(
                                selected = currentTheme == AppTheme.SIMPLE,
                                onClick = { scope.launch { settingsManager.setTheme(AppTheme.SIMPLE) } },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                icon = {
                                    SegmentedButtonDefaults.ActiveIcon()
                                    if (currentTheme != AppTheme.SIMPLE)
                                        Icon(
                                            Icons.Default.Nightlight, null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                        )
                                }
                            ) { Text("Simple") }

                            SegmentedButton(
                                selected = currentTheme == AppTheme.OLED,
                                onClick = { scope.launch { settingsManager.setTheme(AppTheme.OLED) } },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                icon = {
                                    SegmentedButtonDefaults.ActiveIcon()
                                    if (currentTheme != AppTheme.OLED)
                                        Icon(
                                            Icons.Default.DarkMode, null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                        )
                                }
                            ) { Text("OLED") }
                        }

                        Text(
                            text = when (currentTheme) {
                                AppTheme.LIGHT  -> "Világos mód — fehér háttér"
                                AppTheme.SIMPLE -> "Simple stílus — sötét teal háttér"
                                AppTheme.OLED   -> "Igazi fekete — OLED akkumulátor kímélés"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Research Export Section ───────────────────────────────────
                Text(
                    text = "Kutatás",
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Export",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Kutatási adatok exportálása",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Modell súlyok, pontosság, javítási arányok és a leggyakoribb szavak" +
                                    " exportálása .txt fájlba.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Error message (shown only when export fails)
                        if (exportError != null) {
                            Text(
                                text = "Hiba: $exportError",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                exportError = null
                                isExporting = true
                                scope.launch {
                                    try {
                                        val db = AppDatabase.getDatabase(context)
                                        ExportManager(context, db).exportAndShare()
                                    } catch (e: Exception) {
                                        exportError = e.localizedMessage ?: "Ismeretlen hiba"
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportálás…")
                            } else {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportálás")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── About Section ─────────────────────────────────────────────
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