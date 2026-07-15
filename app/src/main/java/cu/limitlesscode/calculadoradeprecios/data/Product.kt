package cu.limitlesscode.calculadoradeprecios.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val equipo: String,
    val marca: String,
    val modelo: String,
    val tipo: String,
    val precioUsd: Double,
    val isActive: Boolean = true,
    val imageUrl: String = "",
    val garantia: String = "",
    val colores: String = "",        // Almacenado como lista separada por comas
    val infoAdicional: String = ""
)
