package com.reymildo.calculadoradelmetal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity
import com.reymildo.calculadoradelmetal.data.local.relation.SupplierWithMaterials
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY sortOrder, name")
    suspend fun listAll(): List<SupplierEntity>

    @Transaction
    @Query("SELECT * FROM suppliers ORDER BY sortOrder, name")
    fun observeSuppliersWithMaterials(): Flow<List<SupplierWithMaterials>>

    @Query("SELECT * FROM suppliers WHERE isBuiltIn = 1 AND name = :name LIMIT 1")
    suspend fun findBuiltInByName(name: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SupplierEntity?

    @Insert
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Delete
    suspend fun delete(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers")
    suspend fun deleteAll()
}
