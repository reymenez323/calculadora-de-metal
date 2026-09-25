package com.reymildo.calculadoradelmetal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.BuildConfig
import com.reymildo.calculadoradelmetal.R
import kotlinx.coroutines.delay

/**
 * In-app loading screen shown right after the (brief, OS-drawn) system splash.
 * The system SplashScreen API only supports an icon on a flat background, so the
 * name/credit/version content requested for the launch screen lives here instead.
 */
@Composable
fun SplashScreen(dataReady: Boolean, onFinished: () -> Unit) {
    LaunchedEffect(dataReady) {
        if (!dataReady) return@LaunchedEffect
        delay(350)
        onFinished()
    }

    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                IsoIBeamGlyph(color = MaterialTheme.colorScheme.primary, sizeDp = 88.dp)
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.splash_developed_by),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )

            Text(
                text = stringResource(R.string.splash_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
    }
}

/**
 * The app's isometric I-beam mark, matching the launcher icon and the in-app shape glyphs: the
 * front profile extruded edge by edge (not by its bounding box), so the web's notch shows the far
 * flange's top and the web's own inner wall instead of a single flat slab on each side.
 */
@Composable
private fun IsoIBeamGlyph(color: androidx.compose.ui.graphics.Color, sizeDp: androidx.compose.ui.unit.Dp) {
    val topColor = lerp(color, androidx.compose.ui.graphics.Color.White, 0.5f)
    val sideColor = lerp(color, androidx.compose.ui.graphics.Color.Black, 0.42f)

    Canvas(modifier = Modifier.size(sizeDp)) {
        val u = size.minDimension / 108f
        fun quad(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float) =
            androidx.compose.ui.graphics.Path().apply {
                moveTo(x1 * u, y1 * u); lineTo(x2 * u, y2 * u); lineTo(x3 * u, y3 * u); lineTo(x4 * u, y4 * u); close()
            }

        // Caras laterales: borde derecho de cada tramo del ala y pared derecha del alma.
        drawPath(quad(60f, 32f, 60f, 39f, 74f, 31f, 74f, 24f), sideColor)
        drawPath(quad(50f, 39f, 50f, 71f, 64f, 63f, 64f, 31f), sideColor)
        drawPath(quad(60f, 71f, 60f, 78f, 74f, 70f, 74f, 63f), sideColor)

        // Caras superiores: arriba del ala superior y las dos mitades del ala inferior, visibles
        // a través de la muesca del alma.
        drawPath(quad(32f, 32f, 60f, 32f, 74f, 24f, 46f, 24f), topColor)
        drawPath(quad(32f, 71f, 42f, 71f, 56f, 63f, 46f, 63f), topColor)
        drawPath(quad(50f, 71f, 60f, 71f, 74f, 63f, 64f, 63f), topColor)

        val front = androidx.compose.ui.graphics.Path().apply {
            moveTo(32f * u, 32f * u); lineTo(60f * u, 32f * u); lineTo(60f * u, 39f * u); lineTo(50f * u, 39f * u)
            lineTo(50f * u, 71f * u); lineTo(60f * u, 71f * u); lineTo(60f * u, 78f * u); lineTo(32f * u, 78f * u)
            lineTo(32f * u, 71f * u); lineTo(42f * u, 71f * u); lineTo(42f * u, 39f * u); lineTo(32f * u, 39f * u); close()
        }
        drawPath(front, color)
    }
}
