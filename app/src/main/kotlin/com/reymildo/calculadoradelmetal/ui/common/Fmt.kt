package com.reymildo.calculadoradelmetal.ui.common

import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import java.text.NumberFormat
import java.math.BigDecimal
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
        editable(value)

    /** A locale-neutral representation intended to be placed back in an editable field. */
    fun editable(value: Double): String =
        if (!value.isFinite()) "" else BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

/** Accepts one decimal separator and rejects signs, fractions and grouping ambiguities. */
fun String.toDecimalOrNull(): Double? {
    val value = trim()
    if (value.isEmpty() || !value.isValidDecimalInput(allowEmpty = false)) return null
    return value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

fun String.isValidDecimalInput(allowEmpty: Boolean = true): Boolean {
    if (isEmpty()) return allowEmpty
    if (isBlank() || any { it.isWhitespace() } || (contains('.') && contains(','))) return false
    return matches(Regex("^\\d+(?:[.,]\\d*)?$") ) || (allowEmpty && matches(Regex("^0?[.,]\\d*$")))
}

/** Keeps the last valid value instead of silently changing what the user typed. */
fun String.sanitizeDecimal(previous: String = ""): String =
    if (isValidDecimalInput()) this else previous

fun String.sanitizeInt(): String = filter { it.isDigit() }.take(6)
