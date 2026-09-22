package com.reymildo.calculadoradelmetal.data.repository

import com.reymildo.calculadoradelmetal.data.local.DimensionsCodec
import com.reymildo.calculadoradelmetal.data.local.dao.MaterialDao
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.domain.calculation.StockPricing
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.Shape
import kotlinx.coroutines.flow.Flow

class MaterialRepository(private val materialDao: MaterialDao) {

    fun observeBySupplier(supplierId: Long): Flow<List<MaterialEntity>> =
        materialDao.observeBySupplier(supplierId)

    fun observeAll(): Flow<List<MaterialEntity>> = materialDao.observeAll()

    suspend fun addMaterial(
        supplierId: Long,
        name: String,
        stockShape: Shape,
        stockDimensions: Map<DimensionType, DimensionValue>,
        stockPrice: Double,
    ): Result<Long> {
        val costPerVolumeCm3 = StockPricing.costPerVolumeCm3(stockShape, stockDimensions, stockPrice)
            .getOrElse { return Result.failure(it) }

        val id = materialDao.insert(
            MaterialEntity(
                supplierId = supplierId,
                name = name,
                stockShapeId = stockShape.id,
                stockDimensionsJson = DimensionsCodec.encode(stockDimensions),
                stockPrice = stockPrice,
                costPerVolumeCm3 = costPerVolumeCm3,
            ),
        )
        return Result.success(id)
    }

    suspend fun updateMaterial(
        existing: MaterialEntity,
        name: String,
        stockShape: Shape,
        stockDimensions: Map<DimensionType, DimensionValue>,
        stockPrice: Double,
    ): Result<Unit> {
        val costPerVolumeCm3 = StockPricing.costPerVolumeCm3(stockShape, stockDimensions, stockPrice)
            .getOrElse { return Result.failure(it) }

        materialDao.update(
            existing.copy(
                name = name,
                stockShapeId = stockShape.id,
                stockDimensionsJson = DimensionsCodec.encode(stockDimensions),
                stockPrice = stockPrice,
                costPerVolumeCm3 = costPerVolumeCm3,
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return Result.success(Unit)
    }

    suspend fun deleteMaterial(material: MaterialEntity) = materialDao.delete(material)
}
