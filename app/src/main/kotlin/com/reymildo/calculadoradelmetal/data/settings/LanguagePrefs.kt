package com.reymildo.calculadoradelmetal.data.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Copia síncrona del idioma elegido. La Activity necesita saberlo en attachBaseContext, antes de
 * que exista ninguna corrutina; con el idioma aplicado ahí, los diálogos, hojas y menús (que
 * Compose resuelve desde la Activity, no desde nuestro árbol) también salen en ese idioma.
 * La fuente de verdad sigue siendo DataStore; AppRoot mantiene esta copia al día.
 */
object LanguagePrefs {
    private const val FILE = "language_prefs"
    private const val KEY = "tag"

    fun read(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null) ?: AppLanguage.ES.tag

    fun write(context: Context, tag: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
    }

    fun wrap(base: Context): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(read(base)))
        return base.createConfigurationContext(configuration)
    }
}
