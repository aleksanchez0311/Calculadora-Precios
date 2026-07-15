package cu.limitlesscode.calculadoradeprecios

import cu.limitlesscode.calculadoradeprecios.data.Product
import java.text.DecimalFormat

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
        }
        if (coloresList != null) {
            append("🌈 Colores: $coloresList")
        }
    }.trimEnd()
}
