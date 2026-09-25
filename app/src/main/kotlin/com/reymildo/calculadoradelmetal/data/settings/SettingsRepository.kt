package com.reymildo.calculadoradelmetal.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val DEFAULT_LENGTH_UNIT = stringPreferencesKey("default_length_unit")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val TUTORIAL_DISMISSED = booleanPreferencesKey("tutorial_dismissed")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.ES,
            defaultLengthUnit = prefs[Keys.DEFAULT_LENGTH_UNIT]?.let { runCatching { LengthUnit.valueOf(it) }.getOrNull() }
                ?: LengthUnit.IN,
            decimalPrecision = prefs[Keys.DECIMAL_PRECISION] ?: 2,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "$",
            tutorialDismissed = prefs[Keys.TUTORIAL_DISMISSED] ?: false,
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setDefaultLengthUnit(unit: LengthUnit) {
        context.dataStore.edit { it[Keys.DEFAULT_LENGTH_UNIT] = unit.name }
    }

    suspend fun setDecimalPrecision(precision: Int) {
        context.dataStore.edit { it[Keys.DECIMAL_PRECISION] = precision }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setTutorialDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.TUTORIAL_DISMISSED] = dismissed }
    }
}
