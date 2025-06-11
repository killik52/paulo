package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityCriarNovoClienteBinding
import com.example.myapplication.model.CnpjData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CriarNovoClienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarNovoClienteBinding
    private var dbHelper: ClienteDbHelper? = null
    private var clienteId: Long = -1

    private lateinit var cnpjTextWatcher: TextWatcher
    private lateinit var cpfTextWatcher: TextWatcher
    private lateinit var cepTextWatcher: TextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCriarNovoClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("CriarNovoCliente", "onCreate chamado com ViewBinding")

        try {
            dbHelper = ClienteDbHelper(this)
        } catch (e: Exception) {
            Log.e("CriarNovoCliente", "Erro ao inicializar banco: ${e.message}")
            showToast("Erro ao inicializar o banco de dados.")
            finish()
            return
        }

        clienteId = intent.getLongExtra("cliente_id", -1)
        if (clienteId != -1L) {
            val nome = intent.getStringExtra("nome_cliente")
            binding.editTextNomeCliente.setText(nome)
            binding.editTextEmailCliente.setText(intent.getStringExtra("email"))
            binding.editTextTelefoneCliente.setText(intent.getStringExtra("telefone"))
            binding.editTextInformacoesAdicionais.setText(intent.getStringExtra("informacoes_adicionais"))
            binding.editTextCNPJCliente.setText(intent.getStringExtra("cnpj"))
            binding.editTextCPFCliente.setText(intent.getStringExtra("cpf"))
            binding.editTextLogradouro.setText(intent.getStringExtra("logradouro"))
            binding.editTextNumero.setText(intent.getStringExtra("numero"))
            binding.editTextComplemento.setText(intent.getStringExtra("complemento"))
            binding.editTextBairro.setText(intent.getStringExtra("bairro"))
            binding.editTextMunicipio.setText(intent.getStringExtra("municipio"))
            binding.editTextUF.setText(intent.getStringExtra("uf"))
            binding.editTextCEP.setText(intent.getStringExtra("cep"))
            Log.d("CriarNovoCliente", "Carregando cliente para edição: ID=$clienteId, Nome=$nome")
        }

        setupInputMasks()

        binding.textViewGuardarCliente.setOnClickListener {
            val nome = binding.editTextNomeCliente.text.toString().trim()
            val cpf = binding.editTextCPFCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()
            val cnpj = binding.editTextCNPJCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()

            if (nome.isEmpty()) {
                showToast("Por favor, insira o nome do cliente.")
                Log.w("CriarNovoCliente", "Nome do cliente está vazio")
                return@setOnClickListener
            }

            verificarClienteBloqueado(nome, cpf, cnpj) { bloqueado, clienteBloqueado ->
                if (bloqueado && clienteBloqueado != null) {
                    mostrarDialogoClienteBloqueado(clienteBloqueado)
                } else {
                    salvarCliente()
                }
            }
        }

        binding.buttonBloquearCliente.setOnClickListener {
            confirmarBloqueioCliente()
        }
    }

    private fun setupInputMasks() {
        cnpjTextWatcher = object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true
                val unmasked = s.toString().replace("[^0-9]".toRegex(), "")
                val masked = StringBuilder()
                val mask = "##.###.###/####-##"

                var digitIndex = 0
                for (m in mask) {
                    if (digitIndex >= unmasked.length) break
                    if (m == '#') {
                        masked.append(unmasked[digitIndex])
                        digitIndex++
                    } else {
                        masked.append(m)
                    }
                }

                val newText = masked.toString()
                binding.editTextCNPJCliente.setText(newText)

                // Ajusta a posição do cursor com base no número de dígitos digitados
                val digitsEntered = unmasked.length
                val cursorPosition = when {
                    digitsEntered == 0 -> 0
                    digitsEntered <= 2 -> digitsEntered
                    digitsEntered <= 5 -> digitsEntered + 1 // Após o ponto (ex.: 12.3)
                    digitsEntered <= 8 -> digitsEntered + 2 // Após o segundo ponto (ex.: 12.345.6)
                    digitsEntered <= 12 -> digitsEntered + 3 // Após a barra (ex.: 12.345.678/9)
                    else -> digitsEntered + 4 // Após o hífen (ex.: 12.345.678/9012-3)
                }.coerceAtMost(newText.length)
                binding.editTextCNPJCliente.setSelection(cursorPosition)

                isUpdating = false

                if (unmasked.length == 14) {
                    buscarDadosCnpj(unmasked)
                }
            }
        }
        binding.editTextCNPJCliente.addTextChangedListener(cnpjTextWatcher)

        cpfTextWatcher = object : TextWatcher {
            private var isUpdating = false
            private var old = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString().replace("[^0-9]".toRegex(), "")
                if (isUpdating || str == old) {
                    return
                }
                isUpdating = true
                val mascara = "###.###.###-##"
                var i = 0
                val novaString = StringBuilder()
                for (m in mascara.toCharArray()) {
                    if (m != '#' && str.length > old.length) {
                        novaString.append(m)
                        continue
                    }
                    try {
                        novaString.append(str[i])
                    } catch (e: Exception) {
                        break
                    }
                    i++
                }
                val currentSelection = binding.editTextCPFCliente.selectionStart
                binding.editTextCPFCliente.setText(novaString.toString())
                try {
                    binding.editTextCPFCliente.setSelection(if (currentSelection > novaString.length) novaString.length else currentSelection)
                } catch (e: Exception) {
                    binding.editTextCPFCliente.setSelection(novaString.length)
                }
                old = str
                isUpdating = false
            }
        }
        binding.editTextCPFCliente.addTextChangedListener(cpfTextWatcher)

        cepTextWatcher = object : TextWatcher {
            private var isUpdating = false
            private var old = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString().replace("[^0-9]".toRegex(), "")
                if (isUpdating || str == old) {
                    return
                }
                isUpdating = true
                val mascara = "#####-###"
                var i = 0
                val novaString = StringBuilder()
                for (m in mascara.toCharArray()) {
                    if (m != '#' && str.length > old.length) {
                        novaString.append(m)
                        continue
                    }
                    try {
                        novaString.append(str[i])
                    } catch (e: Exception) {
                        break
                    }
                    i++
                }
                val currentSelection = binding.editTextCEP.selectionStart
                binding.editTextCEP.setText(novaString.toString())
                try {
                    binding.editTextCEP.setSelection(if (currentSelection > novaString.length) novaString.length else currentSelection)
                } catch (e: Exception) {
                    binding.editTextCEP.setSelection(novaString.length)
                }
                old = str
                isUpdating = false
            }
        }
        binding.editTextCEP.addTextChangedListener(cepTextWatcher)
    }

    private fun confirmarBloqueioCliente() {
        val nome = binding.editTextNomeCliente.text.toString().trim()
        if (nome.isEmpty()) {
            showToast("Por favor, insira o nome do cliente para bloquear.")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar Bloqueio")
            .setMessage("Tem certeza que deseja bloquear o cliente '$nome'? Ele será movido para a lista de bloqueados e, se existir na lista de clientes ativos, será removido de lá.")
            .setPositiveButton("Bloquear") { _, _ ->
                bloquearClienteEFinalizar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun bloquearClienteEFinalizar() {
        val nome = binding.editTextNomeCliente.text.toString().trim()
        if (nome.isEmpty()) {
            showToast("O nome do cliente é obrigatório para bloquear.")
            return
        }

        val email = binding.editTextEmailCliente.text.toString().trim()
        val telefone = binding.editTextTelefoneCliente.text.toString().trim()
        val informacoesAdicionais = binding.editTextInformacoesAdicionais.text.toString().trim()
        val cpf = binding.editTextCPFCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()
        val cnpj = binding.editTextCNPJCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()
        val logradouro = binding.editTextLogradouro.text.toString().trim()
        val numero = binding.editTextNumero.text.toString().trim()
        val complemento = binding.editTextComplemento.text.toString().trim()
        val bairro = binding.editTextBairro.text.toString().trim()
        val municipio = binding.editTextMunicipio.text.toString().trim()
        val uf = binding.editTextUF.text.toString().trim()
        val cep = binding.editTextCEP.text.toString().replace("[^0-9]".toRegex(), "").trim()

        val db = dbHelper?.writableDatabase
        if (db == null) {
            showToast("Erro ao acessar o banco de dados.")
            return
        }

        var numeroSerialParaBloqueio: String? = null
        val nomeClienteParaBuscarFaturas = nome

        if (nomeClienteParaBuscarFaturas.isNotEmpty()) {
            val dbReadable = dbHelper?.readableDatabase
            if (dbReadable != null) {
                Log.d("CriarNovoCliente", "Buscando faturas para o cliente: $nomeClienteParaBuscarFaturas para encontrar serial.")
                val faturaCursor: Cursor? = dbReadable.query(
                    FaturaContract.FaturaEntry.TABLE_NAME,
                    arrayOf(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS),
                    "${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE} = ?",
                    arrayOf(nomeClienteParaBuscarFaturas),
                    null, null,
                    "${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} DESC"
                )

                faturaCursor?.use { fc ->
                    while (fc.moveToNext()) {
                        if (numeroSerialParaBloqueio != null) break
                        val artigosString = fc.getString(fc.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS))
                        if (!artigosString.isNullOrEmpty()) {
                            val artigosArray = artigosString.split("|")
                            for (artigoData in artigosArray) {
                                val parts = artigoData.split(",")
                                if (parts.size >= 5) {
                                    val serial = parts[4].takeIf { it.isNotEmpty() && it.lowercase() != "null" }
                                    if (serial != null) {
                                        numeroSerialParaBloqueio = serial
                                        Log.d("CriarNovoCliente", "Número serial encontrado para cliente '$nomeClienteParaBuscarFaturas': $serial")
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                faturaCursor?.close()
                if (numeroSerialParaBloqueio == null) {
                    Log.d("CriarNovoCliente", "Nenhum número serial encontrado nas faturas do cliente '$nomeClienteParaBuscarFaturas'.")
                }
            }
        }

        val values = ContentValues().apply {
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME, nome)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_EMAIL, email)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_TELEFONE, telefone)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, informacoesAdicionais)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF, cpf)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ, cnpj)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_LOGRADOURO, logradouro)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO, numero)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_COMPLEMENTO, complemento)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_BAIRRO, bairro)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_MUNICIPIO, municipio)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_UF, uf)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CEP, cep)
            put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL, numeroSerialParaBloqueio ?: "")
        }

        val newBlockedRowId = db.insert(ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME, null, values)

        if (newBlockedRowId != -1L) {
            var originalClienteRemovido = false
            if (clienteId != -1L) {
                val rowsDeleted = db.delete(
                    ClienteContract.ClienteEntry.TABLE_NAME,
                    "${android.provider.BaseColumns._ID} = ?",
                    arrayOf(clienteId.toString())
                )
                if (rowsDeleted > 0) {
                    originalClienteRemovido = true
                    Log.d("CriarNovoCliente", "Cliente original ID $clienteId removido da tabela clientes.")
                }
            }
            showToast("Cliente '$nome' adicionado à lista de bloqueados.")
            Log.d("CriarNovoCliente", "Cliente '$nome' bloqueado. ID na lista de bloqueados: $newBlockedRowId. Serial associado: ${numeroSerialParaBloqueio ?: "Nenhum"}")

            val resultIntent = Intent().apply {
                putExtra("cliente_bloqueado_com_sucesso", true)
                putExtra("nome_cliente_bloqueado", nome)
                putExtra("cliente_bloqueado_id", newBlockedRowId)
                putExtra("email_cliente_bloqueado", email)
                putExtra("telefone_cliente_bloqueado", telefone)
                putExtra("info_adicionais_cliente_bloqueado", informacoesAdicionais)
                putExtra("cpf_cliente_bloqueado", cpf)
                putExtra("cnpj_cliente_bloqueado", cnpj)
                putExtra("logradouro_cliente_bloqueado", logradouro)
                putExtra("numero_cliente_bloqueado", numero)
                putExtra("complemento_cliente_bloqueado", complemento)
                putExtra("bairro_cliente_bloqueado", bairro)
                putExtra("municipio_cliente_bloqueado", municipio)
                putExtra("uf_cliente_bloqueado", uf)
                putExtra("cep_cliente_bloqueado", cep)
                putExtra("serial_cliente_bloqueado", numeroSerialParaBloqueio ?: "")
                if (originalClienteRemovido) {
                    putExtra("cliente_original_id_removido", clienteId)
                }
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            showToast("Erro ao adicionar cliente à lista de bloqueados.")
            Log.e("CriarNovoCliente", "Falha ao inserir cliente na tabela 'clientes_bloqueados'.")
        }
    }

    private fun verificarClienteBloqueado(nome: String, cpf: String, cnpj: String, callback: (Boolean, ClienteBloqueado?) -> Unit) {
        val db = dbHelper?.readableDatabase ?: run {
            callback(false, null)
            return
        }

        var selection = ""
        val selectionArgs = mutableListOf<String>()

        if (nome.isNotEmpty()) {
            selection += "${ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME} = ?"
            selectionArgs.add(nome)
        }
        if (cpf.isNotEmpty()) {
            if (selection.isNotEmpty()) selection += " OR "
            selection += "${ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF} = ?"
            selectionArgs.add(cpf)
        }
        if (cnpj.isNotEmpty()) {
            if (selection.isNotEmpty()) selection += " OR "
            selection += "${ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ} = ?"
            selectionArgs.add(cnpj)
        }

        if (selection.isEmpty()) {
            callback(false, null)
            return
        }

        val cursor: Cursor? = db.query(
            ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs.toTypedArray(),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                val nomeBloqueado = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME)) ?: ""
                val email = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_EMAIL)) ?: ""
                val telefone = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_TELEFONE)) ?: ""
                val informacoesAdicionais = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS)) ?: ""
                val cpfBloqueado = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF)) ?: ""
                val cnpjBloqueado = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ)) ?: ""
                val logradouro = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_LOGRADOURO)) ?: ""
                val numero = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO)) ?: ""
                val complemento = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_COMPLEMENTO)) ?: ""
                val bairro = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_BAIRRO)) ?: ""
                val municipio = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_MUNICIPIO)) ?: ""
                val uf = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_UF)) ?: ""
                val cep = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CEP)) ?: ""
                val numeroSerial = it.getString(it.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL)) ?: ""

                val cliente = ClienteBloqueado(id, nomeBloqueado, email, telefone, informacoesAdicionais, cpfBloqueado, cnpjBloqueado, logradouro, numero, complemento, bairro, municipio, uf, cep, numeroSerial)
                callback(true, cliente)
                return
            }
        }
        callback(false, null)
    }

    private fun mostrarDialogoClienteBloqueado(cliente: ClienteBloqueado) {
        AlertDialog.Builder(this)
            .setTitle("Cliente Bloqueado")
            .setMessage("O cliente '${cliente.nome}' já está na lista de bloqueados. Deseja ver as informações?")
            .setPositiveButton("Sim") { _, _ ->
                val intent = Intent(this, ClientesBloqueadosActivity::class.java).apply {
                    putExtra("cliente_bloqueado_id", cliente.id)
                    putExtra("nome_cliente_bloqueado", cliente.nome)
                    putExtra("email_cliente_bloqueado", cliente.email)
                    putExtra("telefone_cliente_bloqueado", cliente.telefone)
                    putExtra("cpf_cliente_bloqueado", cliente.cpf)
                    putExtra("cnpj_cliente_bloqueado", cliente.cnpj)
                    putExtra("serial_cliente_bloqueado", cliente.numeroSerial)
                    putExtra("info_adicionais_cliente_bloqueado", cliente.informacoesAdicionais)
                }
                startActivity(intent)
            }
            .setNegativeButton("Não") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun salvarCliente() {
        try {
            val cnpj = binding.editTextCNPJCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()
            val cpf = binding.editTextCPFCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()
            val nome = binding.editTextNomeCliente.text.toString().trim()
            val email = binding.editTextEmailCliente.text.toString().trim()
            val telefone = binding.editTextTelefoneCliente.text.toString().trim()
            val informacoesAdicionais = binding.editTextInformacoesAdicionais.text.toString().trim()
            val logradouro = binding.editTextLogradouro.text.toString().trim()
            val numero = binding.editTextNumero.text.toString().trim()
            val complemento = binding.editTextComplemento.text.toString().trim()
            val bairro = binding.editTextBairro.text.toString().trim()
            val municipio = binding.editTextMunicipio.text.toString().trim()
            val uf = binding.editTextUF.text.toString().trim()
            val cep = binding.editTextCEP.text.toString().replace("[^0-9]".toRegex(), "").trim()

            Log.d("CriarNovoCliente", "Tentando salvar cliente: nome='$nome'")

            val db = dbHelper?.writableDatabase
            val values = ContentValues().apply {
                put(ClienteContract.ClienteEntry.COLUMN_NAME_NOME, nome)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL, email)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE, telefone)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, informacoesAdicionais)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_CPF, cpf)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ, cnpj)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_LOGRADOURO, logradouro)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO, numero)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_COMPLEMENTO, complemento)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_BAIRRO, bairro)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_MUNICIPIO, municipio)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_UF, uf)
                put(ClienteContract.ClienteEntry.COLUMN_NAME_CEP, cep)
            }

            if (clienteId != -1L) {
                val rowsUpdated = db?.update(
                    ClienteContract.ClienteEntry.TABLE_NAME,
                    values,
                    "${android.provider.BaseColumns._ID} = ?",
                    arrayOf(clienteId.toString())
                )
                if (rowsUpdated != null && rowsUpdated > 0) {
                    showToast("Cliente atualizado com sucesso!")
                    Log.d("CriarNovoCliente", "Cliente atualizado: ID=$clienteId, Nome=$nome")
                } else {
                    showToast("Erro ao atualizar o cliente.")
                    Log.e("CriarNovoCliente", "Falha ao atualizar cliente: ID=$clienteId, RowsUpdated=$rowsUpdated")
                    return
                }
            } else {
                val newRowId = db?.insert(ClienteContract.ClienteEntry.TABLE_NAME, null, values)
                if (newRowId != null && newRowId != -1L) {
                    clienteId = newRowId
                    showToast("Cliente salvo com sucesso!")
                    Log.d("CriarNovoCliente", "Novo cliente salvo: ID=$newRowId, Nome=$nome")
                } else {
                    showToast("Erro ao salvar o cliente.")
                    Log.e("CriarNovoCliente", "Falha ao inserir cliente no banco, newRowId=$newRowId")
                    return
                }
            }

            val resultIntent = Intent().apply {
                putExtra("cliente_id", clienteId)
                putExtra("nome_cliente", nome)
                putExtra("email", email)
                putExtra("telefone", telefone)
                putExtra("informacoes_adicionais", informacoesAdicionais)
                putExtra("cpf", cpf)
                putExtra("cnpj", cnpj)
                putExtra("logradouro", logradouro)
                putExtra("numero", numero)
                putExtra("complemento", complemento)
                putExtra("bairro", bairro)
                putExtra("municipio", municipio)
                putExtra("uf", uf)
                putExtra("cep", cep)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
            Log.d("CriarNovoCliente", "Retornando resultado: nome_cliente=$nome, cliente_id=$clienteId")
        } catch (e: Exception) {
            Log.e("CriarNovoCliente", "Erro ao salvar cliente: ${e.message}")
            showToast("Erro ao salvar cliente: ${e.message}")
        }
    }

    private fun buscarDadosCnpj(cnpj: String) {
        RetrofitClient.cnpjApiService.getCnpjData(cnpj).enqueue(object : Callback<CnpjData> {
            override fun onResponse(call: Call<CnpjData>, response: Response<CnpjData>) {
                if (response.isSuccessful) {
                    val cnpjData = response.body()
                    if (cnpjData?.status == "OK") {
                        binding.editTextNomeCliente.setText(cnpjData.nome ?: "")
                        binding.editTextEmailCliente.setText(cnpjData.email ?: "")
                        binding.editTextTelefoneCliente.setText(cnpjData.telefone ?: "")
                        binding.editTextLogradouro.setText(cnpjData.logradouro ?: "")
                        binding.editTextNumero.setText(cnpjData.numero ?: "")
                        binding.editTextComplemento.setText(cnpjData.complemento ?: "")
                        binding.editTextBairro.setText(cnpjData.bairro ?: "")
                        binding.editTextMunicipio.setText(cnpjData.municipio ?: "")
                        binding.editTextUF.setText(cnpjData.uf ?: "")

                        val cepSemFormatacao = cnpjData.cep?.replace("[^0-9]".toRegex(), "") ?: ""
                        binding.editTextCEP.removeTextChangedListener(cepTextWatcher)
                        binding.editTextCEP.setText(cepSemFormatacao)
                        binding.editTextCEP.addTextChangedListener(cepTextWatcher)

                        showToast("Dados do CNPJ carregados com sucesso!")
                        Log.d("CriarNovoCliente", "Dados do CNPJ carregados: Nome=${cnpjData.nome}")
                    } else {
                        showToast("CNPJ inválido ou não encontrado: ${cnpjData?.mensagem}")
                        Log.w("CriarNovoCliente", "CNPJ inválido: ${cnpjData?.mensagem}")
                    }
                } else {
                    showToast("Erro na resposta da API: ${response.message()}")
                    Log.e("CriarNovoCliente", "Erro na resposta da API: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<CnpjData>, t: Throwable) {
                Log.e("CriarNovoCliente", "Erro ao buscar CNPJ: ${t.message}")
                showToast("Erro ao buscar CNPJ: ${t.message}")
            }
        })
    }

    override fun onDestroy() {
        try {
            dbHelper?.close()
        } catch (e: Exception) {
            Log.e("CriarNovoCliente", "Erro ao fechar banco: ${e.message}")
        }
        super.onDestroy()
        Log.d("CriarNovoCliente", "onDestroy chamado")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}