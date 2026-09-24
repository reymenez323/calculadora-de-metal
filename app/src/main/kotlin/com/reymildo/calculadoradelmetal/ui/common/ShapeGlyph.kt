package com.reymildo.calculadoradelmetal.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.Shape
import kotlin.math.min

@StringRes
fun shapeNameRes(shape: Shape): Int = when (shape) {
    Shape.RoundBar -> R.string.shape_round_bar
    Shape.SquareBar -> R.string.shape_square_bar
    Shape.RectangularBar -> R.string.shape_rectangular_bar
    Shape.HexBar -> R.string.shape_hex_bar
    Shape.Plate -> R.string.shape_plate
    Shape.RoundTube -> R.string.shape_round_tube
    Shape.RectangularTube -> R.string.shape_rectangular_tube
    Shape.Angle -> R.string.shape_angle
    Shape.Channel -> R.string.shape_channel
    Shape.IBeam -> R.string.shape_i_beam
    Shape.TBar -> R.string.shape_t_bar
}

@StringRes
fun dimensionNameRes(type: DimensionType): Int = when (type) {
    DimensionType.DIAMETER -> R.string.dim_diameter
    DimensionType.SIDE -> R.string.dim_side
    DimensionType.WIDTH -> R.string.dim_width
    DimensionType.HEIGHT -> R.string.dim_height
    DimensionType.THICKNESS -> R.string.dim_thickness
    DimensionType.LENGTH -> R.string.dim_length
    DimensionType.WALL_THICKNESS -> R.string.dim_wall_thickness
    DimensionType.ACROSS_FLATS -> R.string.dim_across_flats
    DimensionType.WEB_THICKNESS -> R.string.dim_web_thickness
    DimensionType.FLANGE_THICKNESS -> R.string.dim_flange_thickness
}

private const val ISO_COS30 = 0.8660254f
private const val ISO_SIN30 = 0.5f

private data class UnitBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Caja del perfil de cada forma en el espacio de unidades 0..32 que usan los trazos de abajo. */
private fun shapeBoundingBox(shape: Shape): UnitBox = when (shape) {
    Shape.RoundBar, Shape.RoundTube -> UnitBox(4f, 4f, 28f, 28f)
    Shape.SquareBar -> UnitBox(5f, 5f, 27f, 27f)
    Shape.RectangularBar -> UnitBox(4f, 10f, 28f, 22f)
    Shape.HexBar -> UnitBox(5.2f, 3.5f, 26.8f, 28.5f)
    Shape.Plate -> UnitBox(3f, 13f, 29f, 19f)
    Shape.RectangularTube -> UnitBox(4f, 8f, 28f, 24f)
    Shape.Angle -> UnitBox(7f, 4f, 28f, 28f)
    Shape.Channel -> UnitBox(6f, 4f, 26f, 28f)
    Shape.IBeam -> UnitBox(6f, 4f, 26f, 28f)
    Shape.TBar -> UnitBox(4f, 4f, 28f, 28f)
}

