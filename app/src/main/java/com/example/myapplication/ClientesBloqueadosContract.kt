package com.example.myapplication

import android.provider.BaseColumns

// 1. Define o contrato para a tabela de clientes bloqueados no banco de dados
object ClientesBloqueadosContract {
    // 2. Objeto interno que herda BaseColumns para incluir _ID
    object ClienteBloqueadoEntry : BaseColumns {
        // 3. Define o nome da tabela de clientes bloqueados
        const val TABLE_NAME = "clientes_bloqueados"
        // 4. Define a coluna para o nome do cliente
        const val COLUMN_NAME_NOME = "nome"
        // 5. Define a coluna para o email do cliente
        const val COLUMN_NAME_EMAIL = "email"
        // 6. Define a coluna para o telefone do cliente
        const val COLUMN_NAME_TELEFONE = "telefone"
        // 7. Define a coluna para informações adicionais do cliente
        const val COLUMN_NAME_INFORMACOES_ADICIONAIS = "informacoes_adicionais"
        // 8. Define a coluna para o CPF do cliente
        const val COLUMN_NAME_CPF = "cpf"
        // 9. Define a coluna para o CNPJ do cliente
        const val COLUMN_NAME_CNPJ = "cnpj"
        // 10. Define a coluna para o logradouro do endereço
        const val COLUMN_NAME_LOGRADOURO = "logradouro"
        // 11. Define a coluna para o número do endereço
        const val COLUMN_NAME_NUMERO = "numero"
        // 12. Define a coluna para o complemento do endereço
        const val COLUMN_NAME_COMPLEMENTO = "complemento"
        // 13. Define a coluna para o bairro do endereço
        const val COLUMN_NAME_BAIRRO = "bairro"
        // 14. Define a coluna para o município do endereço
        const val COLUMN_NAME_MUNICIPIO = "municipio"
        // 15. Define a coluna para a UF (estado) do endereço
        const val COLUMN_NAME_UF = "uf"
        // 16. Define a coluna para o CEP do endereço
        const val COLUMN_NAME_CEP = "cep"
        // 17. Define a coluna para o(s) número(s) serial(is) do cliente, separados por vírgula
        const val COLUMN_NAME_NUMERO_SERIAL = "numero_serial" // Continua como TEXT

        // 18. Define o comando SQL para criar a tabela de clientes bloqueados
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NOME TEXT NOT NULL,
                $COLUMN_NAME_EMAIL TEXT,
                $COLUMN_NAME_TELEFONE TEXT,
                $COLUMN_NAME_INFORMACOES_ADICIONAIS TEXT,
                $COLUMN_NAME_CPF TEXT,
                $COLUMN_NAME_CNPJ TEXT,
                $COLUMN_NAME_LOGRADOURO TEXT,
                $COLUMN_NAME_NUMERO TEXT,
                $COLUMN_NAME_COMPLEMENTO TEXT,
                $COLUMN_NAME_BAIRRO TEXT,
                $COLUMN_NAME_MUNICIPIO TEXT,
                $COLUMN_NAME_UF TEXT,
                $COLUMN_NAME_CEP TEXT,
                $COLUMN_NAME_NUMERO_SERIAL TEXT 
            )
        """

        // 19. Define o comando SQL para excluir a tabela de clientes bloqueados
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}