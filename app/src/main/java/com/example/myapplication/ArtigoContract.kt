package com.example.myapplication

import android.provider.BaseColumns

object ArtigoContract {
    object ArtigoEntry : BaseColumns {
        // 1. Define o nome da tabela de artigos
        const val TABLE_NAME = "artigos"
        // 2. Define a coluna para o nome do artigo
        const val COLUMN_NAME_NOME = "nome"
        // 3. Define a coluna para o preço do artigo
        const val COLUMN_NAME_PRECO = "preco"
        // 4. Define a coluna para a quantidade do artigo
        const val COLUMN_NAME_QUANTIDADE = "quantidade"
        // 5. Define a coluna para o desconto do artigo
        const val COLUMN_NAME_DESCONTO = "desconto"
        // 6. Define a coluna para a descrição do artigo
        const val COLUMN_NAME_DESCRICAO = "descricao"
        // 7. Define a coluna para indicar se a fatura deve ser guardada
        const val COLUMN_NAME_GUARDAR_FATURA = "guardar_fatura"
        // 8. Define a coluna para o número serial do artigo
        const val COLUMN_NAME_NUMERO_SERIAL = "numero_serial"

        // 9. Define o comando SQL para criar a tabela de artigos
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NOME TEXT,
                $COLUMN_NAME_PRECO REAL,
                $COLUMN_NAME_QUANTIDADE INTEGER,
                $COLUMN_NAME_DESCONTO REAL,
                $COLUMN_NAME_DESCRICAO TEXT,
                $COLUMN_NAME_GUARDAR_FATURA INTEGER,
                $COLUMN_NAME_NUMERO_SERIAL TEXT
            )
        """

        // 10. Define o comando SQL para excluir a tabela de artigos
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}