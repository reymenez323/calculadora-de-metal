package com.reymildo.calculadoradelmetal.data.local.seed

import com.reymildo.calculadoradelmetal.data.local.DimensionsCodec
import com.reymildo.calculadoradelmetal.data.local.dao.MaterialDao
import com.reymildo.calculadoradelmetal.data.local.dao.SupplierDao
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.domain.model.Shape

const val DEFAULT_SUPPLIER_NAME = "Predeterminado"

class DefaultDataSeeder {

    private data class SeedMaterial(
        val name: String,
        val shape: Shape,
        val dimensions: Map<DimensionType, DimensionValue>,
        val technicalMaterialId: String? = null,
    )

    private val seedMaterials = listOf(
        SeedMaterial(
            name = "Acero negro (A36)",
            shape = Shape.RoundBar,
            dimensions = mapOf(
                DimensionType.DIAMETER to DimensionValue(1.0, LengthUnit.IN),
                DimensionType.LENGTH to DimensionValue(20.0, LengthUnit.FT),
            ),
            technicalMaterialId = "a36",
        ),
        SeedMaterial(
            name = "Acero inoxidable 304",
            shape = Shape.Plate,
            dimensions = mapOf(
                DimensionType.WIDTH to DimensionValue(4.0, LengthUnit.FT),
                DimensionType.LENGTH to DimensionValue(8.0, LengthUnit.FT),
                DimensionType.THICKNESS to DimensionValue(0.125, LengthUnit.IN),
            ),
            technicalMaterialId = "ss304",
        ),
        SeedMaterial(
            name = "Acero inoxidable 316",
            shape = Shape.Plate,
            dimensions = mapOf(
                DimensionType.WIDTH to DimensionValue(4.0, LengthUnit.FT),
                DimensionType.LENGTH to DimensionValue(8.0, LengthUnit.FT),
                DimensionType.THICKNESS to DimensionValue(0.125, LengthUnit.IN),
            ),
            technicalMaterialId = "ss316",
        ),
        SeedMaterial(
            name = "Aluminio 6061",
            shape = Shape.Plate,
            dimensions = mapOf(
                DimensionType.WIDTH to DimensionValue(4.0, LengthUnit.FT),
                DimensionType.LENGTH to DimensionValue(8.0, LengthUnit.FT),
                DimensionType.THICKNESS to DimensionValue(0.25, LengthUnit.IN),
            ),
            technicalMaterialId = "al6061",
        ),
        SeedMaterial(
            name = "Aluminio 6063",
            shape = Shape.RoundBar,
            dimensions = mapOf(
                DimensionType.DIAMETER to DimensionValue(1.0, LengthUnit.IN),
                DimensionType.LENGTH to DimensionValue(12.0, LengthUnit.FT),
            ),
            technicalMaterialId = "al6063",
        ),
        SeedMaterial(
            name = "Cobre",
            shape = Shape.RoundBar,
            dimensions = mapOf(
                DimensionType.DIAMETER to DimensionValue(0.5, LengthUnit.IN),
                DimensionType.LENGTH to DimensionValue(10.0, LengthUnit.FT),
            ),
        ),
        SeedMaterial(
            name = "Bronce",
            shape = Shape.RoundBar,
            dimensions = mapOf(
                DimensionType.DIAMETER to DimensionValue(1.0, LengthUnit.IN),
                DimensionType.LENGTH to DimensionValue(12.0, LengthUnit.FT),
            ),
        ),
        SeedMaterial(
            name = "Acero galvanizado",
            shape = Shape.Plate,
            dimensions = mapOf(
                DimensionType.WIDTH to DimensionValue(4.0, LengthUnit.FT),
                DimensionType.LENGTH to DimensionValue(8.0, LengthUnit.FT),
                DimensionType.THICKNESS to DimensionValue(0.06, LengthUnit.IN),
            ),
        ),
    )

    suspend fun seed(supplierDao: SupplierDao, materialDao: MaterialDao) {
        val supplierId = supplierDao.insert(
            SupplierEntity(name = DEFAULT_SUPPLIER_NAME, isBuiltIn = true, sortOrder = 0),
        )

        seedMaterials.forEach { seedMaterial ->
            materialDao.insert(
                MaterialEntity(
                    supplierId = supplierId,
                    name = seedMaterial.name,
                    stockShapeId = seedMaterial.shape.id,
                    stockDimensionsJson = DimensionsCodec.encode(seedMaterial.dimensions),
                    stockPrice = 0.0,
                    costPerVolumeCm3 = 0.0,
                    technicalMaterialId = seedMaterial.technicalMaterialId,
                    isBuiltIn = true,
                ),
            )
        }
    }
}
