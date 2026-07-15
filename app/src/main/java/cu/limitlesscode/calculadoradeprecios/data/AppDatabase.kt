package cu.limitlesscode.calculadoradeprecios.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración manual 1 → 2:
 * Añade las columnas imageUrl, garantia, colores e infoAdicional con valor por defecto vacío.
 * Se usa en lugar de AutoMigration para no requerir el archivo schemas/1.json en el CI.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE products ADD COLUMN garantia TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE products ADD COLUMN colores TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE products ADD COLUMN infoAdicional TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(entities = [Product::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
