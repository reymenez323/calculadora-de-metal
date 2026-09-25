package com.reymildo.calculadoradelmetal.domain.machining

/**
 * Valores iniciales GENÉRICOS y conservadores para cuando no hay datos del fabricante.
 * Son puntos de partida de taller, no garantías: sustitúyelos por los de tu inserto o fresa.
 */
object MachiningCatalog {
    const val SOURCE = "Valores genéricos conservadores de la app"

    private fun r(
        vc: Triple<Double, Double, Double>,
        feed: Triple<Double, Double, Double>,
    ) = CuttingRecommendation(vc.first, vc.second, vc.third, feed.first, feed.second, feed.third)

    private fun t(a: Double, b: Double, c: Double) = Triple(a, b, c)

    private val carbideTurning = mapOf(
        MaterialCategory.CARBON_STEEL to r(t(140.0, 180.0, 220.0), t(0.10, 0.20, 0.35)),
        MaterialCategory.ALLOY_STEEL to r(t(110.0, 150.0, 190.0), t(0.10, 0.18, 0.30)),
        MaterialCategory.STAINLESS to r(t(80.0, 110.0, 140.0), t(0.08, 0.14, 0.22)),
        MaterialCategory.ALUMINUM to r(t(250.0, 350.0, 500.0), t(0.10, 0.25, 0.40)),
        MaterialCategory.CAST_IRON to r(t(100.0, 140.0, 180.0), t(0.10, 0.20, 0.35)),
        MaterialCategory.COPPER_ALLOY to r(t(150.0, 220.0, 300.0), t(0.10, 0.20, 0.35)),
        MaterialCategory.TITANIUM to r(t(35.0, 50.0, 70.0), t(0.08, 0.15, 0.25)),
        MaterialCategory.PLASTIC to r(t(200.0, 350.0, 500.0), t(0.10, 0.20, 0.35)),
    )

    private val hssTurning = mapOf(
        MaterialCategory.CARBON_STEEL to r(t(20.0, 28.0, 38.0), t(0.05, 0.10, 0.20)),
        MaterialCategory.ALLOY_STEEL to r(t(15.0, 22.0, 30.0), t(0.05, 0.10, 0.18)),
        MaterialCategory.STAINLESS to r(t(8.0, 12.0, 18.0), t(0.04, 0.08, 0.15)),
        MaterialCategory.ALUMINUM to r(t(60.0, 120.0, 200.0), t(0.05, 0.12, 0.25)),
        MaterialCategory.CAST_IRON to r(t(15.0, 22.0, 30.0), t(0.05, 0.12, 0.20)),
        MaterialCategory.COPPER_ALLOY to r(t(40.0, 70.0, 100.0), t(0.05, 0.12, 0.25)),
        MaterialCategory.TITANIUM to r(t(8.0, 12.0, 18.0), t(0.04, 0.08, 0.12)),
        MaterialCategory.PLASTIC to r(t(60.0, 120.0, 200.0), t(0.05, 0.15, 0.25)),
    )

    private val carbideMilling = mapOf(
        MaterialCategory.CARBON_STEEL to r(t(100.0, 140.0, 180.0), t(0.03, 0.05, 0.08)),
        MaterialCategory.ALLOY_STEEL to r(t(80.0, 120.0, 160.0), t(0.02, 0.04, 0.07)),
        MaterialCategory.STAINLESS to r(t(50.0, 80.0, 110.0), t(0.02, 0.03, 0.05)),
        MaterialCategory.ALUMINUM to r(t(250.0, 400.0, 600.0), t(0.04, 0.08, 0.15)),
        MaterialCategory.CAST_IRON to r(t(80.0, 120.0, 160.0), t(0.03, 0.06, 0.10)),
        MaterialCategory.COPPER_ALLOY to r(t(120.0, 200.0, 300.0), t(0.03, 0.06, 0.12)),
        MaterialCategory.TITANIUM to r(t(35.0, 50.0, 70.0), t(0.02, 0.03, 0.05)),
        MaterialCategory.PLASTIC to r(t(200.0, 350.0, 500.0), t(0.04, 0.08, 0.15)),
    )

    private val hssMilling = mapOf(
        MaterialCategory.CARBON_STEEL to r(t(18.0, 25.0, 32.0), t(0.02, 0.04, 0.06)),
        MaterialCategory.ALLOY_STEEL to r(t(12.0, 18.0, 25.0), t(0.02, 0.03, 0.05)),
        MaterialCategory.STAINLESS to r(t(8.0, 12.0, 18.0), t(0.015, 0.025, 0.04)),
        MaterialCategory.ALUMINUM to r(t(60.0, 120.0, 200.0), t(0.03, 0.06, 0.12)),
        MaterialCategory.CAST_IRON to r(t(12.0, 20.0, 28.0), t(0.02, 0.04, 0.07)),
        MaterialCategory.COPPER_ALLOY to r(t(30.0, 60.0, 90.0), t(0.03, 0.06, 0.10)),
        MaterialCategory.TITANIUM to r(t(8.0, 12.0, 16.0), t(0.01, 0.02, 0.03)),
        MaterialCategory.PLASTIC to r(t(60.0, 120.0, 200.0), t(0.03, 0.06, 0.12)),
    )

    fun generic(category: MaterialCategory, tool: ToolKind, turning: Boolean): CuttingRecommendation {
        val table = when {
            turning && tool == ToolKind.HSS -> hssTurning
            turning -> carbideTurning
            tool == ToolKind.HSS -> hssMilling
            else -> carbideMilling
        }
        return table.getValue(category)
    }
}
