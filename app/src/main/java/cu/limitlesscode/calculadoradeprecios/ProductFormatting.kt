package cu.limitlesscode.calculadoradeprecios

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import cu.limitlesscode.calculadoradeprecios.data.Product
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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
 * Lanza un Intent para compartir un producto individual por WhatsApp.
 * Usa ACTION_SEND para incluir imagen (si existe) + texto como caption.
 */
fun launchShareIntent(
    context: android.content.Context,
    product: Product,
    exchangeRate: Double,
    format: DecimalFormat
) {
    val precioCup = product.precioUsd * exchangeRate
    val mensaje = buildShareMessage(product, format, precioCup)

    val intent = Intent(Intent.ACTION_SEND).apply {
        if (product.imageUrl.isNotBlank()) {
            val rawUri = Uri.parse(product.imageUrl)
            val shareUri = if (rawUri.scheme == "file") {
                val file = File(rawUri.path ?: "")
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                rawUri
            }

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

    // Intentar WhatsApp regular
    val whatsappIntent = Intent(intent).setPackage("com.whatsapp")
    // Intentar WhatsApp Business
    val businessIntent = Intent(intent).setPackage("com.whatsapp.w4b")
    // Fallback genérico (Selector de Android)
    val genericIntent = Intent.createChooser(intent, "Compartir producto")

    try {
        context.startActivity(whatsappIntent)
    } catch (e1: Exception) {
        try {
            context.startActivity(businessIntent)
        } catch (e2: Exception) {
            try {
                context.startActivity(genericIntent)
            } catch (e3: Exception) {
                // Último recurso: enlace web
                val fallbackWeb = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/?text=${Uri.encode(mensaje)}")
                }
                context.startActivity(fallbackWeb)
            }
        }
    }
}
