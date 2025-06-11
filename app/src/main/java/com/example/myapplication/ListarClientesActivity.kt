package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import android.database.Cursor
import android.util.Log
import android.view.Menu
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar

class ListarClientesActivity : AppCompatActivity() {

    private lateinit var listViewClientes: ListView
    private var dbHelper: ClienteDbHelper? = null

    // Lista completa de clientes para restaurar a busca
    private val todosOsClientes = mutableListOf<ClienteItem>()

    // Lista que será exibida e filtrada
    private val clientesParaExibir = mutableListOf<String>()

    private lateinit var adapter: ArrayAdapter<String>
    private val REQUEST_CODE_EDIT_CLIENTE = 1001

    data class ClienteItem(val id: Long, val nome: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_clientes)

        val toolbar: Toolbar = findViewById(R.id.toolbar_listar_clientes)
        setSupportActionBar(toolbar)

        try {
            dbHelper = ClienteDbHelper(this)
        } catch (e: Exception) {
            Log.e("ListarClientes", "Erro ao inicializar banco: ${e.message}")
            showToast("Erro ao inicializar o banco de dados: ${e.message}")
            finish()
            return
        }

        listViewClientes = findViewById(R.id.listViewClientes)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, clientesParaExibir)
        listViewClientes.adapter = adapter

        carregarClientes()

        listViewClientes.setOnItemClickListener { _, _, position, _ ->
            try {
                // Encontra o cliente correspondente na lista original completa
                val nomeSelecionado = clientesParaExibir[position]
                val clienteSelecionado = todosOsClientes.find { it.nome == nomeSelecionado }

                if (clienteSelecionado != null) {
                    abrirDetalhesDoCliente(clienteSelecionado.id)
                }
            } catch (e: Exception) {
                Log.e("ListarClientes", "Erro ao abrir cliente: ${e.message}")
                showToast("Erro ao abrir cliente: ${e.message}")
            }
        }
    }

    private fun carregarClientes() {
        todosOsClientes.clear()
        try {
            val db = dbHelper?.readableDatabase
            if (db == null) {
                showToast("Erro: Banco de dados não inicializado.")
                finish()
                return
            }

            val cursor: Cursor? = db.rawQuery(
                "SELECT ${android.provider.BaseColumns._ID}, " +
                        "${ClienteContract.ClienteEntry.COLUMN_NAME_NOME} " +
                        "FROM ${ClienteContract.ClienteEntry.TABLE_NAME} " +
                        "ORDER BY ${ClienteContract.ClienteEntry.COLUMN_NAME_NOME} ASC",
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                    val nome = it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME))
                    todosOsClientes.add(ClienteItem(id, nome))
                }
            } ?: run {
                showToast("Erro: Não foi possível acessar os dados dos clientes.")
            }

            filtrarLista("") // Exibe a lista completa inicialmente

        } catch (e: Exception) {
            Log.e("ListarClientes", "Erro ao carregar clientes: ${e.message}")
            showToast("Erro ao carregar clientes: ${e.message}")
        }
    }

    private fun abrirDetalhesDoCliente(clienteId: Long) {
        val db = dbHelper?.readableDatabase ?: return
        val cursor = db.rawQuery(
            "SELECT * FROM ${ClienteContract.ClienteEntry.TABLE_NAME} WHERE ${android.provider.BaseColumns._ID} = ?",
            arrayOf(clienteId.toString())
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val intent = Intent(this, ClienteActivity::class.java).apply {
                    putExtra("id", it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID)))
                    putExtra("nome", it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME)))
                    putExtra("email", it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL)))
                    putExtra("telefone", it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE)))
                    putExtra("informacoesAdicionais", it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS)))
                    putExtra("cpf", it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CPF)))
                    putExtra("cnpj", it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ)))
                }
                startActivityForResult(intent, REQUEST_CODE_EDIT_CLIENTE)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Não é necessário, pois a filtragem acontece em tempo real
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarLista(newText.orEmpty())
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    private fun filtrarLista(query: String) {
        val listaFiltrada = if (query.isEmpty()) {
            todosOsClientes.map { it.nome }
        } else {
            todosOsClientes.filter {
                it.nome.contains(query, ignoreCase = true)
            }.map { it.nome }
        }

        clientesParaExibir.clear()
        clientesParaExibir.addAll(listaFiltrada)
        adapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT_CLIENTE) {
            // Recarrega a lista para refletir quaisquer alterações (exclusão, edição de nome)
            carregarClientes()
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