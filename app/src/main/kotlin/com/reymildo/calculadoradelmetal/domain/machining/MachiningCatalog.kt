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

    // Un valor por grupo ISO: el más conservador de los materiales que cubre (acero aleado para P,
    // cobre/latón para N…), porque el usuario no tiene por qué saber el material exacto.
    private val carbideTurning = mapOf(
        IsoGroup.P to r(t(110.0, 150.0, 190.0), t(0.10, 0.18, 0.30)),
        IsoGroup.M to r(t(80.0, 110.0, 140.0), t(0.08, 0.14, 0.22)),
        IsoGroup.K to r(t(100.0, 140.0, 180.0), t(0.10, 0.20, 0.35)),
        IsoGroup.N to r(t(150.0, 220.0, 300.0), t(0.10, 0.20, 0.35)),
        IsoGroup.S to r(t(35.0, 50.0, 70.0), t(0.08, 0.15, 0.25)),
        IsoGroup.H to r(t(50.0, 70.0, 100.0), t(0.05, 0.10, 0.15)),
    )

    private val hssTurning = mapOf(
        IsoGroup.P to r(t(15.0, 22.0, 30.0), t(0.05, 0.10, 0.18)),
        IsoGroup.M to r(t(8.0, 12.0, 18.0), t(0.04, 0.08, 0.15)),
        IsoGroup.K to r(t(15.0, 22.0, 30.0), t(0.05, 0.12, 0.20)),
        IsoGroup.N to r(t(40.0, 70.0, 100.0), t(0.05, 0.12, 0.25)),
        IsoGroup.S to r(t(8.0, 12.0, 18.0), t(0.04, 0.08, 0.12)),
        IsoGroup.H to r(t(4.0, 6.0, 9.0), t(0.03, 0.05, 0.08)),
    )

    private val carbideMilling = mapOf(
        IsoGroup.P to r(t(80.0, 120.0, 160.0), t(0.02, 0.04, 0.07)),
        IsoGroup.M to r(t(50.0, 80.0, 110.0), t(0.02, 0.03, 0.05)),
        IsoGroup.K to r(t(80.0, 120.0, 160.0), t(0.03, 0.06, 0.10)),
        IsoGroup.N to r(t(120.0, 200.0, 300.0), t(0.03, 0.06, 0.12)),
        IsoGroup.S to r(t(35.0, 50.0, 70.0), t(0.02, 0.03, 0.05)),
        IsoGroup.H to r(t(40.0, 60.0, 80.0), t(0.02, 0.03, 0.05)),
    )

    private val hssMilling = mapOf(
        IsoGroup.P to r(t(12.0, 18.0, 25.0), t(0.02, 0.03, 0.05)),
        IsoGroup.M to r(t(8.0, 12.0, 18.0), t(0.015, 0.025, 0.04)),
        IsoGroup.K to r(t(12.0, 20.0, 28.0), t(0.02, 0.04, 0.07)),
        IsoGroup.N to r(t(30.0, 60.0, 90.0), t(0.03, 0.06, 0.10)),
        IsoGroup.S to r(t(8.0, 12.0, 16.0), t(0.01, 0.02, 0.03)),
        IsoGroup.H to r(t(4.0, 6.0, 8.0), t(0.01, 0.02, 0.03)),
    )

    fun generic(group: IsoGroup, tool: ToolKind, turning: Boolean): CuttingRecommendation {
        val table = when {
            turning && tool == ToolKind.HSS -> hssTurning
            turning -> carbideTurning
            tool == ToolKind.HSS -> hssMilling
            else -> carbideMilling
        }
        return table.getValue(group)
    }
}
