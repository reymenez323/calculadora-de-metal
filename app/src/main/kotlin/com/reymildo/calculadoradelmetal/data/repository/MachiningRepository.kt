package com.reymildo.calculadoradelmetal.data.repository

import androidx.room.withTransaction
import com.reymildo.calculadoradelmetal.data.local.AppDatabase
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind

/** Herramientas de corte con sus valores de fabricante por grupo ISO. */
class MachiningRepository(private val db: AppDatabase) {
    private val dao = db.machiningDao()

    fun observeTools() = dao.observeTools()

    /** Idempotente: repone lo de fábrica sin tocar lo que el usuario haya añadido. */
    suspend fun ensureDefaults() {
        db.withTransaction {
            if (dao.countBuiltInTools() == 0) builtInTools.forEach { dao.upsertTool(it) }
        }
    }

    /** Guarda la herramienta y reemplaza en bloque sus valores de fabricante. */
    suspend fun saveTool(tool: CuttingToolEntity, recommendations: List<ToolRecommendationEntity>): Result<Long> = runCatching {
        require(tool.name.isNotBlank())
        require(!tool.isBuiltIn)
        db.withTransaction {
            val id = dao.upsertTool(tool).let { if (tool.id != 0L) tool.id else it }
            dao.deleteRecommendationsOf(id)
            recommendations.forEach { dao.upsertRecommendation(it.copy(id = 0, toolId = id)) }
            id
        }
    }

    suspend fun deleteTool(tool: CuttingToolEntity) {
        if (!tool.isBuiltIn) dao.deleteTool(tool)
    }

    companion object {
        val builtInTools = listOf(
            CuttingToolEntity(name = "HSS genérico", kind = ToolKind.HSS.name, machineType = "BOTH", isBuiltIn = true),
            CuttingToolEntity(name = "Inserto de carburo genérico", kind = ToolKind.CARBIDE_INSERT.name, machineType = "BOTH", isBuiltIn = true),
            CuttingToolEntity(name = "Fresa de carburo genérica", kind = ToolKind.CARBIDE_ENDMILL.name, machineType = "MILL", isBuiltIn = true),
        )
    }
}
