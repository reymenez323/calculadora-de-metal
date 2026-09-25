package com.reymildo.calculadoradelmetal.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.ui.common.Glyph
import com.reymildo.calculadoradelmetal.ui.common.GlyphIcon

/** A dónde debe llevar la app (detrás del cuadro) cada paso del tutorial. */
enum class TutorialTarget { NONE, STOCK, MACHINES, MACHINING, CALC_STOCK, CALC_TURNING, SETTINGS }

data class TutorialStep(
    val glyph: Glyph,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val target: TutorialTarget,
)

val tutorialSteps = listOf(
    TutorialStep(Glyph.HELP, R.string.tut_welcome_title, R.string.tut_welcome_body, TutorialTarget.NONE),
    TutorialStep(Glyph.RAW_STOCK, R.string.tut_stock_title, R.string.tut_stock_body, TutorialTarget.STOCK),
    TutorialStep(Glyph.LATHE, R.string.tut_machines_title, R.string.tut_machines_body, TutorialTarget.MACHINES),
    TutorialStep(Glyph.TOOL_INSERT, R.string.tut_machining_title, R.string.tut_machining_body, TutorialTarget.MACHINING),
    TutorialStep(Glyph.MATERIAL_CUBE, R.string.tut_calc_stock_title, R.string.tut_calc_stock_body, TutorialTarget.CALC_STOCK),
    TutorialStep(Glyph.OP_TURNING, R.string.tut_calc_machining_title, R.string.tut_calc_machining_body, TutorialTarget.CALC_TURNING),
    TutorialStep(Glyph.HELP, R.string.tut_settings_title, R.string.tut_settings_body, TutorialTarget.SETTINGS),
)

/**
 * Guía de primer uso sobre la app real. "Omitir" la cierra solo esta vez; "No volver a mostrar"
 * la desactiva de forma permanente (se puede repetir desde Configuración).
 */
@Composable
fun TutorialOverlay(
    step: Int,
    onStepChange: (Int) -> Unit,
    onSkip: () -> Unit,
    onNeverShow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = tutorialSteps[step.coerceIn(0, tutorialSteps.lastIndex)]
    val last = step >= tutorialSteps.lastIndex
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = {}),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlyphIcon(current.glyph, MaterialTheme.colorScheme.primary, size = 44.dp)
                Text(stringResource(current.title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    stringResource(current.body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    tutorialSteps.indices.forEach { index ->
                        Box(
                            Modifier
                                .size(if (index == step) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.tut_skip)) }
                    Box(Modifier.weight(1f))
                    if (step > 0) {
                        TextButton(onClick = { onStepChange(step - 1) }) { Text(stringResource(R.string.tut_back)) }
                    }
                    Button(onClick = { if (last) onNeverShow() else onStepChange(step + 1) }) {
                        Text(stringResource(if (last) R.string.tut_finish else R.string.tut_next))
                    }
                }
                if (!last) {
                    TextButton(onClick = onNeverShow) {
                        Text(stringResource(R.string.tut_never), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
