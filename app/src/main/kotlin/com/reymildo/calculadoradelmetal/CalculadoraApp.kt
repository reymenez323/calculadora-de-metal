package com.reymildo.calculadoradelmetal

import android.app.Application
import com.reymildo.calculadoradelmetal.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalculadoraApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            container.supplierRepository.ensureSeeded()
            container.machineProfileRepository.ensureDefaults()
            container.machiningRepository.ensureDefaults()
        }
    }
}
