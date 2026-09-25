package com.reymildo.calculadoradelmetal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "materials",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("supplierId")],
)
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val name: String,
    val stockShapeId: String,
    val stockDimensionsJson: String,
    val stockPrice: Double,
    val costPerVolumeCm3: Double,
    val priceConfigured: Boolean = stockPrice > 0.0,
    val currencyCode: String? = null,
    val technicalMaterialId: String? = null,
    val isBuiltIn: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)
