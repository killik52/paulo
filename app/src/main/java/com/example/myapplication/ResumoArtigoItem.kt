package com.example.myapplication

data class ResumoArtigoItem(
    val nomeArtigo: String,
    val quantidadeTotalVendida: Int,
    val valorTotalVendido: Double,
    val artigoId: Long? // Opcional, se quiser navegar para detalhes do artigo
)