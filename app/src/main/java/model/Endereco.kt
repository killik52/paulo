package com.example.myapplication.model

// 1. Classe de dados que representa um endereço
data class Endereco(
    // 2. Logradouro do endereço, pode ser nulo
    val logradouro: String?,
    // 3. Número do endereço, pode ser nulo
    val numero: String?,
    // 4. Complemento do endereço, pode ser nulo
    val complemento: String?,
    // 5. Bairro do endereço, pode ser nulo
    val bairro: String?,
    // 6. CEP do endereço, pode ser nulo
    val cep: String?,
    // 7. Município do endereço, pode ser nulo
    val municipio: String?,
    // 8. Unidade federativa (estado), pode ser nulo
    val uf: String?
)