package com.reymildo.calculadoradelmetal.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.Shape
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
private val DEPTH_UNITS = Offset(6f * ISO_COS30, -6f * ISO_SIN30)

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

private fun polygonOf(vararg pts: Float): List<Offset> {
    val list = ArrayList<Offset>(pts.size / 2)
    var i = 0
    while (i < pts.size) {
        list.add(Offset(pts[i], pts[i + 1]))
        i += 2
    }
    return list
}

/** Aproxima un círculo con un polígono cerrado, recorrido en sentido horario (igual que los demás contornos). */
private fun circleOutline(cx: Float, cy: Float, r: Float, segments: Int = 48): List<Offset> =
    (0 until segments).map { i ->
        val theta = (2.0 * Math.PI * i / segments)
        Offset(cx + r * cos(theta).toFloat(), cy + r * sin(theta).toFloat())
    }

/** Contorno (frente) de cada forma en unidades 0..32, como polígono cerrado (los redondos, aproximados). */
private fun shapeOutline(shape: Shape): List<Offset> = when (shape) {
    Shape.SquareBar -> polygonOf(5f, 5f, 27f, 5f, 27f, 27f, 5f, 27f)
    Shape.RectangularBar -> polygonOf(4f, 10f, 28f, 10f, 28f, 22f, 4f, 22f)
    Shape.HexBar -> polygonOf(16f, 3.5f, 26.8f, 9.75f, 26.8f, 22.25f, 16f, 28.5f, 5.2f, 22.25f, 5.2f, 9.75f)
    Shape.Plate -> polygonOf(3f, 13f, 29f, 13f, 29f, 19f, 3f, 19f)
    Shape.Angle -> polygonOf(7f, 4f, 12f, 4f, 12f, 23f, 28f, 23f, 28f, 28f, 7f, 28f)
    Shape.Channel -> polygonOf(6f, 4f, 26f, 4f, 26f, 9f, 11f, 9f, 11f, 23f, 26f, 23f, 26f, 28f, 6f, 28f)
    Shape.IBeam -> polygonOf(
        6f, 4f, 26f, 4f, 26f, 9f, 18.5f, 9f, 18.5f, 23f, 26f, 23f,
        26f, 28f, 6f, 28f, 6f, 23f, 13.5f, 23f, 13.5f, 9f, 6f, 9f,
    )
    Shape.TBar -> polygonOf(4f, 4f, 28f, 4f, 28f, 9f, 18.5f, 9f, 18.5f, 28f, 13.5f, 28f, 13.5f, 9f, 4f, 9f)
    Shape.RoundBar -> circleOutline(16f, 16f, 12f)
    Shape.RoundTube -> circleOutline(16f, 16f, 12f)
    Shape.RectangularTube -> polygonOf(4f, 8f, 28f, 8f, 28f, 24f, 4f, 24f)
}

/** Contorno del hueco (para tubos), en unidades 0..32; null si la forma es maciza. */
private fun holeOutline(shape: Shape): List<Offset>? = when (shape) {
    Shape.RoundTube -> circleOutline(16f, 16f, 7f)
    Shape.RectangularTube -> polygonOf(8.5f, 12.5f, 23.5f, 12.5f, 23.5f, 19.5f, 8.5f, 19.5f)
    else -> null
}

private fun Path.addPolygon(points: List<Offset>, toPx: (Float, Float) -> Offset) {
    val start = toPx(points[0].x, points[0].y)
    moveTo(start.x, start.y)
    for (i in 1 until points.size) {
        val p = toPx(points[i].x, points[i].y)
        lineTo(p.x, p.y)
    }
    close()
}

/** Cara frontal rellena de cada forma (con hueco en los tubos), en unidades 0..32. */
private fun frontPath(shape: Shape, toPx: (Float, Float) -> Offset): Path {
    val path = Path()
    path.addPolygon(shapeOutline(shape), toPx)
    holeOutline(shape)?.let { hole ->
        val holePath = Path().apply { addPolygon(hole, toPx) }
        path.fillType = PathFillType.EvenOdd
        path.addPath(holePath)
    }
    return path
}

