package cu.limitlesscode.calculadoradeprecios.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class ProductRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "calculadora_precios_db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()
    private val dao = database.productDao()

    private val EXCHANGE_RATE_KEY = doublePreferencesKey("exchange_rate_usd_to_cup")

    val productsFlow: Flow<List<Product>> = dao.getAllProducts()

    val exchangeRateFlow: Flow<Double> = appContext.dataStore.data
        .map { preferences -> preferences[EXCHANGE_RATE_KEY] ?: 1.0 }
        .distinctUntilChanged()

    suspend fun saveProduct(product: Product) {
        dao.insertProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        dao.deleteProduct(product)
    }

    suspend fun updateExchangeRate(rate: Double) {
        appContext.dataStore.edit { preferences ->
            preferences[EXCHANGE_RATE_KEY] = rate
        }
    }
}
