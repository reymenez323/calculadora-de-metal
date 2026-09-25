package com.reymildo.calculadoradelmetal.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.MillingOperation
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import com.reymildo.calculadoradelmetal.domain.machining.TurningOperation

/** Iconos lineales dibujados a mano en una cuadrícula de 24×24 (sin depender de material-icons). */
enum class Glyph {
    WORKSHOP, RAW_STOCK, LATHE, MILL, MATERIAL_CUBE,
    TOOL_HSS, TOOL_INSERT, TOOL_ENDMILL,
    OP_TURNING, OP_BORING, OP_FACING, OP_GROOVING, OP_PARTING, OP_THREADING,
    OP_FACE_MILL, OP_CONTOUR, OP_SLOT,
    HELP,
}

fun turningGlyph(op: TurningOperation) = when (op) {
    TurningOperation.TURNING -> Glyph.OP_TURNING
    TurningOperation.BORING -> Glyph.OP_BORING
    TurningOperation.FACING -> Glyph.OP_FACING
    TurningOperation.GROOVING -> Glyph.OP_GROOVING
    TurningOperation.PARTING -> Glyph.OP_PARTING
    TurningOperation.THREADING -> Glyph.OP_THREADING
}

fun millingGlyph(op: MillingOperation) = when (op) {
    MillingOperation.FACE -> Glyph.OP_FACE_MILL
    MillingOperation.CONTOUR -> Glyph.OP_CONTOUR
    MillingOperation.SLOT -> Glyph.OP_SLOT
}

fun machineGlyph(type: MachineType) = if (type == MachineType.LATHE) Glyph.LATHE else Glyph.MILL

fun toolGlyph(kind: ToolKind) = when (kind) {
    ToolKind.HSS -> Glyph.TOOL_HSS
    ToolKind.CARBIDE_INSERT -> Glyph.TOOL_INSERT
    ToolKind.CARBIDE_ENDMILL -> Glyph.TOOL_ENDMILL
}

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Canvas(modifier = modifier.size(size)) { drawGlyph(glyph, color) }
}

private fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val u = size.minDimension / 24f
    val w = 1.7f * u
    val line = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val soft = color.copy(alpha = 0.16f)

    fun poly(vararg p: Float, close: Boolean = true, fill: Boolean = false) {
        val path = Path().apply {
            moveTo(p[0] * u, p[1] * u)
            for (i in 2 until p.size step 2) lineTo(p[i] * u, p[i + 1] * u)
            if (close) close()
        }
        if (fill) drawPath(path, soft)
        drawPath(path, color, style = line)
    }

    fun seg(x1: Float, y1: Float, x2: Float, y2: Float, dashed: Boolean = false) {
        drawLine(
            color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = if (dashed) w * .7f else w,
            cap = StrokeCap.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(2.2f * u, 2f * u)) else null,
        )
    }

    fun rect(x: Float, y: Float, x2: Float, y2: Float, r: Float = 0.8f, fill: Boolean = false) {
        if (fill) drawRoundRect(soft, Offset(x * u, y * u), Size((x2 - x) * u, (y2 - y) * u), CornerRadius(r * u))
        drawRoundRect(color, Offset(x * u, y * u), Size((x2 - x) * u, (y2 - y) * u), CornerRadius(r * u), style = line)
    }

    fun circle(cx: Float, cy: Float, r: Float) = drawCircle(color, r * u, Offset(cx * u, cy * u), style = line)

    /** Herramienta de torno: cuña que apunta hacia (tx, ty) desde arriba. */
    fun tipDown(tx: Float, ty: Float) = poly(tx - 2.6f, ty - 6f, tx + 2.6f, ty - 6f, tx + 2.6f, ty - 3f, tx, ty, fill = true)

    when (glyph) {
        Glyph.WORKSHOP -> {
            rect(3f, 9f, 21f, 20f, 2f)
            poly(9f, 9f, 9f, 6f, 15f, 6f, 15f, 9f, close = false)
            seg(3f, 14f, 21f, 14f)
            rect(10.5f, 12f, 13.5f, 16f, 0.8f, fill = true)
        }
        Glyph.RAW_STOCK -> {
            poly(3f, 7.5f, 12f, 3f, 21f, 7.5f, 12f, 12f)
            poly(3f, 12.5f, 12f, 17f, 21f, 12.5f, close = false)
            poly(3f, 17f, 12f, 21.5f, 21f, 17f, close = false)
        }
        Glyph.LATHE -> {
            rect(2f, 7f, 6.5f, 17f, 1f, fill = true)
            rect(6.5f, 9.5f, 17f, 14.5f, 0.5f)
            poly(17f, 10.5f, 20f, 12f, 17f, 13.5f)
            rect(20f, 8f, 22.5f, 16f, 1f, fill = true)
            seg(2f, 20.5f, 22.5f, 20.5f)
            tipDown(11.5f, 9.5f)
        }
        Glyph.MILL -> {
            rect(8.5f, 2f, 15.5f, 7f, 1f, fill = true)
            rect(10.5f, 7f, 13.5f, 13.5f, 0.5f)
            seg(10.5f, 9f, 13.5f, 8f); seg(10.5f, 12f, 13.5f, 11f)
            rect(4f, 13.5f, 20f, 18f, 0.8f, fill = true)
            seg(2f, 20.5f, 22f, 20.5f)
        }
        Glyph.MATERIAL_CUBE -> {
            poly(12f, 3f, 20f, 7.5f, 20f, 16.5f, 12f, 21f, 4f, 16.5f, 4f, 7.5f)
            poly(4f, 7.5f, 12f, 12f, 20f, 7.5f, close = false)
            seg(12f, 12f, 12f, 21f)
        }
        Glyph.TOOL_HSS -> {
            poly(2.5f, 8.5f, 15.5f, 8.5f, 21.5f, 15.5f, 2.5f, 15.5f, fill = true)
            seg(6f, 12f, 12f, 12f)
        }
        Glyph.TOOL_INSERT -> {
            poly(12f, 2.5f, 21f, 12f, 12f, 21.5f, 3f, 12f, fill = true)
            circle(12f, 12f, 2.6f)
            poly(12f, 6f, 17.5f, 12f, 12f, 18f, 6.5f, 12f)
        }
        Glyph.TOOL_ENDMILL -> {
            rect(9.5f, 1.5f, 14.5f, 9f, 0.6f)
            poly(8.5f, 9f, 15.5f, 9f, 15.5f, 19f, 12f, 21.5f, 8.5f, 19f, fill = true)
            seg(8.5f, 12.5f, 15.5f, 10.5f); seg(8.5f, 16.5f, 15.5f, 14.5f)
        }
        Glyph.OP_TURNING -> {
            poly(2f, 8f, 9.5f, 8f, 9.5f, 10.5f, 22f, 10.5f, 22f, 15f, 9.5f, 15f, 9.5f, 17f, 2f, 17f, fill = true)
            tipDown(17f, 10.5f)
            seg(15f, 20.5f, 21f, 20.5f)
        }
        Glyph.OP_BORING -> {
            rect(3f, 4.5f, 21f, 9f, 0.6f, fill = true)
            rect(3f, 15f, 21f, 19.5f, 0.6f, fill = true)
            rect(9f, 11f, 22.5f, 13f, 0.6f)
            poly(9f, 11f, 9f, 9f, 11.5f, 11f)
            seg(3f, 12f, 7f, 12f, dashed = true)
        }
        Glyph.OP_FACING -> {
            rect(2f, 7.5f, 15.5f, 16.5f, 0.6f, fill = true)
            poly(15.5f, 12f, 18.5f, 8.5f, 22f, 8.5f, 22f, 15.5f, 18.5f, 15.5f, fill = true)
            seg(20f, 2.5f, 20f, 6f); poly(18.5f, 4.5f, 20f, 6.5f, 21.5f, 4.5f, close = false)
        }
        Glyph.OP_GROOVING -> {
            poly(2f, 8f, 9.5f, 8f, 9.5f, 12.5f, 14.5f, 12.5f, 14.5f, 8f, 22f, 8f, 22f, 17f, 2f, 17f, fill = true)
            rect(10.6f, 2.5f, 13.4f, 10f, 0.4f)
        }
        Glyph.OP_PARTING -> {
            poly(2f, 8f, 11f, 8f, 11f, 12.5f, 13f, 12.5f, 13f, 8f, 22f, 8f, 22f, 17f, 2f, 17f, fill = true)
            seg(2f, 12.5f, 22f, 12.5f, dashed = true)
            rect(11.3f, 2.5f, 12.7f, 10.5f, 0.3f)
        }
        Glyph.OP_THREADING -> {
            poly(2f, 17f, 2f, 10f, 4.5f, 8f, 7f, 10f, 9.5f, 8f, 12f, 10f, 14.5f, 8f, 17f, 10f, 19.5f, 8f, 22f, 10f, 22f, 17f, fill = true)
            poly(10f, 1.5f, 14f, 1.5f, 12f, 6f)
        }
        Glyph.OP_FACE_MILL -> {
            rect(10.5f, 1.5f, 13.5f, 4.5f, 0.5f)
            rect(4f, 4.5f, 20f, 9f, 0.8f, fill = true)
            rect(4f, 9f, 6.5f, 11.5f, 0.3f); rect(10.75f, 9f, 13.25f, 11.5f, 0.3f); rect(17.5f, 9f, 20f, 11.5f, 0.3f)
            rect(2f, 13.5f, 22f, 19f, 0.8f, fill = true)
        }
        Glyph.OP_CONTOUR -> {
            poly(2f, 20f, 2f, 13.5f, 11f, 13.5f, 11f, 17f, 22f, 17f, 22f, 20f, fill = true)
            rect(11f, 2f, 15f, 11f, 0.6f)
            seg(11f, 5f, 15f, 4f); seg(11f, 8.5f, 15f, 7.5f)
            poly(11f, 11f, 15f, 11f, 15f, 13.5f, 11f, 13.5f, close = false)
        }
        Glyph.OP_SLOT -> {
            rect(2f, 4f, 22f, 20f, 1.5f, fill = true)
            rect(5f, 9.5f, 19f, 14.5f, 2.5f)
            circle(15.5f, 12f, 2f)
        }
        Glyph.HELP -> {
            circle(12f, 12f, 9f)
            seg(12f, 16.5f, 12f, 16.6f)
            poly(9.6f, 9.6f, 10.5f, 8f, 13.5f, 8f, 14.6f, 9.8f, 12f, 12.4f, 12f, 13.6f, close = false)
        }
    }
}
