@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.calculadoradeprecios

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// ---------- Rutas de navegación ----------

sealed class Screen(val route: String, val title: String) {
    object Calculator : Screen("calculator", "Calculadora")
    object Management : Screen("management", "Administración")
}

// ---------- Activity ----------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalculadoraPreciosApp() }
    }
}

// ---------- Paleta de colores ----------

private val AppColors = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF1976D2),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E)
)

// ---------- Raíz de la app ----------

@Composable
fun CalculadoraPreciosApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val exchangeRate by viewModel.exchangeRate.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    MaterialTheme(colorScheme = AppColors) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Calculadora de Precios", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = { BottomNavigationBar(navController) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
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

// ---------- Navegación inferior ----------

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
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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

// ---------- Pantalla principal (Calculadora) ----------

@Composable
fun CalculatorScreen(
    exchangeRate: Double,
    products: List<com.example.calculadoradeprecios.data.Product>,
    searchQuery: String,
    onExchangeRateChange: (Double) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val format = remember { createDecimalFormat() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        SectionTitle(text = "Tasa de cambio USD → CUP")
        CurrencyRateInput(rate = exchangeRate, onRateChange = onExchangeRateChange)
        Spacer(modifier = Modifier.size(12.dp))
        SearchField(value = searchQuery, onValueChange = onSearchQueryChange)
        Spacer(modifier = Modifier.size(12.dp))
        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay productos. Agrégalos en la pestaña Administración.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(products) { product ->
                    ProductCard(product = product, exchangeRate = exchangeRate, format = format)
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

// ---------- Pantalla de administración ----------

@Composable
fun ManagementScreen(
    products: List<com.example.calculadoradeprecios.data.Product>,
    onSaveProduct: (com.example.calculadoradeprecios.data.Product) -> Unit,
    onDeleteProduct: (com.example.calculadoradeprecios.data.Product) -> Unit
) {
    val context = LocalContext.current

    var currentProduct by remember { mutableStateOf<com.example.calculadoradeprecios.data.Product?>(null) }
    var sku by rememberSaveable { mutableStateOf("") }
    var equipo by rememberSaveable { mutableStateOf("") }
    var marca by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf("") }
    var precioUsdText by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf("") }
    var garantia by rememberSaveable { mutableStateOf("") }
    var colores by rememberSaveable { mutableStateOf("") }
    var infoAdicional by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    // Selector de imagen de la galería
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Persistir permiso de lectura para que el URI siga siendo válido tras reiniciar la app
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* No todos los URIs soportan persistencia */ }
            imageUri = it.toString()
        }
    }

    fun resetForm() {
        currentProduct = null
        sku = ""; equipo = ""; marca = ""; modelo = ""; tipo = ""
        precioUsdText = ""; imageUri = ""; garantia = ""; colores = ""; infoAdicional = ""
        errorMessage = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                SectionTitle(text = "Administrar productos")
                ProductManagementForm(
                    sku = sku,
                    equipo = equipo,
                    marca = marca,
                    modelo = modelo,
                    tipo = tipo,
                    precioUsdText = precioUsdText,
                    imageUri = imageUri,
                    garantia = garantia,
                    colores = colores,
                    infoAdicional = infoAdicional,
                    errorMessage = errorMessage,
                    onSkuChange = { sku = it },
                    onEquipoChange = { equipo = it },
                    onMarcaChange = { marca = it },
                    onModeloChange = { modelo = it },
                    onTipoChange = { tipo = it },
                    onPrecioUsdChange = { precioUsdText = it },
                    onPickImage = {
                        imagePickerLauncher.launch(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    },
                    onClearImage = { imageUri = "" },
                    onGarantiaChange = { garantia = it },
                    onColoresChange = { colores = it },
                    onInfoAdicionalChange = { infoAdicional = it },
                    onSubmit = {
                        val precioUsd = precioUsdText.replace(',', '.').toDoubleOrNull()
                        if (sku.isBlank() || equipo.isBlank() || marca.isBlank() || modelo.isBlank() || tipo.isBlank() || precioUsd == null) {
                            errorMessage = "Los campos SKU, Equipo, Marca, Modelo, Tipo y Precio son obligatorios"
                        } else {
                            val product = currentProduct?.copy(
                                sku = sku, equipo = equipo, marca = marca, modelo = modelo,
                                tipo = tipo, precioUsd = precioUsd, imageUrl = imageUri,
                                garantia = garantia, colores = colores, infoAdicional = infoAdicional
                            ) ?: com.example.calculadoradeprecios.data.Product(
                                sku = sku, equipo = equipo, marca = marca, modelo = modelo,
                                tipo = tipo, precioUsd = precioUsd, imageUrl = imageUri,
                                garantia = garantia, colores = colores, infoAdicional = infoAdicional
                            )
                            onSaveProduct(product)
                            resetForm()
                        }
                    },
                    onCancel = { resetForm() }
                )
                Spacer(modifier = Modifier.size(24.dp))
                Text(
                    text = "Productos guardados (${products.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            items(products) { product ->
                ProductManagementItem(
                    product = product,
                    onEdit = {
                        currentProduct = product
                        sku = product.sku; equipo = product.equipo; marca = product.marca
                        modelo = product.modelo; tipo = product.tipo
                        precioUsdText = product.precioUsd.toString()
                        imageUri = product.imageUrl; garantia = product.garantia
                        colores = product.colores; infoAdicional = product.infoAdicional
                    },
                    onDelete = { onDeleteProduct(product) }
                )
            }
        }
    }
}

