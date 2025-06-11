package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

// 1. Define uma classe de dados para armazenar informações de CNPJ retornadas por uma API
data class CnpjData(
    // 2. Nome oficial da empresa
    @SerializedName("nome") val nome: String?,
    // 3. Nome fantasia da empresa
    @SerializedName("fantasia") val nomeFantasia: String?,
    // 4. Logradouro (rua, avenida, etc.) do endereço
    @SerializedName("logradouro") val logradouro: String?,
    // 5. Número do endereço
    @SerializedName("numero") val numero: String?,
    // 6. Complemento do endereço (apartamento, casa, etc.)
    @SerializedName("complemento") val complemento: String?,
    // 7. Bairro do endereço
    @SerializedName("bairro") val bairro: String?,
    // 8. Município (cidade) do endereço
    @SerializedName("municipio") val municipio: String?,
    // 9. Unidade Federativa (estado) do endereço
    @SerializedName("uf") val uf: String?,
    // 10. CEP do endereço
    @SerializedName("cep") val cep: String?,
    // 11. Email da empresa
    @SerializedName("email") val email: String?,
    // 12. Telefone da empresa
    @SerializedName("telefone") val telefone: String?,
    // 13. Status da consulta do CNPJ
    @SerializedName("status") val status: String?,
    // 14. Mensagem retornada pela API (ex.: erro ou informação adicional)
    @SerializedName("mensagem") val mensagem: String?
)