package cu.limitlesscode.calculadoradeprecios

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
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
        } else {
            appendLine("📝 Garantia: No")
        }
        if (coloresList != null) {
            appendLine("🌈 Colores: $coloresList")
        }
    }.trimEnd()
}

/**
 * Carga un bitmap desde una URI, aplicando escalado para ahorrar memoria.
 */
private fun loadScaledBitmap(context: android.content.Context, uri: Uri, targetSize: Int): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            
            var scale = 1
            while (options.outWidth / scale / 2 >= targetSize && options.outHeight / scale / 2 >= targetSize) {
                scale *= 2
            }
            
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            context.contentResolver.openInputStream(uri)?.use { input2 ->
                BitmapFactory.decodeStream(input2, null, decodeOptions)
            }
        }
    } catch (_: Exception) {
        null
    }
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
        if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else null
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
 * 2. CATÁLOGO DE TEXTO: Comparte un resumen de texto único de todos los productos.
 * Incluye TODA la información de cada tarjeta.
 */
fun launchSummaryShareIntent(
    context: android.content.Context,
    products: List<Product>,
    exchangeRate: Double,
    format: DecimalFormat,
    targetNumber: String = ""
) {
    val header = context.getString(R.string.share_summary_header)
    val divider = context.getString(R.string.share_summary_divider)
    
    val body = products.joinToString(divider) { product ->
        val precioCup = product.precioUsd * exchangeRate
        buildShareMessage(product, format, precioCup)
    }
    val mensaje = "$header\n\n$body".trim()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, mensaje)
    }

    executeWhatsAppIntent(context, intent, mensaje, targetNumber)
}

/**
 * 4. CATÁLOGO PDF: Genera un documento profesional con fotos e información completa.
 */
suspend fun launchPdfCatalogShareIntent(
    context: android.content.Context,
    products: List<Product>,
    exchangeRate: Double,
    format: DecimalFormat,
    targetNumber: String = ""
) = withContext(Dispatchers.IO) {
    val document = PdfDocument()
    val pageWidth = 595 // A4
    val pageHeight = 842
    
    val paint = Paint()
    val textPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 11f
        isAntiAlias = true
    }
    val titlePaint = TextPaint(textPaint).apply {
        textSize = 14f
        isFakeBoldText = true
    }
    
    var yPos = 40f
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas
    
    canvas.drawText(context.getString(R.string.share_summary_header), 40f, yPos, titlePaint)
    yPos += 50f
    
    products.forEach { product ->
        if (yPos + 150 > pageHeight) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = 40f
        }
        
        // Imagen escalada para el PDF
        val bitmap = loadScaledBitmap(context, Uri.parse(product.imageUrl), 300)
        if (bitmap != null) {
            val scaled = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
            canvas.drawBitmap(scaled, 40f, yPos, paint)
        }
        
        val leftMargin = 155f
        val nombre = buildDisplayName(product.equipo, product.marca, product.modelo, product.tipo)
        canvas.drawText(nombre, leftMargin, yPos + 15, titlePaint)
        
        val precioUsd = "${format.format(product.precioUsd)} USD"
        val precioCup = "${format.format(product.precioUsd * exchangeRate)} CUP"
        canvas.drawText("Precio: $precioUsd / $precioCup", leftMargin, yPos + 35, textPaint)
        
        val garantia = if (product.garantia.isNotBlank()) product.garantia else "No"
        canvas.drawText("Garantía: $garantia", leftMargin, yPos + 55, textPaint)
        
        if (product.colores.isNotBlank()) {
            canvas.drawText("Colores: ${product.colores}", leftMargin, yPos + 75, textPaint)
        }
        
        if (product.infoAdicional.isNotBlank()) {
            val info = if (product.infoAdicional.length > 60) product.infoAdicional.take(57) + "..." else product.infoAdicional
            canvas.drawText("Info: $info", leftMargin, yPos + 95, textPaint)
        }
        
        yPos += 120f
        canvas.drawLine(40f, yPos - 10, (pageWidth - 40).toFloat(), yPos - 10, paint)
    }
    
    document.finishPage(page)
    
    val file = File(context.cacheDir, "catalogo.pdf")
    try {
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            executeWhatsAppIntent(context, intent, "", targetNumber)
        }
    } catch (e: Exception) { e.printStackTrace() }
}