// ---------- Componentes de campo de texto ----------

@Composable
fun CurrencyRateInput(rate: Double, onRateChange: (Double) -> Unit) {
    var text by rememberSaveable { mutableStateOf(rate.toString().replace('.', ',')) }
    LaunchedEffect(rate) { text = rate.toString().replace('.', ',') }
    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            newValue.replace(',', '.').toDoubleOrNull()?.let(onRateChange)
        },
        label = { Text("Tasa de cambio") },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
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

// ---------- Tarjeta de producto (vista principal) ----------

@Composable
fun ProductCard(
    product: com.example.calculadoradeprecios.data.Product,
    exchangeRate: Double,
    format: DecimalFormat
) {
    val context = LocalContext.current
    val precioCup = product.precioUsd * exchangeRate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Imagen local del producto (content:// URI o vacío)
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = Uri.parse(product.imageUrl),
                    contentDescription = "Imagen de ${product.modelo}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${product.equipo} ${product.marca} ${product.modelo}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${product.tipo} • SKU: ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.size(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💵 USD: ${format.format(product.precioUsd)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "💰 CUP: ${format.format(precioCup)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // -------- Botón Compartir WhatsApp --------
                    IconButton(
                        onClick = {
                            val coloresList = if (product.colores.isNotBlank())
                                product.colores.split(",").joinToString(", ") { it.trim() }
                            else "No especificado"

                            val mensaje = buildString {
                                appendLine("✅ ${product.equipo} ${product.marca} ${product.modelo} - ${product.tipo}")
                                if (product.infoAdicional.isNotBlank()) {
                                    appendLine()
                                    appendLine(product.infoAdicional)
                                }
                                appendLine()
                                appendLine("💰 Precio USD: ${format.format(product.precioUsd)} USD")
                                appendLine("💰 Precio CUP: ${format.format(precioCup)} CUP")
                                if (product.garantia.isNotBlank()) {
                                    appendLine("📝 Garantía: ${product.garantia}")
                                }
                                append("🌈 Colores: $coloresList")
                            }

                            val imageContentUri = if (product.imageUrl.isNotBlank())
                                Uri.parse(product.imageUrl) else null

                            if (imageContentUri != null) {
                                // Compartir imagen + texto directamente a WhatsApp
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_TEXT, mensaje)
                                    putExtra(Intent.EXTRA_STREAM, imageContentUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    // Intentar abrir WhatsApp directamente
                                    setPackage("com.whatsapp")
                                }
                                // Si WhatsApp no está instalado, abrir chooser genérico
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    // Fallback: WhatsApp Business
                                    intent.setPackage("com.whatsapp.w4b")
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        // Fallback final: chooser del sistema
                                        context.startActivity(Intent.createChooser(intent.apply { setPackage(null) }, "Compartir producto"))
                                    }
                                }
                            } else {
                                // Sin imagen: abrir WhatsApp con solo texto via wa.me
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/?text=${Uri.encode(mensaje)}")
                                }
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir en WhatsApp",
                            tint = Color(0xFF25D366)
                        )
                    }
                }

                if (product.garantia.isNotBlank() || product.colores.isNotBlank()) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Divider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (product.garantia.isNotBlank()) {
                            Text(
                                text = "📝 ${product.garantia}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        if (product.colores.isNotBlank()) {
                            Text(
                                text = "🌈 ${product.colores}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Formulario de gestión ----------

@Composable
fun ProductManagementForm(
    sku: String,
    equipo: String,
    marca: String,
    modelo: String,
    tipo: String,
    precioUsdText: String,
    imageUri: String,
    garantia: String,
    colores: String,
    infoAdicional: String,
    errorMessage: String,
    onSkuChange: (String) -> Unit,
    onEquipoChange: (String) -> Unit,
    onMarcaChange: (String) -> Unit,
    onModeloChange: (String) -> Unit,
    onTipoChange: (String) -> Unit,
    onPrecioUsdChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onGarantiaChange: (String) -> Unit,
    onColoresChange: (String) -> Unit,
    onInfoAdicionalChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- Campos base ---
            FormField(value = sku, onValueChange = onSkuChange, label = "SKU *")
            FormField(value = equipo, onValueChange = onEquipoChange, label = "Equipo *")
            FormField(value = marca, onValueChange = onMarcaChange, label = "Marca *")
            FormField(value = modelo, onValueChange = onModeloChange, label = "Modelo *")
            FormField(value = tipo, onValueChange = onTipoChange, label = "Tipo *")
            FormField(
                value = precioUsdText,
                onValueChange = onPrecioUsdChange,
                label = "Precio USD *",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.size(4.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.size(12.dp))

            // --- Selector de imagen local ---
            Text(
                text = "Imagen del producto",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(8.dp))

            if (imageUri.isNotBlank()) {
                // Vista previa de la imagen seleccionada
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = Uri.parse(imageUri),
                        contentDescription = "Imagen seleccionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
                    )
                    // Botón para cambiar imagen
                    TextButton(
                        onClick = onClearImage,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("Cambiar", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                // Botón para seleccionar imagen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFBBBBBB), RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { onPickImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Toca para seleccionar una foto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            // --- Campos adicionales ---
            FormField(value = garantia, onValueChange = onGarantiaChange, label = "Garantía (ej: 12 meses)")
            FormField(
                value = colores,
                onValueChange = onColoresChange,
                label = "Colores disponibles (separados por coma)"
            )
            FormField(
                value = infoAdicional,
                onValueChange = onInfoAdicionalChange,
                label = "Información adicional",
                singleLine = false,
                minLines = 3
            )

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSubmit, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "Guardar", color = MaterialTheme.colorScheme.onPrimary)
                }
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(text = "Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ---------- Campo de texto reutilizable ----------

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        ),
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

// ---------- Item en la lista de administración ----------

@Composable
fun ProductManagementItem(
    product: com.example.calculadoradeprecios.data.Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Miniatura de imagen si existe
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = Uri.parse(product.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.size(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${product.equipo} ${product.marca} ${product.modelo}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = "${product.tipo} • SKU: ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Precio: ${product.precioUsd} USD",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (product.garantia.isNotBlank()) {
                    Text(
                        text = "Garantía: ${product.garantia}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ---------- Utilidades ----------

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp),
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold
    )
}

fun createDecimalFormat(): DecimalFormat {
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    return DecimalFormat("#,##0.00", symbols)
}

// ---------- Preview ----------

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val sampleProducts = listOf(
        com.example.calculadoradeprecios.data.Product(
            id = 1, sku = "SKU123", equipo = "Teléfono", marca = "Samsung",
            modelo = "Galaxy S24", tipo = "Alta gama", precioUsd = 650.0,
            garantia = "12 meses", colores = "Negro, Blanco, Azul",
            infoAdicional = "Incluye cargador rápido de 45W", imageUrl = ""
        )
    )
    CalculatorScreen(
        exchangeRate = 350.0,
        products = sampleProducts,
        searchQuery = "",
        onExchangeRateChange = {},
        onSearchQueryChange = {}
    )
}
