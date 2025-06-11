// 1. Define o pacote onde a classe está localizada
package com.example.myapplication

// 2. Define uma classe de dados para representar um cliente bloqueado
data class ClienteBloqueado(
    // 3. Identificador único do cliente
    val id: Long,
    // 4. Nome completo do cliente
    val nome: String,
    // 5. Endereço de email do cliente
    val email: String,
    // 6. Número de telefone do cliente
    val telefone: String,
    // 7. Informações adicionais sobre o cliente
    val informacoesAdicionais: String,
    // 8. CPF do cliente (para pessoas físicas)
    val cpf: String,
    // 9. CNPJ do cliente (para pessoas jurídicas)
    val cnpj: String,
    // 10. Logradouro (rua, avenida, etc.) do endereço
    val logradouro: String,
    // 11. Número do endereço
    val numero: String,
    // 12. Complemento do endereço (apartamento, casa, etc.)
    val complemento: String,
    // 13. Bairro do endereço
    val bairro: String,
    // 14. Município (cidade) do endereço
    val municipio: String,
    // 15. Unidade Federativa (estado) do endereço
    val uf: String,
    // 16. CEP do endereço
    val cep: String,
    // 17. Número serial associado ao cliente
    val numeroSerial: String
)