package com.reymildo.calculadoradelmetal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE supplierId = :supplierId ORDER BY name")
    fun observeBySupplier(supplierId: Long): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials ORDER BY name")
    fun observeAll(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials ORDER BY id")
    suspend fun listAll(): List<MaterialEntity>

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MaterialEntity?

    @Insert
    suspend fun insert(material: MaterialEntity): Long

    @Update
    suspend fun update(material: MaterialEntity)

    @Delete
    suspend fun delete(material: MaterialEntity)

    @Query("DELETE FROM materials")
    suspend fun deleteAll()
}
