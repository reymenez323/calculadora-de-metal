package com.reymildo.calculadoradelmetal.data.local

import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object DimensionsCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), DimensionValue.serializer())

    fun encode(dimensions: Map<DimensionType, DimensionValue>): String {
        val byName = dimensions.mapKeys { (type, _) -> type.name }
        return json.encodeToString(serializer, byName)
    }

    fun decode(raw: String): Map<DimensionType, DimensionValue> {
        val byName = json.decodeFromString(serializer, raw)
        return byName.mapKeys { (name, _) -> DimensionType.valueOf(name) }
    }
}
