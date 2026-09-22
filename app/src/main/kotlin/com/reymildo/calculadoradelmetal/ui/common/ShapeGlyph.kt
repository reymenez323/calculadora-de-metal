package com.reymildo.calculadoradelmetal.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.Shape

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

/**
 * Icono de la sección transversal de cada perfil, dibujado en un lienzo de 32×32 unidades
 * y escalado al tamaño del modifier. Sin assets.
 */
@Composable
fun ShapeGlyph(
    shape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.7f,
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 32f
        val ox = (size.width - 32f * u) / 2f
        val oy = (size.height - 32f * u) / 2f
        val stroke = Stroke(width = strokeWidth * u)

        fun offset(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)

        fun polygon(vararg pts: Float): Path {
            val path = Path()
            path.moveTo(ox + pts[0] * u, oy + pts[1] * u)
            var i = 2
            while (i < pts.size) {
                path.lineTo(ox + pts[i] * u, oy + pts[i + 1] * u)
                i += 2
            }
            path.close()
            return path
        }

        fun rect(x: Float, y: Float, w: Float, h: Float) {
            drawRect(color = color, topLeft = offset(x, y), size = Size(w * u, h * u), style = stroke)
        }

        when (shape) {
            Shape.RoundBar -> drawCircle(color, radius = 12f * u, center = offset(16f, 16f), style = stroke)

            Shape.SquareBar -> rect(5f, 5f, 22f, 22f)

            Shape.RectangularBar -> rect(4f, 10f, 24f, 12f)

            Shape.HexBar -> drawPath(
                polygon(16f, 3.5f, 26.8f, 9.75f, 26.8f, 22.25f, 16f, 28.5f, 5.2f, 22.25f, 5.2f, 9.75f),
                color, style = stroke,
            )

            Shape.Plate -> {
                rect(3f, 13f, 26f, 6f)
                val lid = Path().apply {
                    moveTo(ox + 3f * u, oy + 13f * u)
                    lineTo(ox + 7f * u, oy + 10f * u)
                    lineTo(ox + 29f * u, oy + 10f * u)
                    lineTo(ox + 29f * u, oy + 13f * u)
                }
                drawPath(lid, color, style = stroke)
            }

            Shape.RoundTube -> {
                drawCircle(color, radius = 12f * u, center = offset(16f, 16f), style = stroke)
                drawCircle(color, radius = 7f * u, center = offset(16f, 16f), style = stroke)
            }

            Shape.RectangularTube -> {
                rect(4f, 8f, 24f, 16f)
                rect(8.5f, 12.5f, 15f, 7f)
            }

            Shape.Angle -> drawPath(
                polygon(7f, 4f, 12f, 4f, 12f, 23f, 28f, 23f, 28f, 28f, 7f, 28f),
                color, style = stroke,
            )

            Shape.Channel -> drawPath(
                polygon(6f, 4f, 26f, 4f, 26f, 9f, 11f, 9f, 11f, 23f, 26f, 23f, 26f, 28f, 6f, 28f),
                color, style = stroke,
            )

            Shape.IBeam -> drawPath(
                polygon(
                    6f, 4f, 26f, 4f, 26f, 9f, 18.5f, 9f, 18.5f, 23f, 26f, 23f,
                    26f, 28f, 6f, 28f, 6f, 23f, 13.5f, 23f, 13.5f, 9f, 6f, 9f,
                ),
                color, style = stroke,
            )

            Shape.TBar -> drawPath(
                polygon(4f, 4f, 28f, 4f, 28f, 9f, 18.5f, 9f, 18.5f, 28f, 13.5f, 28f, 13.5f, 9f, 4f, 9f),
                color, style = stroke,
            )
        }
    }
}
