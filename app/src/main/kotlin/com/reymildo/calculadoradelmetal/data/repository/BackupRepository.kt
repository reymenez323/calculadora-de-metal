package com.reymildo.calculadoradelmetal.data.repository

import androidx.room.withTransaction
import com.reymildo.calculadoradelmetal.data.local.AppDatabase
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Solo para leer respaldos antiguos: los materiales de mecanizado ya no existen (ahora son las letras ISO). */
@Serializable
private data class LegacyMachiningMaterial(val id: String, val category: String = "")

private val LEGACY_TECHNICAL_IDS = mapOf("a36" to "P", "ss304" to "M", "ss316" to "M", "al6061" to "N", "al6063" to "N")

private fun legacyGroup(id: String?, categories: Map<String, String>): String? = when {
    id == null -> null
    id.length == 1 && id in "PMKNSH" -> id
    id in LEGACY_TECHNICAL_IDS -> LEGACY_TECHNICAL_IDS.getValue(id)
    else -> when (categories[id]) {
        "STAINLESS" -> "M"
        "CAST_IRON" -> "K"
        "ALUMINUM", "COPPER_ALLOY", "PLASTIC" -> "N"
        "TITANIUM" -> "S"
        "HARDENED_STEEL" -> "H"
        null -> null
        else -> "P"
    }
}

@Serializable
private data class BackupPayload(
    val formatVersion: Int = 4,
    val exportedAtEpochMillis: Long,
    val suppliers: List<SupplierEntity>,
    val materials: List<MaterialEntity>,
    val machineProfiles: List<MachineProfileEntity>,
    // Ausentes en los respaldos de formato 1: se reponen con los valores de fábrica al importar.
    val machiningMaterials: List<LegacyMachiningMaterial> = emptyList(),
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
                    cuttingTools = database.machiningDao().listTools(),
                    toolRecommendations = database.machiningDao().listRecommendations(),
                ),
            )
        }
    }

    suspend fun importJson(raw: String): Result<Unit> = runCatching {
        val payload = json.decodeFromString<BackupPayload>(raw)
        require(payload.formatVersion in 1..4) { "Versión de respaldo no compatible." }
        require(payload.suppliers.isNotEmpty()) { "El respaldo no contiene proveedores." }
        val supplierIds = payload.suppliers.map { it.id }.toSet()
        require(payload.materials.all { it.supplierId in supplierIds }) { "El respaldo contiene materiales sin proveedor." }
        require(payload.suppliers.map { it.id }.distinct().size == payload.suppliers.size) { "Hay identificadores de proveedor duplicados." }
        require(payload.materials.map { it.id }.distinct().size == payload.materials.size) { "Hay identificadores de material duplicados." }
        val toolIds = payload.cuttingTools.map { it.id }.toSet()
        // Los formatos anteriores a 3 guardaban los valores por material, no por grupo ISO: se descartan.
        // De 3 en adelante los valores van por grupo ISO; los códigos con número (N2) se reducen a su letra.
        val recommendations = (if (payload.formatVersion >= 3) payload.toolRecommendations else emptyList())
            .map { it.copy(isoGroup = it.isoGroup.take(1)) }
            .filter { it.isoGroup.length == 1 && it.isoGroup in "PMKNSH" }
            .distinctBy { it.toolId to it.isoGroup }
        val categories = payload.machiningMaterials.associate { it.id to it.category }
        require(recommendations.all { it.toolId in toolIds }) { "El respaldo contiene valores de una herramienta que no existe." }

        database.withTransaction {
            val machiningDao = database.machiningDao()
            database.materialDao().deleteAll()
            database.supplierDao().deleteAll()
            database.machineProfileDao().deleteAll()
            machiningDao.deleteAllTools()
            payload.suppliers.forEach { database.supplierDao().insert(it) }
            payload.materials.forEach {
                database.materialDao().insert(it.copy(technicalMaterialId = legacyGroup(it.technicalMaterialId, categories)))
            }
            payload.machineProfiles.filter { payload.formatVersion >= 2 || !it.isBuiltIn }
                .forEach { database.machineProfileDao().upsert(it) }
            payload.cuttingTools.forEach { machiningDao.upsertTool(it) }
            recommendations.forEach { machiningDao.upsertRecommendation(it) }
        }
        // Un respaldo antiguo (formato 1) no trae máquinas nuevas, materiales ni herramientas.
        machineProfiles.ensureDefaults()
        machining.ensureDefaults()
    }
}
