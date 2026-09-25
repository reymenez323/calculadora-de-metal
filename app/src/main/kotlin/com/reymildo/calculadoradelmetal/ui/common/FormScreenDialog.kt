package com.reymildo.calculadoradelmetal.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reymildo.calculadoradelmetal.R

/**
 * Pantalla completa para AÑADIR o EDITAR algo. A diferencia de una hoja inferior no se cierra al
 * deslizar (era fácil perder lo escrito sin querer): se cierra con la ✕ de arriba o con "atrás".
 * Si ya hay cambios sin guardar ([dirty]), antes de cerrar pide confirmación.
 */
@Composable
fun FormScreenDialog(
    title: String,
    dirty: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestClose = { if (dirty) confirmDiscard = true else onClose() }

    Dialog(
        onDismissRequest = requestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = requestClose) {
                        Text("✕", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) { content() }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.form_discard_title)) },
            text = { Text(stringResource(R.string.form_discard_body)) },
            confirmButton = {
                Button(onClick = { confirmDiscard = false; onClose() }) { Text(stringResource(R.string.form_discard_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.form_discard_keep)) } },
        )
    }
}
