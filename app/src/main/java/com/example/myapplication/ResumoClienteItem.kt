package com.example.myapplication

data class ResumoClienteItem(
    val nomeCliente: String,
    val totalGasto: Double,
    val clienteId: Long? // Opcional, se quiser navegar para detalhes do cliente
)