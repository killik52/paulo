package com.example.myapplication

import android.provider.BaseColumns

object FaturaContract {
    object FaturaEntry : BaseColumns {
        const val TABLE_NAME = "faturas"
        const val COLUMN_NAME_NUMERO_FATURA = "numero_fatura"
        const val COLUMN_NAME_CLIENTE = "cliente"
        const val COLUMN_NAME_ARTIGOS = "artigos"
        const val COLUMN_NAME_SUBTOTAL = "subtotal"
        const val COLUMN_NAME_DESCONTO = "desconto"
        const val COLUMN_NAME_DESCONTO_PERCENT = "desconto_percent"
        const val COLUMN_NAME_TAXA_ENTREGA = "taxa_entrega"
        const val COLUMN_NAME_SALDO_DEVEDOR = "saldo_devedor"
        const val COLUMN_NAME_DATA = "data"
        const val COLUMN_NAME_FOTO_IMPRESSORA = "fotos_impressora"
        const val COLUMN_NAME_NOTAS = "notas"
        const val COLUMN_NAME_FOI_ENVIADA = "foi_enviada"

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
                $COLUMN_NAME_NOTAS TEXT,
                $COLUMN_NAME_FOI_ENVIADA INTEGER DEFAULT 0
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    object FaturaItemEntry : BaseColumns {
        const val TABLE_NAME = "fatura_itens"
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        const val COLUMN_NAME_ARTIGO_ID = "artigo_id"
        const val COLUMN_NAME_QUANTIDADE = "quantidade"
        const val COLUMN_NAME_PRECO = "preco"
        const val COLUMN_NAME_CLIENTE_ID = "cliente_id"

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_ARTIGO_ID INTEGER,
                $COLUMN_NAME_QUANTIDADE INTEGER,
                $COLUMN_NAME_PRECO REAL,
                $COLUMN_NAME_CLIENTE_ID INTEGER,
                FOREIGN KEY($COLUMN_NAME_FATURA_ID) REFERENCES ${FaturaEntry.TABLE_NAME}(${BaseColumns._ID}),
                FOREIGN KEY($COLUMN_NAME_ARTIGO_ID) REFERENCES ${ArtigoContract.ArtigoEntry.TABLE_NAME}(${BaseColumns._ID}),
                FOREIGN KEY($COLUMN_NAME_CLIENTE_ID) REFERENCES ${ClienteContract.ClienteEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    object FaturaNotaEntry : BaseColumns {
        const val TABLE_NAME = "fatura_notas"
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        const val COLUMN_NAME_NOTA = "nota"

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_NOTA TEXT,
                FOREIGN KEY($COLUMN_NAME_FATURA_ID) REFERENCES ${FaturaEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    object FaturaFotoEntry : BaseColumns {
        const val TABLE_NAME = "fatura_fotos"
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        const val COLUMN_NAME_PHOTO_PATH = "photo_path"

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_PHOTO_PATH TEXT,
                FOREIGN KEY($COLUMN_NAME_FATURA_ID) REFERENCES ${FaturaEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}