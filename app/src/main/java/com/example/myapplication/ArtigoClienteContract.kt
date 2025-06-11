package com.example.myapplication

import android.provider.BaseColumns

object ArtigoClienteContract {
    object ArtigoClienteEntry : BaseColumns {
        const val TABLE_NAME = "artigo_cliente"
        const val COLUMN_NAME_ARTIGO_ID = "artigo_id"
        const val COLUMN_NAME_CLIENTE_ID = "cliente_id"
        const val COLUMN_NAME_NUMERO_SERIAL = "numero_serial"
        const val COLUMN_NAME_FATURA_ID = "fatura_id"

        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_ARTIGO_ID INTEGER,
                $COLUMN_NAME_CLIENTE_ID INTEGER,
                $COLUMN_NAME_NUMERO_SERIAL TEXT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                FOREIGN KEY($COLUMN_NAME_ARTIGO_ID) REFERENCES ${ArtigoContract.ArtigoEntry.TABLE_NAME}(${BaseColumns._ID}),
                FOREIGN KEY($COLUMN_NAME_CLIENTE_ID) REFERENCES ${ClienteContract.ClienteEntry.TABLE_NAME}(${BaseColumns._ID}),
                FOREIGN KEY($COLUMN_NAME_FATURA_ID) REFERENCES ${FaturaContract.FaturaEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}