package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class InstrucoesPagamentoActivity : AppCompatActivity() {

    // Variáveis para os novos campos
    private lateinit var editTextPix: EditText
    private lateinit var editTextBanco: EditText
    private lateinit var editTextAgencia: EditText
    private lateinit var editTextConta: EditText
    private lateinit var editTextOutrasInstrucoes: EditText // Renomeado do original

    private lateinit var backButton: ImageView
    private lateinit var saveButton: TextView
    private lateinit var sharedPreferences: SharedPreferences

    // Constantes para as chaves de armazenamento
    private val PREFS_NAME = "InstrucoesPagamentoPrefs"
    private val KEY_PIX = "instrucoes_pix"
    private val KEY_BANCO = "instrucoes_banco"
    private val KEY_AGENCIA = "instrucoes_agencia"
    private val KEY_CONTA = "instrucoes_conta"
    private val KEY_OUTRAS = "instrucoes_outras" // Chave para o campo de texto livre

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instrucoes_pagamento)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Inicializa todos os componentes da UI
        editTextPix = findViewById(R.id.editTextPix)
        editTextBanco = findViewById(R.id.editTextBanco)
        editTextAgencia = findViewById(R.id.editTextAgencia)
        editTextConta = findViewById(R.id.editTextConta)
        editTextOutrasInstrucoes = findViewById(R.id.editTextOutrasInstrucoes) // O ID antigo era editTextInstrucoesPagamento

        backButton = findViewById(R.id.backButtonInstrucoesPagamento)
        saveButton = findViewById(R.id.saveButtonInstrucoesPagamento)

        // Carrega os dados salvos nos campos
        loadInstrucoes()

        backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        saveButton.setOnClickListener {
            saveInstrucoes()
            Toast.makeText(this, "Instruções de pagamento salvas!", Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun loadInstrucoes() {
        // Carrega o valor de cada campo individualmente
        editTextPix.setText(sharedPreferences.getString(KEY_PIX, ""))
        editTextBanco.setText(sharedPreferences.getString(KEY_BANCO, ""))
        editTextAgencia.setText(sharedPreferences.getString(KEY_AGENCIA, ""))
        editTextConta.setText(sharedPreferences.getString(KEY_CONTA, ""))
        editTextOutrasInstrucoes.setText(sharedPreferences.getString(KEY_OUTRAS, ""))
        Log.d("InstrucoesPagamento", "Instruções carregadas dos SharedPreferences.")
    }

    private fun saveInstrucoes() {
        val editor = sharedPreferences.edit()
        // Salva o valor de cada campo individualmente
        editor.putString(KEY_PIX, editTextPix.text.toString())
        editor.putString(KEY_BANCO, editTextBanco.text.toString())
        editor.putString(KEY_AGENCIA, editTextAgencia.text.toString())
        editor.putString(KEY_CONTA, editTextConta.text.toString())
        editor.putString(KEY_OUTRAS, editTextOutrasInstrucoes.text.toString())
        editor.apply() // Aplica as alterações
        Log.d("InstrucoesPagamento", "Instruções salvas nos SharedPreferences.")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}