package com.reymildo.calculadoradelmetal.data.repository

import androidx.room.withTransaction
import com.reymildo.calculadoradelmetal.data.local.AppDatabase
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity
import com.reymildo.calculadoradelmetal.domain.machining.MaterialCategory
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import java.util.UUID

/** Materiales a mecanizar y herramientas de corte (con sus valores de fabricante). */
class MachiningRepository(private val db: AppDatabase) {
    private val dao = db.machiningDao()

    fun observeMaterials() = dao.observeMaterials()
    fun observeTools() = dao.observeTools()

    /** Idempotente: repone lo de fábrica sin tocar lo que el usuario haya añadido. */
    suspend fun ensureDefaults() {
        db.withTransaction {
            builtInMaterials.forEach { dao.insertMaterialIfAbsent(it) }
            if (dao.countBuiltInTools() == 0) builtInTools.forEach { dao.upsertTool(it) }
        }
    }

    suspend fun saveMaterial(material: MachiningMaterialEntity): Result<Unit> = runCatching {
        require(material.name.isNotBlank())
        val id = material.id.ifBlank { "custom_" + UUID.randomUUID().toString().take(8) }
        dao.upsertMaterial(material.copy(id = id))
    }

    suspend fun deleteMaterial(material: MachiningMaterialEntity) {
        if (!material.isBuiltIn) dao.deleteMaterial(material)
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
        val builtInMaterials = listOf(
            MachiningMaterialEntity("a36", "ASTM A36", MaterialCategory.CARBON_STEEL.name, "Laminado", "≈ 119–159 HB", isBuiltIn = true),
            MachiningMaterialEntity("ss304", "AISI 304", MaterialCategory.STAINLESS.name, "Recocido", "≤ 215 HB", isBuiltIn = true),
            MachiningMaterialEntity("ss316", "AISI 316", MaterialCategory.STAINLESS.name, "Recocido", "≤ 217 HB", isBuiltIn = true),
            MachiningMaterialEntity("al6061", "Aluminio 6061", MaterialCategory.ALUMINUM.name, "T6", "≈ 95 HB", isBuiltIn = true),
            MachiningMaterialEntity("al6063", "Aluminio 6063", MaterialCategory.ALUMINUM.name, "T6", "≈ 73 HB", isBuiltIn = true),
        )

        val builtInTools = listOf(
            CuttingToolEntity(name = "HSS genérico", kind = ToolKind.HSS.name, machineType = "BOTH", isBuiltIn = true),
            CuttingToolEntity(name = "Inserto de carburo genérico", kind = ToolKind.CARBIDE_INSERT.name, machineType = "BOTH", isBuiltIn = true),
            CuttingToolEntity(name = "Fresa de carburo genérica", kind = ToolKind.CARBIDE_ENDMILL.name, machineType = "MILL", isBuiltIn = true),
        )
    }
}
