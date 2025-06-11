package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ArquivosRecentesActivity : AppCompatActivity() {

    private var listViewArquivosRecentes: ListView? = null
    private var editTextPesquisa: EditText? = null
    private var textViewVerTudoPesquisa: TextView? = null
    private var textViewNovoArquivo: TextView? = null
    private var dbHelper: ClienteDbHelper? = null
    private val artigosList = mutableListOf<ArtigoRecenteItem>()
    private var adapter: ArrayAdapter<String>? = null
    private val displayList = mutableListOf<String>()

    private var isFinishingDueToResultPropagation = false // Flag para otimizar onResume

    data class ArtigoRecenteItem(val id: Long, val nome: String, val preco: Double, val quantidade: Int, val numeroSerial: String?, val descricao: String?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arquivos_recentes)

        dbHelper = ClienteDbHelper(this)

        listViewArquivosRecentes = findViewById(R.id.listViewArquivosRecentes)
        editTextPesquisa = findViewById(R.id.editTextPesquisa)
        textViewVerTudoPesquisa = findViewById(R.id.textViewVerTudoPesquisa)
        textViewNovoArquivo = findViewById(R.id.textViewNovoArquivo)

        displayList.clear()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listViewArquivosRecentes?.adapter = adapter

        // carregarArtigos() será chamado em onResume se necessário

        listViewArquivosRecentes?.setOnItemClickListener { _, _, position, _ ->
            try {
                val nomeArtigoSelecionado = displayList.getOrNull(position)
                val artigoSelecionado = artigosList.find { it.nome == nomeArtigoSelecionado }


                if (artigoSelecionado != null) {
                    val intent = Intent().apply {
                        putExtra("artigo_id", artigoSelecionado.id)
                        putExtra("nome_artigo", artigoSelecionado.nome)
                        putExtra("quantidade", artigoSelecionado.quantidade)
                        putExtra("preco_unitario_artigo", artigoSelecionado.preco)
                        putExtra("valor", artigoSelecionado.preco * artigoSelecionado.quantidade)
                        putExtra("numero_serial", artigoSelecionado.numeroSerial)
                        putExtra("descricao", artigoSelecionado.descricao)
                        putExtra("salvar_fatura", true)
                    }
                    isFinishingDueToResultPropagation = true
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    Log.w("ArquivosRecentes", "Artigo selecionado na posição $position não encontrado nos dados base.")
                    showToast("Erro ao encontrar dados do artigo selecionado.")
                }
            } catch (e: Exception) {
                Log.e("ArquivosRecentes", "Erro ao selecionar artigo: ${e.message}")
                showToast("Erro ao selecionar artigo: ${e.message}")
            }
        }

        textViewNovoArquivo?.setOnClickListener {
            val intent = Intent(this, CriarNovoArtigoActivity::class.java)
            startActivityForResult(intent, 792)
        }

        editTextPesquisa?.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = editTextPesquisa?.text.toString().trim()
                filtrarArtigos(query)
                true
            } else {
                false
            }
        }

        textViewVerTudoPesquisa?.setOnClickListener {
            editTextPesquisa?.setText("")
            filtrarArtigos("")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishingDueToResultPropagation) {
            carregarArtigos()
        }
        // A flag será resetada naturalmente na próxima vez que onCreate for chamado,
        // ou se a atividade for retomada sem estar finalizando por propagação.
    }

    override fun onPause() {
        super.onPause()
        // Se a atividade está finalizando por propagação, resetamos a flag
        // para que um próximo onResume (se ocorrer por algum motivo inesperado)
        // não pule o carregamento. Mas normalmente finish() já resolve.
        if (isFinishing && isFinishingDueToResultPropagation) {
            isFinishingDueToResultPropagation = false
        }
    }

    private fun carregarArtigos() {
        // ... (implementação de carregarArtigos como antes) ...
        artigosList.clear()
        displayList.clear()
        try {
            val db = dbHelper?.readableDatabase
            if (db == null) {
                Log.e("ArquivosRecentes", "Banco de dados não inicializado.")
                return
            }

            val tableCheckCursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='${ArtigoContract.ArtigoEntry.TABLE_NAME}'", null
            )
            if (tableCheckCursor.count == 0) {
                tableCheckCursor.close()
                return
            }
            tableCheckCursor.close()

            val cursor = db.rawQuery(
                "SELECT ${android.provider.BaseColumns._ID}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_PRECO}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_QUANTIDADE}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_DESCRICAO} " +
                        "FROM ${ArtigoContract.ArtigoEntry.TABLE_NAME} " +
                        "WHERE ${ArtigoContract.ArtigoEntry.COLUMN_NAME_GUARDAR_FATURA} = 1 " +
                        "ORDER BY ${android.provider.BaseColumns._ID} DESC",
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                    val nome = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME))
                    val preco = it.getDouble(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_PRECO))
                    val quantidade = it.getInt(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_QUANTIDADE))
                    val numeroSerial = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL))
                    val descricao = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_DESCRICAO))

                    artigosList.add(ArtigoRecenteItem(id, nome, preco, quantidade, numeroSerial, descricao))
                    displayList.add(nome)
                }
            }
            adapter?.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("ArquivosRecentes", "Erro ao carregar artigos guardados: ${e.message}")
        }
    }

    private fun filtrarArtigos(query: String) {
        // ... (implementação de filtrarArtigos como antes) ...
        val filteredListNomes = if (query.isEmpty()) {
            artigosList.map { it.nome }
        } else {
            artigosList.filter {
                it.nome.contains(query, ignoreCase = true) ||
                        (it.numeroSerial?.contains(query, ignoreCase = true) == true) ||
                        (it.descricao?.contains(query, ignoreCase = true) == true)
            }.map { it.nome }
        }

        displayList.clear()
        displayList.addAll(filteredListNomes)
        adapter?.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 792) { // Retorno de CriarNovoArtigoActivity
            if (resultCode == RESULT_OK && data != null) {
                isFinishingDueToResultPropagation = true
                setResult(RESULT_OK, data)
                finish()
            } else {
                isFinishingDueToResultPropagation = false // Garante que onResume recarregue se o usuário cancelou
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}