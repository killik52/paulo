package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// 1. Define modelo de dados para Artigo com serialização
@Parcelize
data class Artigo(
    val id: Long,
    val nome: String,
    val preco: Double,
    val quantidade: Int,
    val desconto: Double,
    val descricao: String?,
    val guardarFatura: Boolean,
    val numeroSerial: String?
) : Parcelable