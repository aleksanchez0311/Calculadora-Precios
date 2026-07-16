package cu.limitlesscode.calculadoradeprecios

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import cu.limitlesscode.calculadoradeprecios.data.Product
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import cu.limitlesscode.calculadoradeprecios.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun buildDisplayName(equipo: String, marca: String, modelo: String, tipo: String): String {
    return listOf(equipo, marca, modelo, tipo)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

fun buildShareMessage(product: Product, format: DecimalFormat, precioCup: Double): String {
    val nombreCompleto = buildDisplayName(product.equipo, product.marca, product.modelo, product.tipo)
    val coloresList = if (product.colores.isNotBlank()) {
        product.colores.split(",").joinToString(", ") { it.trim() }
    } else null

    return buildString {
        appendLine("✅ $nombreCompleto")
        if (product.infoAdicional.isNotBlank()) {
            appendLine("(${product.infoAdicional.trim()})")
        }
        appendLine()
        appendLine("💰 Precio USD: ${format.format(product.precioUsd)}")
        appendLine("💰 Precio CUP: ${format.format(precioCup)}")
        if (product.garantia.isNotBlank()) {
            appendLine("📝 Garantia: ${product.garantia}")
        }else{
            appendLine("📝 Garantia: No")
        }
        if (coloresList != null) {
            append("🌈 Colores: $coloresList")
        }
    }.trimEnd()
}

fun createDecimalFormat(): DecimalFormat {
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    return DecimalFormat("#,##0.00", symbols)
}

/**
 * Convierte una URI de archivo interno en una URI de ContentProvider si es necesario.
 */
private fun getShareableUri(context: android.content.Context, imageUrl: String): Uri? {
    if (imageUrl.isBlank()) return null
    val rawUri = Uri.parse(imageUrl)
    return if (rawUri.scheme == "file") {
        val file = File(rawUri.path ?: "")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } else {
        rawUri
    }
}

/**
 * Lanza un Intent para compartir un producto individual por WhatsApp.
 * Usa ACTION_SEND para incluir imagen (si existe) + texto como caption.
 */
fun launchShareIntent(
    context: android.content.Context,
    product: Product,
    exchangeRate: Double,
    format: DecimalFormat,
    targetNumber: String = ""
) {
    val precioCup = product.precioUsd * exchangeRate
    val mensaje = buildShareMessage(product, format, precioCup)

    val intent = Intent(Intent.ACTION_SEND).apply {
        val shareUri = getShareableUri(context, product.imageUrl)
        if (shareUri != null) {
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, mensaje)
            putExtra(Intent.EXTRA_STREAM, shareUri)
            clipData = ClipData.newRawUri("Product Image", shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }
    }

    executeWhatsAppIntent(context, intent, mensaje, targetNumber)
}

/**
 * Comparte un resumen de texto de múltiples productos en un solo mensaje.
 */
fun launchSummaryShareIntent(
    context: android.content.Context,
    products: List<Product>,
    exchangeRate: Double,
    format: DecimalFormat,
    targetNumber: String = ""
) {
    val header = context.getString(R.string.share_summary_header)
    val body = products.joinToString("\n") { product ->
        val precioCup = product.precioUsd * exchangeRate
        val nombre = buildDisplayName(product.equipo, product.marca, product.modelo, product.tipo)
        context.getString(
            R.string.share_summary_item,
            nombre,
            "${format.format(product.precioUsd)} USD",
            "${format.format(precioCup)} CUP"
        )
    }
    val mensaje = "$header\n\n$body".trim()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, mensaje)
    }

    executeWhatsAppIntent(context, intent, mensaje, targetNumber)
}

/**
 * Comparte todas las imágenes de los productos seleccionados en un solo lote.
 */
