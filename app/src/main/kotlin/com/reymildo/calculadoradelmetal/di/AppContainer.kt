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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()

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

        /**
         * Los valores del fabricante pasan de "por material" a "por grupo ISO" (P, M, K, N, S, H),
         * que es como los publican los fabricantes. Los ya guardados se llevan al grupo de su
         * material; si dos materiales caían en el mismo grupo, se conserva uno.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tool_recommendations_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, toolId INTEGER NOT NULL, isoGroup TEXT NOT NULL, " +
                        "vcMin REAL NOT NULL, vcStart REAL NOT NULL, vcMax REAL NOT NULL, feedMin REAL NOT NULL, " +
                        "feedStart REAL NOT NULL, feedMax REAL NOT NULL, depthMaxMm REAL, " +
                        "FOREIGN KEY(toolId) REFERENCES cutting_tools(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO tool_recommendations_new (toolId, isoGroup, vcMin, vcStart, vcMax, feedMin, feedStart, feedMax, depthMaxMm) " +
                        "SELECT toolId, iso, vcMin, vcStart, vcMax, feedMin, feedStart, feedMax, depthMaxMm FROM (" +
                        "SELECT r.toolId AS toolId, CASE m.category " +
                        "WHEN 'STAINLESS' THEN 'M' WHEN 'CAST_IRON' THEN 'K' WHEN 'ALUMINUM' THEN 'N' " +
                        "WHEN 'COPPER_ALLOY' THEN 'N' WHEN 'PLASTIC' THEN 'N' WHEN 'TITANIUM' THEN 'S' " +
                        "WHEN 'HARDENED_STEEL' THEN 'H' ELSE 'P' END AS iso, " +
                        "r.vcMin AS vcMin, r.vcStart AS vcStart, r.vcMax AS vcMax, r.feedMin AS feedMin, " +
                        "r.feedStart AS feedStart, r.feedMax AS feedMax, r.depthMaxMm AS depthMaxMm " +
                        "FROM tool_recommendations r JOIN machining_materials m ON m.id = r.materialId " +
                        "ORDER BY r.id) GROUP BY toolId, iso",
                )
                db.execSQL("DROP TABLE tool_recommendations")
                db.execSQL("ALTER TABLE tool_recommendations_new RENAME TO tool_recommendations")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tool_recommendations_toolId ON tool_recommendations (toolId)")
            }
        }

        /** Subgrupo ISO (P1, N2…) en los materiales de mecanizado; los de fábrica reciben el suyo. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE machining_materials ADD COLUMN isoCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE machining_materials SET isoCode = 'P1' WHERE id = 'a36'")
                db.execSQL("UPDATE machining_materials SET isoCode = 'M2' WHERE id IN ('ss304', 'ss316')")
                db.execSQL("UPDATE machining_materials SET isoCode = 'N1' WHERE id IN ('al6061', 'al6063')")
            }
        }

        /**
         * Los materiales de mecanizado pasan a ser solo los seis grupos ISO (P, M, K, N, S, H), sin
         * subgrupos ni materiales sueltos. Lo ya vinculado se lleva a la letra de su familia y los
         * valores del fabricante por subgrupo (N2…) se reducen a su letra (se conserva uno por letra).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE materials SET technicalMaterialId = (SELECT CASE m.category " +
                        "WHEN 'STAINLESS' THEN 'M' WHEN 'CAST_IRON' THEN 'K' WHEN 'ALUMINUM' THEN 'N' " +
                        "WHEN 'COPPER_ALLOY' THEN 'N' WHEN 'PLASTIC' THEN 'N' WHEN 'TITANIUM' THEN 'S' " +
                        "WHEN 'HARDENED_STEEL' THEN 'H' ELSE 'P' END " +
                        "FROM machining_materials m WHERE m.id = materials.technicalMaterialId) " +
                        "WHERE technicalMaterialId IS NOT NULL",
                )
                db.execSQL("UPDATE tool_recommendations SET isoGroup = substr(isoGroup, 1, 1)")
                db.execSQL(
                    "DELETE FROM tool_recommendations WHERE id NOT IN " +
                        "(SELECT MIN(id) FROM tool_recommendations GROUP BY toolId, isoGroup)",
                )
                db.execSQL("DROP TABLE machining_materials")
            }
        }

        /**
         * Los materiales de mecanizado vuelven a ser individuales (con su dureza), cada uno con una
         * categoría ISO (P, M, K, N, S, H). Los de fábrica se reponen al arrancar; los vínculos de
         * las materias primas se restauran por nombre y las letras sueltas se descartan.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS machining_materials (" +
                        "id TEXT NOT NULL, name TEXT NOT NULL, isoGroup TEXT NOT NULL, condition TEXT NOT NULL, " +
                        "hardness TEXT NOT NULL, notes TEXT NOT NULL, isBuiltIn INTEGER NOT NULL, PRIMARY KEY(id))",
                )
                db.execSQL("UPDATE materials SET technicalMaterialId = NULL WHERE technicalMaterialId IN ('P', 'M', 'K', 'N', 'S', 'H')")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'a36' WHERE name = 'Acero negro (A36)'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'ss304' WHERE name = 'Acero inoxidable 304'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'ss316' WHERE name = 'Acero inoxidable 316'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'al6061' WHERE name = 'Aluminio 6061'")
                db.execSQL("UPDATE materials SET technicalMaterialId = 'al6063' WHERE name = 'Aluminio 6063'")
            }
        }
    }
}
