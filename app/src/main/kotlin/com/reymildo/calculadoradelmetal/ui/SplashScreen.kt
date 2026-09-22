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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
                DiameterGlyph(color = MaterialTheme.colorScheme.primary, sizeDp = 88.dp)
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

/** The app's ⌀ mark, drawn to match the launcher icon and the rest of the hand-drawn glyphs. */
@Composable
private fun DiameterGlyph(color: androidx.compose.ui.graphics.Color, sizeDp: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val u = size.minDimension / 108f
        val stroke = Stroke(width = 6.5f * u)
        drawCircle(color = color, radius = 24f * u, center = Offset(54f * u, 54f * u), style = stroke)
        drawLine(
            color = color,
            start = Offset(28f * u, 80f * u),
            end = Offset(80f * u, 28f * u),
            strokeWidth = 6.5f * u,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
