package com.example.wikipedia_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wikipedia_app.navigation.Screen
import com.example.wikipedia_app.ui.theme.CreamOffWhite
import com.example.wikipedia_app.ui.theme.TealCyan
import com.example.wikipedia_app.ui.viewmodels.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    currentTheme: String,
    onThemeChanged: (String) -> Unit,
    textSize: String,
    onTextSizeChanged: (String) -> Unit,
    speechRate: Float,
    onSpeechRateChanged: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChanged: (Float) -> Unit,
    settingsViewModel: SettingsViewModel
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val themeOptions = listOf("System Default", "Light", "Dark")
    val textSizeOptions = listOf("Small", "Normal", "Large", "Huge")
    val cacheSize by settingsViewModel.cacheSize.collectAsState()

    // Slider local state (commit to VM only on finger-up)
    var rateSlider by remember(speechRate) { mutableStateOf(speechRate) }
    var pitchSlider by remember(speechPitch) { mutableStateOf(speechPitch) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = CreamOffWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CreamOffWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealCyan)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // ── Appearance ──────────────────────────────────────────────────
            SectionHeader("Appearance")

            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = {
                    Text(currentTheme, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null, tint = TealCyan) },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable { showThemeDialog = true }
            )
            RowDivider()

            ListItem(
                headlineContent = { Text("Text Size") },
                supportingContent = {
                    Text(textSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingContent = { Icon(Icons.Default.FormatSize, contentDescription = null, tint = TealCyan) },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable { showTextSizeDialog = true }
            )
            RowDivider()

            // ── Text-to-Speech ───────────────────────────────────────────────
            SectionHeader("Text-to-Speech")

            ListItem(
                headlineContent = { Text("Speech Rate") },
                supportingContent = {
                    Column {
                        Text(
                            "%.1f×".format(rateSlider),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = rateSlider,
                            onValueChange = { rateSlider = it },
                            onValueChangeFinished = { onSpeechRateChanged(rateSlider) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.padding(top = 4.dp),
                            colors = SliderDefaults.colors(thumbColor = TealCyan, activeTrackColor = TealCyan)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0.5×", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("2.0×", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                leadingContent = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = TealCyan) }
            )
            RowDivider()

            ListItem(
                headlineContent = { Text("Speech Pitch") },
                supportingContent = {
                    Column {
                        Text(
                            "%.1f".format(pitchSlider),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = pitchSlider,
                            onValueChange = { pitchSlider = it },
                            onValueChangeFinished = { onSpeechPitchChanged(pitchSlider) },
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.padding(top = 4.dp),
                            colors = SliderDefaults.colors(thumbColor = TealCyan, activeTrackColor = TealCyan)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Low", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("High", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                leadingContent = {
                    Spacer(Modifier.size(24.dp)) // align with other rows
                }
            )
            RowDivider()

            // ── Storage ──────────────────────────────────────────────────────
            SectionHeader("Storage")

            ListItem(
                headlineContent = { Text("Game Cache") },
                supportingContent = {
                    Text(
                        if (cacheSize == 0) "Empty" else "$cacheSize cached articles",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null, tint = TealCyan) },
                trailingContent = {
                    if (cacheSize > 0) {
                        TextButton(
                            onClick = { showClearCacheDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Clear") }
                    }
                }
            )
            RowDivider()

            // ── General ──────────────────────────────────────────────────────
            SectionHeader("General")

            ListItem(
                headlineContent = { Text("Language") },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null, tint = TealCyan) },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable { navController.navigate(Screen.LanguageSelection.route) }
            )
            RowDivider()
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showThemeDialog) {
        OptionPickerDialog(
            title = "Choose Theme",
            options = themeOptions,
            current = currentTheme,
            onSelect = { onThemeChanged(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showTextSizeDialog) {
        OptionPickerDialog(
            title = "Text Size",
            options = textSizeOptions,
            current = textSize,
            onSelect = { onTextSizeChanged(it); showTextSizeDialog = false },
            onDismiss = { showTextSizeDialog = false }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Remove all $cacheSize cached game articles?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.clearCache()
                        showClearCacheDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TealCyan
        ),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun ChevronIcon() {
    Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    )
}

@Composable
private fun RowDivider() {
    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
}

@Composable
private fun OptionPickerDialog(
    title: String,
    options: List<String>,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == current,
                            onClick = { onSelect(option) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
