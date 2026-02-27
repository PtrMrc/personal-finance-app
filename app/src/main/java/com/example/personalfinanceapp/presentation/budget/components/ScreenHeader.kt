package com.example.personalfinanceapp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Unified screen header that consumes the status bar inset internally.
 *
 * Usage:
 *   ScreenHeader(title = "Áttekintés", subtitle = "Pénzügyi aktivitásod")
 *
 * With trailing action icons (e.g. HomeScreen):
 *   ScreenHeader(title = "Áttekintés", subtitle = "Pénzügyi aktivitásod") {
 *       IconButton(...) { ... }
 *   }
 *
 * Rules for every screen that uses this:
 *   - Remove any windowInsetsPadding(WindowInsets.statusBars) from the root container.
 *   - Remove any extra top padding (e.g. padding(top = 8.dp) or padding(top = 24.dp)) from
 *     the root container. Regular content padding (16.dp) below the header is fine.
 *   - Scaffold screens: keep padding(paddingValues) on the Column but remove the topBar
 *     inset workarounds — Scaffold with no topBar gives top=0 anyway, so this composable
 *     is the single source of truth for the top gap.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)   // ← single place
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 8.dp)
    ) {
        if (trailingContent != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                TitleBlock(title, subtitle, modifier = Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.offset(x = 4.dp)   // compensates IconButton internal padding
                ) {
                    trailingContent()
                }
            }
        } else {
            TitleBlock(title, subtitle)
        }
    }
}

@Composable
private fun TitleBlock(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}