package cu.limitlesscode.calculadoradeprecios.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupData(
    val products: List<Product>,
    val exchangeRate: Double
)

class BackupManager(private val context: Context) {

    suspend fun exportBackup(
        products: List<Product>,
        exchangeRate: Double,
        outputUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = JSONObject()
            val items = JSONArray()

            // Carpeta temporal para imágenes
            val tempImagesDir = File(context.cacheDir, "backup_images").apply { mkdirs() }
            tempImagesDir.deleteRecursively()
            tempImagesDir.mkdirs()

            products.forEach { product ->
                val item = JSONObject().apply {
                    put("id", product.id)
                    put("equipo", product.equipo)
                    put("marca", product.marca)
                    put("modelo", product.modelo)
                    put("tipo", product.tipo)
                    put("precioUsd", product.precioUsd)
                    put("isActive", product.isActive)
                    put("imageUrl", product.imageUrl)
                    put("garantia", product.garantia)
                    put("colores", product.colores)
                    put("infoAdicional", product.infoAdicional)
                    put("imageFileName", if (product.imageUrl.isNotBlank()) {
                        val uri = Uri.parse(product.imageUrl)
                        copyImageToTemp(uri, tempImagesDir)
                    } else "")
                }
                items.put(item)
            }

            json.put("products", items)
            json.put("exchangeRate", exchangeRate)

            // Crear ZIP con JSON e imágenes
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                ZipOutputStream(output).use { zipOut ->
                    // Agregar JSON
                    val jsonEntry = ZipEntry("backup.json")
                    zipOut.putNextEntry(jsonEntry)
                    zipOut.write(json.toString(2).toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()

                    // Agregar imágenes
                    tempImagesDir.listFiles()?.forEach { file ->
                        val imageEntry = ZipEntry("images/${file.name}")
                        zipOut.putNextEntry(imageEntry)
                        file.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            context.cacheDir.resolve("backup_images").deleteRecursively()
        }
    }

    suspend fun importBackup(inputUri: Uri): BackupData? = withContext(Dispatchers.IO) {
        return@withContext try {
            val mimeType = context.contentResolver.getType(inputUri)
            val isZip = mimeType == "application/zip" || inputUri.path?.endsWith(".zip", true) == true

            if (isZip) {
                processZipBackup(inputUri)
            } else {
                processJsonBackup(inputUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun processZipBackup(inputUri: Uri): BackupData? = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_temp").apply { mkdirs() }
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            // Extraer ZIP
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                ZipInputStream(input).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val file = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { output ->
                                zipIn.copyTo(output)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            val backupJsonFile = File(tempDir, "backup.json")
            if (!backupJsonFile.exists()) return@withContext null

            val backupJson = backupJsonFile.bufferedReader().readText()
            val json = JSONObject(backupJson)
            val items = json.optJSONArray("products") ?: JSONArray()
            val exchangeRate = json.optDouble("exchangeRate", 1.0)

            val products = mutableListOf<Product>()
            val imagesDir = File(tempDir, "images")

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val imageFileName = item.optString("imageFileName", "")
                val newImageUri = if (imageFileName.isNotBlank()) {
                    val tempImage = File(imagesDir, imageFileName)
                    if (tempImage.exists()) {
                        val savedUri = saveImageToInternalStorage(tempImage)
                        savedUri.toString()
                    } else ""
                } else ""

                products.add(parseProductFromJson(item, newImageUri))
            }

            BackupData(products, exchangeRate)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun processJsonBackup(inputUri: Uri): BackupData? = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText() }
            if (text.isNullOrBlank()) return@withContext null

            val json = JSONObject(text)
            val items = json.optJSONArray("products") ?: JSONArray()
            val exchangeRate = json.optDouble("exchangeRate", 1.0)
            val list = mutableListOf<Product>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                list.add(parseProductFromJson(item, item.optString("imageUrl", "")))
            }
            BackupData(list, exchangeRate)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseProductFromJson(item: JSONObject, imageUrl: String): Product {
        return Product(
            id = 0L, // Siempre 0 para insertar como nuevo en Room tras limpiar
            equipo = item.optString("equipo", ""),
            marca = item.optString("marca", ""),
            modelo = item.optString("modelo", ""),
            tipo = item.optString("tipo", ""),
            precioUsd = item.optDouble("precioUsd", 0.0),
            isActive = item.optBoolean("isActive", true),
            imageUrl = imageUrl,
            garantia = item.optString("garantia", ""),
            colores = item.optString("colores", ""),
            infoAdicional = item.optString("infoAdicional", "")
        )
    }

    private fun copyImageToTemp(uri: Uri, destDir: File): String {
        return try {
            val fileName = "img_${System.currentTimeMillis()}_${uri.lastPathSegment?.hashCode() ?: 0}.jpg"
            val destFile = File(destDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun saveImageToInternalStorage(imageFile: File): Uri? {
        return try {
            val imagesDir = File(context.filesDir, "product_images").apply { mkdirs() }
            val destFile = File(imagesDir, imageFile.name)
            imageFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
