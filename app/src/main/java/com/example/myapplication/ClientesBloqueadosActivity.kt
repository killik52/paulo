package com.example.myapplication

import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ClientesBloqueadosActivity : AppCompatActivity() {

    private var dbHelper: ClienteDbHelper? = null
    private var clienteBloqueado: ClienteBloqueado? = null

    private lateinit var editTextNome: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextTelefone: EditText
    private lateinit var editTextCPF: EditText
    private lateinit var editTextCNPJ: EditText
    private lateinit var editTextSerial: EditText
    private lateinit var editTextInformacoesAdicionais: EditText
    private lateinit var buttonExcluir: Button
    private lateinit var backButton: ImageView

    private lateinit var listViewClientesBloqueados: ListView
    private lateinit var adapterListaBloqueados: ArrayAdapter<String>
    private val listaNomesClientesBloqueados = mutableListOf<String>()
    private val listaObjetosClientesBloqueados = mutableListOf<ClienteBloqueado>()

    private lateinit var telefoneTextWatcher: TextWatcher
    private lateinit var cpfTextWatcher: TextWatcher
    private lateinit var cnpjTextWatcher: TextWatcher


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_clientes_bloqueados)
            Log.d("ClientesBloqueados", "Layout activity_clientes_bloqueados carregado")
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao carregar layout: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar tela.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            dbHelper = ClienteDbHelper(this)
            Log.d("ClientesBloqueados", "ClienteDbHelper inicializado")
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao inicializar banco: ${e.message}", e)
            Toast.makeText(this, "Erro no banco.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            editTextNome = findViewById(R.id.editTextNomeDetalhe)
            editTextEmail = findViewById(R.id.editTextEmailDetalhe)
            editTextTelefone = findViewById(R.id.editTextTelefoneDetalhe)
            editTextCPF = findViewById(R.id.editTextCPFDetalhe)
            editTextCNPJ = findViewById(R.id.editTextCNPJDetalhe)
            editTextSerial = findViewById(R.id.editTextSerialDetalhe)
            editTextInformacoesAdicionais = findViewById(R.id.editTextInformacoesAdicionaisDetalhe)
            buttonExcluir = findViewById(R.id.buttonExcluir)
            backButton = findViewById(R.id.backButton)
            listViewClientesBloqueados = findViewById(R.id.listViewClientesBloqueados)

            adapterListaBloqueados = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaNomesClientesBloqueados)
            listViewClientesBloqueados.adapter = adapterListaBloqueados

            aplicarMascaras() // Chama para inicializar e adicionar os watchers

            val clienteBloqueadoIdRecebido = intent.getLongExtra("cliente_bloqueado_id", -1L)
            val serialRecebidoDoIntent = intent.getStringExtra("serial_cliente_bloqueado")


            if (clienteBloqueadoIdRecebido != -1L) {
                loadClienteBloqueadoPorId(clienteBloqueadoIdRecebido)
                if (!serialRecebidoDoIntent.isNullOrEmpty() &&
                    (clienteBloqueado?.numeroSerial.isNullOrEmpty() || clienteBloqueado?.numeroSerial != serialRecebidoDoIntent)
                ) {
                    editTextSerial.setText(serialRecebidoDoIntent)
                    clienteBloqueado = clienteBloqueado?.copy(numeroSerial = serialRecebidoDoIntent)
                }
            } else {
                val nomeClienteIntent = intent.getStringExtra("nome_cliente_bloqueado")
                if (nomeClienteIntent != null) {
                    preencherCamposComDadosDoIntent()
                }
            }


            listViewClientesBloqueados.setOnItemClickListener { _, _, position, _ ->
                if (position < listaObjetosClientesBloqueados.size) {
                    val clienteSelecionado = listaObjetosClientesBloqueados[position]
                    saveClienteSeModificado()
                    loadClienteBloqueadoPorId(clienteSelecionado.id)
                }
            }

            backButton.setOnClickListener {
                Log.d("ClientesBloqueados", "Botão de voltar clicado")
                saveClienteSeModificado()
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }

            buttonExcluir.setOnClickListener {
                confirmarExclusao()
            }
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao inicializar componentes: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar componentes.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun preencherCamposComDadosDoIntent() {
        editTextNome.setText(intent.getStringExtra("nome_cliente_bloqueado") ?: "")
        editTextEmail.setText(intent.getStringExtra("email_cliente_bloqueado") ?: "")

        val telefoneIntent = intent.getStringExtra("telefone_cliente_bloqueado") ?: ""
        editTextTelefone.removeTextChangedListener(telefoneTextWatcher)
        editTextTelefone.setText(telefoneIntent.replace("[^0-9]".toRegex(), "")) // Aplica texto sem máscara
        telefoneTextWatcher.afterTextChanged(editTextTelefone.editableText) // Aplica a máscara
        editTextTelefone.addTextChangedListener(telefoneTextWatcher)

        val cpfIntent = intent.getStringExtra("cpf_cliente_bloqueado") ?: ""
        editTextCPF.removeTextChangedListener(cpfTextWatcher)
        editTextCPF.setText(cpfIntent.replace("[^0-9]".toRegex(), ""))
        cpfTextWatcher.afterTextChanged(editTextCPF.editableText)
        editTextCPF.addTextChangedListener(cpfTextWatcher)

        val cnpjIntent = intent.getStringExtra("cnpj_cliente_bloqueado") ?: ""
        editTextCNPJ.removeTextChangedListener(cnpjTextWatcher)
        editTextCNPJ.setText(cnpjIntent.replace("[^0-9]".toRegex(), ""))
        cnpjTextWatcher.afterTextChanged(editTextCNPJ.editableText)
        editTextCNPJ.addTextChangedListener(cnpjTextWatcher)

        editTextSerial.setText(intent.getStringExtra("serial_cliente_bloqueado") ?: "")
        editTextInformacoesAdicionais.setText(intent.getStringExtra("info_adicionais_cliente_bloqueado") ?: "")

        val nomeClienteIntent = intent.getStringExtra("nome_cliente_bloqueado")
        this.clienteBloqueado = ClienteBloqueado(
            id = intent.getLongExtra("cliente_bloqueado_id", -1L),
            nome = nomeClienteIntent ?: "",
            email = editTextEmail.text.toString(),
            telefone = editTextTelefone.text.toString(),
            informacoesAdicionais = editTextInformacoesAdicionais.text.toString(),
            cpf = editTextCPF.text.toString(),
            cnpj = editTextCNPJ.text.toString(),
            logradouro = intent.getStringExtra("logradouro_cliente_bloqueado") ?: "",
            numero = intent.getStringExtra("numero_cliente_bloqueado") ?: "",
            complemento = intent.getStringExtra("complemento_cliente_bloqueado") ?: "",
            bairro = intent.getStringExtra("bairro_cliente_bloqueado") ?: "",
            municipio = intent.getStringExtra("municipio_cliente_bloqueado") ?: "",
            uf = intent.getStringExtra("uf_cliente_bloqueado") ?: "",
            cep = intent.getStringExtra("cep_cliente_bloqueado") ?: "",
            numeroSerial = editTextSerial.text.toString()
        )
    }

    override fun onResume() {
        super.onResume()
        carregarListaTodosClientesBloqueados()

        val clienteBloqueadoIdRecebido = intent.getLongExtra("cliente_bloqueado_id", -1L)
        if (clienteBloqueadoIdRecebido != -1L) {
            val clienteAindaExiste = listaObjetosClientesBloqueados.any { it.id == clienteBloqueadoIdRecebido }
            if (clienteAindaExiste) {
                if (clienteBloqueado?.id != clienteBloqueadoIdRecebido) {
                    loadClienteBloqueadoPorId(clienteBloqueadoIdRecebido)
                    val serialRecebidoDoIntent = intent.getStringExtra("serial_cliente_bloqueado")
                    if (!serialRecebidoDoIntent.isNullOrEmpty() &&
                        (editTextSerial.text.toString() != serialRecebidoDoIntent)) {
                        editTextSerial.setText(serialRecebidoDoIntent)
                        this.clienteBloqueado = this.clienteBloqueado?.copy(numeroSerial = serialRecebidoDoIntent)
                    }
                }
            } else {
                Log.w("ClientesBloqueados", "Cliente com ID $clienteBloqueadoIdRecebido não encontrado na lista após onResume, carregando primeiro se houver.")
                if (listaObjetosClientesBloqueados.isNotEmpty()) {
                    // loadPrimeiroClienteBloqueado() já é chamado indiretamente por carregarListaTodosClientesBloqueados
                } else {
                    limparCamposUI()
                    this.clienteBloqueado = null
                }
            }
        } else if (intent.hasExtra("nome_cliente_bloqueado") && (clienteBloqueado == null || clienteBloqueado?.id == -1L || clienteBloqueado?.id == 0L)) {
            // Se veio com dados de um novo bloqueio e o cliente atual não é esse ou é inválido
            preencherCamposComDadosDoIntent()
        }
        else if (clienteBloqueado == null && listaObjetosClientesBloqueados.isNotEmpty()) {
            // Se nenhum cliente carregado e a lista não está vazia, carregar o primeiro
            // loadPrimeiroClienteBloqueado() já é chamado indiretamente
        } else if (listaObjetosClientesBloqueados.isEmpty()){
            limparCamposUI()
            this.clienteBloqueado = null
        }
    }

    private fun aplicarMascaras() {
        telefoneTextWatcher = object : TextWatcher {
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
                val mascara = if (str.length > 10) "(##) #####-####" else "(##) ####-####"
                var i = 0
                val novaString = StringBuilder()
                for (m in mascara.toCharArray()) {
                    if ((m != '#') && (str.length > old.length || str.length == mascara.filterNot { it == '#' }.length + i)) {
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
                val currentSelection = editTextTelefone.selectionStart
                editTextTelefone.setText(novaString.toString())
                try {
                    editTextTelefone.setSelection(if (currentSelection > novaString.length) novaString.length else currentSelection)
                } catch (e: Exception) {
                    editTextTelefone.setSelection(novaString.length)
                }
                old = str
                isUpdating = false
            }
        }
        editTextTelefone.addTextChangedListener(telefoneTextWatcher)

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
                    if ((m != '#') && (str.length > old.length || str.length == mascara.filterNot { it == '#' }.length + i)) {
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
                val currentSelection = editTextCPF.selectionStart
                editTextCPF.setText(novaString.toString())
                try {
                    editTextCPF.setSelection(if (currentSelection > novaString.length) novaString.length else currentSelection)
                } catch (e: Exception) {
                    editTextCPF.setSelection(novaString.length)
                }
                old = str
                isUpdating = false
            }
        }
        editTextCPF.addTextChangedListener(cpfTextWatcher)

        cnpjTextWatcher = object : TextWatcher {
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
                val mascara = "##.###.###/####-##"
                var i = 0
                val novaString = StringBuilder()
                for (m in mascara.toCharArray()) {
                    if ((m != '#') && (str.length > old.length || str.length == mascara.filterNot { it == '#' }.length + i)) {
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
                val currentSelection = editTextCNPJ.selectionStart
                editTextCNPJ.setText(novaString.toString())
                try {
                    editTextCNPJ.setSelection(if (currentSelection > novaString.length) novaString.length else currentSelection)
                } catch (e: Exception) {
                    editTextCNPJ.setSelection(novaString.length)
                }
                old = str
                isUpdating = false
            }
        }
        editTextCNPJ.addTextChangedListener(cnpjTextWatcher)
    }

    private fun limparCamposUI() {
        editTextNome.text.clear()
        editTextEmail.text.clear()

        // É importante remover o listener antes de limpar, para evitar que a máscara reaja
        editTextTelefone.removeTextChangedListener(telefoneTextWatcher)
        editTextTelefone.text.clear()
        editTextTelefone.addTextChangedListener(telefoneTextWatcher)

        editTextCPF.removeTextChangedListener(cpfTextWatcher)
        editTextCPF.text.clear()
        editTextCPF.addTextChangedListener(cpfTextWatcher)

        editTextCNPJ.removeTextChangedListener(cnpjTextWatcher)
        editTextCNPJ.text.clear()
        editTextCNPJ.addTextChangedListener(cnpjTextWatcher)

        editTextSerial.text.clear()
        editTextInformacoesAdicionais.text.clear()
    }

    private fun preencherCamposComCliente(cliente: ClienteBloqueado) {
        editTextNome.setText(cliente.nome)
        editTextEmail.setText(cliente.email)

        editTextTelefone.removeTextChangedListener(telefoneTextWatcher)
        editTextTelefone.setText(cliente.telefone.replace("[^0-9]".toRegex(), "")) // Aplicar texto sem máscara
        telefoneTextWatcher.afterTextChanged(editTextTelefone.editableText) // Chamar para aplicar a máscara
        editTextTelefone.addTextChangedListener(telefoneTextWatcher)

        editTextCPF.removeTextChangedListener(cpfTextWatcher)
        editTextCPF.setText(cliente.cpf.replace("[^0-9]".toRegex(), ""))
        cpfTextWatcher.afterTextChanged(editTextCPF.editableText)
        editTextCPF.addTextChangedListener(cpfTextWatcher)

        editTextCNPJ.removeTextChangedListener(cnpjTextWatcher)
        editTextCNPJ.setText(cliente.cnpj.replace("[^0-9]".toRegex(), ""))
        cnpjTextWatcher.afterTextChanged(editTextCNPJ.editableText)
        editTextCNPJ.addTextChangedListener(cnpjTextWatcher)

        editTextSerial.setText(cliente.numeroSerial)
        editTextInformacoesAdicionais.setText(cliente.informacoesAdicionais)
    }

    private fun carregarListaTodosClientesBloqueados() {
        val oldSelectedClienteId = clienteBloqueado?.id
        val oldSelectedNomeSeTemporario = if (clienteBloqueado?.id == -1L || clienteBloqueado?.id == 0L) clienteBloqueado?.nome else null

        listaNomesClientesBloqueados.clear()
        listaObjetosClientesBloqueados.clear()
        val db = dbHelper?.readableDatabase ?: return

        try {
            db.query(
                ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME,
                null, null, null, null, null,
                "${ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME} ASC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                    val nome = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME)) ?: ""
                    val email = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_EMAIL)) ?: ""
                    val telefone = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_TELEFONE)) ?: ""
                    val informacoesAdicionais = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS)) ?: ""
                    val cpf = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF)) ?: ""
                    val cnpj = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ)) ?: ""
                    val logradouro = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_LOGRADOURO)) ?: ""
                    val numero = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO)) ?: ""
                    val complemento = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_COMPLEMENTO)) ?: ""
                    val bairro = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_BAIRRO)) ?: ""
                    val municipio = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_MUNICIPIO)) ?: ""
                    val uf = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_UF)) ?: ""
                    val cep = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CEP)) ?: ""
                    val numeroSerial = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL)) ?: ""

                    listaNomesClientesBloqueados.add(nome)
                    listaObjetosClientesBloqueados.add(
                        ClienteBloqueado(id, nome, email, telefone, informacoesAdicionais, cpf, cnpj, logradouro, numero, complemento, bairro, municipio, uf, cep, numeroSerial)
                    )
                }
            }
            adapterListaBloqueados.notifyDataSetChanged()

            if (listaObjetosClientesBloqueados.isNotEmpty()) {
                var clienteASerExibido: ClienteBloqueado? = null
                val clienteBloqueadoIdRecebido = intent.getLongExtra("cliente_bloqueado_id", -1L)

                if (clienteBloqueadoIdRecebido != -1L) {
                    clienteASerExibido = listaObjetosClientesBloqueados.find { it.id == clienteBloqueadoIdRecebido }
                }

                if (clienteASerExibido == null && oldSelectedClienteId != null && oldSelectedClienteId > 0L) {
                    clienteASerExibido = listaObjetosClientesBloqueados.find { it.id == oldSelectedClienteId }
                } else if (clienteASerExibido == null && oldSelectedNomeSeTemporario != null && (oldSelectedClienteId == -1L || oldSelectedClienteId == 0L)) {
                    clienteASerExibido = listaObjetosClientesBloqueados.find { it.nome == oldSelectedNomeSeTemporario && it.id <= 0L }
                }

                if (clienteASerExibido == null) {
                    clienteASerExibido = listaObjetosClientesBloqueados.first()
                }

                if (clienteASerExibido.id > 0) {
                    loadClienteBloqueadoPorId(clienteASerExibido.id)
                } else if (this.clienteBloqueado?.id != clienteASerExibido.id && clienteASerExibido.id <= 0) {
                    preencherCamposComCliente(clienteASerExibido)
                    this.clienteBloqueado = clienteASerExibido
                } else if (clienteASerExibido.id <=0 && this.clienteBloqueado?.id == clienteASerExibido.id) {
                    // Já está mostrando o cliente temporário correto, não faz nada
                } else {
                    limparCamposUI()
                    this.clienteBloqueado = null
                }
            } else {
                limparCamposUI()
                this.clienteBloqueado = null
            }
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao carregar lista de todos os clientes bloqueados: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar lista de bloqueados.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadClienteBloqueadoPorId(id: Long) {
        val db = dbHelper?.readableDatabase ?: run {
            Log.e("ClientesBloqueados", "Erro ao acessar banco para carregar por ID")
            return
        }
        try {
            db.query(
                ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME, null,
                "${android.provider.BaseColumns._ID} = ?", arrayOf(id.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val nome = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME)) ?: ""
                    val email = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_EMAIL)) ?: ""
                    val telefone = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_TELEFONE)) ?: ""
                    val informacoesAdicionais = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS)) ?: ""
                    val cpf = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF)) ?: ""
                    val cnpj = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ)) ?: ""
                    val logradouro = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_LOGRADOURO)) ?: ""
                    val numero = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO)) ?: ""
                    val complemento = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_COMPLEMENTO)) ?: ""
                    val bairro = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_BAIRRO)) ?: ""
                    val municipio = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_MUNICIPIO)) ?: ""
                    val uf = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_UF)) ?: ""
                    val cep = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CEP)) ?: ""
                    val numeroSerial = cursor.getString(cursor.getColumnIndexOrThrow(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL)) ?: ""

                    clienteBloqueado = ClienteBloqueado(
                        id, nome, email, telefone, informacoesAdicionais, cpf, cnpj,
                        logradouro, numero, complemento, bairro, municipio, uf, cep, numeroSerial
                    )
                    preencherCamposComCliente(clienteBloqueado!!)
                } else {
                    limparCamposUI()
                    this.clienteBloqueado = null
                    carregarListaTodosClientesBloqueados()
                }
            }
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao carregar cliente por ID: ${e.message}", e)
        }
    }

    private fun saveClienteSeModificado() {
        clienteBloqueado?.let { clienteOriginal ->
            val nomeAtual = editTextNome.text.toString().trim()
            val emailAtual = editTextEmail.text.toString().trim()
            val telefoneAtualComMascara = editTextTelefone.text.toString()
            val cpfAtualComMascara = editTextCPF.text.toString()
            val cnpjAtualComMascara = editTextCNPJ.text.toString()
            val serialAtual = editTextSerial.text.toString().trim()
            val infoAtual = editTextInformacoesAdicionais.text.toString().trim()

            // Para comparação, precisamos formatar o original da mesma forma que o atual é exibido, ou comparar sem máscara
            // Aqui, vamos comparar com o valor formatado, assumindo que `preencherCamposComCliente` aplicou a máscara.
            if (nomeAtual != clienteOriginal.nome ||
                emailAtual != clienteOriginal.email ||
                telefoneAtualComMascara != clienteOriginal.telefone || // Assumindo que clienteOriginal.telefone já está formatado
                cpfAtualComMascara != clienteOriginal.cpf ||
                cnpjAtualComMascara != clienteOriginal.cnpj ||
                serialAtual != clienteOriginal.numeroSerial ||
                infoAtual != clienteOriginal.informacoesAdicionais) {

                saveCliente(
                    nomeAtual, emailAtual,
                    telefoneAtualComMascara.replace("[^0-9]".toRegex(), ""), // Salva só números
                    cpfAtualComMascara.replace("[^0-9]".toRegex(), ""),
                    cnpjAtualComMascara.replace("[^0-9]".toRegex(), ""),
                    serialAtual, infoAtual
                )
            }
        }
    }

    private fun saveCliente(nome: String, email: String, telefoneSemMascara: String, cpfSemMascara: String, cnpjSemMascara: String, numeroSerial: String, informacoesAdicionais: String) {
        if (nome.isBlank() && (clienteBloqueado == null || clienteBloqueado?.id == -1L || clienteBloqueado?.id == 0L) ) {
            Log.w("ClientesBloqueados", "Nome não fornecido para novo cliente ou cliente com ID temporário, não salvando.")
            return
        }
        if (nome.isBlank() && clienteBloqueado == null) {
            Log.w("ClientesBloqueados", "Nome não fornecido e nenhum cliente carregado, não salvando.")
            return
        }

        try {
            val db = dbHelper?.writableDatabase
            val values = ContentValues().apply {
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NOME, nome)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_EMAIL, email)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_TELEFONE, telefoneSemMascara)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CPF, cpfSemMascara)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CNPJ, cnpjSemMascara)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO_SERIAL, numeroSerial)
                put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, informacoesAdicionais)
                clienteBloqueado?.let {
                    put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_LOGRADOURO, it.logradouro)
                    put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_NUMERO, it.numero)
                    put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_COMPLEMENTO, it.complemento)
                    put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_BAIRRO, it.bairro)
                    put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_MUNICIPIO, it.municipio)
                    put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_UF, it.uf)
                    put(ClientesBloqueadosContract.ClienteBloqueadoEntry.COLUMN_NAME_CEP, it.cep)
                }
            }

            clienteBloqueado?.let { clienteAtual ->
                if (clienteAtual.id > 0) {
                    val rowsUpdated = db?.update(
                        ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME,
                        values,
                        "${android.provider.BaseColumns._ID} = ?",
                        arrayOf(clienteAtual.id.toString())
                    )
                    if (rowsUpdated != null && rowsUpdated > 0) {
                        Log.d("ClientesBloqueados", "Cliente atualizado: $nome, Serial: $numeroSerial")
                        this.clienteBloqueado = clienteAtual.copy(
                            nome = nome, email = email, telefone = telefoneSemMascara,
                            informacoesAdicionais = informacoesAdicionais, cpf = cpfSemMascara, cnpj = cnpjSemMascara,
                            numeroSerial = numeroSerial
                        )
                        val indexInList = listaObjetosClientesBloqueados.indexOfFirst { it.id == clienteAtual.id }
                        if (indexInList != -1) {
                            listaObjetosClientesBloqueados[indexInList] = this.clienteBloqueado!!
                            if (listaNomesClientesBloqueados[indexInList] != nome) {
                                listaNomesClientesBloqueados[indexInList] = nome
                                adapterListaBloqueados.notifyDataSetChanged()
                            }
                        } else {
                            Log.w("ClientesBloqueados", "Cliente atualizado (ID ${clienteAtual.id}) não encontrado na lista local para atualização do nome de exibição.")
                        }
                    } else {
                        Log.w("ClientesBloqueados", "Erro ao atualizar cliente: $nome, nenhuma linha afetada.")
                    }
                } else {
                    Log.w("ClientesBloqueados", "Tentativa de salvar cliente com ID inválido (${clienteAtual.id}). Nome: $nome. Esta ação deve ser um INSERT, não UPDATE.")
                }
            }
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao salvar cliente: ${e.message}", e)
        }
    }

    private fun confirmarExclusao() {
        clienteBloqueado?.let { cliente ->
            if (cliente.id == -1L || cliente.id == 0L) {
                Toast.makeText(this, "Este cliente não está salvo no banco e não pode ser excluído.", Toast.LENGTH_LONG).show()
                return
            }
            AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja remover '${cliente.nome}' da lista de bloqueados?")
                .setPositiveButton("Excluir") { _, _ ->
                    excluirClienteBloqueado(cliente)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } ?: Toast.makeText(this, "Nenhum cliente selecionado para excluir.", Toast.LENGTH_LONG).show()
    }

    private fun excluirClienteBloqueado(cliente: ClienteBloqueado) {
        try {
            val db = dbHelper?.writableDatabase
            val rowsDeleted = db?.delete(
                ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(cliente.id.toString())
            )
            if (rowsDeleted != null && rowsDeleted > 0) {
                Log.d("ClientesBloqueados", "Cliente excluído da lista negra: ${cliente.nome}")
                Toast.makeText(this, "Cliente removido da lista negra.", Toast.LENGTH_LONG).show()
                this.clienteBloqueado = null
                carregarListaTodosClientesBloqueados()
            } else {
                Log.w("ClientesBloqueados", "Erro ao excluir cliente da lista negra: ${cliente.nome}")
                Toast.makeText(this, "Erro ao excluir cliente.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao excluir cliente da lista negra: ${e.message}", e)
            Toast.makeText(this, "Erro ao excluir cliente.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        try {
            saveClienteSeModificado()
            dbHelper?.close()
            Log.d("ClientesBloqueados", "ClienteDbHelper fechado")
        } catch (e: Exception) {
            Log.e("ClientesBloqueados", "Erro ao fechar banco: ${e.message}", e)
        }
        super.onDestroy()
    }
}