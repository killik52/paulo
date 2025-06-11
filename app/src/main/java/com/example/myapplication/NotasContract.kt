package com.example.myapplication

import android.provider.BaseColumns

object NotasContract {
    object NotaEntry : BaseColumns {
        const val TABLE_NAME = "notas"
        const val COLUMN_NAME_TITULO = "titulo_nota" // Nome da coluna para o título da nota
        const val COLUMN_NAME_CONTEUDO = "conteudo_nota" // Nome da coluna para o conteúdo da nota
        const val COLUMN_NAME_DATA = "data_nota"         // Nome da coluna para a data da nota
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${NotaEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," + // Definido como autoincremento
                "${NotaEntry.COLUMN_NAME_TITULO} TEXT," +
                "${NotaEntry.COLUMN_NAME_CONTEUDO} TEXT," +
                "${NotaEntry.COLUMN_NAME_DATA} TEXT)" // TEXT para armazenar data como string (ou INTEGER para timestamp)

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${NotaEntry.TABLE_NAME}"
}