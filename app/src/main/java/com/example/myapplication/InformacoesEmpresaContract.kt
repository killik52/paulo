package com.example.myapplication

import android.provider.BaseColumns

object InformacoesEmpresaContract {
    object InformacoesEmpresaEntry : BaseColumns {
        const val TABLE_NAME = "informacoes_empresa"
        const val COLUMN_NAME_NOME_EMPRESA = "nome_empresa"
        const val COLUMN_NAME_ENDERECO = "endereco"
        const val COLUMN_NAME_TELEFONE = "telefone"
        const val COLUMN_NAME_EMAIL = "email"
        const val COLUMN_NAME_CNPJ_CPF = "cnpj_cpf" // Adicionado
        const val COLUMN_NAME_LOGO_PATH = "logo_path"  // Adicionado
        // Adicione outras colunas conforme sua necessidade
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${InformacoesEmpresaEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_NOME_EMPRESA} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_ENDERECO} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_TELEFONE} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_EMAIL} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_CNPJ_CPF} TEXT," +
                "${InformacoesEmpresaEntry.COLUMN_NAME_LOGO_PATH} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${InformacoesEmpresaEntry.TABLE_NAME}"
}