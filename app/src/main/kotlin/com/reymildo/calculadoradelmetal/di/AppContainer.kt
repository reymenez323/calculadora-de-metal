package com.reymildo.calculadoradelmetal.di

import android.content.Context
import androidx.room.Room
import com.reymildo.calculadoradelmetal.data.local.AppDatabase
import com.reymildo.calculadoradelmetal.data.local.seed.DefaultDataSeeder
import com.reymildo.calculadoradelmetal.data.repository.MaterialRepository
import com.reymildo.calculadoradelmetal.data.repository.SupplierRepository
import com.reymildo.calculadoradelmetal.data.settings.SettingsRepository

class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    ).build()

    private val seeder = DefaultDataSeeder()

    val supplierRepository: SupplierRepository by lazy {
        SupplierRepository(database, database.supplierDao(), database.materialDao(), seeder)
    }

    val materialRepository: MaterialRepository by lazy {
        MaterialRepository(database.materialDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.applicationContext)
    }
}
