package com.reymildo.calculadoradelmetal.data.repository

import com.reymildo.calculadoradelmetal.data.local.dao.MachineProfileDao
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.domain.machining.MachineKind
import com.reymildo.calculadoradelmetal.domain.machining.MachineType

class MachineProfileRepository(private val dao: MachineProfileDao) {
    fun observeAll() = dao.observeAll()

    /** Un perfil genérico por cada una de las cuatro categorías, para poder calcular desde el primer uso. */
    suspend fun ensureDefaults() {
        if (dao.countBuiltIn() != 0) return
        dao.upsert(
            MachineProfileEntity(
                name = "Torno convencional genérico",
                kind = MachineKind.CONVENTIONAL.name,
                machineType = MachineType.LATHE.name,
                maxRpm = 2000.0,
                maxFeedMmMin = 1000.0,
                steppedRpmCsv = "80,125,200,315,500,800,1250,1600,2000",
                maxThreadingRpm = 500.0,
                maxPartingRpm = 1000.0,
                isBuiltIn = true,
            ),
        )
        dao.upsert(
            MachineProfileEntity(
                name = "Torno CNC genérico",
                kind = MachineKind.CNC.name,
                machineType = MachineType.LATHE.name,
                maxRpm = 4000.0,
                maxFeedMmMin = 5000.0,
                maxThreadingRpm = 2000.0,
                maxPartingRpm = 3000.0,
                isBuiltIn = true,
            ),
        )
        dao.upsert(
            MachineProfileEntity(
                name = "Fresadora convencional genérica",
                kind = MachineKind.CONVENTIONAL.name,
                machineType = MachineType.MILL.name,
                maxRpm = 4200.0,
                maxFeedMmMin = 1000.0,
                steppedRpmCsv = "65,110,190,320,540,900,1500,2500,4200",
                isBuiltIn = true,
            ),
        )
        dao.upsert(
            MachineProfileEntity(
                name = "Fresadora CNC genérica",
                kind = MachineKind.CNC.name,
                machineType = MachineType.MILL.name,
                maxRpm = 8000.0,
                maxFeedMmMin = 5000.0,
                isBuiltIn = true,
            ),
        )
    }

    suspend fun save(profile: MachineProfileEntity): Result<Long> = runCatching {
        require(profile.name.isNotBlank())
        require(profile.maxRpm.isFinite() && profile.maxRpm > 0)
        require(profile.maxFeedMmMin.isFinite() && profile.maxFeedMmMin > 0)
        dao.upsert(profile)
    }

    suspend fun delete(profile: MachineProfileEntity) {
        if (!profile.isBuiltIn) dao.delete(profile)
    }
}
