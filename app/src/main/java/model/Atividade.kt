package com.example.myapplication.model

// 1. Classe de dados que representa uma atividade com um código e texto associados
data class Atividade(
    // 2. Código da atividade, pode ser nulo
    val code: String?,
    // 3. Texto descritivo da atividade, pode ser nulo
    val text: String?
)