/** Perfil (cara frontal) de cada forma, en unidades 0..32. El mismo trazo sirve para el ícono chico y el diagrama grande. */
private fun DrawScope.drawShapeFront(
    shape: Shape,
    color: Color,
    strokeWidthUnits: Float,
    toPx: (Float, Float) -> Offset,
    unitToPx: Float,
) {
    val stroke = Stroke(width = strokeWidthUnits * unitToPx)

    fun polygon(vararg pts: Float): Path {
        val path = Path()
        val start = toPx(pts[0], pts[1])
        path.moveTo(start.x, start.y)
        var i = 2
        while (i < pts.size) {
            val p = toPx(pts[i], pts[i + 1])
            path.lineTo(p.x, p.y)
            i += 2
        }
        path.close()
        return path
    }

    fun rect(x: Float, y: Float, w: Float, h: Float) {
        drawRect(color = color, topLeft = toPx(x, y), size = Size(w * unitToPx, h * unitToPx), style = stroke)
    }

    when (shape) {
        Shape.RoundBar -> drawCircle(color, radius = 12f * unitToPx, center = toPx(16f, 16f), style = stroke)

        Shape.SquareBar -> rect(5f, 5f, 22f, 22f)

        Shape.RectangularBar -> rect(4f, 10f, 24f, 12f)

        Shape.HexBar -> drawPath(
            polygon(16f, 3.5f, 26.8f, 9.75f, 26.8f, 22.25f, 16f, 28.5f, 5.2f, 22.25f, 5.2f, 9.75f),
            color,
            style = stroke,
        )

        Shape.Plate -> rect(3f, 13f, 26f, 6f)

        Shape.RoundTube -> {
            drawCircle(color, radius = 12f * unitToPx, center = toPx(16f, 16f), style = stroke)
            drawCircle(color, radius = 7f * unitToPx, center = toPx(16f, 16f), style = stroke)
        }

        Shape.RectangularTube -> {
            rect(4f, 8f, 24f, 16f)
            rect(8.5f, 12.5f, 15f, 7f)
        }

        Shape.Angle -> drawPath(
            polygon(7f, 4f, 12f, 4f, 12f, 23f, 28f, 23f, 28f, 28f, 7f, 28f),
            color,
            style = stroke,
        )

        Shape.Channel -> drawPath(
            polygon(6f, 4f, 26f, 4f, 26f, 9f, 11f, 9f, 11f, 23f, 26f, 23f, 26f, 28f, 6f, 28f),
            color,
            style = stroke,
        )

        Shape.IBeam -> drawPath(
            polygon(
                6f, 4f, 26f, 4f, 26f, 9f, 18.5f, 9f, 18.5f, 23f, 26f, 23f,
                26f, 28f, 6f, 28f, 6f, 23f, 13.5f, 23f, 13.5f, 9f, 6f, 9f,
            ),
            color,
            style = stroke,
        )

        Shape.TBar -> drawPath(
            polygon(4f, 4f, 28f, 4f, 28f, 9f, 18.5f, 9f, 18.5f, 28f, 13.5f, 28f, 13.5f, 9f, 4f, 9f),
            color,
            style = stroke,
        )
    }
}

/** Techo y costado semitransparentes detrás del perfil, para leer la forma como un tramo extruido. */
private fun DrawScope.drawIsoDepthFaces(bbox: UnitBox, color: Color, toPx: (Float, Float) -> Offset, depthPx: Offset) {
    val tl = toPx(bbox.left, bbox.top)
    val tr = toPx(bbox.right, bbox.top)
    val br = toPx(bbox.right, bbox.bottom)
    val tlBack = tl + depthPx
    val trBack = tr + depthPx
    val brBack = br + depthPx

    val top = Path().apply {
        moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(trBack.x, trBack.y); lineTo(tlBack.x, tlBack.y); close()
    }
    drawPath(top, color.copy(alpha = 0.20f))

    val side = Path().apply {
        moveTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(brBack.x, brBack.y); lineTo(trBack.x, trBack.y); close()
    }
    drawPath(side, color.copy(alpha = 0.11f))

    drawLine(color.copy(alpha = 0.3f), tr, trBack, strokeWidth = 1f)
    drawLine(color.copy(alpha = 0.3f), br, brBack, strokeWidth = 1f)
}

/**
 * Ícono isométrico del perfil de cada forma: cara frontal (el mismo trazo de siempre) más un
 * techo y un costado que sugieren el tramo extruido hacia atrás. Sin assets, todo vectorial.
 */
