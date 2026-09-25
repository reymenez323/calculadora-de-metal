@file:OptIn(ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.BuildConfig
import com.reymildo.calculadoradelmetal.data.settings.AppLanguage
import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.Stepper
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onLanguageChange: (AppLanguage) -> Unit,
    onDefaultUnitChange: (LengthUnit) -> Unit,
    onDecimalsChange: (Int) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onExportBackup: suspend () -> Result<String>,
    onImportBackup: suspend (String) -> Result<Unit>,
    onShowTutorial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var unitMenu by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportSuccess = stringResource(R.string.set_backup_exported)
    val importSuccess = stringResource(R.string.set_backup_imported)
    val backupFailure = stringResource(R.string.set_backup_failed)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            onExportBackup().onSuccess { raw ->
                runCatching { requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(raw) } }
                    .onSuccess { backupMessage = exportSuccess }
                    .onFailure { backupMessage = backupFailure }
            }.onFailure { backupMessage = backupFailure }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader().use { it.readText() } }
                .onSuccess { raw -> pendingImport = raw }
                .onFailure { backupMessage = backupFailure }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingRow(title = stringResource(R.string.set_language)) {
                    SingleChoiceSegmentedButtonRow {
                        AppLanguage.entries.forEachIndexed { index, language ->
                            SegmentedButton(
                                selected = settings.language == language,
                                onClick = { onLanguageChange(language) },
                                shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size),
                                label = {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.ES -> stringResource(R.string.set_language_es)
                                            AppLanguage.EN -> stringResource(R.string.set_language_en)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.set_default_unit),
                    subtitle = stringResource(R.string.set_default_unit_hint),
                ) {
                    Column {
                        OutlinedButton(
                            onClick = { unitMenu = true },
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = settings.defaultLengthUnit.symbol,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                            LengthUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.symbol) },
                                    onClick = {
                                        onDefaultUnitChange(unit)
                                        unitMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(title = stringResource(R.string.set_decimals)) {
                    Stepper(
                        value = settings.decimalPrecision.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { onDecimalsChange(it.coerceIn(0, 4)) }
                        },
                        onDecrement = { onDecimalsChange((settings.decimalPrecision - 1).coerceAtLeast(0)) },
                        onIncrement = { onDecimalsChange((settings.decimalPrecision + 1).coerceAtMost(4)) },
                        editable = false,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(title = stringResource(R.string.set_currency)) {
                    OutlinedTextField(
                        value = settings.currencySymbol,
                        onValueChange = { onCurrencyChange(it.take(4)) },
                        modifier = Modifier.width(96.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = NumberFamily,
                            textAlign = TextAlign.Center,
                        ),
                        shape = RoundedCornerShape(11.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
            }
        }

        SectionCard(title = stringResource(R.string.set_tutorial_title)) {
            Text(stringResource(R.string.set_tutorial_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onShowTutorial, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.set_tutorial_cta))
            }
        }

        SectionCard(title = stringResource(R.string.set_backup_title)) {
            Text(stringResource(R.string.set_backup_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportLauncher.launch("calculadora-metal-respaldo.json") }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.set_backup_export))
                }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.set_backup_import))
                }
            }
            backupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }

        SectionCard {
            Text(
                text = stringResource(R.string.set_reset_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.set_reset_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { resetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.set_reset_cta), style = MaterialTheme.typography.labelLarge)
            }
        }

        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.set_about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.set_about_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text(stringResource(R.string.set_reset_dialog_title)) },
            text = { Text(stringResource(R.string.set_reset_dialog_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        resetDialog = false
                    },
                ) { Text(stringResource(R.string.set_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { resetDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    pendingImport?.let { raw ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.set_backup_import_title)) },
            text = { Text(stringResource(R.string.set_backup_import_body)) },
            confirmButton = {
                Button(onClick = {
                    pendingImport = null
                    scope.launch {
                        onImportBackup(raw)
                            .onSuccess { backupMessage = importSuccess }
                            .onFailure { backupMessage = it.message ?: backupFailure }
                    }
                }) { Text(stringResource(R.string.set_backup_import_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}
