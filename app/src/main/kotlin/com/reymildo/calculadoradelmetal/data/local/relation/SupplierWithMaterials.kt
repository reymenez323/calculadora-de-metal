package com.reymildo.calculadoradelmetal.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity

data class SupplierWithMaterials(
    @Embedded val supplier: SupplierEntity,
    @Relation(parentColumn = "id", entityColumn = "supplierId")
    val materials: List<MaterialEntity>,
)
