@file:OptIn(ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.data.local.relation.SupplierWithMaterials
import com.reymildo.calculadoradelmetal.data.local.relation.ToolWithRecommendations
import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.data.settings.LanguagePrefs
import com.reymildo.calculadoradelmetal.di.AppContainer
import com.reymildo.calculadoradelmetal.ui.calc.CalcHeader
import com.reymildo.calculadoradelmetal.ui.calc.CalcMode
import com.reymildo.calculadoradelmetal.ui.calc.CalculatorScreen
import com.reymildo.calculadoradelmetal.ui.common.Glyph
import com.reymildo.calculadoradelmetal.ui.common.GlyphIcon
import com.reymildo.calculadoradelmetal.ui.machining.MachiningScreen
import com.reymildo.calculadoradelmetal.ui.settings.SettingsScreen
import com.reymildo.calculadoradelmetal.ui.suppliers.SuppliersScreen
import com.reymildo.calculadoradelmetal.ui.theme.CalculadoraTheme
import com.reymildo.calculadoradelmetal.ui.workshop.MachinesScreen
import com.reymildo.calculadoradelmetal.ui.workshop.MachiningLibraryScreen
import com.reymildo.calculadoradelmetal.ui.workshop.WorkshopHost
import com.reymildo.calculadoradelmetal.ui.workshop.WorkshopSection
import kotlinx.coroutines.launch

private enum class Tab { CALC, WORKSHOP, SETTINGS }

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

@Composable
fun AppRoot(container: AppContainer) {
    // null = todavía cargando; evita mostrar el tutorial a quien ya lo desactivó antes de leer el ajuste.
    val loadedSettings by container.settingsRepository.settings.collectAsState(initial = null)
    val settings = loadedSettings ?: AppSettings()
    val suppliers by container.supplierRepository
        .observeSuppliersWithMaterials()
        .collectAsState(initial = emptyList())
    val machineProfiles by container.machineProfileRepository.observeAll().collectAsState(initial = emptyList())
    val machiningMaterials by container.machiningRepository.observeMaterials().collectAsState(initial = emptyList())
    val tools by container.machiningRepository.observeTools().collectAsState(initial = emptyList())

    // El idioma se aplica a la Activity entera (MainActivity.attachBaseContext), no solo a este árbol:
    // Compose resuelve los textos de diálogos, hojas y menús desde la Activity, y con el idioma del
    // teléfono en inglés esas ventanas salían en inglés. Aquí se mantiene al día la copia síncrona
    // y, si cambió (o es la primera vez tras actualizar), se recrea la Activity para aplicarlo.
    val context = LocalContext.current
    LaunchedEffect(loadedSettings?.language) {
        val language = loadedSettings?.language ?: return@LaunchedEffect
        if (LanguagePrefs.read(context) != language.tag) {
            LanguagePrefs.write(context, language.tag)
            context.findActivity()?.recreate()
        }
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }

    CalculadoraTheme {
        if (showSplash) {
            SplashScreen(
                dataReady = loadedSettings != null && suppliers.isNotEmpty() && machineProfiles.isNotEmpty() &&
                    machiningMaterials.isNotEmpty() && tools.isNotEmpty(),
                onFinished = { showSplash = false },
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                AppScaffold(
                    container = container,
                    settings = settings,
                    suppliers = suppliers,
                    machineProfiles = machineProfiles,
                    machiningMaterials = machiningMaterials,
                    tools = tools,
                )
            }
        }
    }
}

@Composable
private fun AppScaffold(
    container: AppContainer,
    settings: AppSettings,
    suppliers: List<SupplierWithMaterials>,
    machineProfiles: List<MachineProfileEntity>,
    machiningMaterials: List<MachiningMaterialEntity>,
    tools: List<ToolWithRecommendations>,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.CALC) }
    var calcMode by rememberSaveable { mutableStateOf(CalcMode.MATERIA_PRIMA) }
    var workshopSection by rememberSaveable { mutableStateOf(WorkshopSection.STOCK) }
    var calcHeader by remember { mutableStateOf<CalcHeader?>(null) }
    var tutorialVisible by rememberSaveable { mutableStateOf(true) }
    var tutorialStep by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val showTutorial = tutorialVisible && !settings.tutorialDismissed

    // Cada paso del tutorial lleva la app detrás del cuadro a la sección que explica.
    LaunchedEffect(tutorialStep, showTutorial) {
        if (!showTutorial) return@LaunchedEffect
        when (tutorialSteps[tutorialStep].target) {
            TutorialTarget.NONE -> Unit
            TutorialTarget.STOCK -> { tab = Tab.WORKSHOP; workshopSection = WorkshopSection.STOCK }
            TutorialTarget.MACHINES -> { tab = Tab.WORKSHOP; workshopSection = WorkshopSection.MACHINES }
            TutorialTarget.MACHINING -> { tab = Tab.WORKSHOP; workshopSection = WorkshopSection.MACHINING }
            TutorialTarget.CALC_STOCK -> { tab = Tab.CALC; calcMode = CalcMode.MATERIA_PRIMA; calcHeader = null }
            TutorialTarget.CALC_TURNING -> { tab = Tab.CALC; calcMode = CalcMode.TORNEADO; calcHeader = null }
            TutorialTarget.SETTINGS -> tab = Tab.SETTINGS
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    val header = calcHeader
                    if (tab == Tab.CALC && header != null) {
                        IconButton(onClick = header.onBack) {
                            Text("‹", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                },
                title = {
                    when (tab) {
                        Tab.CALC -> {
                            val header = calcHeader
                            if (header != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(text = header.title, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = header.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                CalcModeDropdown(
                                    mode = calcMode,
                                    onModeChange = {
                                        calcMode = it
                                        calcHeader = null
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                            }
                        }
                        Tab.WORKSHOP -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.tab_workshop),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = stringResource(R.string.workshop_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Tab.SETTINGS -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.tab_settings),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = stringResource(R.string.set_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                                    Tab.WORKSHOP -> stringResource(R.string.tab_workshop)
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
            Tab.CALC -> if (calcMode == CalcMode.MATERIA_PRIMA) {
                CalculatorScreen(
                    suppliers = suppliers,
                    settings = settings,
                    modifier = Modifier.padding(padding),
                    onHeaderChange = { calcHeader = it },
                )
            } else {
                MachiningScreen(
                    mode = calcMode,
                    machines = machineProfiles,
                    materials = machiningMaterials,
                    tools = tools,
                    decimals = settings.decimalPrecision,
                    modifier = Modifier.padding(padding),
                    onOpenMachines = {
                        workshopSection = WorkshopSection.MACHINES
                        tab = Tab.WORKSHOP
                    },
                )
            }

            Tab.WORKSHOP -> WorkshopHost(
                section = workshopSection,
                onSectionChange = { workshopSection = it },
                modifier = Modifier.padding(padding),
            ) { section, sectionModifier ->
                when (section) {
                    WorkshopSection.STOCK -> SuppliersScreen(
                        suppliers = suppliers,
                        machiningMaterials = machiningMaterials,
                        settings = settings,
                        modifier = sectionModifier,
                        onSaveMaterial = { supplierId, existing, name, shape, dimensions, price, technicalMaterialId ->
                            if (existing == null) {
                                container.materialRepository.addMaterial(
                                    supplierId = supplierId,
                                    name = name,
                                    stockShape = shape,
                                    stockDimensions = dimensions,
                                    stockPrice = price,
                                    technicalMaterialId = technicalMaterialId,
                                    currencyCode = settings.currencySymbol,
                                ).map { Unit }
                            } else {
                                container.materialRepository.updateMaterial(
                                    existing = existing,
                                    name = name,
                                    stockShape = shape,
                                    stockDimensions = dimensions,
                                    stockPrice = price,
                                    technicalMaterialId = technicalMaterialId,
                                    currencyCode = settings.currencySymbol,
                                )
                            }
                        },
                        onDeleteMaterial = { entity ->
                            scope.launch { container.materialRepository.deleteMaterial(entity) }
                        },
                        onAddSupplier = { name ->
                            scope.launch { container.supplierRepository.addSupplier(name) }
                        },
                        onRenameSupplier = { id, name -> scope.launch { container.supplierRepository.renameSupplier(id, name) } },
                        onDeleteSupplier = { supplier -> scope.launch { container.supplierRepository.deleteSupplier(supplier) } },
                    )

                    WorkshopSection.MACHINES -> MachinesScreen(
                        machines = machineProfiles,
                        onSave = { container.machineProfileRepository.save(it) },
                        onDelete = { machine -> scope.launch { container.machineProfileRepository.delete(machine) } },
                        modifier = sectionModifier,
                    )

                    WorkshopSection.MACHINING -> MachiningLibraryScreen(
                        materials = machiningMaterials,
                        tools = tools,
                        onSaveMaterial = { container.machiningRepository.saveMaterial(it) },
                        onDeleteMaterial = { material -> scope.launch { container.machiningRepository.deleteMaterial(material) } },
                        onSaveTool = { tool, recommendations -> container.machiningRepository.saveTool(tool, recommendations) },
                        onDeleteTool = { tool -> scope.launch { container.machiningRepository.deleteTool(tool) } },
                        modifier = sectionModifier,
                    )
                }
            }

            Tab.SETTINGS -> SettingsScreen(
                settings = settings,
                modifier = Modifier.padding(padding),
                onLanguageChange = { scope.launch { container.settingsRepository.setLanguage(it) } },
                onDefaultUnitChange = { scope.launch { container.settingsRepository.setDefaultLengthUnit(it) } },
                onDecimalsChange = { scope.launch { container.settingsRepository.setDecimalPrecision(it) } },
                onCurrencyChange = { scope.launch { container.settingsRepository.setCurrencySymbol(it) } },
                onResetDefaults = { scope.launch { container.supplierRepository.resetToDefaults() } },
                onExportBackup = { container.backupRepository.exportJson() },
                onImportBackup = { raw -> container.backupRepository.importJson(raw) },
                onShowTutorial = {
                    scope.launch { container.settingsRepository.setTutorialDismissed(false) }
                    tutorialStep = 0
                    tutorialVisible = true
                },
            )
        }
    }
    if (showTutorial) {
        TutorialOverlay(
            step = tutorialStep,
            onStepChange = { tutorialStep = it },
            onSkip = { tutorialVisible = false },
            onNeverShow = {
                tutorialVisible = false
                scope.launch { container.settingsRepository.setTutorialDismissed(true) }
            },
        )
    }
    }
}

@Composable
private fun calcModeLabel(mode: CalcMode): String = when (mode) {
    CalcMode.MATERIA_PRIMA -> stringResource(R.string.calc_mode_materia_prima)
    CalcMode.TORNEADO -> stringResource(R.string.calc_mode_torneado)
    CalcMode.FRESADO -> stringResource(R.string.calc_mode_fresado)
}

/** Permite cambiar entre los tres modos de cálculo desde el encabezado de la pestaña. */
@Composable
private fun CalcModeDropdown(
    mode: CalcMode,
    onModeChange: (CalcMode) -> Unit,
    style: androidx.compose.ui.text.TextStyle,
) {
    var expanded by remember { mutableStateOf(false) }
    // DropdownMenu usa una ventana Compose independiente; resolver los recursos aquí conserva
    // el idioma elegido por la aplicación también dentro de esa ventana.
    val modeLabels = CalcMode.entries.associateWith { calcModeLabel(it) }
    Box {
        Text(
            text = modeLabels.getValue(mode) + " ⌄",
            style = style,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickableNoRipple { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CalcMode.entries.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(modeLabels.getValue(candidate)) },
                    onClick = {
                        onModeChange(candidate)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    ),
)

/** Iconos de la barra inferior, dibujados a mano para no depender de material-icons-extended. */
@Composable
private fun TabIcon(tab: Tab, color: Color) {
    if (tab == Tab.WORKSHOP) {
        GlyphIcon(Glyph.WORKSHOP, color, size = 22.dp)
        return
    }
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

            Tab.WORKSHOP -> Unit

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
