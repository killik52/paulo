package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapplication.BuildConfig
import com.example.myapplication.databinding.ActivityCriarNovoArtigoBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CriarNovoArtigoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarNovoArtigoBinding
    private var dbHelper: ClienteDbHelper? = null
    private var artigoId: Long = -1
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    })
    private var photoUri: Uri? = null
    private val REQUEST_IMAGE_CAPTURE = 100

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("CriarNovoArtigo", "Permissão da câmera concedida.")
            openCamera()
        } else {
            Log.w("CriarNovoArtigo", "Permissão da câmera negada.")
            showToast("Permissão da câmera negada. Não é possível abrir a câmera.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCriarNovoArtigoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        artigoId = intent.getLongExtra("artigo_id", -1)
        Log.d("CriarNovoArtigo", "Recebido artigo_id: $artigoId")

        if (artigoId != -1L) {
            binding.textViewArtigoTitolo.text = "Editar Artigo"
            loadArtigoData(artigoId)
        } else {
            binding.textViewArtigoTitolo.text = "Novo Artigo"
            intent.getStringExtra("nome_artigo")?.let { binding.editTextNome.setText(it) }
            intent.getIntExtra("quantidade", 1).let { binding.editTextQtd.setText(it.toString()) }
            val precoUnitarioIntent = intent.getDoubleExtra("valor", 0.0)
            binding.editTextPreco.setText(if (precoUnitarioIntent == 0.0) "" else decimalFormat.format(precoUnitarioIntent))
            intent.getStringExtra("numero_serial")?.let { binding.editTextNumeroSerial.setText(it) }
            intent.getStringExtra("descricao")?.let { binding.editTextDescricao.setText(it) }
        }

        val sharedPreferences = getSharedPreferences("DefinicoesGuardarArtigo", MODE_PRIVATE)
        val guardarArtigoPadrao = sharedPreferences.getBoolean("guardar_artigo_padrao", true)
        binding.switchGuardarFatura.isChecked = guardarArtigoPadrao
        Log.d("CriarNovoArtigo", "SwitchGuardarArtigo definido para: $guardarArtigoPadrao (Padrão da SharedPreferences ou true)")

        atualizarValorTotal()

        binding.textViewAddFoto.setOnClickListener {
            Log.d("CriarNovoArtigo", "Clicado em textViewAddFoto. Verificando permissão da câmera.")
            checkCameraPermission()
        }

        binding.editTextPreco.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.editTextPreco.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(Regex("[R$\\s,.]"), "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toDouble() / 100.0
                            val formatted = decimalFormat.format(parsed)
                            current = "R$ $formatted"
                            binding.editTextPreco.setText(current)
                            binding.editTextPreco.setSelection(current.length)
                        } catch (e: NumberFormatException) {
                            Log.e("CriarNovoArtigo", "Erro ao formatar preço: ${e.message}, input: $cleanString")
                            current = s.toString()
                            binding.editTextPreco.setText(current)
                            binding.editTextPreco.setSelection(current.length)
                        }
                    } else {
                        current = ""
                        binding.editTextPreco.setText("")
                    }
                    binding.editTextPreco.addTextChangedListener(this)
                    atualizarValorTotal()
                }
            }
        })

        binding.editTextQtd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                atualizarValorTotal()
            }
        })

        binding.textViewGuardarArtigo.setOnClickListener {
            val nome = binding.editTextNome.text.toString().trim()
            val numeroSerialDoArtigo = binding.editTextNumeroSerial.text.toString().trim()

            if (nome.isEmpty()) {
                showToast("Por favor, insira o nome do artigo.")
                return@setOnClickListener
            }

            if (numeroSerialDoArtigo.isNotEmpty()) {
                verificarArtigoBloqueadoPorSerial(numeroSerialDoArtigo) { bloqueado, clienteBloqueado ->
                    if (bloqueado && clienteBloqueado != null) {
                        mostrarDialogoArtigoBloqueado(clienteBloqueado, numeroSerialDoArtigo)
                    } else {
                        procederComSalvarArtigo()
                    }
                }
            } else {
                procederComSalvarArtigo()
            }
        }

        binding.buttonExcluirArtigo.setOnClickListener {
            if (artigoId != -1L) {
                AlertDialog.Builder(this)
                    .setTitle("Excluir Artigo dos Recentes")
                    .setMessage("Tem certeza que deseja excluir este artigo da lista de itens recentes? Ele não será removido de faturas já existentes.")
                    .setPositiveButton("Excluir dos Recentes") { _, _ ->
                        excluirArtigoDoBanco(artigoId)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                showToast("Este artigo ainda não foi salvo nos recentes.")
            }
        }
    }

    private fun excluirArtigoDoBanco(id: Long) {
        val db = dbHelper?.writableDatabase
        try {
            val values = ContentValues().apply {
                put(ArtigoContract.ArtigoEntry.COLUMN_NAME_GUARDAR_FATURA, 0)
            }
            val count = db?.update(
                ArtigoContract.ArtigoEntry.TABLE_NAME,
                values,
                "${BaseColumns._ID} = ?",
                arrayOf(id.toString())
            )

            if (count != null && count > 0) {
                showToast("Artigo removido dos itens recentes.")
                Log.d("CriarNovoArtigo", "Artigo ID $id marcado para não ser guardado para recentes.")
                val resultIntent = Intent()
                resultIntent.putExtra("artigo_id", id)
                resultIntent.putExtra("salvar_fatura", false)
                resultIntent.putExtra("nome_artigo", binding.editTextNome.text.toString().trim())
                resultIntent.putExtra("quantidade", binding.editTextQtd.text.toString().trim().toIntOrNull() ?: 1)
                val precoUnitario = try {
                    normalizeInput(binding.editTextPreco.text.toString().trim().replace("R$\\s*".toRegex(), "")).toDouble()
                } catch (e: Exception) { 0.0 }
                resultIntent.putExtra("valor", precoUnitario * (binding.editTextQtd.text.toString().trim().toIntOrNull() ?: 1))
                resultIntent.putExtra("preco_unitario_artigo", precoUnitario)
                resultIntent.putExtra("numero_serial", binding.editTextNumeroSerial.text.toString().trim())
                resultIntent.putExtra("descricao", binding.editTextDescricao.text.toString().trim())

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                showToast("Erro ao remover artigo dos recentes.")
                Log.w("CriarNovoArtigo", "Nenhuma linha afetada ao tentar remover artigo ID $id dos recentes.")
            }
        } catch (e: Exception) {
            showToast("Erro ao remover artigo dos recentes: ${e.message}")
            Log.e("CriarNovoArtigo", "Erro ao remover artigo ID $id dos recentes: ${e.message}")
        }
    }

    private fun procederComSalvarArtigo() {
        val nome = binding.editTextNome.text.toString().trim()
        val precoStrInput = binding.editTextPreco.text.toString().trim().replace("R$\\s*".toRegex(), "")
        val quantidadeStr = binding.editTextQtd.text.toString().trim()
        val numeroSerial = binding.editTextNumeroSerial.text.toString().trim()
        val descricao = binding.editTextDescricao.text.toString().trim()
        val guardarParaRecentes = binding.switchGuardarFatura.isChecked

        if (nome.isEmpty()) {
            showToast("Por favor, insira o nome do artigo.")
            return
        }

        val quantidade = if (quantidadeStr.isNotEmpty()) {
            try {
                quantidadeStr.toInt().coerceAtLeast(1)
            } catch (e: NumberFormatException) {
                showToast("Quantidade inválida.")
                return
            }
        } else {
            showToast("A quantidade é obrigatória.")
            return
        }

        val precoUnitario = try {
            val normalizedPreco = normalizeInput(precoStrInput)
            normalizedPreco.toDouble().coerceAtLeast(0.0)
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "Erro ao parsear preço em salvar: ${e.message}, input: $precoStrInput")
            showToast("Preço inválido.")
            return
        }

        Log.d("CriarNovoArtigo", "Salvando: nome=$nome, qtd=$quantidade, precoUnit=$precoUnitario, guardar=$guardarParaRecentes")

        val valorTotalItem = precoUnitario * quantidade
        var idParaRetorno = artigoId

        if (guardarParaRecentes) {
            try {
                val db = dbHelper?.writableDatabase
                val values = ContentValues().apply {
                    put(ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME, nome)
                    put(ArtigoContract.ArtigoEntry.COLUMN_NAME_PRECO, precoUnitario)
                    put(ArtigoContract.ArtigoEntry.COLUMN_NAME_QUANTIDADE, 1)
                    put(ArtigoContract.ArtigoEntry.COLUMN_NAME_DESCONTO, 0.0)
                    put(ArtigoContract.ArtigoEntry.COLUMN_NAME_DESCRICAO, descricao)
                    put(ArtigoContract.ArtigoEntry.COLUMN_NAME_GUARDAR_FATURA, 1)
                    put(ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL, numeroSerial)
                }

                if (artigoId != -1L) {
                    val rowsUpdated = db?.update(
                        ArtigoContract.ArtigoEntry.TABLE_NAME,
                        values,
                        "${BaseColumns._ID} = ?",
                        arrayOf(artigoId.toString())
                    )
                    if (rowsUpdated == null || rowsUpdated <= 0) {
                        showToast("Erro ao atualizar o artigo nos recentes.")
                    } else {
                        showToast("Artigo atualizado e guardado para recentes!")
                    }
                } else {
                    val newRowId = db?.insert(ArtigoContract.ArtigoEntry.TABLE_NAME, null, values)
                    if (newRowId == null || newRowId == -1L) {
                        showToast("Erro ao salvar o novo artigo nos recentes.")
                    } else {
                        idParaRetorno = newRowId
                        showToast("Novo artigo salvo e guardado para recentes!")
                    }
                }
            } catch (e: Exception) {
                Log.e("CriarNovoArtigo", "Erro ao salvar/atualizar artigo no DB: ${e.message}")
                showToast("Erro ao interagir com o banco de dados para 'Recentes'.")
            }
        } else {
            if (artigoId != -1L) {
                val db = dbHelper?.writableDatabase
                val values = ContentValues()
                values.put(ArtigoContract.ArtigoEntry.COLUMN_NAME_GUARDAR_FATURA, 0)
                db?.update(
                    ArtigoContract.ArtigoEntry.TABLE_NAME,
                    values,
                    "${BaseColumns._ID} = ?",
                    arrayOf(artigoId.toString())
                )
                Log.d("CriarNovoArtigo", "Artigo ID $artigoId removido dos recentes (flag atualizada).")
            }
            if (idParaRetorno == -1L || idParaRetorno == 0L) {
                idParaRetorno = -System.currentTimeMillis()
            }
            showToast("Artigo será usado apenas na fatura atual.")
        }

        val resultIntent = Intent().apply {
            putExtra("artigo_id", idParaRetorno)
            putExtra("nome_artigo", nome)
            putExtra("quantidade", quantidade)
            putExtra("valor", valorTotalItem)
            putExtra("preco_unitario_artigo", precoUnitario)
            putExtra("numero_serial", numeroSerial)
            putExtra("salvar_fatura", guardarParaRecentes)
            putExtra("descricao", descricao)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun loadArtigoData(id: Long) {
        val db = dbHelper?.readableDatabase ?: return
        val cursor = db.query(
            ArtigoContract.ArtigoEntry.TABLE_NAME,
            null,
            "${BaseColumns._ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                binding.editTextNome.setText(it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NOME)))
                val precoUnitarioDB = it.getDouble(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_PRECO))
                binding.editTextPreco.setText(if (precoUnitarioDB == 0.0) "" else decimalFormat.format(precoUnitarioDB))

                val quantidadeParaExibir = if (intent.hasExtra("quantidade_fatura")) {
                    intent.getIntExtra("quantidade_fatura", 1)
                } else {
                    it.getInt(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_QUANTIDADE))
                }
                binding.editTextQtd.setText(quantidadeParaExibir.toString())

                binding.editTextDescricao.setText(it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_DESCRICAO)))
                binding.editTextNumeroSerial.setText(it.getString(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL)))
                binding.switchGuardarFatura.isChecked = it.getInt(it.getColumnIndexOrThrow(ArtigoContract.ArtigoEntry.COLUMN_NAME_GUARDAR_FATURA)) == 1
                Log.d("CriarNovoArtigo", "Dados do artigo ID $id carregados. Guardar Fatura: ${binding.switchGuardarFatura.isChecked}")
            }
        }
        atualizarValorTotal()
    }

    private fun verificarArtigoBloqueadoPorSerial(numeroSerialArtigo: String, callback: (Boolean, ClienteBloqueado?) -> Unit) {
        if (numeroSerialArtigo.isBlank()) {
            callback(false, null)
            return
        }

        val db = dbHelper?.readableDatabase ?: run {
            callback(false, null)
            return
        }

        val cursor: Cursor? = db.rawQuery(
            """
            SELECT cb.*
            FROM ${ClientesBloqueadosContract.ClienteBloqueadoEntry.TABLE_NAME} cb
            INNER JOIN ${FaturaContract.FaturaItemEntry.TABLE_NAME} fi
                ON fi.${FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID} = cb.${BaseColumns._ID}
            INNER JOIN ${ArtigoContract.ArtigoEntry.TABLE_NAME} a
                ON fi.${FaturaContract.FaturaItemEntry.COLUMN_NAME_ARTIGO_ID} = a.${BaseColumns._ID}
            WHERE a.${ArtigoContract.ArtigoEntry.COLUMN_NAME_NUMERO_SERIAL} = ?
            """,
            arrayOf(numeroSerialArtigo)
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
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

                val clienteEncontrado = ClienteBloqueado(id, nomeBloqueado, email, telefone, informacoesAdicionais, cpfBloqueado, cnpjBloqueado, logradouro, numero, complemento, bairro, municipio, uf, cep, numeroSerial)
                callback(true, clienteEncontrado)
                return
            }
        }
        callback(false, null)
    }

    private fun mostrarDialogoArtigoBloqueado(cliente: ClienteBloqueado, serialDoArtigo: String) {
        AlertDialog.Builder(this)
            .setTitle("Artigo Associado a Cliente Bloqueado")
            .setMessage("O número de série '$serialDoArtigo' está associado ao cliente bloqueado '${cliente.nome}'. Deseja ver as informações do cliente bloqueado?")
            .setPositiveButton("Sim") { _, _ ->
                val intent = Intent(this, ClientesBloqueadosActivity::class.java).apply {
                    putExtra("cliente_bloqueado_id", cliente.id)
                    putExtra("nome_cliente_bloqueado", cliente.nome)
                    putExtra("serial_cliente_bloqueado", cliente.numeroSerial)
                }
                startActivity(intent)
            }
            .setNegativeButton("Não, continuar cadastro") { dialog, _ ->
                dialog.dismiss()
                procederComSalvarArtigo()
            }
            .setNeutralButton("Cancelar cadastro do artigo") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("CriarNovoArtigo", "Permissão da câmera já concedida.")
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.w("CriarNovoArtigo", "Permissão da câmera foi negada anteriormente. Explicação necessária.")
                AlertDialog.Builder(this)
                    .setTitle("Permissão Necessária")
                    .setMessage("A permissão da câmera é necessária para tirar fotos do número de série.")
                    .setPositiveButton("OK") { _, _ ->
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> {
                Log.d("CriarNovoArtigo", "Solicitando permissão da câmera.")
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        Log.d("CriarNovoArtigo", "Iniciando câmera.")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("CriarNovoArtigo", "Erro ao criar arquivo para foto: ${ex.message}", ex)
                showToast("Erro ao criar arquivo para foto: ${ex.message}")
                return
            }

            photoFile?.also { file ->
                val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
                Log.d("CriarNovoArtigo", "Usando authority para FileProvider: $authority")
                photoUri = FileProvider.getUriForFile(
                    this,
                    authority,
                    file
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                try {
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                } catch (e: Exception) {
                    Log.e("CriarNovoArtigo", "Erro ao iniciar atividade da câmera: ${e.message}", e)
                    showToast("Não foi possível abrir a câmera.")
                }
            }
        } else {
            showToast("Nenhum aplicativo de câmera disponível.")
            Log.w("CriarNovoArtigo", "Nenhum aplicativo de câmera encontrado para ACTION_IMAGE_CAPTURE.")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir == null) {
            Log.e("CriarNovoArtigo", "Diretório de armazenamento externo (Pictures) não disponível.")
            val internalStorageDir: File = filesDir
            val imagesDir = File(internalStorageDir, "images_fallback")
            if (!imagesDir.exists()) {
                if (!imagesDir.mkdirs()) {
                    Log.e("CriarNovoArtigo", "Falha ao criar diretório de imagens interno de fallback: ${imagesDir.absolutePath}")
                    throw IOException("Diretório de armazenamento não disponível e fallback de diretório interno falhou.")
                }
            }
            Log.d("CriarNovoArtigo", "Usando diretório de armazenamento interno de fallback: ${imagesDir.absolutePath}")
            return File.createTempFile("JPEG_${timeStamp}_", ".jpg", imagesDir).also {
                Log.d("CriarNovoArtigo", "Arquivo para foto criado em fallback: ${it.absolutePath}")
            }
        }

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("CriarNovoArtigo", "Falha ao criar diretório de armazenamento de imagens: ${storageDir.absolutePath}")
                throw IOException("Não foi possível criar o diretório de armazenamento: ${storageDir.absolutePath}")
            }
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).also {
            Log.d("CriarNovoArtigo", "Arquivo para foto criado em Pictures: ${it.absolutePath}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d("CriarNovoArtigo", "Foto capturada com sucesso. URI: $photoUri")
            photoUri?.let { uri ->
                try {
                    val image = InputImage.fromFilePath(this, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            Log.d("CriarNovoArtigo", "Texto reconhecido: ${visionText.text}")
                            showTextSelectionDialog(visionText)
                        }
                        .addOnFailureListener { e ->
                            Log.e("CriarNovoArtigo", "Erro ao reconhecer texto: ${e.message}", e)
                            showToast("Erro ao reconhecer texto da imagem.")
                        }
                } catch (e: Exception) {
                    Log.e("CriarNovoArtigo", "Erro ao processar imagem para OCR: ${e.message}", e)
                    showToast("Erro ao processar imagem para reconhecimento de texto.")
                }
            } ?: Log.w("CriarNovoArtigo", "photoUri é nulo após captura da imagem.")
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Log.w("CriarNovoArtigo", "Captura de imagem cancelada ou falhou. ResultCode: $resultCode")
        }
    }

    private fun showTextSelectionDialog(visionText: Text) {
        val textLines = visionText.textBlocks.flatMap { block -> block.lines.map { it.text } }
        if (textLines.isEmpty()) {
            showToast("Nenhum texto reconhecido na imagem.")
            return
        }
        val dialogItems = textLines.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Selecione o código")
            .setItems(dialogItems) { _, which ->
                val selectedText = textLines[which]
                binding.editTextNumeroSerial.setText(selectedText)
                Log.d("CriarNovoArtigo", "Texto selecionado: $selectedText")
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun normalizeInput(input: String?): String {
        if (input.isNullOrEmpty()) return "0.00"
        val cleanedInputInitial = input.replace(Regex("[R$\\s]"), "")
        val cleanedInputWithDot = cleanedInputInitial.replace(',', '.')
        var firstDotFound = false
        val cleanedInput = cleanedInputWithDot.filter { char ->
            if (char.isDigit()) true
            else if (char == '.' && !firstDotFound) { firstDotFound = true; true }
            else false
        }
        if (cleanedInput.isEmpty()) return "0.00"
        return try {
            val parts: List<String> = cleanedInput.split('.')
            val integerPart = parts.getOrNull(0)?.ifEmpty { "0" } ?: "0"
            val decimalPartRaw = if (parts.size > 1) parts.getOrNull(1) ?: "" else ""
            val decimalPart = when {
                decimalPartRaw.isEmpty() -> "00"
                decimalPartRaw.length == 1 -> "${decimalPartRaw}0"
                decimalPartRaw.length > 2 -> decimalPartRaw.substring(0, 2)
                else -> decimalPartRaw
            }
            "$integerPart.$decimalPart"
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "Erro ao normalizar input de preço '$cleanedInput': ${e.message}")
            "0.00"
        }
    }

    private fun atualizarValorTotal() {
        try {
            val precoStr = binding.editTextPreco.text.toString().trim().replace("R$\\s*".toRegex(), "")
            val quantidadeStr = binding.editTextQtd.text.toString().trim()
            val precoUnitario = if (precoStr.isNotEmpty()) {
                normalizeInput(precoStr).toDouble()
            } else {
                0.0
            }
            val quantidade = if (quantidadeStr.isNotEmpty()) {
                quantidadeStr.toInt().coerceAtLeast(1)
            } else {
                1
            }
            val total = precoUnitario * quantidade
            binding.textViewValorEsquerda.text = "R$ ${decimalFormat.format(total)}"
            Log.d("CriarNovoArtigo", "Valor total atualizado: ${binding.textViewValorEsquerda.text}")
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "Erro ao atualizar valor total: ${e.message}")
            binding.textViewValorEsquerda.text = "R$ 0,00"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
        Log.d("CriarNovoArtigo", "onDestroy chamado")
    }
}