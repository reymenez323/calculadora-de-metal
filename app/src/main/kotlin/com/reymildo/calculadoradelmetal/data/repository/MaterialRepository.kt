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
        stockPrice: Double?,
        technicalMaterialId: String? = null,
        currencyCode: String? = null,
    ): Result<Long> {
        val storedPrice = stockPrice ?: 0.0
        val costPerVolumeCm3 = StockPricing.costPerVolumeCm3(stockShape, stockDimensions, storedPrice)
            .getOrElse { return Result.failure(it) }

        val id = materialDao.insert(
            MaterialEntity(
                supplierId = supplierId,
                name = name,
                stockShapeId = stockShape.id,
                stockDimensionsJson = DimensionsCodec.encode(stockDimensions),
                stockPrice = storedPrice,
                costPerVolumeCm3 = costPerVolumeCm3,
                priceConfigured = stockPrice != null,
                technicalMaterialId = technicalMaterialId,
                currencyCode = currencyCode,
            ),
        )
        return Result.success(id)
    }

    suspend fun updateMaterial(
        existing: MaterialEntity,
        name: String,
        stockShape: Shape,
        stockDimensions: Map<DimensionType, DimensionValue>,
        stockPrice: Double?,
        technicalMaterialId: String? = existing.technicalMaterialId,
        currencyCode: String? = existing.currencyCode,
    ): Result<Unit> {
        val storedPrice = stockPrice ?: 0.0
        val costPerVolumeCm3 = StockPricing.costPerVolumeCm3(stockShape, stockDimensions, storedPrice)
            .getOrElse { return Result.failure(it) }

        materialDao.update(
            existing.copy(
                name = name,
                stockShapeId = stockShape.id,
                stockDimensionsJson = DimensionsCodec.encode(stockDimensions),
                stockPrice = storedPrice,
                costPerVolumeCm3 = costPerVolumeCm3,
                priceConfigured = stockPrice != null,
                technicalMaterialId = technicalMaterialId,
                currencyCode = currencyCode,
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return Result.success(Unit)
    }

    suspend fun deleteMaterial(material: MaterialEntity) = materialDao.delete(material)
}
