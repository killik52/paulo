package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import android.database.Cursor
import android.util.Log
import android.widget.ArrayAdapter

// 1. Classe que representa a atividade para listar artigos
class ListarArtigosActivity : AppCompatActivity() {

    // 2. Variável para o ListView que exibe os artigos
    private var listViewArtigos: ListView? = null
    // 3. Helper para acessar o banco de dados
    private var dbHelper: ClienteDbHelper? = null
    // 4. Lista mutável para armazenar os itens de artigo
    private val artigosList = mutableListOf<ArtigoItem>()

    // 5. Classe de dados para representar um artigo com ID e nome
    data class ArtigoItem(val id: Long, val nome: String)

    // 6. Método chamado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 7. Define o layout da atividade
        setContentView(R.layout.activity_listar_artigos)

        try {
            // 8. Inicializa o helper do banco de dados
            dbHelper = ClienteDbHelper(this)
        } catch (e: Exception) {
            // 9. Registra e exibe erro se o banco não for inicializado
            Log.e("ListarArtigos", "Erro ao inicializar banco: ${e.message}")
            showToast("Erro ao inicializar o banco de dados: ${e.message}")
            finish()
            return
        }

        // 10. Referencia o ListView do layout
        listViewArtigos = findViewById(R.id.listViewArtigos)
        // 11. Carrega os artigos do banco de dados
        carregarArtigos()
    }

    // 12. Método para carregar os artigos do banco de dados
    private fun carregarArtigos() {
        // 13. Limpa a lista de artigos
        artigosList.clear()
        // 14. Cria uma lista para os nomes dos artigos
        val nomesList = mutableListOf<String>()
        try {
            // 15. Obtém o banco de dados para leitura
            val db = dbHelper?.readableDatabase
            if (db == null) {
                // 16. Exibe mensagem e finaliza se o banco não estiver inicializado
                showToast("Erro: Banco de dados não inicializado.")
                finish()
                return
            }

            // 17. Consulta os artigos no banco de dados, ordenados por nome
            val cursor: Cursor? = db.rawQuery(
                "SELECT ${android.provider.BaseColumns._ID}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_PRECO}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_QUANTIDADE}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL}, " +
                        "${ArtigoContract.ArtigoEntry.COLUMN_NAME_DESCRICAO} " +
                        "FROM ${ArtigoContract.ArtigoEntry.TABLE_NAME} " +
                        "ORDER BY ${ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME} ASC",
                null
            )

            // 18. Itera sobre o cursor para preencher as listas
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                    val nome = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME))
                    nomesList.add(nome)
                    artigosList.add(ArtigoItem(id, nome))
                }
            } ?: run {
                // 19. Exibe mensagem se o cursor for nulo
                showToast("Erro: Não foi possível acessar os dados dos artigos.")
            }

            // 20. Cria um adaptador para o ListView com os nomes dos artigos
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nomesList)
            // 21. Define o adaptador no ListView
            listViewArtigos?.adapter = adapter

            // 22. Adiciona listener para abrir a tela de edição ao clicar em um artigo
            listViewArtigos?.setOnItemClickListener { _, _, position, _ ->
                try {
                    // 23. Obtém o artigo selecionado
                    val artigoSelecionado = artigosList[position]
                    // 24. Obtém o banco de dados para leitura
                    val db = dbHelper?.readableDatabase
                    // 25. Consulta os detalhes do artigo selecionado
                    val cursor = db?.rawQuery(
                        "SELECT * FROM ${ArtigoContract.ArtigoEntry.TABLE_NAME} " +
                                "WHERE ${android.provider.BaseColumns._ID} = ?",
                        arrayOf(artigoSelecionado.id.toString())
                    )

                    // 26. Itera sobre o cursor para obter os dados do artigo
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val id = it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                            val nome = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME))
                            val preco = it.getDouble(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_PRECO))
                            val quantidade = it.getInt(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_QUANTIDADE))
                            val numeroSerial = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL))
                            val descricao = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_DESCRICAO))

                            // 27. Cria uma Intent para abrir a tela de edição
                            val intent = Intent(this, CriarNovoArtigoActivity::class.java).apply {
                                putExtra("artigo_id", id)
                                putExtra("nome_artigo", nome)
                                putExtra("valor", preco)
                                putExtra("quantidade", quantidade)
                                putExtra("numero_serial", numeroSerial)
                                putExtra("descricao", descricao)
                            }
                            // 28. Inicia a atividade de edição
                            startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    // 29. Registra e exibe erro se houver falha ao abrir o artigo
                    Log.e("ListarArtigos", "Erro ao abrir artigo: ${e.message}")
                    showToast("Erro ao abrir artigo: ${e.message}")
                }
            }
        } catch (e: Exception) {
            // 30. Registra e exibe erro se houver falha ao carregar os artigos
            Log.e("ListarArtigos", "Erro ao carregar artigos: ${e.message}")
            showToast("Erro ao carregar artigos: ${e.message}")
        }
    }

    // 31. Método para exibir mensagens toast
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    // 32. Método chamado quando a atividade é destruída
    override fun onDestroy() {
        // 33. Fecha o helper do banco de dados
        dbHelper?.close()
        // 34. Chama o método onDestroy da superclasse
        super.onDestroy()
    }
}