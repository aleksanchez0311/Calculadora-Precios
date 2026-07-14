package com.example.calculadoradeprecios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculadoradeprecios.data.Product
import com.example.calculadoradeprecios.data.ProductRepository
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
}
