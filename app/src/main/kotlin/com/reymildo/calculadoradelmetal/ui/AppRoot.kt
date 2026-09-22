@file:OptIn(ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.di.AppContainer
import com.reymildo.calculadoradelmetal.ui.calc.CalculatorScreen
import com.reymildo.calculadoradelmetal.ui.settings.SettingsScreen
import com.reymildo.calculadoradelmetal.ui.suppliers.SuppliersScreen
import com.reymildo.calculadoradelmetal.ui.theme.CalculadoraTheme
import kotlinx.coroutines.launch
import java.util.Locale

private enum class Tab { CALC, SUPPLIERS, SETTINGS }

@Composable
fun AppRoot(container: AppContainer) {
    val settings by container.settingsRepository.settings.collectAsState(initial = AppSettings())
    val suppliers by container.supplierRepository
        .observeSuppliersWithMaterials()
        .collectAsState(initial = emptyList())

    // Idioma en caliente: se reescribe el Context para que stringResource lea values-en/.
    val baseContext = LocalContext.current
    val localizedContext = remember(settings.language, baseContext) {
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocale(Locale(settings.language.tag))
        baseContext.createConfigurationContext(configuration)
    }

    CalculadoraTheme {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedContext.resources.configuration,
        ) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                AppScaffold(container = container, settings = settings, suppliers = suppliers)
            }
        }
    }
}

@Composable
private fun AppScaffold(
    container: AppContainer,
    settings: AppSettings,
    suppliers: List<com.reymildo.calculadoradelmetal.data.local.relation.SupplierWithMaterials>,
) {
    var tab by remember { mutableStateOf(Tab.CALC) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = when (tab) {
                                Tab.CALC -> stringResource(R.string.tab_calc)
                                Tab.SUPPLIERS -> stringResource(R.string.tab_suppliers)
                                Tab.SETTINGS -> stringResource(R.string.tab_settings)
                            },
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = when (tab) {
                                Tab.CALC -> stringResource(R.string.calc_subtitle)
                                Tab.SUPPLIERS -> stringResource(R.string.sup_subtitle)
                                Tab.SETTINGS -> stringResource(R.string.set_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { candidate ->
                    NavigationBarItem(
                        selected = tab == candidate,
                        onClick = { tab = candidate },
                        icon = {
                            TabIcon(
                                tab = candidate,
                                color = if (tab == candidate) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        label = {
                            Text(
                                text = when (candidate) {
                                    Tab.CALC -> stringResource(R.string.tab_calc)
                                    Tab.SUPPLIERS -> stringResource(R.string.tab_suppliers)
                                    Tab.SETTINGS -> stringResource(R.string.tab_settings)
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.CALC -> CalculatorScreen(
                suppliers = suppliers,
                settings = settings,
                modifier = Modifier.padding(padding),
            )

            Tab.SUPPLIERS -> SuppliersScreen(
                suppliers = suppliers,
                settings = settings,
                modifier = Modifier.padding(padding),
                onSaveMaterial = { supplierId, existing, name, shape, dimensions, price ->
                    scope.launch {
                        if (existing == null) {
                            container.materialRepository.addMaterial(
                                supplierId = supplierId,
                                name = name,
                                stockShape = shape,
                                stockDimensions = dimensions,
                                stockPrice = price,
                            )
                        } else {
                            container.materialRepository.updateMaterial(
                                existing = existing,
                                name = name,
                                stockShape = shape,
                                stockDimensions = dimensions,
                                stockPrice = price,
                            )
                        }
                    }
                },
                onDeleteMaterial = { entity ->
                    scope.launch { container.materialRepository.deleteMaterial(entity) }
                },
                onAddSupplier = { name ->
                    scope.launch { container.supplierRepository.addSupplier(name) }
                },
            )

            Tab.SETTINGS -> SettingsScreen(
                settings = settings,
                modifier = Modifier.padding(padding),
                onLanguageChange = { scope.launch { container.settingsRepository.setLanguage(it) } },
                onDefaultUnitChange = { scope.launch { container.settingsRepository.setDefaultLengthUnit(it) } },
                onDecimalsChange = { scope.launch { container.settingsRepository.setDecimalPrecision(it) } },
                onCurrencyChange = { scope.launch { container.settingsRepository.setCurrencySymbol(it) } },
                onResetDefaults = { scope.launch { container.supplierRepository.resetToDefaults() } },
            )
        }
    }
}

/** Iconos de la barra inferior, dibujados a mano para no depender de material-icons-extended. */
@Composable
private fun TabIcon(tab: Tab, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val u = size.minDimension / 24f
        val stroke = Stroke(width = 1.9f * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = 1.9f * u)
        }
        when (tab) {
            Tab.CALC -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(4f * u, 3f * u),
                    size = Size(16f * u, 18f * u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * u, 2.5f * u),
                    style = stroke,
                )
                line(8f, 7.5f, 16f, 7.5f)
                line(8f, 12f, 10f, 12f)
                line(12f, 12f, 14f, 12f)
                line(8f, 16f, 10f, 16f)
                line(12f, 16f, 16f, 16f)
            }

            Tab.SUPPLIERS -> {
                val top = Path().apply {
                    moveTo(3f * u, 7.5f * u)
                    lineTo(12f * u, 3f * u)
                    lineTo(21f * u, 7.5f * u)
                    lineTo(12f * u, 12f * u)
                    close()
                }
                drawPath(top, color, style = stroke)
                val mid = Path().apply {
                    moveTo(3f * u, 12.5f * u)
                    lineTo(12f * u, 17f * u)
                    lineTo(21f * u, 12.5f * u)
                }
                drawPath(mid, color, style = stroke)
                val bottom = Path().apply {
                    moveTo(3f * u, 17f * u)
                    lineTo(12f * u, 21.5f * u)
                    lineTo(21f * u, 17f * u)
                }
                drawPath(bottom, color, style = stroke)
            }

            Tab.SETTINGS -> {
                line(4f, 7f, 20f, 7f)
                line(4f, 12f, 20f, 12f)
                line(4f, 17f, 20f, 17f)
                drawCircle(color, radius = 2.4f * u, center = Offset(9f * u, 7f * u), style = stroke)
                drawCircle(color, radius = 2.4f * u, center = Offset(15f * u, 12f * u), style = stroke)
                drawCircle(color, radius = 2.4f * u, center = Offset(10f * u, 17f * u), style = stroke)
            }
        }
    }
}
