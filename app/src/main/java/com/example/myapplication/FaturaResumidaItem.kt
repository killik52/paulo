package com.example.myapplication

// 1. Define uma classe de dados para representar uma fatura resumida
data class FaturaResumidaItem(
    // 2. Identificador único da fatura
    val id: Long,
    // 3. Número da fatura
    val numeroFatura: String,
    // 4. Nome do cliente associado à fatura
    val cliente: String,
    // 5. Lista de números de série dos artigos, podendo conter valores nulos
    val serialNumbers: List<String?>,
    // 6. Saldo devedor da fatura
    val saldoDevedor: Double,
    // 7. Data da fatura
    val data: String,
    var foiEnviada: Boolean = false // Novo campo para status de envio
)