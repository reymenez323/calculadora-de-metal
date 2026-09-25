package com.reymildo.calculadoradelmetal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reymildo.calculadoradelmetal.domain.machining.MaterialCategory
import kotlinx.serialization.Serializable

/**
 * Material a mecanizar. Solo guarda lo que importa para el maquinado (familia, estado, dureza);
 * no lleva costo, dimensiones ni proveedor. El id es texto para que los materiales de fábrica
 * conserven sus claves históricas ("a36", "ss304"…).
 */
@Serializable
@Entity(tableName = "machining_materials")
data class MachiningMaterialEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val condition: String = "",
    val hardness: String = "",
    val notes: String = "",
    val isBuiltIn: Boolean = false,
) {
    val materialCategory: MaterialCategory
        get() = runCatching { MaterialCategory.valueOf(category) }.getOrDefault(MaterialCategory.CARBON_STEEL)
}
