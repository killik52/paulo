package com.example.myapplication.model

// 1. Classe de dados que representa a resposta da API para consulta de CNPJ
data class CnpjResponse(
    // 2. Lista de atividades principais da empresa, pode ser nula
    val atividade_principal: List<Atividade>?,
    // 3. Lista de atividades secundárias da empresa, pode ser nula
    val atividades_secundarias: List<Atividade>?,
    // 4. Número do CNPJ, pode ser nulo
    val cnpj: String?,
    // 5. Nome oficial da empresa, pode ser nulo
    val nome: String?,
    // 6. Nome fantasia da empresa, pode ser nulo
    val fantasia: String?,
    // 7. Natureza jurídica da empresa, pode ser nula
    val natureza_juridica: String?,
    // 8. Logradouro do endereço, pode ser nulo
    val logradouro: String?,
    // 9. Número do endereço, pode ser nulo
    val numero: String?,
    // 10. Complemento do endereço, pode ser nulo
    val complemento: String?,
    // 11. Bairro do endereço, pode ser nulo
    val bairro: String?,
    // 12. Município do endereço, pode ser nulo
    val municipio: String?,
    // 13. Unidade federativa (estado), pode ser nula
    val uf: String?,
    // 14. CEP do endereço, pode ser nulo
    val cep: String?,
    // 15. E-mail da empresa, pode ser nulo
    val email: String?,
    // 16. Telefone da empresa, pode ser nulo
    val telefone: String?,
    // 17. Data de abertura da empresa, pode ser nula
    val abertura: String?,
    // 18. Situação cadastral da empresa, pode ser nula
    val situacao: String?,
    // 19. Data da situação cadastral, pode ser nula
    val data_situacao: String?,
    // 20. Tipo da empresa (e.g., matriz/filial), pode ser nulo
    val tipo: String?,
    // 21. Porte da empresa, pode ser nulo
    val porte: String?,
    // 22. Data da última atualização dos dados, pode ser nula
    val ultima_atualizacao: String?,
    // 23. Status da resposta da API (e.g., "OK" ou erro), pode ser nulo
    val status: String?,
    // 24. Mensagem de erro ou informação adicional, pode ser nula
    val message: String?
) {
    // 25. Classe de dados interna que representa uma atividade (principal ou secundária)
    data class Atividade(
        // 26. Código da atividade, pode ser nulo
        val code: String?,
        // 27. Texto descritivo da atividade, pode ser nulo
        val text: String?
    )
}