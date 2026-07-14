@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.calculadoradeprecios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

sealed class Screen(val route: String, val title: String) {
    object Calculator : Screen("calculator", "Calculadora")
    object Management : Screen("management", "Administración")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculadoraPreciosApp()
        }
    }
}

@Composable
fun CalculadoraPreciosApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val exchangeRate by viewModel.exchangeRate.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val LightColors = androidx.compose.material3.lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF0D47A1),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001D35),
        secondary = androidx.compose.ui.graphics.Color(0xFF1976D2),
        onSecondary = androidx.compose.ui.graphics.Color.White,
        background = androidx.compose.ui.graphics.Color(0xFFF8F9FA),
        surface = androidx.compose.ui.graphics.Color.White,
        onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C1E)
    )

    MaterialTheme(colorScheme = LightColors) {
        Scaffold(
            topBar = { TopAppBar(
                title = { Text("Calculadora de Precios", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) },
            bottomBar = { BottomNavigationBar(navController) }
        ) { paddingValues ->
            Box(modifier = Modifier
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
            ) {
                NavHost(navController = navController, startDestination = Screen.Calculator.route) {
                    composable(Screen.Calculator.route) {
                        CalculatorScreen(
                            exchangeRate = exchangeRate,
                            products = products,
                            searchQuery = searchQuery,
                            onExchangeRateChange = { viewModel.updateExchangeRate(it) },
                            onSearchQueryChange = viewModel::setSearchQuery
                        )
                    }
                    composable(Screen.Management.route) {
                        ManagementScreen(
                            products = products,
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Calculator, Screen.Management)
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry.value?.destination

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentDestination?.route == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (screen is Screen.Calculator) Icons.Default.Home else Icons.Default.Inventory,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) }
            )
        }
    }
}

@Composable
fun CalculatorScreen(
    exchangeRate: Double,
    products: List<com.example.calculadoradeprecios.data.Product>,
    searchQuery: String,
    onExchangeRateChange: (Double) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val format = remember { createDecimalFormat() }
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        SectionTitle(text = "Tasa de cambio USD → CUP")
        CurrencyRateInput(
            rate = exchangeRate,
            onRateChange = onExchangeRateChange
        )
        Spacer(modifier = Modifier.size(16.dp))
        SearchField(value = searchQuery, onValueChange = onSearchQueryChange)
        Spacer(modifier = Modifier.size(16.dp))
        ProductList(products = products, exchangeRate = exchangeRate, format = format)
    }
}

@Composable
fun ManagementScreen(
    products: List<com.example.calculadoradeprecios.data.Product>,
    onSaveProduct: (com.example.calculadoradeprecios.data.Product) -> Unit,
    onDeleteProduct: (com.example.calculadoradeprecios.data.Product) -> Unit
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var currentProduct by remember { mutableStateOf<com.example.calculadoradeprecios.data.Product?>(null) }
    var sku by rememberSaveable { mutableStateOf("") }
    var equipo by rememberSaveable { mutableStateOf("") }
    var marca by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf("") }
    var precioUsdText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    fun resetForm() {
        isEditing = false
        currentProduct = null
        sku = ""
        equipo = ""
        marca = ""
        modelo = ""
        tipo = ""
        precioUsdText = ""
        errorMessage = ""
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        SectionTitle(text = "Administrar productos")
        ProductManagementForm(
            sku = sku,
            equipo = equipo,
            marca = marca,
            modelo = modelo,
            tipo = tipo,
            precioUsdText = precioUsdText,
            errorMessage = errorMessage,
            onSkuChange = { sku = it },
            onEquipoChange = { equipo = it },
            onMarcaChange = { marca = it },
            onModeloChange = { modelo = it },
            onTipoChange = { tipo = it },
            onPrecioUsdChange = { precioUsdText = it },
            onSubmit = {
                val precioUsd = precioUsdText.replace(',', '.').toDoubleOrNull()
                if (sku.isBlank() || equipo.isBlank() || marca.isBlank() || modelo.isBlank() || tipo.isBlank() || precioUsd == null) {
                    errorMessage = "Todos los campos son obligatorios y el precio debe ser válido"
                } else {
                    val product = currentProduct?.copy(
                        sku = sku,
                        equipo = equipo,
                        marca = marca,
                        modelo = modelo,
                        tipo = tipo,
                        precioUsd = precioUsd
                    ) ?: com.example.calculadoradeprecios.data.Product(
                        sku = sku,
                        equipo = equipo,
                        marca = marca,
                        modelo = modelo,
                        tipo = tipo,
                        precioUsd = precioUsd
                    )
                    onSaveProduct(product)
                    resetForm()
                }
            },
            onCancel = { resetForm() }
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = "Productos guardados", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(products) { product ->
                ProductManagementItem(
                    product = product,
                    onEdit = {
                        currentProduct = product
                        isEditing = true
                        sku = product.sku
                        equipo = product.equipo
                        marca = product.marca
                        modelo = product.modelo
                        tipo = product.tipo
                        precioUsdText = product.precioUsd.toString()
                    },
                    onDelete = { onDeleteProduct(product) }
                )
            }
        }
    }
}

