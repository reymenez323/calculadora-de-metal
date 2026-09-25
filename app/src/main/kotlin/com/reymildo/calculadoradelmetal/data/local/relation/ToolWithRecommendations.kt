package com.reymildo.calculadoradelmetal.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity

data class ToolWithRecommendations(
    @Embedded val tool: CuttingToolEntity,
    @Relation(parentColumn = "id", entityColumn = "toolId")
    val recommendations: List<ToolRecommendationEntity>,
)
