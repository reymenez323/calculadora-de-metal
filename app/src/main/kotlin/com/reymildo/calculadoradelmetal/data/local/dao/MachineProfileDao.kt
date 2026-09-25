package com.reymildo.calculadoradelmetal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineProfileDao {
    @Query("SELECT * FROM machine_profiles ORDER BY isBuiltIn DESC, name")
    fun observeAll(): Flow<List<MachineProfileEntity>>

    @Query("SELECT * FROM machine_profiles ORDER BY id")
    suspend fun listAll(): List<MachineProfileEntity>

    @Query("SELECT COUNT(*) FROM machine_profiles")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM machine_profiles WHERE isBuiltIn = 1")
    suspend fun countBuiltIn(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: MachineProfileEntity): Long

    @Delete
    suspend fun delete(profile: MachineProfileEntity)

    @Query("DELETE FROM machine_profiles")
    suspend fun deleteAll()
}
