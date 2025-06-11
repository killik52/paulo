package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class LixeiraActivity : AppCompatActivity() {

    private var dbHelper: ClienteDbHelper? = null
    private lateinit var faturasRecyclerView: RecyclerView
    private lateinit var faturaLixeiraAdapter: FaturaLixeiraAdapter
    private val RESTORE_FATURA_REQUEST_CODE = 790

    /**
     * [1] Inicializa a atividade, configurando o layout, banco de dados, RecyclerView e adaptador.
     * Carrega as faturas da lixeira e exibe na interface.
     * @param savedInstanceState Estado salvo da atividade, usado para restaurar dados após mudanças de configuração (não utilizado aqui).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lixeira)

        dbHelper = ClienteDbHelper(this)

        faturasRecyclerView = findViewById(R.id.faturasLixeiraRecyclerView)
        faturasRecyclerView.layoutManager = LinearLayoutManager(this)
        faturaLixeiraAdapter = FaturaLixeiraAdapter(
            emptyList(),
            onRestoreClick = { faturaId -> restaurarFatura(faturaId) },
            onLongClick = { faturaId -> excluirFaturaPermanente(faturaId) }
        )
        faturasRecyclerView.adapter = faturaLixeiraAdapter

        carregarFaturasLixeira()
    }

    /**
     * [2] Carrega as faturas da tabela de lixeira do banco de dados e atualiza o RecyclerView.
     * Exibe uma mensagem se a lixeira estiver vazia ou em caso de erro no acesso ao banco.
     */
    private fun carregarFaturasLixeira() {
        val db = dbHelper?.readableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return
        }

        val faturas = mutableListOf<FaturaLixeiraItem>()
        val cursor = db.query(
            FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NUMERO_FATURA))
                val cliente = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_CLIENTE))
                val data = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA))
                faturas.add(FaturaLixeiraItem(id, numeroFatura, cliente, data))
            }
        }
        cursor?.close()

        faturaLixeiraAdapter.updateFaturas(faturas)
        Log.d("LixeiraActivity", "Faturas carregadas da lixeira: ${faturas.size} itens")
        if (faturas.isEmpty()) {
            Toast.makeText(this, "Nenhuma fatura na lixeira", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * [3] Move uma fatura da tabela principal para a tabela de lixeira.
     * @param faturaId O ID da fatura a ser movida para a lixeira.
     */
    fun excluirFatura(faturaId: Long) {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val cursor = db.query(
                FaturaContract.FaturaEntry.TABLE_NAME,
                null,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(faturaId.toString()),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val values = ContentValues().apply {
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NUMERO_FATURA,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_CLIENTE,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_ARTIGOS,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SUBTOTAL,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SUBTOTAL)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO_PERCENT,
                            it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_TAXA_ENTREGA,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SALDO_DEVEDOR,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_FOTO_IMPRESSORA,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NOTAS,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS)))
                    }

                    val newRowId = db.insert(FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME, null, values)
                    if (newRowId != -1L) {
                        val rowsDeleted = db.delete(
                            FaturaContract.FaturaEntry.TABLE_NAME,
                            "${android.provider.BaseColumns._ID} = ?",
                            arrayOf(faturaId.toString())
                        )
                        if (rowsDeleted > 0) {
                            Log.d("LixeiraActivity", "Fatura ID=$faturaId movida para a lixeira com sucesso")
                            Toast.makeText(this, "Fatura movida para a lixeira", Toast.LENGTH_SHORT).show()
                            carregarFaturasLixeira()
                        } else {
                            Log.e("LixeiraActivity", "Erro ao remover fatura ID=$faturaId da tabela faturas")
                            Toast.makeText(this, "Erro ao mover fatura para a lixeira", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("LixeiraActivity", "Erro ao inserir fatura ID=$faturaId na tabela faturas_lixeira")
                        Toast.makeText(this, "Erro ao mover fatura para a lixeira", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("LixeiraActivity", "Fatura ID=$faturaId não encontrada na tabela faturas")
                    Toast.makeText(this, "Fatura não encontrada", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("LixeiraActivity", "Erro ao excluir fatura: ${e.message}")
            Toast.makeText(this, "Erro ao mover fatura para a lixeira: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * [4] Restaura uma fatura da lixeira, movendo-a de volta para a tabela principal de faturas.
     * Inclui logs detalhados para depurar falhas na restauração.
     * @param faturaId O ID da fatura a ser restaurada.
     */
    private fun restaurarFatura(faturaId: Long) {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados: dbHelper.writableDatabase é nulo")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Consulta a fatura na tabela de lixeira
            Log.d("LixeiraActivity", "Consultando fatura ID=$faturaId na tabela faturas_lixeira")
            val cursor = db.query(
                FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
                null,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(faturaId.toString()),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    // Log dos dados da fatura para depuração
                    val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NUMERO_FATURA))
                    val cliente = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_CLIENTE))
                    Log.d("LixeiraActivity", "Fatura encontrada: ID=$faturaId, numero_fatura=$numeroFatura, cliente=$cliente")

                    // Prepara os dados da fatura para inserção na tabela principal
                    val values = ContentValues().apply {
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA, numeroFatura)
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE, cliente)
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_ARTIGOS)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_SUBTOTAL,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SUBTOTAL)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT,
                            it.getInt(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO_PERCENT)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_TAXA_ENTREGA)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SALDO_DEVEDOR)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_DATA,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_FOTO_IMPRESSORA)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NOTAS)))
                    }

                    // Log dos valores a serem inseridos
                    Log.d("LixeiraActivity", "Valores preparados para inserção: $values")

                    // Insere a fatura na tabela principal
                    val newRowId = db.insert(FaturaContract.FaturaEntry.TABLE_NAME, null, values)
                    if (newRowId != -1L) {
                        Log.d("LixeiraActivity", "Fatura inserida na tabela faturas com novo ID=$newRowId")
                        // Remove a fatura da lixeira
                        val rowsDeleted = db.delete(
                            FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
                            "${android.provider.BaseColumns._ID} = ?",
                            arrayOf(faturaId.toString())
                        )
                        if (rowsDeleted > 0) {
                            Log.d("LixeiraActivity", "Fatura ID=$faturaId removida da lixeira com sucesso")
                            Toast.makeText(this, "Fatura restaurada com sucesso", Toast.LENGTH_SHORT).show()
                            carregarFaturasLixeira()
                            // Configura o resultado para a atividade chamadora
                            val resultIntent = Intent().apply {
                                putExtra("fatura_restaurada", true)
                                putExtra("fatura_id", newRowId)
                            }
                            Log.d("LixeiraActivity", "Resultado configurado: fatura_restaurada=true, fatura_id=$newRowId")
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {
                            Log.e("LixeiraActivity", "Erro ao remover fatura ID=$faturaId da tabela faturas_lixeira: nenhuma linha afetada")
                            Toast.makeText(this, "Erro ao remover fatura da lixeira", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("LixeiraActivity", "Erro ao inserir fatura ID=$faturaId na tabela faturas: inserção falhou")
                        Toast.makeText(this, "Erro ao inserir fatura na tabela principal", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("LixeiraActivity", "Fatura ID=$faturaId não encontrada na tabela faturas_lixeira")
                    Toast.makeText(this, "Fatura não encontrada na lixeira", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Log.e("LixeiraActivity", "Cursor nulo ao consultar fatura ID=$faturaId")
                Toast.makeText(this, "Erro ao consultar fatura", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("LixeiraActivity", "Exceção ao restaurar fatura ID=$faturaId: ${e.message}", e)
            Toast.makeText(this, "Erro ao restaurar fatura: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * [5] Exclui uma fatura permanentemente da tabela de lixeira.
     * @param faturaId O ID da fatura a ser excluída.
     * @return True se a exclusão for bem-sucedida, false caso contrário.
     */
    private fun excluirFaturaPermanente(faturaId: Long): Boolean {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return false
        }

        try {
            val rowsDeleted = db.delete(
                FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(faturaId.toString())
            )
            if (rowsDeleted > 0) {
                Log.d("LixeiraActivity", "Fatura ID=$faturaId excluída permanentemente com sucesso")
                Toast.makeText(this, "Fatura excluída permanentemente", Toast.LENGTH_SHORT).show()
                carregarFaturasLixeira()
                return true
            } else {
                Log.w("LixeiraActivity", "Fatura ID=$faturaId não encontrada na lixeira")
                Toast.makeText(this, "Fatura não encontrada na lixeira", Toast.LENGTH_LONG).show()
                return false
            }
        } catch (e: Exception) {
            Log.e("LixeiraActivity", "Erro ao excluir fatura permanentemente: ${e.message}")
            Toast.makeText(this, "Erro ao excluir fatura: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }
}

data class FaturaLixeiraItem(
    val id: Long,
    val numeroFatura: String,
    val cliente: String,
    val data: String
)

class FaturaLixeiraAdapter(
    private var faturas: List<FaturaLixeiraItem>,
    private val onRestoreClick: (Long) -> Unit,
    private val onLongClick: (Long) -> Boolean
) : RecyclerView.Adapter<FaturaLixeiraAdapter.FaturaViewHolder>() {

    class FaturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numeroFaturaTextView: TextView = itemView.findViewById(R.id.numeroFaturaTextView)
        val clienteTextView: TextView = itemView.findViewById(R.id.clienteTextView)
        val dataTextView: TextView = itemView.findViewById(R.id.dataTextView)
        val restoreButton: Button = itemView.findViewById(R.id.restoreButton)
    }

    /**
     * [6] Cria um novo ViewHolder para um item da lista de faturas.
     * @param parent O ViewGroup pai onde a view será inflada.
     * @param viewType O tipo de view (não utilizado aqui).
     * @return Um novo FaturaViewHolder com a view inflada.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaturaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fatura_lixeira, parent, false)
        return FaturaViewHolder(view)
    }

    /**
     * [7] Vincula os dados de uma fatura ao ViewHolder correspondente.
     * Configura os listeners de clique e clique longo.
     * @param holder O ViewHolder que será atualizado.
     * @param position A posição do item na lista.
     */
    override fun onBindViewHolder(holder: FaturaViewHolder, position: Int) {
        val fatura = faturas[position]
        holder.numeroFaturaTextView.text = fatura.numeroFatura
        holder.clienteTextView.text = fatura.cliente
        holder.dataTextView.text = fatura.data
        holder.restoreButton.setOnClickListener {
            onRestoreClick(fatura.id)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(fatura.id)
            true
        }
    }

    /**
     * [8] Retorna o número total de faturas na lista.
     * @return O tamanho da lista de faturas.
     */
    override fun getItemCount(): Int = faturas.size

    /**
     * [9] Atualiza a lista de faturas no adaptador e notifica a UI sobre mudanças.
     * @param newFaturas A nova lista de faturas a ser exibida.
     */
    fun updateFaturas(newFaturas: List<FaturaLixeiraItem>) {
        faturas = newFaturas
        notifyDataSetChanged()
    }
}