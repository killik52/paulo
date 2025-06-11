package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private var dbHelper: ClienteDbHelper? = null
    private var isGridViewVisible = false
    private val SECOND_SCREEN_REQUEST_CODE = 1
    private val STORAGE_PERMISSION_CODE = 100
    private val LIXEIRA_REQUEST_CODE = 1002
    private lateinit var faturaAdapter: FaturaResumidaAdapter
    private var isSearchActive = false
    private var mediaPlayer: MediaPlayer? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            showToast("Leitura cancelada")
        } else {
            val barcodeValue = result.contents
            Log.d("MainActivity", "Código de barras lido (bruto): '$barcodeValue'")
            emitBeep()
            openInvoiceByBarcode(barcodeValue)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "onCreate chamado com ViewBinding")

        dbHelper = ClienteDbHelper(this)

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep)
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("MainActivity", "Erro no MediaPlayer: what=$what, extra=$extra")
                showToast("Erro ao inicializar o som de beep")
                true
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao inicializar MediaPlayer: ${e.message}")
            showToast("Erro ao carregar o som de beep")
        }

        binding.recyclerViewFaturas.layoutManager = LinearLayoutManager(this)
        faturaAdapter = FaturaResumidaAdapter(
            this,
            onItemClick = { fatura ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val db = dbHelper?.readableDatabase
                        if (db == null) {
                            withContext(Dispatchers.Main) {
                                showToast("Erro: Banco de dados não acessível.")
                            }
                            return@launch
                        }

                        val cursor = db.query(
                            FaturaContract.FaturaEntry.TABLE_NAME,
                            arrayOf(BaseColumns._ID, FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA),
                            "${BaseColumns._ID} = ?",
                            arrayOf(fatura.id.toString()),
                            null, null, null
                        )

                        cursor?.use {
                            if (it.moveToFirst()) {
                                val foiEnviada = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1
                                withContext(Dispatchers.Main) {
                                    val intent = Intent(this@MainActivity, SecondScreenActivity::class.java).apply {
                                        putExtra("fatura_id", fatura.id)
                                        putExtra("foi_enviada", foiEnviada)
                                    }
                                    startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showToast("Fatura não encontrada.")
                                }
                            }
                        } ?: withContext(Dispatchers.Main) {
                            showToast("Erro ao consultar fatura.")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("MainActivity", "Erro ao abrir fatura: ${e.message}")
                            showToast("Erro ao abrir fatura: ${e.message}")
                        }
                    }
                }
            },
            onItemLongClick = { fatura ->
                Log.d("MainActivity", "Iniciando exclusão da fatura ID=${fatura.id}")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val db = dbHelper?.writableDatabase
                        if (db == null) {
                            withContext(Dispatchers.Main) {
                                Log.e("MainActivity", "Banco de dados não está acessível para exclusão")
                                showToast(getString(R.string.database_error_message))
                            }
                            return@launch
                        }

                        val cursor = db.query(
                            FaturaContract.FaturaEntry.TABLE_NAME,
                            null,
                            "${BaseColumns._ID} = ?",
                            arrayOf(fatura.id.toString()),
                            null, null, null
                        )

                        cursor?.use {
                            if (it.moveToFirst()) {
                                val values = ContentValues().apply {
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NUMERO_FATURA, it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_CLIENTE, it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_ARTIGOS, it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SUBTOTAL, it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SUBTOTAL)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO, it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO_PERCENT, it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_TAXA_ENTREGA, it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SALDO_DEVEDOR, it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA, it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_FOTO_IMPRESSORA, it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA)))
                                    put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NOTAS, it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS)))
                                }

                                val newRowId = db.insert(FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME, null, values)
                                if (newRowId != -1L) {
                                    val rowsDeleted = db.delete(
                                        FaturaContract.FaturaEntry.TABLE_NAME,
                                        "${BaseColumns._ID} = ?",
                                        arrayOf(fatura.id.toString())
                                    )
                                    withContext(Dispatchers.Main) {
                                        if (rowsDeleted > 0) {
                                            showToast("Fatura movida para a lixeira!")
                                            viewModel.carregarFaturas() // Pede ao ViewModel para recarregar
                                        } else {
                                            showToast("Erro ao mover fatura para a lixeira.")
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        showToast("Erro ao mover fatura para a lixeira.")
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showToast("Fatura não encontrada.")
                                }
                            }
                        } ?: withContext(Dispatchers.Main) {
                            showToast("Erro ao consultar fatura para exclusão.")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("MainActivity", "Erro ao mover fatura para a lixeira: ${e.message}", e)
                            showToast("Erro ao mover fatura: ${e.message}")
                        }
                    }
                }
            }
        )
        binding.recyclerViewFaturas.adapter = faturaAdapter

        // Adiciona a decoração (espaçamento)
        if (binding.recyclerViewFaturas.itemDecorationCount > 0) {
            for (i in (binding.recyclerViewFaturas.itemDecorationCount - 1) downTo 0) {
                binding.recyclerViewFaturas.getItemDecorationAt(i)?.let {
                    binding.recyclerViewFaturas.removeItemDecoration(it)
                }
            }
        }
        val spaceInDp = 4f
        val spaceInPixels = (spaceInDp * resources.displayMetrics.density).toInt()
        binding.recyclerViewFaturas.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))


        // ### OBSERVA O VIEWMODEL ###
        viewModel.faturas.observe(this) { faturas ->
            // Atualiza o adapter sempre que a lista no ViewModel mudar
            faturaAdapter.updateFaturas(faturas)
            Log.d("MainActivity", "Adapter atualizado com dados do ViewModel. Total: ${faturas.size}")
        }

        val menuOptionsAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.menu_options,
            R.layout.item_menu
        )
        binding.menuGridView.adapter = menuOptionsAdapter

        binding.menuGridView.setOnItemClickListener { _, _, position, _ ->
            try {
                val selectedOption = menuOptionsAdapter.getItem(position).toString()
                when (selectedOption) {
                    "Fatura" -> toggleGridView()
                    "Cliente" -> {
                        startActivity(Intent(this, ListarClientesActivity::class.java))
                        toggleGridView()
                    }
                    "Artigo" -> {
                        startActivity(Intent(this, ListarArtigosActivity::class.java))
                        toggleGridView()
                    }
                    "Lixeira" -> {
                        startActivityForResult(Intent(this, LixeiraActivity::class.java), LIXEIRA_REQUEST_CODE)
                        toggleGridView()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao abrir atividade: ${e.message}")
                showToast("Erro ao abrir a tela: ${e.message}")
            }
        }

        binding.faturaTitleContainer.setOnClickListener {
            toggleGridView()
        }

        binding.dollarIcon.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.CODE_128)
                setPrompt("Escaneie o código de barras no PDF")
                setCameraId(0)
                setBeepEnabled(false)
                setOrientationLocked(false)
            }
            barcodeLauncher.launch(options)
        }

        binding.homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.moreIcon.setOnClickListener {
            val intent = Intent(this, DefinicoesActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.searchButton.setOnClickListener {
            Log.d("MainActivity", "Botão de busca clicado")
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.search_dialog_title))

            val input = EditText(this)
            input.hint = getString(R.string.search_dialog_hint)
            builder.setView(input)

            builder.setPositiveButton(getString(R.string.search_dialog_positive_button)) { dialog, _ ->
                val query = input.text.toString().trim()
                Log.d("MainActivity", "Botão 'Pesquisar' clicado no diálogo, termo: '$query'")
                if (query.isEmpty()) {
                    showToast(getString(R.string.search_empty_query_message))
                    viewModel.carregarFaturas()
                    isSearchActive = false
                } else {
                    buscarFaturas(query)
                    isSearchActive = true
                }
                dialog.dismiss()
            }
            builder.setNegativeButton(getString(R.string.search_dialog_negative_button)) { dialog, _ ->
                Log.d("MainActivity", "Botão 'Cancelar' clicado no diálogo")
                dialog.cancel()
            }
            builder.show()
        }

        binding.graficosButton.setOnClickListener {
            Log.d("MainActivity", "Botão de Gráficos clicado")
            val intent = Intent(this, ResumoFinanceiroActivity::class.java)
            startActivity(intent)
        }

        logDatabaseContents()

        binding.addButton.setOnClickListener {
            requestStorageAndCameraPermissions()
        }
    }

    private fun emitBeep() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                    try {
                        player.prepare()
                    } catch (e: IllegalStateException){
                        Log.e("MainActivity", "Erro ao preparar MediaPlayer após stop: ${e.message}")
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer.create(this, R.raw.beep)
                    }
                }
                player.start()
            } ?: run {
                mediaPlayer = MediaPlayer.create(this, R.raw.beep)
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao reproduzir som de beep: ${e.message}")
        }
    }

    private fun openInvoiceByBarcode(barcodeValue: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cleanedBarcodeValue = barcodeValue.trim()
                val faturaIdFromBarcode = cleanedBarcodeValue.toLongOrNull() ?: cleanedBarcodeValue.replace("[^0-9]".toRegex(), "").toLongOrNull()

                if (faturaIdFromBarcode == null) {
                    withContext(Dispatchers.Main) {
                        showToast("Código de barras inválido: $cleanedBarcodeValue")
                    }
                    return@launch
                }
                abrirFaturaPorId(faturaIdFromBarcode, cleanedBarcodeValue)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Erro ao abrir fatura por código de barras: ${e.message}")
                    showToast("Erro ao abrir fatura: ${e.message}")
                }
            }
        }
    }

    private fun abrirFaturaPorId(faturaId: Long, barcodeScaneado: String) {
        val db = dbHelper?.readableDatabase
        if (db == null) {
            lifecycleScope.launch(Dispatchers.Main) {
                showToast(getString(R.string.database_error_message))
            }
            return
        }
        val cursor = db.query(
            FaturaContract.FaturaEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID, FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA),
            "${BaseColumns._ID} = ?",
            arrayOf(faturaId.toString()),
            null, null, null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val foiEnviada = cursor.getInt(cursor.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1
            cursor.close()
            Log.d("MainActivity", "Fatura encontrada com ID: $faturaId. Foi enviada: $foiEnviada")
            lifecycleScope.launch(Dispatchers.Main) {
                val intent = Intent(this@MainActivity, SecondScreenActivity::class.java).apply {
                    putExtra("fatura_id", faturaId)
                    putExtra("foi_enviada", foiEnviada)
                }
                startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
            }
        } else {
            cursor?.close()
            lifecycleScope.launch(Dispatchers.Main) {
                Log.w("MainActivity", "Fatura não encontrada com ID: $faturaId (código de barras: $barcodeScaneado)")
                showToast("Fatura não encontrada para o código de barras: $barcodeScaneado")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume chamado")
        if (!isSearchActive) {
            viewModel.carregarFaturas()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart chamado")
    }

    override fun onBackPressed() {
        if (isSearchActive) {
            Log.d("MainActivity", "Botão Voltar pressionado, busca estava ativa. Restaurando lista completa de faturas.")
            viewModel.carregarFaturas()
            isSearchActive = false
        } else if (isGridViewVisible) {
            toggleGridView()
        } else {
            super.onBackPressed()
        }
    }

    private fun buscarFaturas(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val tempFaturasList = mutableListOf<FaturaResumidaItem>()
            try {
                val db = dbHelper?.readableDatabase
                if (db == null) {
                    withContext(Dispatchers.Main) {
                        Log.e("MainActivity", "Banco de dados não acessível para busca.")
                        showToast(getString(R.string.database_error_message))
                    }
                    return@launch
                }

                Log.d("MainActivity", "Iniciando busca de faturas com o termo: '$query'")

                val sqlQuery = """
                    SELECT DISTINCT 
                        f.${BaseColumns._ID},
                        f.${FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA},
                        f.${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE},
                        f.${FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS},
                        f.${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR},
                        f.${FaturaContract.FaturaEntry.COLUMN_NAME_DATA},
                        f.${FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA}
                    FROM ${FaturaContract.FaturaEntry.TABLE_NAME} AS f
                    LEFT JOIN ${ClienteContract.ClienteEntry.TABLE_NAME} AS c
                        ON f.${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE} = c.${ClienteContract.ClienteEntry.COLUMN_NAME_NOME}
                    WHERE f.${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE} LIKE ?
                        OR c.${ClienteContract.ClienteEntry.COLUMN_NAME_CPF} LIKE ?
                        OR c.${ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ} LIKE ?
                        OR c.${ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE} LIKE ?
                        OR f.${FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA} LIKE ?
                        OR f.${FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS} LIKE ?
                    ORDER BY f.${BaseColumns._ID} DESC
                """
                val selectionArgs = arrayOf(
                    "%$query%",
                    "%$query%",
                    "%$query%",
                    "%$query%",
                    "%$query%",
                    "%$query%"
                )

                val cursor = db.rawQuery(sqlQuery, selectionArgs)
                cursor?.use {
                    Log.d("MainActivity", "Busca retornou ${it.count} registros.")
                    while (it.moveToNext()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
                        val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA)) ?: ""
                        val cliente = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)) ?: ""
                        val artigosString = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS)) ?: ""
                        val saldoDevedor = it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR))
                        val dataFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA)) ?: ""
                        val foiEnviada = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1

                        val serialNumbers = mutableListOf<String?>()
                        artigosString.split("|").forEach { artigo ->
                            val parts = artigo.split(",")
                            if (parts.size >= 5) {
                                val serial = parts[4].takeIf { s -> s.isNotEmpty() && s != "null" }
                                serialNumbers.add(serial)
                            }
                        }

                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val formattedData = try {
                            val date = inputFormat.parse(dataFatura)
                            outputFormat.format(date!!)
                        } catch (e: Exception) {
                            dataFatura
                        }

                        tempFaturasList.add(
                            FaturaResumidaItem(
                                id,
                                numeroFatura,
                                cliente,
                                serialNumbers,
                                saldoDevedor,
                                formattedData,
                                foiEnviada
                            )
                        )
                        Log.d("MainActivity", "Fatura encontrada: #$numeroFatura, Cliente: $cliente")
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        Log.e("MainActivity", "Cursor nulo ao buscar faturas com query: $query")
                        showToast("Erro: busca retornou resultado inválido.")
                    }
                }

                withContext(Dispatchers.Main) {
                    faturaAdapter.updateFaturas(tempFaturasList)
                    Log.d("MainActivity", "Lista atualizada com ${tempFaturasList.size} faturas após busca.")
                    if (tempFaturasList.isEmpty()) {
                        showToast("Nenhuma fatura encontrada para '$query'.")
                    } else {
                        showToast("${tempFaturasList.size} fatura(s) encontrada(s) para '$query'.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Erro ao buscar faturas: ${e.message}", e)
                    showToast("Erro ao buscar faturas: ${e.message}")
                    viewModel.carregarFaturas()
                    isSearchActive = false
                }
            }
        }
    }

    private fun logDatabaseContents() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = dbHelper?.readableDatabase
            if (db == null) {
                Log.e("MainActivity", "Banco de dados não está acessível para log.")
                return@launch
            }
            Log.d("DB_CONTENT_FATURAS", "--- Conteúdo da Tabela Faturas ---")
            db.rawQuery("SELECT * FROM ${FaturaContract.FaturaEntry.TABLE_NAME}", null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
                    val numFatura = cursor.getString(cursor.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA))
                    val cliente = cursor.getString(cursor.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE))
                    val saldo = cursor.getDouble(cursor.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR))
                    val data = cursor.getString(cursor.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA))
                    val enviada = cursor.getInt(cursor.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA))
                    Log.d("DB_CONTENT_FATURAS", "ID: $id, Num: $numFatura, Cliente: $cliente, Saldo: $saldo, Data: $data, Enviada: $enviada")
                }
            }
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkCameraPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStorageAndCameraPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!checkCameraPermission()) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (!checkStoragePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), STORAGE_PERMISSION_CODE)
        } else {
            openSecondScreen()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            var allEssentialPermissionsGranted = true
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]

                if (permission == Manifest.permission.CAMERA ||
                    permission == Manifest.permission.READ_MEDIA_IMAGES ||
                    permission == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allEssentialPermissionsGranted = false
                    }
                }
            }

            if (allEssentialPermissionsGranted) {
                showToast("Permissões concedidas!")
                openSecondScreen()
            } else {
                showToast("Algumas permissões essenciais foram negadas. Funcionalidades podem ser limitadas.")
                if (permissions.any { p ->
                        (p == Manifest.permission.CAMERA || p == Manifest.permission.READ_MEDIA_IMAGES || p == Manifest.permission.READ_EXTERNAL_STORAGE) &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(this, p) &&
                                ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED
                    }) {
                    AlertDialog.Builder(this)
                        .setTitle("Permissões Necessárias")
                        .setMessage("Este aplicativo precisa de permissões de câmera e armazenamento para funcionar corretamente. Por favor, habilite-as nas configurações do aplicativo.")
                        .setPositiveButton("Configurações") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else if (permissions.any { p ->
                        (p == Manifest.permission.CAMERA || p == Manifest.permission.READ_MEDIA_IMAGES || p == Manifest.permission.READ_EXTERNAL_STORAGE) &&
                                ActivityCompat.shouldShowRequestPermissionRationale(this, p)
                    }) {
                    AlertDialog.Builder(this)
                        .setTitle("Permissões Requeridas")
                        .setMessage("As permissões de câmera e armazenamento são necessárias para adicionar fotos e salvar faturas. Por favor, conceda as permissões.")
                        .setPositiveButton("OK") { _, _ ->
                            requestStorageAndCameraPermissions()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
    }

    private fun openSecondScreen() {
        val intent = Intent(this, SecondScreenActivity::class.java)
        startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
    }

    private fun toggleGridView() {
        if (isGridViewVisible) {
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            slideUp.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    binding.menuGridView.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            binding.menuGridView.startAnimation(slideUp)
        } else {
            binding.menuGridView.visibility = View.VISIBLE
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            binding.menuGridView.startAnimation(slideDown)
        }
        isGridViewVisible = !isGridViewVisible
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SECOND_SCREEN_REQUEST_CODE && resultCode == RESULT_OK) {
            viewModel.carregarFaturas()
            isSearchActive = false
        } else if (requestCode == LIXEIRA_REQUEST_CODE && resultCode == RESULT_OK) {
            val faturaRestaurada = data?.getBooleanExtra("fatura_restaurada", false) ?: false
            val restoredFaturaId = data?.getLongExtra("fatura_id", -1L) ?: -1L

            if (faturaRestaurada && restoredFaturaId != -1L) {
                viewModel.carregarFaturas()
                isSearchActive = false
                val intent = Intent(this, SecondScreenActivity::class.java).apply {
                    putExtra("fatura_id", restoredFaturaId)
                }
                startActivityForResult(intent, SECOND_SCREEN_REQUEST_CODE)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        dbHelper?.close()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
        Log.d("MainActivity", "onDestroy chamado")
    }
}