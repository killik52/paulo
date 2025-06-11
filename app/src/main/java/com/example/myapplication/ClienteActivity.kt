package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ClienteActivity : AppCompatActivity() {

    // Referências para os campos da UI
    private lateinit var editTextNomeDetalhe: EditText
    private lateinit var editTextEmailDetalhe: EditText
    private lateinit var editTextTelefoneDetalhe: EditText
    private lateinit var editTextInformacoesAdicionaisDetalhe: EditText
    private lateinit var editTextCPFDetalhe: EditText
    private lateinit var editTextCNPJDetalhe: EditText
    private lateinit var editTextNumeroSerialDetalhe: EditText
    private lateinit var buttonExcluirCliente: Button
    private lateinit var buttonBloquearCliente: Button
    private lateinit var editTextLogradouroDetalhe: EditText
    private lateinit var editTextBairroDetalhe: EditText

    private lateinit var dbHelper: ClienteDbHelper
    private var clienteId: Long = -1

    // Variáveis para armazenar os dados completos do endereço
    private var logradouroCliente: String? = null
    private var numeroCliente: String? = null
    private var complementoCliente: String? = null
    private var bairroCliente: String? = null
    private var municipioCliente: String? = null
    private var ufCliente: String? = null
    private var cepCliente: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente)

        dbHelper = ClienteDbHelper(this)

        // Referencia todos os elementos da interface
        editTextNomeDetalhe = findViewById(R.id.editTextNomeDetalhe)
        editTextEmailDetalhe = findViewById(R.id.editTextEmailDetalhe)
        editTextTelefoneDetalhe = findViewById(R.id.editTextTelefoneDetalhe)
        editTextInformacoesAdicionaisDetalhe = findViewById(R.id.editTextInformacoesAdicionaisDetalhe)
        editTextCPFDetalhe = findViewById(R.id.editTextCPFDetalhe)
        editTextCNPJDetalhe = findViewById(R.id.editTextCNPJDetalhe)
        editTextNumeroSerialDetalhe = findViewById(R.id.editTextNumeroSerialDetalhe)
        buttonExcluirCliente = findViewById(R.id.textViewExcluirArtigo)
        buttonBloquearCliente = findViewById(R.id.buttonBloquearCliente)
        editTextLogradouroDetalhe = findViewById(R.id.editTextLogradouroDetalhe)
        editTextBairroDetalhe = findViewById(R.id.editTextBairroDetalhe)

        clienteId = intent.getLongExtra("id", -1)

        if (clienteId != -1L) {
            loadAndDisplayClientData(clienteId)
        }

        buttonExcluirCliente.setOnClickListener {
            confirmarExclusao()
        }

        buttonBloquearCliente.setOnClickListener {
            confirmarBloqueio()
        }
    }

    private fun loadAndDisplayClientData(id: Long) {
        val db = dbHelper.readableDatabase
        db.query(
            ClienteContract.ClienteEntry.TABLE_NAME, null, "${BaseColumns._ID} = ?", arrayOf(id.toString()),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                // Carrega dados básicos
                editTextNomeDetalhe.setText(cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME)))
                editTextEmailDetalhe.setText(cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL)))
                editTextTelefoneDetalhe.setText(cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE)))
                editTextCPFDetalhe.setText(cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CPF)))
                editTextCNPJDetalhe.setText(cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ)))
                editTextInformacoesAdicionaisDetalhe.setText(cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS)))

                // Carrega dados de endereço para as variáveis locais e exibe nos campos
                logradouroCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_LOGRADOURO))
                numeroCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO))
                complementoCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_COMPLEMENTO))
                bairroCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_BAIRRO))
                municipioCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_MUNICIPIO))
                ufCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_UF))
                cepCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CEP))

                val enderecoCompleto = listOfNotNull(logradouroCliente, numeroCliente, complementoCliente).joinToString(", ")
                editTextLogradouroDetalhe.setText(enderecoCompleto)
                editTextBairroDetalhe.setText(listOfNotNull(bairroCliente, municipioCliente, ufCliente, cepCliente).joinToString(" - "))

                // **LÓGICA CORRIGIDA**: Carrega todos os seriais, tanto do cadastro quanto das faturas
                loadAllAssociatedSerials(id)
            }
        }
    }

    /**
     * LÓGICA CORRIGIDA: Busca seriais tanto do cadastro do cliente (importado via CSV)
     * quanto do histórico de faturas, combinando tudo em uma única lista.
     */
    private fun loadAllAssociatedSerials(clientId: Long) {
        val db = dbHelper.readableDatabase
        val allSerials = mutableSetOf<String>() // Usar um Set evita duplicatas
        var clientName: String? = null

        // Passo 1: Obter o serial do cadastro do cliente (importado do CSV) e o nome.
        db.query(
            ClienteContract.ClienteEntry.TABLE_NAME,
            arrayOf(ClienteContract.ClienteEntry.COLUMN_NAME_NOME, ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO_SERIAL),
            "${BaseColumns._ID} = ?",
            arrayOf(clientId.toString()),
            null, null, null
        )?.use {
            if (it.moveToFirst()) {
                clientName = it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME))
                val importedSerial = it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO_SERIAL))
                if (!importedSerial.isNullOrBlank()) {
                    // Adiciona seriais do CSV, separando por vírgula se houver mais de um
                    importedSerial.split(',').forEach { serial ->
                        if (serial.trim().isNotEmpty()) allSerials.add(serial.trim())
                    }
                }
            }
        }

        if (clientName == null) {
            Log.w("ClienteActivity", "Não foi possível encontrar o nome do cliente para o ID: $clientId")
            return
        }

        // Passo 2: Buscar seriais de todas as faturas do cliente.
        db.query(
            FaturaContract.FaturaEntry.TABLE_NAME,
            arrayOf(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS),
            "${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE} = ?",
            arrayOf(clientName),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val artigosString = cursor.getString(cursor.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS))
                if (!artigosString.isNullOrEmpty()) {
                    artigosString.split("|").forEach { artigoData ->
                        val parts = artigoData.split(",")
                        if (parts.size >= 5) {
                            val serial = parts[4].takeIf { it.isNotEmpty() && it.lowercase() != "null" }
                            if (serial != null) {
                                // Adiciona seriais da fatura, separando por vírgula se houver
                                serial.split(',').forEach { s ->
                                    if (s.trim().isNotEmpty()) allSerials.add(s.trim())
                                }
                            }
                        }
                    }
                }
            }
        }

        // Passo 3: Exibir a lista combinada e sem duplicatas.
        if (allSerials.isNotEmpty()) {
            editTextNumeroSerialDetalhe.setText(allSerials.joinToString(", "))
            Log.d("ClienteActivity", "Seriais combinados para '$clientName': ${allSerials.joinToString(", ")}")
        } else {
            editTextNumeroSerialDetalhe.setText("")
            Log.d("ClienteActivity", "Nenhum serial encontrado para '$clientName'")
        }
    }


    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir este cliente? Esta ação não poderá ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                excluirCliente()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirCliente() {
        if (clienteId != -1L) {
            val db = dbHelper.writableDatabase
            val rowsDeleted = db.delete(
                ClienteContract.ClienteEntry.TABLE_NAME,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(clienteId.toString())
            )
            if (rowsDeleted > 0) {
                showToast("Cliente excluído com sucesso!")
                val resultIntent = Intent()
                resultIntent.putExtra("cliente_excluido", true)
                resultIntent.putExtra("cliente_id_excluido", clienteId)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                showToast("Erro ao excluir cliente.")
            }
        } else {
            showToast("Não é possível excluir. Cliente não salvo.")
        }
    }

    private fun confirmarBloqueio() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Bloqueio")
            .setMessage("Tem certeza que deseja bloquear este cliente? Ele será movido para a lista de bloqueados e removido da lista de clientes ativos.")
            .setPositiveButton("Bloquear") { _, _ ->
                bloquearCliente()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun bloquearCliente() {
        if (clienteId == -1L) {
            showToast("Salve o cliente antes de bloquear.")
            return
        }

        val nome = editTextNomeDetalhe.text.toString().trim()
        val email = editTextEmailDetalhe.text.toString().trim()
        val telefone = editTextTelefoneDetalhe.text.toString().trim()
        val informacoesAdicionais = editTextInformacoesAdicionaisDetalhe.text.toString().trim()
        val cpf = editTextCPFDetalhe.text.toString().trim()
        val cnpj = editTextCNPJDetalhe.text.toString().trim()
        val numeroSerial = editTextNumeroSerialDetalhe.text.toString().trim()


        if (nome.isEmpty()) {
            showToast("O nome do cliente é obrigatório para bloquear.")
            return
        }

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME, nome)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_EMAIL, email)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_TELEFONE, telefone)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, informacoesAdicionais)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF, cpf)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ, cnpj)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_LOGRADOURO, logradouroCliente ?: "")
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO, numeroCliente ?: "")
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_COMPLEMENTO, complementoCliente ?: "")
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_BAIRRO, bairroCliente ?: "")
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_MUNICIPIO, municipioCliente ?: "")
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_UF, ufCliente ?: "")
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CEP, cepCliente?.replace("[^0-9]".toRegex(), "") ?: "")
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL, numeroSerial)
        }

        val newBlockedRowId = db.insert(ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME, null, values)

        if (newBlockedRowId != -1L) {
            val rowsDeleted = db.delete(
                ClienteContract.ClienteEntry.TABLE_NAME,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(clienteId.toString())
            )
            if (rowsDeleted > 0) {
                showToast("Cliente '$nome' bloqueado e adicionado à lista negra.")
                Log.d("ClienteActivity", "Cliente '$nome' bloqueado com sucesso. ID na lista de bloqueados: $newBlockedRowId")

                val intentBloqueados = Intent(this, ClientesBloqueadosActivity::class.java).apply {
                    putExtra("cliente_bloqueado_id", newBlockedRowId)
                }
                startActivity(intentBloqueados)

                val resultIntent = Intent()
                resultIntent.putExtra("cliente_bloqueado_id_original", clienteId)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                db.delete(ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME, "${android.provider.BaseColumns._ID} = ?", arrayOf(newBlockedRowId.toString()))
                showToast("Erro ao remover cliente da lista original após bloqueio.")
                Log.e("ClienteActivity", "Falha ao remover cliente da tabela 'clientes' após bloqueio bem-sucedido na lista negra.")
            }
        } else {
            showToast("Erro ao adicionar cliente à lista de bloqueados.")
            Log.e("ClienteActivity", "Falha ao inserir cliente na tabela 'clientes_bloqueados'.")
        }
    }


    override fun onBackPressed() {
        salvarDadosCliente()
        super.onBackPressed()
    }

    private fun salvarDadosCliente() {
        val nome = editTextNomeDetalhe.text.toString().trim()
        val email = editTextEmailDetalhe.text.toString().trim()
        val telefone = editTextTelefoneDetalhe.text.toString().trim()
        val informacoesAdicionais = editTextInformacoesAdicionaisDetalhe.text.toString().trim()
        val cpf = editTextCPFDetalhe.text.toString().trim()
        val cnpj = editTextCNPJDetalhe.text.toString().trim()
        // ATENÇÃO: A lógica para salvar o endereço editado precisaria ser mais complexa,
        // pois os campos foram combinados para exibição.
        // Por simplicidade, esta versão não salvará alterações feitas nos campos de endereço.
        // O número serial também não será salvo por aqui, pois ele é um compilado do histórico.

        if (nome.isNotEmpty() && clienteId != -1L) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(ClienteContract.ClienteEntry.COLUMN_NAME_NOME, nome)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL, email)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE, telefone)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, informacoesAdicionais)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_CPF, cpf)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ, cnpj)
            }

            val updatedRows = db.update(
                ClienteContract.ClienteEntry.TABLE_NAME,
                values,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(clienteId.toString())
            )

            if (updatedRows > 0) {
                val resultIntent = Intent()
                resultIntent.putExtra("cliente_atualizado", true)
                setResult(RESULT_OK, resultIntent)
                Log.d("ClienteActivity", "Cliente $nome atualizado com sucesso ao sair/voltar!")
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}