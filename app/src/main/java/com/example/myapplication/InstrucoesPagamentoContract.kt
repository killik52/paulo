package com.example.myapplication

import android.provider.BaseColumns

object InstrucoesPagamentoContract {
    object InstrucoesPagamentoEntry : BaseColumns {
        const val TABLE_NAME = "instrucoes_pagamento"
        const val COLUMN_NAME_TEXTO_INSTRUCAO = "texto_instrucao"
        // Adicione outras colunas se necessário
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${InstrucoesPagamentoEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${InstrucoesPagamentoEntry.COLUMN_NAME_TEXTO_INSTRUCAO} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${InstrucoesPagamentoEntry.TABLE_NAME}"
}