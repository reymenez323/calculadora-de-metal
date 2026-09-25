package com.reymildo.calculadoradelmetal.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.reymildo.calculadoradelmetal.data.local.AppDatabase
import com.reymildo.calculadoradelmetal.data.local.seed.DefaultDataSeeder
import com.reymildo.calculadoradelmetal.data.repository.BackupRepository
import com.reymildo.calculadoradelmetal.data.repository.MachineProfileRepository
import com.reymildo.calculadoradelmetal.data.repository.MachiningRepository
import com.reymildo.calculadoradelmetal.data.repository.MaterialRepository
import com.reymildo.calculadoradelmetal.data.repository.SupplierRepository
import com.reymildo.calculadoradelmetal.data.settings.SettingsRepository

class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

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

    val machineProfileRepository: MachineProfileRepository by lazy {
        MachineProfileRepository(database.machineProfileDao())
    }

    val machiningRepository: MachiningRepository by lazy { MachiningRepository(database) }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(database, machineProfileRepository, machiningRepository)
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE materials ADD COLUMN priceConfigured INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE materials ADD COLUMN currencyCode TEXT")
                db.execSQL("ALTER TABLE materials ADD COLUMN technicalMaterialId TEXT")
                db.execSQL("UPDATE materials SET priceConfigured = CASE WHEN stockPrice > 0 THEN 1 ELSE 0 END")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'a36' WHERE name = 'Acero negro (A36)'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'ss304' WHERE name = 'Acero inoxidable 304'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'ss316' WHERE name = 'Acero inoxidable 316'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'al6061' WHERE name = 'Aluminio 6061'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'al6063' WHERE name = 'Aluminio 6063'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS machine_profiles (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, kind TEXT NOT NULL, maxRpm REAL NOT NULL, " +
                        "maxFeedMmMin REAL NOT NULL, steppedRpmCsv TEXT NOT NULL, " +
                        "maxThreadingRpm REAL, maxPartingRpm REAL, isBuiltIn INTEGER NOT NULL)",
                )
            }
        }

        /**
         * Máquinas con tipo (torno/fresadora) y ficha técnica; materiales de mecanizado y
         * herramientas con valores de fabricante. Los perfiles de máquina de fábrica de la
         * versión anterior mezclaban torno y fresadora, así que se borran y se reponen por
         * separado al arrancar; las máquinas creadas por el usuario se conservan como tornos
         * (el tipo por defecto) y se pueden reclasificar desde Taller.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM machine_profiles WHERE isBuiltIn = 1")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN machineType TEXT NOT NULL DEFAULT 'LATHE'")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN model TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN minRpm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN powerKw REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN swingMm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN centersDistanceMm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN chuckMm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN spindleBoreMm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN travelXMm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN travelYMm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN travelZMm REAL")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN spindleTaper TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN axes INTEGER")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN toolCapacity INTEGER")
                db.execSQL("ALTER TABLE machine_profiles ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS machining_materials (" +
                        "id TEXT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, condition TEXT NOT NULL, " +
                        "hardness TEXT NOT NULL, notes TEXT NOT NULL, isBuiltIn INTEGER NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cutting_tools (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, kind TEXT NOT NULL, " +
                        "machineType TEXT NOT NULL, brand TEXT NOT NULL, code TEXT NOT NULL, coating TEXT NOT NULL, " +
                        "notes TEXT NOT NULL, isBuiltIn INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tool_recommendations (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, toolId INTEGER NOT NULL, materialId TEXT NOT NULL, " +
                        "vcMin REAL NOT NULL, vcStart REAL NOT NULL, vcMax REAL NOT NULL, feedMin REAL NOT NULL, " +
                        "feedStart REAL NOT NULL, feedMax REAL NOT NULL, depthMaxMm REAL, " +
                        "FOREIGN KEY(toolId) REFERENCES cutting_tools(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(materialId) REFERENCES machining_materials(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tool_recommendations_toolId ON tool_recommendations (toolId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tool_recommendations_materialId ON tool_recommendations (materialId)")
            }
        }
    }
}
