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
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
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

/** The app's isometric I-beam mark, matching the launcher icon and the in-app shape glyphs. */
@Composable
private fun IsoIBeamGlyph(color: androidx.compose.ui.graphics.Color, sizeDp: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val u = size.minDimension / 108f

        val top = androidx.compose.ui.graphics.Path().apply {
            moveTo(32f * u, 32f * u); lineTo(60f * u, 32f * u); lineTo(74f * u, 24f * u); lineTo(46f * u, 24f * u); close()
        }
        drawPath(top, color.copy(alpha = 0.35f))

        val side = androidx.compose.ui.graphics.Path().apply {
            moveTo(60f * u, 32f * u); lineTo(60f * u, 78f * u); lineTo(74f * u, 70f * u); lineTo(74f * u, 24f * u); close()
        }
        drawPath(side, color.copy(alpha = 0.2f))

        val front = androidx.compose.ui.graphics.Path().apply {
            moveTo(32f * u, 32f * u); lineTo(60f * u, 32f * u); lineTo(60f * u, 39f * u); lineTo(50f * u, 39f * u)
            lineTo(50f * u, 71f * u); lineTo(60f * u, 71f * u); lineTo(60f * u, 78f * u); lineTo(32f * u, 78f * u)
            lineTo(32f * u, 71f * u); lineTo(42f * u, 71f * u); lineTo(42f * u, 39f * u); lineTo(32f * u, 39f * u); close()
        }
        drawPath(front, color)
    }
}