fun launchBatchShareIntent(
    context: android.content.Context,
    products: List<Product>,
    targetNumber: String = ""
) {
    val uris = ArrayList<Uri>()
    products.forEach { product ->
        getShareableUri(context, product.imageUrl)?.let { uris.add(it) }
    }

    if (uris.isEmpty()) return

    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    executeWhatsAppIntent(context, intent, "", targetNumber)
}

/**
 * Genera imágenes con la información del producto integrada y las comparte en lote.
 */
suspend fun launchOverlayBatchShareIntent(
    context: android.content.Context,
    products: List<Product>,
    exchangeRate: Double,
    format: DecimalFormat,
    targetNumber: String = ""
) = withContext(Dispatchers.IO) {
    val tempDir = File(context.cacheDir, "share_temp").apply { 
        deleteRecursively()
        mkdirs() 
    }
    
    val uris = ArrayList<Uri>()
    
    products.forEach { product ->
        val bitmap = try {
            val uri = Uri.parse(product.imageUrl)
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) { null }

        if (bitmap != null) {
            val nombre = buildDisplayName(product.equipo, product.marca, product.modelo, product.tipo)
            val precioUsd = "${format.format(product.precioUsd)} USD"
            val precioCup = "${format.format(product.precioUsd * exchangeRate)} CUP"
            
            val overlayBitmap = addTextOverlay(bitmap, nombre, precioUsd, precioCup)
            val file = File(tempDir, "share_${product.id}_${System.currentTimeMillis()}.jpg")
            
            try {
                FileOutputStream(file).use { out ->
                    overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                uris.add(uri)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    if (uris.isNotEmpty()) {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            executeWhatsAppIntent(context, intent, "", targetNumber)
        }
    }
}

private fun addTextOverlay(original: Bitmap, name: String, usd: String, cup: String): Bitmap {
    val width = original.width
    val height = original.height
    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val bannerHeight = (height * 0.15).toInt().coerceAtLeast(100)
    val paint = Paint().apply {
        color = Color.argb(204, 0, 0, 0) // Negro semi-transparente
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, (height - bannerHeight).toFloat(), width.toFloat(), height.toFloat(), paint)

    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = (bannerHeight * 0.35).toFloat()
        isAntiAlias = true
        isFakeBoldText = true
    }

    val margin = 20f
    canvas.drawText(name, margin, (height - bannerHeight + bannerHeight * 0.4).toFloat(), textPaint)
    
    val pricePaint = Paint(textPaint).apply {
        textSize = (bannerHeight * 0.3).toFloat()
        isFakeBoldText = false
    }
    canvas.drawText("$usd / $cup", margin, (height - bannerHeight * 0.2).toFloat(), pricePaint)

    return result
}

private fun executeWhatsAppIntent(
    context: android.content.Context, 
    intent: Intent, 
    mensajeFallback: String,
    targetNumber: String = ""
) {
    // Intentar WhatsApp regular
    val whatsappIntent = Intent(intent).setPackage("com.whatsapp")
    // Intentar WhatsApp Business
    val businessIntent = Intent(intent).setPackage("com.whatsapp.w4b")

    if (targetNumber.isNotBlank()) {
        val jid = if (targetNumber.startsWith("+")) targetNumber.substring(1) else targetNumber
        whatsappIntent.putExtra("jid", "$jid@s.whatsapp.net")
        businessIntent.putExtra("jid", "$jid@s.whatsapp.net")
    }

    // Fallback genérico (Selector de Android)
    val genericIntent = Intent.createChooser(intent, "Compartir productos")

    try {
        context.startActivity(whatsappIntent)
    } catch (e1: Exception) {
        try {
            context.startActivity(businessIntent)
        } catch (e2: Exception) {
            try {
                context.startActivity(genericIntent)
            } catch (e3: Exception) {
                if (mensajeFallback.isNotBlank()) {
                    val fallbackWeb = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/?text=${Uri.encode(mensajeFallback)}")
                    }
                    context.startActivity(fallbackWeb)
                }
            }
        }
    }
}