/**
 * 3. CATÁLOGO VISUAL: Genera imágenes con la información del producto integrada.
 */
suspend fun launchOverlayBatchShareIntent(
    context: android.content.Context,
    products: List<Product>,
    exchangeRate: Double,
    format: DecimalFormat,
    targetNumber: String = ""
) = withContext(Dispatchers.IO) {
    val tempDir = File(context.cacheDir, "share_batch").apply { 
        deleteRecursively()
        mkdirs() 
    }
    
    val uris = ArrayList<Uri>()
    
    products.forEach { product ->
        val bitmap = loadScaledBitmap(context, Uri.parse(product.imageUrl), 1000)
        if (bitmap != null) {
            val precioCup = product.precioUsd * exchangeRate
            val nombre = buildDisplayName(product.equipo, product.marca, product.modelo, product.tipo)
            val pUsd = "${format.format(product.precioUsd)} USD"
            val pCup = "${format.format(precioCup)} CUP"
            
            val overlayBitmap = addTextOverlay(bitmap, nombre, pUsd, pCup, product.garantia)
            val file = File(tempDir, "card_${product.id}.jpg")
            
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
            
            val clipData = ClipData.newRawUri("Catálogo", uris[0])
            for (i in 1 until uris.size) {
                clipData.addItem(ClipData.Item(uris[i]))
            }
            this.clipData = clipData
        }
        withContext(Dispatchers.Main) {
            executeWhatsAppIntent(context, intent, "", targetNumber)
        }
    }
}

private fun addTextOverlay(original: Bitmap, name: String, usd: String, cup: String, garantia: String): Bitmap {
    val width = original.width
    val height = original.height
    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val bannerHeight = (height * 0.25).toInt().coerceAtLeast(180)
    val paint = Paint().apply {
        color = Color.argb(210, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, (height - bannerHeight).toFloat(), width.toFloat(), height.toFloat(), paint)

    val margin = width * 0.05f
    val availableWidth = width - (margin * 2)

    val namePaint = Paint().apply {
        color = Color.WHITE
        textSize = (bannerHeight * 0.25).toFloat()
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    // Ajustar tamaño del nombre si es muy largo
    var currentNameSize = namePaint.textSize
    while (namePaint.measureText(name) > availableWidth && currentNameSize > 20f) {
        currentNameSize -= 2f
        namePaint.textSize = currentNameSize
    }
    
    // Fila 1: Nombre
    canvas.drawText(name, margin, (height - bannerHeight + bannerHeight * 0.3f), namePaint)
    
    // Fila 2: Precios
    val priceText = "$usd / $cup"
    val pricePaint = Paint(namePaint).apply {
        color = Color.YELLOW
        textSize = (bannerHeight * 0.22).toFloat()
    }

    var currentPriceSize = pricePaint.textSize
    while (pricePaint.measureText(priceText) > availableWidth && currentPriceSize > 18f) {
        currentPriceSize -= 2f
        pricePaint.textSize = currentPriceSize
    }
    canvas.drawText(priceText, margin, (height - bannerHeight + bannerHeight * 0.6f), pricePaint)
    
    // Fila 3: Garantía
    val detailPaint = Paint(namePaint).apply {
        color = Color.LTGRAY
        textSize = (bannerHeight * 0.18).toFloat()
        isFakeBoldText = false
    }
    val gText = "Garantía: ${if (garantia.isNotBlank()) garantia else "No"}"

    var currentDetailSize = detailPaint.textSize
    while (detailPaint.measureText(gText) > availableWidth && currentDetailSize > 16f) {
        currentDetailSize -= 2f
        detailPaint.textSize = currentDetailSize
    }
    canvas.drawText(gText, margin, (height - bannerHeight + bannerHeight * 0.85f), detailPaint)

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
    val genericIntent = Intent.createChooser(intent, context.getString(R.string.dialog_title_share))

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
