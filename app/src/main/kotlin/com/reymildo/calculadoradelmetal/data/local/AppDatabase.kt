package com.reymildo.calculadoradelmetal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reymildo.calculadoradelmetal.data.local.dao.MaterialDao
import com.reymildo.calculadoradelmetal.data.local.dao.SupplierDao
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity

@Database(
    entities = [SupplierEntity::class, MaterialEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun supplierDao(): SupplierDao
    abstract fun materialDao(): MaterialDao

    companion object {
        const val DATABASE_NAME = "calculadora_de_metal.db"
    }
}
