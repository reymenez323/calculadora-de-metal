package com.reymildo.calculadoradelmetal.ui.workshop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.ui.common.Glyph
import com.reymildo.calculadoradelmetal.ui.common.GlyphIcon

/** Las tres secciones de la pestaña Taller. */
enum class WorkshopSection(val glyph: Glyph) {
    STOCK(Glyph.RAW_STOCK),
    MACHINES(Glyph.LATHE),
    MACHINING(Glyph.TOOL_INSERT),
}

@Composable
private fun sectionLabel(section: WorkshopSection): String = stringResource(
    when (section) {
        WorkshopSection.STOCK -> R.string.workshop_stock
        WorkshopSection.MACHINES -> R.string.workshop_machines
        WorkshopSection.MACHINING -> R.string.workshop_machining
    },
)

/** Selector de sección con iconos y, debajo, el contenido de la sección elegida. */
@Composable
fun WorkshopHost(
    section: WorkshopSection,
    onSectionChange: (WorkshopSection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (WorkshopSection, Modifier) -> Unit,
) {
    val labels = WorkshopSection.entries.associateWith { sectionLabel(it) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorkshopSection.entries.forEach { candidate ->
                val selected = candidate == section
                Card(
                    onClick = { onSectionChange(candidate) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        GlyphIcon(
                            candidate.glyph,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 26.dp,
                        )
                        Text(
                            labels.getValue(candidate),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        content(section, Modifier.weight(1f))
    }
}
