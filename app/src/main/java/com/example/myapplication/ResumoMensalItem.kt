package com.example.myapplication

data class ResumoMensalItem(
    val mesAno: String, // Ex: "07/2025" ou "Julho/2025"
    val valorTotal: Double,
    val ano: Int, // Para facilitar a query de detalhes
    val mes: Int   // Para facilitar a query de detalhes (1-12)
)