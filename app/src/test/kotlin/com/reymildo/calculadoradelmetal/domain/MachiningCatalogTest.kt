package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.repository.MachiningRepository
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.MachiningCatalog
import com.reymildo.calculadoradelmetal.domain.machining.IsoGroup
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MachiningCatalogTest {
    @Test
    fun `every category has ordered generic ranges for every tool kind and operation family`() {
        for (group in IsoGroup.entries) for (kind in ToolKind.entries) for (turning in listOf(true, false)) {
            val r = MachiningCatalog.generic(group, kind, turning)
            assertTrue("$group $kind", r.cuttingSpeedMinMMin <= r.cuttingSpeedStartMMin && r.cuttingSpeedStartMMin <= r.cuttingSpeedMaxMMin)
            assertTrue("$group $kind", r.feedMinMm <= r.feedStartMm && r.feedStartMm <= r.feedMaxMm)
            assertTrue(r.cuttingSpeedMinMMin > 0 && r.feedMinMm > 0)
        }
    }

    @Test
    fun `hss generic values are more conservative than carbide`() {
        for (group in IsoGroup.entries) for (turning in listOf(true, false)) {
            val hss = MachiningCatalog.generic(group, ToolKind.HSS, turning)
            val carbide = MachiningCatalog.generic(group, ToolKind.CARBIDE_INSERT, turning)
            assertTrue("$group", hss.cuttingSpeedStartMMin < carbide.cuttingSpeedStartMMin)
        }
    }

    @Test
    fun `built-in tool defaults keep both generic profiles and separate lathe from mill`() {
        val names = MachiningRepository.builtInTools.map { it.name }
        assertTrue(names.contains("Inserto de carburo genérico"))
        assertFalse(names.any { it.contains("plaquita", ignoreCase = true) })
        val endMill = MachiningRepository.builtInTools.first { it.toolKind == ToolKind.CARBIDE_ENDMILL }
        assertFalse(endMill.fits(MachineType.LATHE))
        assertTrue(endMill.fits(MachineType.MILL))
        val hss = MachiningRepository.builtInTools.first { it.toolKind == ToolKind.HSS }
        assertTrue(hss.fits(MachineType.LATHE) && hss.fits(MachineType.MILL))
    }

    @Test
    fun `built-in materials cover every ISO category and keep the historic ids`() {
        val materials = MachiningRepository.builtInMaterials
        assertEquals(IsoGroup.entries.toSet(), materials.map { it.group }.toSet())
        assertTrue(materials.map { it.id }.containsAll(listOf("a36", "ss304", "ss316", "al6061", "al6063")))
        assertTrue(materials.all { it.isBuiltIn && it.name.isNotBlank() })
    }

    @Test
    fun `user tool is bound to a single machine type`() {
        val insert = CuttingToolEntity(name = "CNMG", kind = ToolKind.CARBIDE_INSERT.name, machineType = MachineType.LATHE.name)
        assertTrue(insert.fits(MachineType.LATHE))
        assertFalse(insert.fits(MachineType.MILL))
    }
}
