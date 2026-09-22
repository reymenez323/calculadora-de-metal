package com.reymildo.calculadoradelmetal.ui.common

import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import java.text.NumberFormat
import java.util.Locale

object Fmt {

    private fun formatter(decimals: Int): NumberFormat =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }

    fun number(value: Double, decimals: Int): String =
        if (value.isFinite()) formatter(decimals).format(value) else "—"

    fun money(value: Double, settings: AppSettings, decimals: Int = settings.decimalPrecision): String =
        if (value.isFinite()) "${settings.currencySymbol} ${formatter(decimals).format(value)}" else "—"

    /** "1 in × 20 ft" — resumen de las medidas del bruto en el orden que declara la forma. */
    fun dimensions(order: List<DimensionType>, dims: Map<DimensionType, DimensionValue>): String =
        order.mapNotNull { type ->
            dims[type]?.let { d -> "${trimNumber(d.value)} ${d.unit.symbol}" }
        }.joinToString(" × ")

    fun trimNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 4
        }.format(value)
}

/** Texto de un campo numérico → Double, aceptando coma decimal. */
fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Filtra lo que el usuario escribe para dejar solo un número decimal. */
fun String.sanitizeDecimal(): String {
    val cleaned = filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
    val firstDot = cleaned.indexOf('.')
    if (firstDot == -1) return cleaned
    return cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).filter { it != '.' }
}

fun String.sanitizeInt(): String = filter { it.isDigit() }.take(6)
