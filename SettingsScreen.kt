package com.foss.madhu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foss.madhu.data.model.EqPreset
import com.foss.madhu.data.model.StreamQuality
import com.foss.madhu.ui.viewmodel.PlayerViewModel

// ──────────────────────────────────────────────────────────────────────────────
// SettingsScreen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val streamQuality    by viewModel.streamQuality.collectAsStateWithLifecycle()
    val eqPreset         by viewModel.eqPreset.collectAsStateWithLifecycle()
    val customBands      by viewModel.customEqBands.collectAsStateWithLifecycle()
    val crossfadeMs      by viewModel.crossfadeDurationMs.collectAsStateWithLifecycle()
    val replayGain       by viewModel.replayGainFactor.collectAsStateWithLifecycle()
    val isAmoled         by viewModel.isAmoledTheme.collectAsStateWithLifecycle()
    val isDynamic        by viewModel.isDynamicColor.collectAsStateWithLifecycle()
    val scrobbling       by viewModel.scrobblingEnabled.collectAsStateWithLifecycle()
    val lfmUsername      by viewModel.lastFmUsername.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── AUDIO QUALITY ─────────────────────────────────────────────
            item {
                SettingsSection(title = "Audio Quality", icon = Icons.Outlined.GraphicEq) {
                    Text(
                        text  = "Controls the streaming bitrate. Higher quality uses more data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    StreamQuality.values().forEach { quality ->
                        QualityRadioItem(
                            quality      = quality,
                            isSelected   = streamQuality == quality,
                            onSelected   = { viewModel.setStreamQuality(quality) }
                        )
                    }
                }
            }

            // ── EQUALIZER ─────────────────────────────────────────────────
            item {
                SettingsSection(title = "Equalizer", icon = Icons.Outlined.Tune) {
                    EqPresetGrid(
                        activePreset    = eqPreset,
                        onPresetSelected = { viewModel.applyEqPreset(it) }
                    )
                    AnimatedVisibility(visible = eqPreset == EqPreset.CUSTOM) {
                        CustomEqSliders(
                            bands   = customBands,
                            onBandChanged = { band, gainMb -> viewModel.setCustomBand(band, gainMb) }
                        )
                    }
                }
            }

            // ── PLAYBACK BEHAVIOUR ────────────────────────────────────────
            item {
                SettingsSection(title = "Playback", icon = Icons.Outlined.PlayCircleOutline) {
                    // Crossfade slider
                    SettingsSliderRow(
                        label       = "Crossfade",
                        description = "Smooth fade between tracks. Set to 0 to disable.",
                        value       = crossfadeMs.toFloat(),
                        valueRange  = 0f..12_000f,
                        steps       = 11,
                        displayText = if (crossfadeMs == 0) "Off" else "${crossfadeMs / 1000} s",
                        onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color.White.copy(0.06f))

                    // ReplayGain multiplier slider
                    SettingsSliderRow(
                        label       = "Loudness Normalisation",
                        description = "Applies a gain multiplier to reduce clipping and normalise " +
                                "perceived loudness across tracks. 1.0 = unity (0 dB).",
                        value       = replayGain,
                        valueRange  = 0.3f..1.0f,
                        steps       = 13,
                        displayText = "%.2f×".format(replayGain),
                        onValueChange = { viewModel.setReplayGainFactor(it) }
                    )
                }
            }

            // ── APPEARANCE ────────────────────────────────────────────────
            item {
                SettingsSection(title = "Appearance", icon = Icons.Outlined.Palette) {
                    SettingsToggleRow(
                        label       = "AMOLED Pure Black",
                        description = "Forces the background to absolute #000000. " +
                                "Saves significant battery on OLED panels.",
                        icon        = Icons.Outlined.DarkMode,
                        checked     = isAmoled,
                        onChecked   = { viewModel.setAmoledTheme(it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color.White.copy(0.06f))
                    SettingsToggleRow(
                        label       = "Dynamic Album Colours",
                        description = "Extracts accent colours from the current album art " +
                                "to tint the Now Playing interface.",
                        icon        = Icons.Outlined.ColorLens,
                        checked     = isDynamic,
                        onChecked   = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            // ── LAST.FM SCROBBLING ────────────────────────────────────────
            item {
                LastFmSection(
                    isScrobblingEnabled = scrobbling,
                    connectedUsername   = lfmUsername,
                    onToggleScrobbling  = { viewModel.setScrobblingEnabled(it) },
                    onSaveCredentials   = { key, secret ->
                        viewModel.saveLastFmCredentials(key, secret)
                    },
                    onAuthenticate      = { user, pass ->
                        viewModel.authenticateLastFm(user, pass)
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Audio Quality radio item
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun QualityRadioItem(
    quality: StreamQuality,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelected)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = isSelected,
            onClick  = onSelected,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text       = quality.displayName,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (isSelected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = quality.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// EQ preset grid
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun EqPresetGrid(
    activePreset: EqPreset,
    onPresetSelected: (EqPreset) -> Unit
) {
    val presets = EqPreset.values()
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        presets.forEach { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPresetSelected(preset) }
                    .background(
                        if (preset == activePreset)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = preset == activePreset,
                    onClick  = { onPresetSelected(preset) },
                    colors   = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text       = preset.displayName,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (preset == activePreset) FontWeight.Medium else FontWeight.Normal,
                        color      = if (preset == activePreset) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurface
                    )
                    if (preset != EqPreset.OFF && preset != EqPreset.CUSTOM) {
                        Text(
                            text  = preset.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Custom EQ sliders (5 bands)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomEqSliders(
    bands: ShortArray,
    onBandChanged: (Int, Short) -> Unit
) {
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text  = "Custom EQ — drag sliders (±15 dB)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        bandLabels.forEachIndexed { idx, label ->
            val currentGain = bands.getOrElse(idx) { 0 }.toFloat()
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = label,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(58.dp)
                )
                Slider(
                    value         = currentGain,
                    onValueChange = { v -> onBandChanged(idx, v.toInt().toShort()) },
                    valueRange    = -1500f..1500f,
                    steps         = 29,   // 100 mB increments
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(
                        thumbColor       = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text     = "%+.0f".format(currentGain / 100f) + " dB",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Last.fm section with collapsible credential inputs
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LastFmSection(
    isScrobblingEnabled: Boolean,
    connectedUsername: String,
    onToggleScrobbling: (Boolean) -> Unit,
    onSaveCredentials: (apiKey: String, apiSecret: String) -> Unit,
    onAuthenticate: (username: String, password: String) -> Unit
) {
    var expanded        by remember { mutableStateOf(false) }
    var apiKey          by remember { mutableStateOf("") }
    var apiSecret       by remember { mutableStateOf("") }
    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }

    SettingsSection(title = "Last.fm Scrobbling", icon = Icons.Outlined.Radio) {
        // Scrobble toggle
        SettingsToggleRow(
            label       = "Enable Scrobbling",
            description = if (connectedUsername.isNotEmpty())
                "Scrobbling as @$connectedUsername"
            else
                "Sign in below to track and scrobble plays to Last.fm.",
            icon        = Icons.Outlined.Radio,
            checked     = isScrobblingEnabled,
            onChecked   = onToggleScrobbling
        )

        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color.White.copy(0.06f))

        // Expand/collapse credential fields
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = if (connectedUsername.isNotEmpty()) "Change Account" else "Sign In to Last.fm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector        = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text  = "Register at last.fm/api to obtain an API key. " +
                            "Madhu never stores your password — only the session key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value         = apiKey,
                    onValueChange = { apiKey = it },
                    label         = { Text("Last.fm API Key") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = apiSecret,
                    onValueChange = { apiSecret = it },
                    label         = { Text("Last.fm Shared Secret") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it },
                    label         = { Text("Username") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text("Password") },
                    singleLine    = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                                              else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onSaveCredentials(apiKey, apiSecret)
                        onAuthenticate(username, password)
                        password  = ""  // Clear from memory immediately
                        expanded  = false
                    },
                    enabled  = apiKey.isNotBlank() && apiSecret.isNotBlank() &&
                               username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to Last.fm")
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Reusable composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section header
        Row(
            modifier          = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = androidx.compose.ui.unit.TextUnit(
                        1.5f, androidx.compose.ui.unit.TextUnitType.Sp
                    )
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            shape  = RoundedCornerShape(16.dp),
            color  = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = SwitchDefaults.colors(
                checkedThumbColor      = MaterialTheme.colorScheme.primary,
                checkedTrackColor      = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = displayText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text  = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = valueRange,
            steps         = steps,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor       = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
