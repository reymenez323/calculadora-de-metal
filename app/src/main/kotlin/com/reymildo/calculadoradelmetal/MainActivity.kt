package com.reymildo.calculadoradelmetal

import android.content.Context
import android.os.Bundle
import com.reymildo.calculadoradelmetal.data.settings.LanguagePrefs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.reymildo.calculadoradelmetal.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguagePrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CalculadoraApp).container
        setContent {
            AppRoot(container = container)
        }
    }
}
