package com.reymildo.calculadoradelmetal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.reymildo.calculadoradelmetal.domain.machining.IsoGroup
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import kotlinx.serialization.Serializable

/**
 * Herramienta de corte (HSS, inserto de carburo, fresa de carburo). [machineType] puede ser
 * "BOTH" solo en las herramientas genéricas de fábrica.
 */
@Serializable
@Entity(tableName = "cutting_tools")
data class CuttingToolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val machineType: String,
    val brand: String = "",
    val code: String = "",
    val coating: String = "",
    val notes: String = "",
    val isBuiltIn: Boolean = false,
) {
    val toolKind: ToolKind
        get() = runCatching { ToolKind.valueOf(kind) }.getOrDefault(ToolKind.CARBIDE_INSERT)

    fun fits(type: MachineType): Boolean = machineType == "BOTH" || machineType == type.name
}

/**
 * Valores recomendados por el fabricante para una herramienta en un grupo ISO. Los fabricantes no
 * dan datos por material concreto sino por grupo (P, M, K, N, S, H).
 */
@Serializable
@Entity(
    tableName = "tool_recommendations",
    foreignKeys = [
        ForeignKey(entity = CuttingToolEntity::class, parentColumns = ["id"], childColumns = ["toolId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("toolId")],
)
data class ToolRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolId: Long,
    val isoGroup: String = IsoGroup.P.name,
    val vcMin: Double,
    val vcStart: Double,
    val vcMax: Double,
    val feedMin: Double,
    val feedStart: Double,
    val feedMax: Double,
    val depthMaxMm: Double? = null,
)
