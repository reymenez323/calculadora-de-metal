package com.reymildo.calculadoradelmetal.data.repository

import androidx.room.withTransaction
import com.reymildo.calculadoradelmetal.data.local.AppDatabase
import com.reymildo.calculadoradelmetal.data.local.dao.MaterialDao
import com.reymildo.calculadoradelmetal.data.local.dao.SupplierDao
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity
import com.reymildo.calculadoradelmetal.data.local.relation.SupplierWithMaterials
import com.reymildo.calculadoradelmetal.data.local.seed.DEFAULT_SUPPLIER_NAME
import com.reymildo.calculadoradelmetal.data.local.seed.DefaultDataSeeder
import kotlinx.coroutines.flow.Flow

class SupplierRepository(
    private val db: AppDatabase,
    private val supplierDao: SupplierDao,
    private val materialDao: MaterialDao,
    private val seeder: DefaultDataSeeder,
) {
    fun observeSuppliersWithMaterials(): Flow<List<SupplierWithMaterials>> =
        supplierDao.observeSuppliersWithMaterials()

    suspend fun ensureSeeded() {
        if (supplierDao.findBuiltInByName(DEFAULT_SUPPLIER_NAME) == null) {
            db.withTransaction { seeder.seed(supplierDao, materialDao) }
        }
    }

    suspend fun resetToDefaults() {
        db.withTransaction {
            supplierDao.deleteAll()
            seeder.seed(supplierDao, materialDao)
        }
    }

    suspend fun addSupplier(name: String): Long = supplierDao.insert(SupplierEntity(name = name))

    suspend fun renameSupplier(id: Long, newName: String) {
        val existing = supplierDao.getById(id) ?: return
        supplierDao.update(existing.copy(name = newName))
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) = supplierDao.delete(supplier)
}
