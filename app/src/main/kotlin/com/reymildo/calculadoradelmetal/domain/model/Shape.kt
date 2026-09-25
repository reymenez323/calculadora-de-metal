package com.reymildo.calculadoradelmetal.domain.model

import kotlin.math.PI
import kotlin.math.sqrt

private fun Map<DimensionType, DimensionValue>.cm(type: DimensionType): Double =
    getValue(type).toCm()

sealed class Shape(val id: String, val requiredDimensions: List<DimensionType>) {

    abstract fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double

    open fun additionalValidation(dims: Map<DimensionType, DimensionValue>): List<String> = emptyList()

    data object RoundBar : Shape("round_bar", listOf(DimensionType.DIAMETER, DimensionType.LENGTH)) {
        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val radius = dims.cm(DimensionType.DIAMETER) / 2
            return PI * radius * radius * dims.cm(DimensionType.LENGTH)
        }
    }

    data object SquareBar : Shape("square_bar", listOf(DimensionType.SIDE, DimensionType.LENGTH)) {
        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val side = dims.cm(DimensionType.SIDE)
            return side * side * dims.cm(DimensionType.LENGTH)
        }
    }

    data object RectangularBar : Shape(
        "rectangular_bar",
        listOf(DimensionType.WIDTH, DimensionType.HEIGHT, DimensionType.LENGTH),
    ) {
        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double =
            dims.cm(DimensionType.WIDTH) * dims.cm(DimensionType.HEIGHT) * dims.cm(DimensionType.LENGTH)
    }

    data object HexBar : Shape("hex_bar", listOf(DimensionType.ACROSS_FLATS, DimensionType.LENGTH)) {
        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val acrossFlats = dims.cm(DimensionType.ACROSS_FLATS)
            return (sqrt(3.0) / 2) * acrossFlats * acrossFlats * dims.cm(DimensionType.LENGTH)
        }
    }

    data object Plate : Shape(
        "plate",
        listOf(DimensionType.WIDTH, DimensionType.LENGTH, DimensionType.THICKNESS),
    ) {
        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double =
            dims.cm(DimensionType.WIDTH) * dims.cm(DimensionType.LENGTH) * dims.cm(DimensionType.THICKNESS)
    }

    data object RoundTube : Shape(
        "round_tube",
        listOf(DimensionType.DIAMETER, DimensionType.WALL_THICKNESS, DimensionType.LENGTH),
    ) {
        private fun innerRadiusCm(dims: Map<DimensionType, DimensionValue>): Double {
            val outerRadius = dims.cm(DimensionType.DIAMETER) / 2
            return outerRadius - dims.cm(DimensionType.WALL_THICKNESS)
        }

        override fun additionalValidation(dims: Map<DimensionType, DimensionValue>): List<String> =
            if (innerRadiusCm(dims) <= 0) {
                listOf("El grosor de pared es demasiado grande para el diámetro indicado.")
            } else {
                emptyList()
            }

        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val outerRadius = dims.cm(DimensionType.DIAMETER) / 2
            val innerRadius = innerRadiusCm(dims)
            return PI * (outerRadius * outerRadius - innerRadius * innerRadius) * dims.cm(DimensionType.LENGTH)
        }
    }

    data object RectangularTube : Shape(
        "rectangular_tube",
        listOf(DimensionType.WIDTH, DimensionType.HEIGHT, DimensionType.WALL_THICKNESS, DimensionType.LENGTH),
    ) {
        private fun innerWidthCm(dims: Map<DimensionType, DimensionValue>) =
            dims.cm(DimensionType.WIDTH) - 2 * dims.cm(DimensionType.WALL_THICKNESS)

        private fun innerHeightCm(dims: Map<DimensionType, DimensionValue>) =
            dims.cm(DimensionType.HEIGHT) - 2 * dims.cm(DimensionType.WALL_THICKNESS)

        override fun additionalValidation(dims: Map<DimensionType, DimensionValue>): List<String> =
            if (innerWidthCm(dims) <= 0 || innerHeightCm(dims) <= 0) {
                listOf("El grosor de pared es demasiado grande para el ancho o alto indicado.")
            } else {
                emptyList()
            }

        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val outerVolume = dims.cm(DimensionType.WIDTH) * dims.cm(DimensionType.HEIGHT) * dims.cm(DimensionType.LENGTH)
            val innerVolume = innerWidthCm(dims) * innerHeightCm(dims) * dims.cm(DimensionType.LENGTH)
            return outerVolume - innerVolume
        }
    }

    data object Angle : Shape(
        "angle",
        listOf(DimensionType.WIDTH, DimensionType.HEIGHT, DimensionType.THICKNESS, DimensionType.LENGTH),
    ) {
        override fun additionalValidation(dims: Map<DimensionType, DimensionValue>): List<String> {
            val thickness = dims.cm(DimensionType.THICKNESS)
            return if (thickness >= dims.cm(DimensionType.WIDTH) || thickness >= dims.cm(DimensionType.HEIGHT)) {
                listOf("El grosor debe ser menor que ambas alas del ángulo.")
            } else {
                emptyList()
            }
        }

        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val a = dims.cm(DimensionType.WIDTH)
            val b = dims.cm(DimensionType.HEIGHT)
            val t = dims.cm(DimensionType.THICKNESS)
            return t * (a + b - t) * dims.cm(DimensionType.LENGTH)
        }
    }

    data object Channel : Shape(
        "channel",
        listOf(DimensionType.WIDTH, DimensionType.HEIGHT, DimensionType.THICKNESS, DimensionType.LENGTH),
    ) {
        private fun innerHeightCm(dims: Map<DimensionType, DimensionValue>) =
            dims.cm(DimensionType.HEIGHT) - 2 * dims.cm(DimensionType.THICKNESS)

        private fun innerWidthCm(dims: Map<DimensionType, DimensionValue>) =
            dims.cm(DimensionType.WIDTH) - dims.cm(DimensionType.THICKNESS)

        override fun additionalValidation(dims: Map<DimensionType, DimensionValue>): List<String> =
            if (innerHeightCm(dims) <= 0 || innerWidthCm(dims) <= 0) {
                listOf("El grosor es demasiado grande para el ancho o alto indicado.")
            } else {
                emptyList()
            }

        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val crossSectionArea = dims.cm(DimensionType.HEIGHT) * dims.cm(DimensionType.WIDTH) -
                innerHeightCm(dims) * innerWidthCm(dims)
            return crossSectionArea * dims.cm(DimensionType.LENGTH)
        }
    }

    data object IBeam : Shape(
        "i_beam",
        listOf(
            DimensionType.HEIGHT,
            DimensionType.WIDTH,
            DimensionType.WEB_THICKNESS,
            DimensionType.FLANGE_THICKNESS,
            DimensionType.LENGTH,
        ),
    ) {
        private fun webHeightCm(dims: Map<DimensionType, DimensionValue>) =
            dims.cm(DimensionType.HEIGHT) - 2 * dims.cm(DimensionType.FLANGE_THICKNESS)

        override fun additionalValidation(dims: Map<DimensionType, DimensionValue>): List<String> =
            buildList {
                if (webHeightCm(dims) <= 0) add("El grosor de las alas es demasiado grande para la altura indicada.")
                if (dims.cm(DimensionType.WEB_THICKNESS) >= dims.cm(DimensionType.WIDTH)) {
                    add("El grosor del alma debe ser menor que el ancho del ala.")
                }
            }

        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val flangesArea = 2 * dims.cm(DimensionType.WIDTH) * dims.cm(DimensionType.FLANGE_THICKNESS)
            val webArea = webHeightCm(dims) * dims.cm(DimensionType.WEB_THICKNESS)
            return (flangesArea + webArea) * dims.cm(DimensionType.LENGTH)
        }
    }

    data object TBar : Shape(
        "t_bar",
        listOf(
            DimensionType.HEIGHT,
            DimensionType.WIDTH,
            DimensionType.WEB_THICKNESS,
            DimensionType.FLANGE_THICKNESS,
            DimensionType.LENGTH,
        ),
    ) {
        private fun stemHeightCm(dims: Map<DimensionType, DimensionValue>) =
            dims.cm(DimensionType.HEIGHT) - dims.cm(DimensionType.FLANGE_THICKNESS)

        override fun additionalValidation(dims: Map<DimensionType, DimensionValue>): List<String> =
            buildList {
                if (stemHeightCm(dims) <= 0) add("El grosor del ala es demasiado grande para la altura indicada.")
                if (dims.cm(DimensionType.WEB_THICKNESS) >= dims.cm(DimensionType.WIDTH)) {
                    add("El grosor del alma debe ser menor que el ancho del ala.")
                }
            }

        override fun volumeCm3(dims: Map<DimensionType, DimensionValue>): Double {
            val flangeArea = dims.cm(DimensionType.WIDTH) * dims.cm(DimensionType.FLANGE_THICKNESS)
            val stemArea = stemHeightCm(dims) * dims.cm(DimensionType.WEB_THICKNESS)
            return (flangeArea + stemArea) * dims.cm(DimensionType.LENGTH)
        }
    }

    companion object {
        // `by lazy` defers building this list until first access, after every nested
        // object below is fully loaded. Building it eagerly can race Android's (ART)
        // class-initialization order for sealed-class + nested-object singletons and
        // yield a null entry (observed as a NullPointerException deep in a consumer).
        val ALL: List<Shape> by lazy {
            listOf(
                RoundBar, SquareBar, RectangularBar, HexBar, Plate,
                RoundTube, RectangularTube, Angle, Channel, IBeam, TBar,
            )
        }

        fun fromId(id: String): Shape =
            ALL.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Forma desconocida: $id")
    }
}
