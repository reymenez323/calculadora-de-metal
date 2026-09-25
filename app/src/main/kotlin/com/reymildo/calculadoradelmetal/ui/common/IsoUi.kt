package com.reymildo.calculadoradelmetal.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.domain.machining.IsoGroup

/** Colores normalizados del código ISO 513 que usan los fabricantes de herramientas. */
fun isoColor(group: IsoGroup): Color = when (group) {
    IsoGroup.P -> Color(0xFF1E6FD9)
    IsoGroup.M -> Color(0xFFF2C21B)
    IsoGroup.K -> Color(0xFFD93A2E)
    IsoGroup.N -> Color(0xFF2E9E4F)
    IsoGroup.S -> Color(0xFFE8801A)
    IsoGroup.H -> Color(0xFF7A7F85)
}

private fun isoLetterColor(group: IsoGroup): Color =
    if (group == IsoGroup.M || group == IsoGroup.S) Color(0xFF241A00) else Color.White

@Composable
fun isoName(group: IsoGroup): String = stringResource(
    when (group) {
        IsoGroup.P -> R.string.iso_p_name
        IsoGroup.M -> R.string.iso_m_name
        IsoGroup.K -> R.string.iso_k_name
        IsoGroup.N -> R.string.iso_n_name
        IsoGroup.S -> R.string.iso_s_name
        IsoGroup.H -> R.string.iso_h_name
    },
)

@Composable
fun isoDescription(group: IsoGroup): String = stringResource(
    when (group) {
        IsoGroup.P -> R.string.iso_p_desc
        IsoGroup.M -> R.string.iso_m_desc
        IsoGroup.K -> R.string.iso_k_desc
        IsoGroup.N -> R.string.iso_n_desc
        IsoGroup.S -> R.string.iso_s_desc
        IsoGroup.H -> R.string.iso_h_desc
    },
)

/** Cuadrito de color con la letra del grupo ISO. */
@Composable
fun IsoBadge(
    group: IsoGroup,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    dimmed: Boolean = false,
    label: String = group.name,
) {
    Box(
        modifier = modifier
            .height(size)
            .widthIn(min = size)
            .alpha(if (dimmed) 0.3f else 1f)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(isoColor(group))
            .padding(horizontal = if (label.length > 1) size * 0.14f else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = isoLetterColor(group),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * (if (label.length > 1) 0.46f else 0.55f)).sp,
        )
    }
}

/**
 * Selector del grupo ISO: los seis cuadritos de color en fila. El elegido lleva borde; los que ya
 * están en uso en otra fila salen atenuados y no se pueden tomar. Debajo, el nombre del grupo y los
 * materiales que cubre.
 */
@Composable
fun IsoGroupPicker(
    selected: IsoGroup,
    taken: Set<IsoGroup>,
    onSelect: (IsoGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IsoGroup.entries.forEach { group ->
                val isSelected = group == selected
                val blocked = group in taken
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.onSurface) else Modifier)
                        .clickable(enabled = !isSelected && !blocked) { onSelect(group) }
                        .padding(if (isSelected) 3.dp else 0.dp),
                ) {
                    IsoBadge(group, size = 34.dp, dimmed = !isSelected && blocked)
                }
            }
        }
        Text(
            "${selected.name} · " + isoName(selected),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            isoDescription(selected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
