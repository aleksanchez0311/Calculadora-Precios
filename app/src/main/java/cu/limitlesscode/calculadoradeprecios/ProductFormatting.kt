package cu.limitlesscode.calculadoradeprecios

import android.content.Intent
import android.net.Uri
import cu.limitlesscode.calculadoradeprecios.data.Product
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
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, mensaje)
            val imageUri = Uri.parse(product.imageUrl)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }
        setPackage("com.whatsapp")
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: si WhatsApp no está instalado, abre el enlace web
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/?text=${Uri.encode(mensaje)}")
        }
        context.startActivity(fallbackIntent)
    }
}