@Composable
fun CurrencyRateInput(rate: Double, onRateChange: (Double) -> Unit) {
    var text by rememberSaveable { mutableStateOf(rate.toString().replace('.', ',')) }
    LaunchedEffect(rate) {
        text = rate.toString().replace('.', ',')
    }
    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            val normalized = newValue.replace(',', '.')
            normalized.toDoubleOrNull()?.let(onRateChange)
        },
        label = { Text("Tasa de cambio") },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        label = { Text("Buscar productos") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
    )
}

@Composable
fun ProductList(products: List<com.example.calculadoradeprecios.data.Product>, exchangeRate: Double, format: DecimalFormat) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(products) { product ->
            ProductCard(product = product, exchangeRate = exchangeRate, format = format)
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
fun ProductCard(product: com.example.calculadoradeprecios.data.Product, exchangeRate: Double, format: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "${product.marca} ${product.modelo} - ${product.tipo}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.size(4.dp))
            Text(text = "SKU: ${product.sku}", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.Gray)
            Spacer(modifier = Modifier.size(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "USD: $${format.format(product.precioUsd)}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "CUP: $${format.format(product.precioUsd * exchangeRate)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProductManagementForm(
    sku: String,
    equipo: String,
    marca: String,
    modelo: String,
    tipo: String,
    precioUsdText: String,
    errorMessage: String,
    onSkuChange: (String) -> Unit,
    onEquipoChange: (String) -> Unit,
    onMarcaChange: (String) -> Unit,
    onModeloChange: (String) -> Unit,
    onTipoChange: (String) -> Unit,
    onPrecioUsdChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        .padding(16.dp)
    ) {
        OutlinedTextField(
            value = sku,
            onValueChange = onSkuChange,
            label = { Text("SKU") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = equipo,
            onValueChange = onEquipoChange,
            label = { Text("Equipo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = marca,
            onValueChange = onMarcaChange,
            label = { Text("Marca") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = modelo,
            onValueChange = onModeloChange,
            label = { Text("Modelo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = tipo,
            onValueChange = onTipoChange,
            label = { Text("Tipo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = precioUsdText,
            onValueChange = { newValue ->
                onPrecioUsdChange(newValue)
            },
            label = { Text("Precio USD") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage.isNotBlank()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.size(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSubmit, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Guardar")
            }
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(text = "Cancelar")
            }
        }
    }
}

@Composable
fun ProductManagementItem(
    product: com.example.calculadoradeprecios.data.Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${product.equipo} ${product.marca} ${product.modelo}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = "${product.tipo} • SKU: ${product.sku}", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.Gray)
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = "Precio: $${product.precioUsd} USD", style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
}

fun createDecimalFormat(): DecimalFormat {
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    return DecimalFormat("#,##0.00", symbols)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val sampleProducts = listOf(
        com.example.calculadoradeprecios.data.Product(id = 1, sku = "SKU123", equipo = "Teléfono", marca = "Samsung", modelo = "Galaxy S23", tipo = "Alta gama", precioUsd = 100.0)
    )
    CalculatorScreen(
        exchangeRate = 350.0,
        products = sampleProducts,
        searchQuery = "",
        onExchangeRateChange = {},
        onSearchQueryChange = {}
    )
}
