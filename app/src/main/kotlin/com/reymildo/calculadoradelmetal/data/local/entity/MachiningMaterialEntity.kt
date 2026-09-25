package com.reymildo.calculadoradelmetal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reymildo.calculadoradelmetal.domain.machining.IsoGroup
import kotlinx.serialization.Serializable

/**
 * Material a mecanizar. Solo guarda lo que importa para el maquinado (categoría ISO, estado,
 * dureza); no lleva costo, dimensiones ni proveedor. El id es texto para que los materiales de
 * fábrica conserven sus claves históricas ("a36", "ss304"…).
 *
 * La categoría ISO (P, M, K, N, S, H) decide qué valores del fabricante de un inserto se aplican.
 */
@Serializable
@Entity(tableName = "machining_materials")
data class MachiningMaterialEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isoGroup: String = IsoGroup.P.name,
    val condition: String = "",
    val hardness: String = "",
    val notes: String = "",
    val isBuiltIn: Boolean = false,
) {
    val group: IsoGroup
        get() = IsoGroup.entries.firstOrNull { it.name == isoGroup } ?: IsoGroup.P
}
