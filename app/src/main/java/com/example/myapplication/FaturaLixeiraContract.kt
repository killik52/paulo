package com.example.myapplication

import android.provider.BaseColumns

// 1. Define o contrato para a tabela de faturas na lixeira no banco de dados
object FaturaLixeiraContract {
    // 2. Objeto para a tabela de faturas na lixeira, herdando BaseColumns para incluir _ID
    object FaturaLixeiraEntry : BaseColumns {
        // 3. Define o nome da tabela de faturas na lixeira
        const val TABLE_NAME = "faturas_lixeira"
        // 4. Define a coluna para o número da fatura
        const val COLUMN_NAME_NUMERO_FATURA = "numero_fatura"
        // 5. Define a coluna para o nome do cliente
        const val COLUMN_NAME_CLIENTE = "cliente"
        // 6. Define a coluna para os artigos associados
        const val COLUMN_NAME_ARTIGOS = "artigos"
        // 7. Define a coluna para o subtotal da fatura
        const val COLUMN_NAME_SUBTOTAL = "subtotal"
        // 8. Define a coluna para o desconto aplicado
        const val COLUMN_NAME_DESCONTO = "desconto"
        // 9. Define a coluna para a porcentagem de desconto
        const val COLUMN_NAME_DESCONTO_PERCENT = "desconto_percent"
        // 10. Define a coluna para a taxa de entrega
        const val COLUMN_NAME_TAXA_ENTREGA = "taxa_entrega"
        // 11. Define a coluna para o saldo devedor
        const val COLUMN_NAME_SALDO_DEVEDOR = "saldo_devedor"
        // 12. Define a coluna para a data da fatura
        const val COLUMN_NAME_DATA = "data"
        // 13. Define a coluna para os caminhos das fotos da impressora
        const val COLUMN_NAME_FOTO_IMPRESSORA = "fotos_impressora"
        // 14. Define a coluna para notas adicionais
        const val COLUMN_NAME_NOTAS = "notas"

        // 15. Define o comando SQL para criar a tabela de faturas na lixeira
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NUMERO_FATURA TEXT,
                $COLUMN_NAME_CLIENTE TEXT,
                $COLUMN_NAME_ARTIGOS TEXT,
                $COLUMN_NAME_SUBTOTAL REAL,
                $COLUMN_NAME_DESCONTO REAL,
                $COLUMN_NAME_DESCONTO_PERCENT INTEGER,
                $COLUMN_NAME_TAXA_ENTREGA REAL,
                $COLUMN_NAME_SALDO_DEVEDOR REAL,
                $COLUMN_NAME_DATA TEXT,
                $COLUMN_NAME_FOTO_IMPRESSORA TEXT,
                $COLUMN_NAME_NOTAS TEXT
            )
        """

        // 16. Define o comando SQL para excluir a tabela de faturas na lixeira
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}