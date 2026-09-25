package com.reymildo.calculadoradelmetal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reymildo.calculadoradelmetal.data.local.dao.MachineProfileDao
import com.reymildo.calculadoradelmetal.data.local.dao.MachiningDao
import com.reymildo.calculadoradelmetal.data.local.dao.MaterialDao
import com.reymildo.calculadoradelmetal.data.local.dao.SupplierDao
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity

@Database(
    entities = [
        SupplierEntity::class,
        MaterialEntity::class,
        MachineProfileEntity::class,
        MachiningMaterialEntity::class,
        CuttingToolEntity::class,
        ToolRecommendationEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun supplierDao(): SupplierDao
    abstract fun materialDao(): MaterialDao
    abstract fun machineProfileDao(): MachineProfileDao
    abstract fun machiningDao(): MachiningDao

    companion object {
        const val DATABASE_NAME = "calculadora_de_metal.db"
    }
}
