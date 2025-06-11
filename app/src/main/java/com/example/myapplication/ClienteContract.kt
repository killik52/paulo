package com.example.myapplication

import android.provider.BaseColumns

// Define o contrato para a tabela de clientes no banco de dados
object ClienteContract {
    // Objeto interno que herda BaseColumns para incluir _ID
    object ClienteEntry : BaseColumns {
        // Nome da tabela de clientes
        const val TABLE_NAME = "clientes"
        // Coluna para o nome do cliente
        const val COLUMN_NAME_NOME = "nome"
        // Coluna para o email do cliente
        const val COLUMN_NAME_EMAIL = "email"
        // Coluna para o telefone do cliente
        const val COLUMN_NAME_TELEFONE = "telefone"
        // Coluna para informações adicionais do cliente
        const val COLUMN_NAME_INFORMACOES_ADICIONAIS = "informacoes_adicionais"
        // Coluna para o CPF do cliente
        const val COLUMN_NAME_CPF = "cpf"
        // Coluna para o CNPJ do cliente
        const val COLUMN_NAME_CNPJ = "cnpj"
        // Coluna para o logradouro do endereço
        const val COLUMN_NAME_LOGRADOURO = "logradouro"
        // Coluna para o número do endereço
        const val COLUMN_NAME_NUMERO = "numero"
        // Coluna para o complemento do endereço
        const val COLUMN_NAME_COMPLEMENTO = "complemento"
        // Coluna para o bairro do endereço
        const val COLUMN_NAME_BAIRRO = "bairro"
        // Coluna para o município do endereço
        const val COLUMN_NAME_MUNICIPIO = "municipio"
        // Coluna para a UF (estado) do endereço
        const val COLUMN_NAME_UF = "uf"
        // Coluna para o CEP do endereço
        const val COLUMN_NAME_CEP = "cep"
        // Coluna para o número serial do cliente
        const val COLUMN_NAME_NUMERO_SERIAL = "numero_serial"

        // Comando SQL para criar a tabela de clientes
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NOME TEXT,
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

        // Comando SQL para excluir a tabela de clientes
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}