/** Vector normal saliente (perpendicular, hacia afuera) del segmento a→b de un polígono horario. */
private fun outwardNormal(a: Offset, b: Offset): Offset {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = sqrt(dx * dx + dy * dy).takeIf { it > 1e-4f } ?: 1f
    return Offset(dy / len, -dx / len)
}

/**
 * Dibuja el cuerpo extruido de una forma: por cada arista visible del contorno (normal saliente
 * hacia arriba o hacia la derecha) traza la franja que la lleva hasta la copia desplazada por
 * `depthPx`, con un degradado que va oscureciendo hacia el fondo — así una silueta cóncava (I,
 * canal, ángulo) o redonda (barra, tubo) se sombrea de verdad en vez de usar la caja del contorno.
 */
private fun DrawScope.drawExtrudedBody(
    outline: List<Offset>,
    toPx: (Float, Float) -> Offset,
    depthPx: Offset,
    gradTop: Brush,
    gradSide: Brush,
    outlineColor: Color,
    strokeWidthPx: Float,
) {
    val n = outline.size
    val topQuads = ArrayList<Path>()
    val sideQuads = ArrayList<Path>()
    for (i in 0 until n) {
        val a = outline[i]
        val b = outline[(i + 1) % n]
        val normal = outwardNormal(a, b)
        val topWeight = -normal.y
        val sideWeight = normal.x
        if (topWeight <= 0.02f && sideWeight <= 0.02f) continue
        val pa = toPx(a.x, a.y)
        val pb = toPx(b.x, b.y)
        val quad = Path().apply {
            moveTo(pa.x, pa.y)
            lineTo(pb.x, pb.y)
            lineTo(pb.x + depthPx.x, pb.y + depthPx.y)
            lineTo(pa.x + depthPx.x, pa.y + depthPx.y)
            close()
        }
        if (topWeight >= sideWeight) topQuads.add(quad) else sideQuads.add(quad)
    }
    sideQuads.forEach { quad ->
        drawPath(quad, gradSide)
        drawPath(quad, outlineColor, style = Stroke(width = strokeWidthPx))
    }
    topQuads.forEach { quad ->
        drawPath(quad, gradTop)
        drawPath(quad, outlineColor, style = Stroke(width = strokeWidthPx))
    }
}

/**
 * Dibuja el ícono isométrico completo de una forma: cuerpo extruido con sombreado real por arista
 * (degradado hacia el fondo), hueco oscuro en los tubos, y la cara frontal encima en el color de
 * acento — como un tramo de material cortado, no como un contorno plano.
 */
private fun DrawScope.drawIsoShape(
    shape: Shape,
    color: Color,
    toPx: (Float, Float) -> Offset,
    depthPx: Offset,
    strokeWidthPx: Float,
) {
    val topColor = lerp(color, Color.White, 0.5f)
    val sideColor = lerp(color, Color.Black, 0.42f)
    val outlineColor = lerp(color, Color.Black, 0.55f)
    val farColor = lerp(color, Color.Black, 0.86f)

    val bbox = shapeBoundingBox(shape)
    val nearAnchor = toPx((bbox.left + bbox.right) / 2f, (bbox.top + bbox.bottom) / 2f)
    val farAnchor = nearAnchor + depthPx
    val gradTop = Brush.linearGradient(listOf(topColor, farColor), start = nearAnchor, end = farAnchor)
    val gradSide = Brush.linearGradient(listOf(sideColor, farColor), start = nearAnchor, end = farAnchor)

    drawExtrudedBody(shapeOutline(shape), toPx, depthPx, gradTop, gradSide, outlineColor, strokeWidthPx)

    holeOutline(shape)?.let { hole ->
        val holePath = Path().apply { addPolygon(hole, toPx) }
        val holeBrush = Brush.linearGradient(listOf(outlineColor, Color.Black), start = nearAnchor, end = farAnchor)
        drawPath(holePath, holeBrush)
    }

    val front = frontPath(shape, toPx)
    drawPath(front, color)
    drawPath(front, outlineColor, style = Stroke(width = strokeWidthPx))
}

