package com.reymildo.calculadoradelmetal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity
import com.reymildo.calculadoradelmetal.data.local.relation.ToolWithRecommendations
import kotlinx.coroutines.flow.Flow

@Dao
interface MachiningDao {
    @Query("SELECT * FROM machining_materials ORDER BY isBuiltIn DESC, name")
    fun observeMaterials(): Flow<List<MachiningMaterialEntity>>

    @Query("SELECT * FROM machining_materials ORDER BY id")
    suspend fun listMaterials(): List<MachiningMaterialEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMaterialIfAbsent(material: MachiningMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterial(material: MachiningMaterialEntity)

    @Delete
    suspend fun deleteMaterial(material: MachiningMaterialEntity)

    @Query("DELETE FROM machining_materials")
    suspend fun deleteAllMaterials()

    @Transaction
    @Query("SELECT * FROM cutting_tools ORDER BY isBuiltIn DESC, name")
    fun observeTools(): Flow<List<ToolWithRecommendations>>

    @Query("SELECT * FROM cutting_tools ORDER BY id")
    suspend fun listTools(): List<CuttingToolEntity>

    @Query("SELECT * FROM tool_recommendations ORDER BY id")
    suspend fun listRecommendations(): List<ToolRecommendationEntity>

    @Query("SELECT COUNT(*) FROM cutting_tools WHERE isBuiltIn = 1")
    suspend fun countBuiltInTools(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTool(tool: CuttingToolEntity): Long

    @Delete
    suspend fun deleteTool(tool: CuttingToolEntity)

    @Query("DELETE FROM cutting_tools")
    suspend fun deleteAllTools()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecommendation(recommendation: ToolRecommendationEntity): Long

    @Query("DELETE FROM tool_recommendations WHERE toolId = :toolId")
    suspend fun deleteRecommendationsOf(toolId: Long)
}
