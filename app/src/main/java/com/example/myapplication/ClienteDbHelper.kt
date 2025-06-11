package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log

class ClienteDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "myapplication.db"
        const val DATABASE_VERSION = 19 // Incrementado de 18 para 19
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ClienteContract.ClienteEntry.SQL_CREATE_ENTRIES)
        db.execSQL(ArtigoContract.ArtigoEntry.SQL_CREATE_ENTRIES)
        db.execSQL(FaturaContract.FaturaEntry.SQL_CREATE_ENTRIES)
        db.execSQL(FaturaContract.FaturaItemEntry.SQL_CREATE_ENTRIES)
        db.execSQL(FaturaContract.FaturaNotaEntry.SQL_CREATE_ENTRIES)
        db.execSQL(FaturaContract.FaturaFotoEntry.SQL_CREATE_ENTRIES)
        db.execSQL(FaturaLixeiraContract.FaturaLixeiraEntry.SQL_CREATE_ENTRIES)
        db.execSQL(ClientesBloqueadosContract.ClienteBloqueadoEntry.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("ClienteDbHelper", "Atualizando banco da versão $oldVersion para $newVersion")
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE ${FaturaContract.FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS} TEXT")
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Erro ao adicionar coluna notas: ${e.message}")
            }
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE ${FaturaContract.FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA} TEXT")
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Coluna fotos_impressora já existe: ${e.message}")
            }
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE ${FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME} ADD COLUMN ${FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_FOTO_IMPRESSORA} TEXT")
                db.execSQL("ALTER TABLE ${FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME} ADD COLUMN ${FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NOTAS} TEXT")
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Colunas fotos_impressora ou notas já existem: ${e.message}")
            }
        }
        if (oldVersion < 10) {
            try {
                db.execSQL(FaturaContract.FaturaFotoEntry.SQL_CREATE_ENTRIES)
                migratePhotosToFaturaFotos(db)
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Erro ao criar fatura_fotos: ${e.message}")
            }
        }
        if (oldVersion < 11) {
            try {
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ} TEXT")
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Coluna cnpj já existe: ${e.message}")
            }
        }
        if (oldVersion < 12) {
            try {
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO_SERIAL} TEXT")
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Coluna numero_serial já existe: ${e.message}")
            }
        }
        if (oldVersion < 13) {
            try {
                db.execSQL(FaturaContract.FaturaItemEntry.SQL_CREATE_ENTRIES)
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Tabela fatura_itens já existe: ${e.message}")
            }
        }
        if (oldVersion < 14) {
            try {
                db.execSQL(FaturaContract.FaturaFotoEntry.SQL_CREATE_ENTRIES)
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Tabela fatura_fotos já existe: ${e.message}")
            }
        }
        if (oldVersion < 15) {
            try {
                db.execSQL(ClientesBloqueadosContract.ClienteBloqueadoEntry.SQL_CREATE_ENTRIES)
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Tabela clientes_bloqueados já existe: ${e.message}")
            }
        }
        if (oldVersion < 16) {
            try {
                db.execSQL("ALTER TABLE ${ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME} ADD COLUMN ${ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL} TEXT")
            } catch (e: Exception) {
                Log.w("ClienteDbHelper", "Coluna numero_serial já existe em clientes_bloqueados: ${e.message}")
            }
        }
        if (oldVersion < 17) {
            try {
                db.execSQL("ALTER TABLE ${FaturaContract.FaturaEntry.TABLE_NAME} ADD COLUMN ${FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA} INTEGER DEFAULT 0")
                Log.i("ClienteDbHelper", "Coluna ${FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA} adicionada à tabela ${FaturaContract.FaturaEntry.TABLE_NAME}")
            } catch (e: Exception) {
                Log.e("ClienteDbHelper", "Erro ao adicionar coluna ${FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA}: ${e.message}")
            }
        }
        if (oldVersion < 18) {
            try {
                db.execSQL("ALTER TABLE ${FaturaContract.FaturaItemEntry.TABLE_NAME} ADD COLUMN ${FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID} INTEGER")
                Log.i("ClienteDbHelper", "Coluna ${FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID} adicionada à tabela ${FaturaContract.FaturaItemEntry.TABLE_NAME}")
            } catch (e: Exception) {
                Log.e("ClienteDbHelper", "Erro ao adicionar coluna ${FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID}: ${e.message}")
            }
        }
        if (oldVersion < 19) {
            try {
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_CPF} TEXT")
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_LOGRADOURO} TEXT")
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO} TEXT")
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_COMPLEMENTO} TEXT")
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_BAIRRO} TEXT")
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_MUNICIPIO} TEXT")
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_UF} TEXT")
                db.execSQL("ALTER TABLE ${ClienteContract.ClienteEntry.TABLE_NAME} ADD COLUMN ${ClienteContract.ClienteEntry.COLUMN_NAME_CEP} TEXT")
                Log.i("ClienteDbHelper", "Colunas cpf, logradouro, numero, complemento, bairro, municipio, uf, cep adicionadas à tabela ${ClienteContract.ClienteEntry.TABLE_NAME}")
            } catch (e: Exception) {
                Log.e("ClienteDbHelper", "Erro ao adicionar colunas à tabela clientes: ${e.message}")
            }
        }
    }

    private fun migratePhotosToFaturaFotos(db: SQLiteDatabase) {
        try {
            val cursor = db.query(
                FaturaContract.FaturaEntry.TABLE_NAME,
                arrayOf(BaseColumns._ID, FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA),
                "${FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA} IS NOT NULL AND ${FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA} != ''",
                null, null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val faturaId = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
                    val fotosString = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA))
                    if (!fotosString.isNullOrEmpty()) {
                        val photoPaths = fotosString.split("|").filter { it.isNotEmpty() }
                        photoPaths.forEach { path ->
                            val values = ContentValues().apply {
                                put(FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID, faturaId)
                                put(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH, path)
                            }
                            val newRowId = db.insert(FaturaContract.FaturaFotoEntry.TABLE_NAME, null, values)
                            if (newRowId != -1L) {
                                Log.d("ClienteDbHelper", "Foto migrada: faturaId=$faturaId, path=$path")
                            } else {
                                Log.w("ClienteDbHelper", "Erro ao migrar foto: faturaId=$faturaId, path=$path")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ClienteDbHelper", "Erro ao migrar fotos: ${e.message}", e)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("ClienteDbHelper", "Downgrading banco da versão $oldVersion para $newVersion")
        try {
            db.execSQL(ClienteContract.ClienteEntry.SQL_DELETE_ENTRIES)
            db.execSQL(ArtigoContract.ArtigoEntry.SQL_DELETE_ENTRIES)
            db.execSQL(FaturaContract.FaturaEntry.SQL_DELETE_ENTRIES)
            db.execSQL(FaturaContract.FaturaItemEntry.SQL_DELETE_ENTRIES)
            db.execSQL(FaturaContract.FaturaNotaEntry.SQL_DELETE_ENTRIES)
            db.execSQL(FaturaContract.FaturaFotoEntry.SQL_DELETE_ENTRIES)
            db.execSQL(FaturaLixeiraContract.FaturaLixeiraEntry.SQL_DELETE_ENTRIES)
            db.execSQL(ClientesBloqueadosContract.ClienteBloqueadoEntry.SQL_DELETE_ENTRIES)
            onCreate(db)
        } catch (e: Exception) {
            Log.e("ClienteDbHelper", "Erro ao downgradear banco: ${e.message}", e)
        }
    }
}