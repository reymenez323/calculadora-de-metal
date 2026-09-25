package com.reymildo.calculadoradelmetal.data.repository

import androidx.room.withTransaction
import com.reymildo.calculadoradelmetal.data.local.AppDatabase
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class BackupPayload(
    val formatVersion: Int = 2,
    val exportedAtEpochMillis: Long,
    val suppliers: List<SupplierEntity>,
    val materials: List<MaterialEntity>,
    val machineProfiles: List<MachineProfileEntity>,
    // Ausentes en los respaldos de formato 1: se reponen con los valores de fábrica al importar.
    val machiningMaterials: List<MachiningMaterialEntity> = emptyList(),
    val cuttingTools: List<CuttingToolEntity> = emptyList(),
    val toolRecommendations: List<ToolRecommendationEntity> = emptyList(),
)

class BackupRepository(
    private val database: AppDatabase,
    private val machineProfiles: MachineProfileRepository,
    private val machining: MachiningRepository,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportJson(): Result<String> = runCatching {
        database.withTransaction {
            json.encodeToString(
                BackupPayload(
                    exportedAtEpochMillis = System.currentTimeMillis(),
                    suppliers = database.supplierDao().listAll(),
                    materials = database.materialDao().listAll(),
                    machineProfiles = database.machineProfileDao().listAll(),
                    machiningMaterials = database.machiningDao().listMaterials(),
                    cuttingTools = database.machiningDao().listTools(),
                    toolRecommendations = database.machiningDao().listRecommendations(),
                ),
            )
        }
    }

    suspend fun importJson(raw: String): Result<Unit> = runCatching {
        val payload = json.decodeFromString<BackupPayload>(raw)
        require(payload.formatVersion in 1..2) { "Versión de respaldo no compatible." }
        require(payload.suppliers.isNotEmpty()) { "El respaldo no contiene proveedores." }
        val supplierIds = payload.suppliers.map { it.id }.toSet()
        require(payload.materials.all { it.supplierId in supplierIds }) { "El respaldo contiene materiales sin proveedor." }
        require(payload.suppliers.map { it.id }.distinct().size == payload.suppliers.size) { "Hay identificadores de proveedor duplicados." }
        require(payload.materials.map { it.id }.distinct().size == payload.materials.size) { "Hay identificadores de material duplicados." }
        val toolIds = payload.cuttingTools.map { it.id }.toSet()
        val machiningMaterialIds = payload.machiningMaterials.map { it.id }.toSet()
        require(payload.toolRecommendations.all { it.toolId in toolIds && it.materialId in machiningMaterialIds }) {
            "El respaldo contiene valores de herramienta sin material o herramienta."
        }

        database.withTransaction {
            val machiningDao = database.machiningDao()
            database.materialDao().deleteAll()
            database.supplierDao().deleteAll()
            database.machineProfileDao().deleteAll()
            machiningDao.deleteAllTools()
            machiningDao.deleteAllMaterials()
            payload.suppliers.forEach { database.supplierDao().insert(it) }
            payload.materials.forEach { database.materialDao().insert(it) }
            payload.machineProfiles.filter { payload.formatVersion >= 2 || !it.isBuiltIn }
                .forEach { database.machineProfileDao().upsert(it) }
            payload.machiningMaterials.forEach { machiningDao.upsertMaterial(it) }
            payload.cuttingTools.forEach { machiningDao.upsertTool(it) }
            payload.toolRecommendations.forEach { machiningDao.upsertRecommendation(it) }
        }
        // Un respaldo antiguo (formato 1) no trae máquinas nuevas, materiales ni herramientas.
        machineProfiles.ensureDefaults()
        machining.ensureDefaults()
    }
}
