package cu.limitlesscode.calculadoradeprecios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.data.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

enum class SortField { EQUIPO, MARCA, MODELO, TIPO, PRECIO }
enum class FilterField { TODOS, EQUIPO, MARCA, MODELO, TIPO }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductRepository(application)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortField = MutableStateFlow(SortField.MARCA)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _filterField = MutableStateFlow(FilterField.TODOS)
    val filterField: StateFlow<FilterField> = _filterField.asStateFlow()

    val exchangeRate: StateFlow<Double> = repository.exchangeRateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0)

    val products: StateFlow<List<Product>> = combine(
        repository.productsFlow, 
        _searchQuery, 
        _sortField, 
        _sortAscending, 
        _filterField
    ) { products, query, sort, ascending, filter ->
        val filtered = if (query.isBlank()) {
            products
        } else {
            products.filter { product ->
                when (filter) {
                    FilterField.TODOS -> {
                        product.equipo.contains(query, ignoreCase = true) ||
                                product.marca.contains(query, ignoreCase = true) ||
                                product.modelo.contains(query, ignoreCase = true) ||
                                product.tipo.contains(query, ignoreCase = true)
                    }
                    FilterField.EQUIPO -> product.equipo.contains(query, ignoreCase = true)
                    FilterField.MARCA -> product.marca.contains(query, ignoreCase = true)
                    FilterField.MODELO -> product.modelo.contains(query, ignoreCase = true)
                    FilterField.TIPO -> product.tipo.contains(query, ignoreCase = true)
                }
            }
        }

        val sorted = when (sort) {
            SortField.EQUIPO -> filtered.sortedBy { it.equipo.lowercase() }
            SortField.MARCA -> filtered.sortedWith(compareBy<Product> { it.marca.lowercase() }.thenBy { it.modelo.lowercase() })
            SortField.MODELO -> filtered.sortedBy { it.modelo.lowercase() }
            SortField.TIPO -> filtered.sortedBy { it.tipo.lowercase() }
            SortField.PRECIO -> filtered.sortedBy { it.precioUsd }
        }

        if (ascending) sorted else sorted.reversed()
        
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSort(field: SortField, ascending: Boolean) {
        _sortField.value = field
        _sortAscending.value = ascending
    }

    fun setFilterField(field: FilterField) {
        _filterField.value = field
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun updateExchangeRate(rate: Double) {
        viewModelScope.launch {
            repository.updateExchangeRate(rate)
        }
    }

    fun restoreBackup(backupData: cu.limitlesscode.calculadoradeprecios.data.BackupData) {
        viewModelScope.launch {
            repository.deleteAllProducts()
            backupData.products.forEach { repository.saveProduct(it) }
            repository.updateExchangeRate(backupData.exchangeRate)
        }
    }

    // ---------- Cola de productos para compartir secuencialmente ----------

    private val _sharingQueue = MutableStateFlow<List<Product>>(emptyList())
    val sharingQueue: StateFlow<List<Product>> = _sharingQueue.asStateFlow()

    private val _isSharingProcessActive = MutableStateFlow(false)

    /**
     * Inicia el proceso de envío múltiple.
     * Almacena los productos seleccionados en una cola para enviarlos uno por uno.
     */
    fun startMultipleSharing(selectedProducts: List<Product>) {
        if (selectedProducts.isEmpty()) return
        _sharingQueue.value = selectedProducts
        _isSharingProcessActive.value = true
    }

    /**
     * Toma y remueve el siguiente producto de la cola.
     * Retorna null si la cola está vacía (proceso terminado).
     */
    fun consumeNextProduct(): Product? {
        val queue = _sharingQueue.value
        if (queue.isEmpty()) {
            _isSharingProcessActive.value = false
            return null
        }
        val next = queue.first()
        _sharingQueue.value = queue.drop(1)
        return next
    }

    /**
     * Indica si aún hay productos pendientes por compartir en la cola.
     */
    fun isSharingActive(): Boolean = _isSharingProcessActive.value

    /**
     * Cancela el proceso de compartir y vacía la cola.
     */
    fun cancelSharing() {
        _sharingQueue.value = emptyList()
        _isSharingProcessActive.value = false
    }
}