/**
 * Ícono isométrico del perfil de cada forma: cuerpo extruido con sombreado por arista y cara
 * frontal en el color de acento, para que se lea como un tramo cortado de verdad. Sin assets,
 * todo vectorial.
 */
@Composable
fun ShapeGlyph(
    shape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.4f,
) {
    Canvas(modifier = modifier) {
        val virtualW = 38f
        val virtualH = 36f
        val u = min(size.width / virtualW, size.height / virtualH)
        val ox = (size.width - virtualW * u) / 2f
        val oy = (size.height - virtualH * u) / 2f + 3f * u
        fun toPx(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
        val depthPx = Offset(DEPTH_UNITS.x * u, DEPTH_UNITS.y * u)

        drawIsoShape(shape, color, ::toPx, depthPx, strokeWidth * u)
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

private data class Tick(val type: DimensionType, val from: Offset, val to: Offset, val labelAt: Offset, val align: TextAlign)

/** Dónde va el tiquete de cada medida "de detalle" (grosores), a mano por forma: bbox no alcanza para ubicarlos. */
private fun detailTicks(shape: Shape): List<Tick> = when (shape) {
    Shape.Plate -> listOf(Tick(DimensionType.THICKNESS, Offset(3f, 13f), Offset(3f, 19f), Offset(1.2f, 16f), TextAlign.End))
    Shape.RoundTube -> listOf(
        Tick(DimensionType.WALL_THICKNESS, Offset(20.95f, 11.05f), Offset(24.49f, 7.51f), Offset(28.5f, 4.2f), TextAlign.Start),
    )
    Shape.RectangularTube -> listOf(
        Tick(DimensionType.WALL_THICKNESS, Offset(4f, 16f), Offset(8.5f, 16f), Offset(2f, 12.5f), TextAlign.Start),
    )
    Shape.Angle -> listOf(Tick(DimensionType.THICKNESS, Offset(20f, 23f), Offset(20f, 28f), Offset(22f, 25.5f), TextAlign.Start))
    Shape.Channel -> listOf(Tick(DimensionType.THICKNESS, Offset(16f, 4f), Offset(16f, 9f), Offset(18f, 2.5f), TextAlign.Start))
    Shape.IBeam -> listOf(
        Tick(DimensionType.WEB_THICKNESS, Offset(13.5f, 16f), Offset(18.5f, 16f), Offset(20.2f, 16f), TextAlign.Start),
        Tick(DimensionType.FLANGE_THICKNESS, Offset(22f, 4f), Offset(22f, 9f), Offset(28.5f, 8.5f), TextAlign.Start),
    )
    Shape.TBar -> listOf(
        Tick(DimensionType.WEB_THICKNESS, Offset(13.5f, 18f), Offset(18.5f, 18f), Offset(20.2f, 18f), TextAlign.Start),
        Tick(DimensionType.FLANGE_THICKNESS, Offset(24f, 4f), Offset(24f, 9f), Offset(28.5f, 8.5f), TextAlign.Start),
    )
    else -> emptyList()
}

private data class Callout(val type: DimensionType, val from: Offset, val to: Offset, val labelAt: Offset, val align: TextAlign, val isSpan: Boolean)

private fun calloutsFor(shape: Shape): List<Callout> {
    val bbox = shapeBoundingBox(shape)
    val details = detailTicks(shape).associateBy { it.type }
    return labelSlots(shape).map { (type, slot) ->
        when (slot) {
            LabelSlot.BOTTOM -> Callout(
                type,
                Offset(bbox.left, bbox.bottom + 1.5f),
                Offset(bbox.right, bbox.bottom + 1.5f),
                Offset((bbox.left + bbox.right) / 2f, bbox.bottom + 3.2f),
                TextAlign.Center,
                isSpan = true,
            )
            LabelSlot.LEFT -> Callout(
                type,
                Offset(bbox.left - 1.5f, bbox.top),
                Offset(bbox.left - 1.5f, bbox.bottom),
                Offset(bbox.left - 2.2f, (bbox.top + bbox.bottom) / 2f),
                TextAlign.End,
                isSpan = true,
            )
            LabelSlot.DEPTH -> {
                val p1 = Offset(bbox.right, bbox.top)
                val p2 = p1 + DEPTH_UNITS
                Callout(type, p1, p2, p2 + Offset(1.3f, -0.4f), TextAlign.Start, isSpan = true)
            }
            LabelSlot.DETAIL -> {
                val tick = details.getValue(type)
                Callout(type, tick.from, tick.to, tick.labelAt, tick.align, isSpan = false)
            }
        }
    }
}

/**
 * Diagrama de referencia: el mismo perfil isométrico con sombreado real por arista, más grande,
 * con las medidas escritas directamente sobre el dibujo — una línea de cota (o un tique corto
 * para los grosores) que termina en el nombre del campo, igual que un plano técnico.
 */
@Composable
fun ShapeIsoDiagram(shape: Shape, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val lineColor = accent.copy(alpha = 0.85f)

    val uDp = 5f
    val oxDp = 60f
    val oyDp = 54f
    val boxWidthDp = 340f
    val boxHeightDp = 250f
    fun toDp(x: Float, y: Float) = Offset(oxDp + x * uDp, oyDp + y * uDp)

    Box(modifier = modifier.size(width = boxWidthDp.dp, height = boxHeightDp.dp), contentAlignment = Alignment.TopStart) {
        Canvas(modifier = Modifier.size(width = boxWidthDp.dp, height = boxHeightDp.dp)) {
            fun toPx(x: Float, y: Float) = Offset((oxDp + x * uDp).dp.toPx(), (oyDp + y * uDp).dp.toPx())
            val depthPx = Offset(DEPTH_UNITS.x, DEPTH_UNITS.y).let { Offset((it.x * uDp).dp.toPx(), (it.y * uDp).dp.toPx()) }

            drawIsoShape(shape, accent, ::toPx, depthPx, 1.6.dp.toPx())

            // Líneas de cota: un trazo entre los dos puntos, con un tique perpendicular en cada
            // extremo (o uno solo para los tiques de detalle, que no cruzan toda la pieza).
            calloutsFor(shape).forEach { callout ->
                val p1 = toPx(callout.from.x, callout.from.y)
                val p2 = toPx(callout.to.x, callout.to.y)
                val strokeW = 1.2.dp.toPx()
                drawLine(lineColor, p1, p2, strokeWidth = strokeW)
                val dir = Offset(p2.x - p1.x, p2.y - p1.y)
                val len = sqrt(dir.x * dir.x + dir.y * dir.y).takeIf { it > 0.01f } ?: 1f
                val perp = Offset(-dir.y / len, dir.x / len) * 3.dp.toPx()
                drawLine(lineColor, p1 - perp, p1 + perp, strokeWidth = strokeW)
                if (callout.isSpan) {
                    drawLine(lineColor, p2 - perp, p2 + perp, strokeWidth = strokeW)
                }
            }
        }

        calloutsFor(shape).forEach { callout ->
            val pos = toDp(callout.labelAt.x, callout.labelAt.y)
            val boxWidth = if (callout.align == TextAlign.Center) 110.dp else 118.dp
            val xOffset = when (callout.align) {
                TextAlign.End -> pos.x - boxWidth.value
                TextAlign.Center -> pos.x - boxWidth.value / 2f
                else -> pos.x
            }
            Text(
                text = stringResource(dimensionNameRes(callout.type)),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                textAlign = callout.align,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = xOffset.dp, y = (pos.y - 7f).dp)
                    .size(width = boxWidth, height = 16.dp),
            )
        }
    }
}
