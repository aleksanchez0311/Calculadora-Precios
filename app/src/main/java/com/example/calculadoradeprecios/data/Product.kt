package com.example.calculadoradeprecios.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sku: String,
    val equipo: String,
    val marca: String,
    val modelo: String,
    val tipo: String,
    val precioUsd: Double
)
