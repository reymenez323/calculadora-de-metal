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

/**
 * Material de mecanizado tal como viaja en el respaldo. Los respaldos anteriores traían una
 * "category" (STAINLESS, ALUMINUM…) en vez de la letra ISO; se traduce al leerlos.
 */
@Serializable
private data class BackupMachiningMaterial(
    val id: String,
    val name: String,
    val isoGroup: String = "",
    val category: String = "",
    val condition: String = "",
    val hardness: String = "",
    val notes: String = "",
    val isBuiltIn: Boolean = false,
) {
    fun toEntity() = MachiningMaterialEntity(
        id = id, name = name, isoGroup = resolveGroup(), condition = condition,
        hardness = hardness, notes = notes, isBuiltIn = isBuiltIn,
    )

    private fun resolveGroup(): String = when {
        isoGroup.length == 1 && isoGroup in "PMKNSH" -> isoGroup
        else -> when (category) {
            "STAINLESS" -> "M"
            "CAST_IRON" -> "K"
            "ALUMINUM", "COPPER_ALLOY", "PLASTIC" -> "N"
            "TITANIUM" -> "S"
            "HARDENED_STEEL" -> "H"
            else -> "P"
        }
    }

    companion object {
        fun of(e: MachiningMaterialEntity) = BackupMachiningMaterial(
            id = e.id, name = e.name, isoGroup = e.isoGroup, condition = e.condition,
            hardness = e.hardness, notes = e.notes, isBuiltIn = e.isBuiltIn,
        )
    }
}

@Serializable
private data class BackupPayload(
    val formatVersion: Int = 5,
    val exportedAtEpochMillis: Long,
    val suppliers: List<SupplierEntity>,
    val materials: List<MaterialEntity>,
    val machineProfiles: List<MachineProfileEntity>,
    // Ausentes en los respaldos de formato 1: se reponen con los valores de fábrica al importar.
    val machiningMaterials: List<BackupMachiningMaterial> = emptyList(),
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
                    machiningMaterials = database.machiningDao().listMaterials().map(BackupMachiningMaterial::of),
                    cuttingTools = database.machiningDao().listTools(),
                    toolRecommendations = database.machiningDao().listRecommendations(),
                ),
            )
        }
    }

    suspend fun importJson(raw: String): Result<Unit> = runCatching {
        val payload = json.decodeFromString<BackupPayload>(raw)
        require(payload.formatVersion in 1..5) { "Versión de respaldo no compatible." }
        require(payload.suppliers.isNotEmpty()) { "El respaldo no contiene proveedores." }
        val supplierIds = payload.suppliers.map { it.id }.toSet()
        require(payload.materials.all { it.supplierId in supplierIds }) { "El respaldo contiene materiales sin proveedor." }
        require(payload.suppliers.map { it.id }.distinct().size == payload.suppliers.size) { "Hay identificadores de proveedor duplicados." }
        require(payload.materials.map { it.id }.distinct().size == payload.materials.size) { "Hay identificadores de material duplicados." }
        val toolIds = payload.cuttingTools.map { it.id }.toSet()
        // Los formatos anteriores a 3 guardaban los valores por material; el 4 usaba letras con
        // subgrupo (N2). Desde el 3 van por letra ISO: los códigos con número se reducen a su letra.
        val recommendations = (if (payload.formatVersion >= 3) payload.toolRecommendations else emptyList())
            .map { it.copy(isoGroup = it.isoGroup.take(1)) }
            .filter { it.isoGroup.length == 1 && it.isoGroup in "PMKNSH" }
            .distinctBy { it.toolId to it.isoGroup }
        require(recommendations.all { it.toolId in toolIds }) { "El respaldo contiene valores de una herramienta que no existe." }
        val machiningMaterials = payload.machiningMaterials.map { it.toEntity() }
        // Un vínculo a un material de mecanizado que ya no existe (o una letra suelta) se descarta.
        val validMaterialIds = machiningMaterials.map { it.id }.toSet() + MachiningRepository.builtInMaterials.map { it.id }

        database.withTransaction {
            val machiningDao = database.machiningDao()
            database.materialDao().deleteAll()
            database.supplierDao().deleteAll()
            database.machineProfileDao().deleteAll()
            machiningDao.deleteAllTools()
            machiningDao.deleteAllMaterials()
            payload.suppliers.forEach { database.supplierDao().insert(it) }
            payload.materials.forEach {
                database.materialDao().insert(it.copy(technicalMaterialId = it.technicalMaterialId?.takeIf { id -> id in validMaterialIds }))
            }
            payload.machineProfiles.filter { payload.formatVersion >= 2 || !it.isBuiltIn }
                .forEach { database.machineProfileDao().upsert(it) }
            machiningMaterials.forEach { machiningDao.upsertMaterial(it) }
            payload.cuttingTools.forEach { machiningDao.upsertTool(it) }
            recommendations.forEach { machiningDao.upsertRecommendation(it) }
        }
        // Un respaldo antiguo no trae máquinas nuevas, materiales ni herramientas de fábrica.
        machineProfiles.ensureDefaults()
        machining.ensureDefaults()
    }
}
