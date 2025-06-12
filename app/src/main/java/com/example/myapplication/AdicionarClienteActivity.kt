package com.example.myapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityClienteBinding

class AdicionarClienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteBinding
    private var dbHelper: ClienteDbHelper? = null
    private var clienteId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        clienteId = intent.getLongExtra("cliente_id", -1)
        Log.d("AdicionarCliente", "Recebido cliente_id: $clienteId")

        if (clienteId != -1L) {
            loadClienteData(clienteId)
        }

        binding.buttonBloquearCliente.setOnClickListener {
            if (clienteId != -1L) {
                showBlockClientDialog()
            } else {
                showToast("Salve o cliente antes de bloquear.")
            }
        }

        binding.textViewExcluirArtigo.setOnClickListener {
            if (clienteId != -1L) {
                showDeleteClientDialog()
            } else {
                showToast("Este cliente ainda não foi salvo.")
            }
        }

        binding.buttonBloquearCliente.setOnClickListener {
            saveCliente()
        }
    }

    private fun loadClienteData(id: Long) {
        val db = dbHelper?.readableDatabase ?: return
        val cursor = db.query(
            ClienteContract.ClienteEntry.TABLE_NAME,
            null,
            "${BaseColumns._ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                binding.editTextNomeDetalhe.setText(it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME)))
                binding.editTextEmailDetalhe.setText(it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL)))
                binding.editTextTelefoneDetalhe.setText(it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE)))
                binding.editTextCPFDetalhe.setText(it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CPF)))
                binding.editTextCNPJDetalhe.setText(it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ)))
                binding.editTextInformacoesAdicionaisDetalhe.setText(it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS)))
                val serial = it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO_SERIAL)) ?: ""
                binding.editTextNumeroSerialDetalhe.setText(serial)
                Log.d("AdicionarCliente", "Dados do cliente ID $id carregados.")
            }
        }
    }

    private fun getClientSerialNumbers(clienteId: Long): List<String> {
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val serials = mutableSetOf<String>()
        val cursor = db.rawQuery(
            """
            SELECT a.${ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL}
            FROM ${ArtigoContract.ArtigoEntry.TABLE_NAME} a
            INNER JOIN ${FaturaContract.FaturaItemEntry.TABLE_NAME} fi
                ON fi.${FaturaContract.FaturaItemEntry.COLUMN_NAME_ARTIGO_ID} = a.${BaseColumns._ID}
            WHERE fi.${FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID} = ?
            """,
            arrayOf(clienteId.toString())
        )
        cursor?.use {
            while (it.moveToNext()) {
                val serial = it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL))
                if (!serial.isNullOrBlank()) {
                    serials.add(serial)
                }
            }
        }
        cursor.close()
        return serials.toList()
    }

    private fun saveCliente() {
        val nome = binding.editTextNomeDetalhe.text.toString().trim()
        val email = binding.editTextEmailDetalhe.text.toString().trim()
        val telefone = binding.editTextTelefoneDetalhe.text.toString().trim()
        val cpf = binding.editTextCPFDetalhe.text.toString().trim()
        val cnpj = binding.editTextCNPJDetalhe.text.toString().trim()
        val informacoesAdicionais = binding.editTextInformacoesAdicionaisDetalhe.text.toString().trim()
        val numeroSerial = binding.editTextNumeroSerialDetalhe.text.toString().trim()

        if (nome.isEmpty()) {
            showToast("O nome do cliente é obrigatório.")
            return
        }

        val db = dbHelper?.writableDatabase ?: run {
            showToast("Erro ao acessar o banco de dados.")
            return
        }

        try {
            db.beginTransaction()
            val values = ContentValues().apply {
                put(ClienteContract.ClienteEntry.COLUMN_NAME_NOME, nome)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL, email)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE, telefone)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_CPF, cpf)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ, cnpj)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, informacoesAdicionais)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO_SERIAL, numeroSerial)
            }

            if (clienteId != -1L) {
                val rowsUpdated = db.update(
                    ClienteContract.ClienteEntry.TABLE_NAME,
                    values,
                    "${BaseColumns._ID} = ?",
                    arrayOf(clienteId.toString())
                )
                if (rowsUpdated > 0) {
                    showToast("Cliente atualizado com sucesso!")
                    db.setTransactionSuccessful()
                } else {
                    showToast("Erro ao atualizar o cliente.")
                }
            } else {
                val newRowId = db.insert(ClienteContract.ClienteEntry.TABLE_NAME, null, values)
                if (newRowId != -1L) {
                    clienteId = newRowId
                    showToast("Cliente salvo com sucesso!")
                    db.setTransactionSuccessful()
                } else {
                    showToast("Erro ao salvar o cliente.")
                    return
                }
            }

            if (clienteId != -1L && numeroSerial.isNotEmpty()) {
                updateFaturaItensSerials(clienteId, numeroSerial)
            }

        } catch (e: Exception) {
            Log.e("AdicionarCliente", "Erro ao salvar cliente: ${e.message}", e)
            showToast("Erro ao salvar cliente: ${e.message}")
        } finally {
            db.endTransaction()
        }

        val resultIntent = Intent().apply {
            putExtra("cliente_id", clienteId)
            putExtra("nome_cliente", nome)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun updateFaturaItensSerials(clienteId: Long, numeroSerial: String) {
        val db = dbHelper?.writableDatabase ?: return
        val serials = numeroSerial.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        serials.forEach { serial ->
            var cursor: Cursor? = null
            try {
                cursor = db.query(
                    ArtigoContract.ArtigoEntry.TABLE_NAME,
                    arrayOf(BaseColumns._ID),
                    "${ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL} = ?",
                    arrayOf(serial),
                    null, null, null
                )
                var artigoId: Long = -1
                if (cursor.moveToFirst()) {
                    artigoId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
                }

                if (artigoId == -1L) {
                    val artigoValues = ContentValues().apply {
                        put(ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME, "Artigo com Serial $serial")
                        put(ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL, serial)
                        put(ArtigoContract.ArtigoEntry.COLUMN_NAME_QUANTIDADE, 1)
                        put(ArtigoContract.ArtigoEntry.COLUMN_NAME_PRECO, 0.0)
                        put(ArtigoContract.ArtigoEntry.COLUMN_NAME_GUARDAR_FATURA, 0)
                    }
                    artigoId = db.insert(ArtigoContract.ArtigoEntry.TABLE_NAME, null, artigoValues)
                }

                if (artigoId != -1L) {
                    val existingCursor = db.query(
                        FaturaContract.FaturaItemEntry.TABLE_NAME,
                        arrayOf(BaseColumns._ID),
                        "${FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID} = ? AND ${FaturaContract.FaturaItemEntry.COLUMN_NAME_ARTIGO_ID} = ?",
                        arrayOf(clienteId.toString(), artigoId.toString()),
                        null, null, null
                    )
                    if (!existingCursor.moveToFirst()) {
                        val faturaValues = ContentValues().apply {
                            put(FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID, clienteId)
                            put(FaturaContract.FaturaItemEntry.COLUMN_NAME_ARTIGO_ID, artigoId)
                            put(FaturaContract.FaturaItemEntry.COLUMN_NAME_QUANTIDADE, 1)
                            put(FaturaContract.FaturaItemEntry.COLUMN_NAME_PRECO, 0.0)
                        }
                        db.insert(FaturaContract.FaturaItemEntry.TABLE_NAME, null, faturaValues)
                    }
                    existingCursor.close()
                }
            } catch (e: Exception) {
                Log.e("AdicionarCliente", "Erro ao atualizar fatura_itens para serial $serial: ${e.message}", e)
            } finally {
                cursor?.close()
            }
        }
    }

    private fun showBlockClientDialog() {
        AlertDialog.Builder(this)
            .setTitle("Bloquear Cliente")
            .setMessage("Tem certeza que deseja bloquear este cliente? Ele será adicionado à lista de clientes bloqueados.")
            .setPositiveButton("Bloquear") { _, _ ->
                blockClient()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun blockClient() {
        val nome = binding.editTextNomeDetalhe.text.toString().trim()
        val email = binding.editTextEmailDetalhe.text.toString().trim()
        val telefone = binding.editTextTelefoneDetalhe.text.toString().trim()
        val cpf = binding.editTextCPFDetalhe.text.toString().trim()
        val cnpj = binding.editTextCNPJDetalhe.text.toString().trim()
        val informacoesAdicionais = binding.editTextInformacoesAdicionaisDetalhe.text.toString().trim()
        val numeroSerial = binding.editTextNumeroSerialDetalhe.text.toString().trim()

        val db = dbHelper?.writableDatabase ?: run {
            showToast("Erro ao acessar o banco de dados.")
            return
        }

        try {
            val values = ContentValues().apply {
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME, nome)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_EMAIL, email)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_TELEFONE, telefone)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF, cpf)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ, cnpj)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, informacoesAdicionais)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL, numeroSerial)
            }
            val newRowId = db.insert(ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME, null, values)
            if (newRowId != -1L) {
                showToast("Cliente bloqueado com sucesso!")
                db.delete(ClienteContract.ClienteEntry.TABLE_NAME, "${BaseColumns._ID} = ?", arrayOf(clienteId.toString()))
                finish()
            } else {
                showToast("Erro ao bloquear o cliente.")
            }
        } catch (e: Exception) {
            Log.e("AdicionarCliente", "Erro ao bloquear cliente: ${e.message}", e)
            showToast("Erro ao bloquear cliente: ${e.message}")
        }
    }

    private fun showDeleteClientDialog() {
        AlertDialog.Builder(this)
            .setTitle("Excluir Cliente")
            .setMessage("Tem certeza que deseja excluir este cliente? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                deleteClient()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteClient() {
        val db = dbHelper?.writableDatabase ?: run {
            showToast("Erro ao acessar o banco de dados.")
            return
        }

        try {
            val rowsDeleted = db.delete(
                ClienteContract.ClienteEntry.TABLE_NAME,
                "${BaseColumns._ID} = ?",
                arrayOf(clienteId.toString())
            )
            if (rowsDeleted > 0) {
                showToast("Cliente excluído com sucesso!")
                finish()
            } else {
                showToast("Erro ao excluir o cliente.")
            }
        } catch (e: Exception) {
            Log.e("AdicionarCliente", "Erro ao excluir cliente: ${e.message}", e)
            showToast("Erro ao excluir cliente: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}