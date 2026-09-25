package com.reymildo.calculadoradelmetal.data.settings

import com.reymildo.calculadoradelmetal.domain.model.LengthUnit

data class AppSettings(
    val language: AppLanguage = AppLanguage.ES,
    val defaultLengthUnit: LengthUnit = LengthUnit.IN,
    val decimalPrecision: Int = 3,
    val currencySymbol: String = "$",
    val tutorialDismissed: Boolean = false,
)
