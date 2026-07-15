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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductRepository(application)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val exchangeRate: StateFlow<Double> = repository.exchangeRateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0)

    val products: StateFlow<List<Product>> = combine(repository.productsFlow, _searchQuery) { products, query ->
        if (query.isBlank()) {
            products
        } else {
            products.filter { product ->
                product.equipo.contains(query, ignoreCase = true) ||
                    product.marca.contains(query, ignoreCase = true) ||
                    product.modelo.contains(query, ignoreCase = true) ||
                    product.tipo.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