@Composable
fun ShapeGlyph(
    shape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.7f,
) {
    Canvas(modifier = modifier) {
        val virtualW = 38f
        val virtualH = 36f
        val u = min(size.width / virtualW, size.height / virtualH)
        val ox = (size.width - virtualW * u) / 2f
        val oy = (size.height - virtualH * u) / 2f + 3f * u
        fun toPx(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
        val depthPx = Offset(6f * u * ISO_COS30, -6f * u * ISO_SIN30)

        drawIsoDepthFaces(shapeBoundingBox(shape), color, ::toPx, depthPx)
        drawShapeFront(shape, color, strokeWidth, ::toPx, u)
    }
}

private enum class LabelSlot { BOTTOM, LEFT, DEPTH, DETAIL }

/** Qué rol visual (borde inferior, borde izquierdo, profundidad o detalle) ilustra cada dimensión. */
private fun labelSlots(shape: Shape): List<Pair<DimensionType, LabelSlot>> = when (shape) {
    Shape.RoundBar -> listOf(DimensionType.DIAMETER to LabelSlot.BOTTOM, DimensionType.LENGTH to LabelSlot.DEPTH)
    Shape.SquareBar -> listOf(DimensionType.SIDE to LabelSlot.BOTTOM, DimensionType.LENGTH to LabelSlot.DEPTH)
    Shape.RectangularBar -> listOf(
        DimensionType.WIDTH to LabelSlot.BOTTOM,
        DimensionType.HEIGHT to LabelSlot.LEFT,
        DimensionType.LENGTH to LabelSlot.DEPTH,
    )
    Shape.HexBar -> listOf(DimensionType.ACROSS_FLATS to LabelSlot.BOTTOM, DimensionType.LENGTH to LabelSlot.DEPTH)
    Shape.Plate -> listOf(
        DimensionType.WIDTH to LabelSlot.BOTTOM,
        DimensionType.LENGTH to LabelSlot.DEPTH,
        DimensionType.THICKNESS to LabelSlot.DETAIL,
    )
    Shape.RoundTube -> listOf(
        DimensionType.DIAMETER to LabelSlot.BOTTOM,
        DimensionType.WALL_THICKNESS to LabelSlot.DETAIL,
        DimensionType.LENGTH to LabelSlot.DEPTH,
    )
    Shape.RectangularTube -> listOf(
        DimensionType.WIDTH to LabelSlot.BOTTOM,
        DimensionType.HEIGHT to LabelSlot.LEFT,
        DimensionType.WALL_THICKNESS to LabelSlot.DETAIL,
        DimensionType.LENGTH to LabelSlot.DEPTH,
    )
    Shape.Angle, Shape.Channel -> listOf(
        DimensionType.WIDTH to LabelSlot.BOTTOM,
        DimensionType.HEIGHT to LabelSlot.LEFT,
        DimensionType.THICKNESS to LabelSlot.DETAIL,
        DimensionType.LENGTH to LabelSlot.DEPTH,
    )
    Shape.IBeam, Shape.TBar -> listOf(
        DimensionType.HEIGHT to LabelSlot.LEFT,
        DimensionType.WIDTH to LabelSlot.BOTTOM,
        DimensionType.WEB_THICKNESS to LabelSlot.DETAIL,
        DimensionType.FLANGE_THICKNESS to LabelSlot.DETAIL,
        DimensionType.LENGTH to LabelSlot.DEPTH,
    )
}

private fun LabelSlot.glyph(): String = when (this) {
    LabelSlot.BOTTOM -> "↔"
    LabelSlot.LEFT -> "↕"
    LabelSlot.DEPTH -> "↗"
    LabelSlot.DETAIL -> "▪"
}

/**
 * Diagrama de referencia: el mismo perfil isométrico, más grande, con una leyenda debajo que
 * dice a qué dimensión corresponde cada campo del formulario (↔ ancho/lado/diámetro, ↕ alto,
 * ↗ largo — el eje que se extruye — y ▪ para grosores). Pensado para mostrarse una vez, al
 * entrar a llenar las medidas de una forma.
 */
@Composable
fun ShapeIsoDiagram(shape: Shape, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(modifier = Modifier.size(width = 168.dp, height = 132.dp)) {
            val virtualW = 38f
            val virtualH = 36f
            val u = min(size.width / virtualW, size.height / virtualH)
            val ox = (size.width - virtualW * u) / 2f
            val oy = (size.height - virtualH * u) / 2f + 3f * u
            fun toPx(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
            val depthPx = Offset(6f * u * ISO_COS30, -6f * u * ISO_SIN30)

            drawIsoDepthFaces(shapeBoundingBox(shape), color, ::toPx, depthPx)
            drawShapeFront(shape, color, 1.7f, ::toPx, u)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            labelSlots(shape).forEach { (type, slot) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = slot.glyph(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(dimensionNameRes(type)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
