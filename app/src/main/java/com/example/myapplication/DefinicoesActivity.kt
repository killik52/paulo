package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.content.ContentResolver
import android.provider.OpenableColumns
import android.widget.ImageView


class DefinicoesActivity : AppCompatActivity() {

    private var dbHelper: ClienteDbHelper? = null
    private var pendingAction: String? = null

    // Launcher para importação de banco de dados (ZIP)
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            if (pendingAction == "import_db") {
                importDatabase(it)
            }
        }
        pendingAction = null
    }

    // Launcher para seleção de diretório para exportação de banco de dados
    private val exportDirLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            if (pendingAction == "export_db") {
                exportDatabase(it)
            }
        }
        pendingAction = null
    }

    // Launcher para seleção de arquivo CSV
    private val pickCsvLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (pendingAction == "import_csv") {
                confirmarImportacaoClientesCsv(it)
            }
        }
        pendingAction = null
    }


    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        var allGranted = true
        permissions.forEach { _, isGranted ->
            if (!isGranted) {
                allGranted = false
                return@forEach
            }
        }

        if (allGranted) {
            showToast("Permissões concedidas.")
            when (pendingAction) {
                "export_db" -> exportDirLauncher.launch(null)
                "import_db" -> importFileLauncher.launch(arrayOf("application/zip"))
                "import_csv" -> pickCsvLauncher.launch("text/comma-separated-values")
            }

        } else {
            showToast("Permissões de armazenamento são necessárias.")
        }
        pendingAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_definicoes)
            Log.d("DefinicoesActivity", "Layout activity_definicoes carregado")
        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro ao carregar layout: ${e.message}", e)
            showToast("Erro ao carregar tela.")
            finish()
            return
        }

        try {
            dbHelper = ClienteDbHelper(this)
            Log.d("DefinicoesActivity", "ClienteDbHelper inicializado")
        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro ao inicializar banco: ${e.message}", e)
            showToast("Erro no banco de dados.")
            finish()
            return
        }

        try {
            val backButton: ImageView = findViewById(R.id.backButton)
            backButton.setOnClickListener {
                Log.d("DefinicoesActivity", "Botão de voltar clicado")
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }

            val logotipoRow: LinearLayout = findViewById(R.id.logotipoRow)
            logotipoRow.setOnClickListener {
                Log.d("DefinicoesActivity", "Ícone de Logotipo clicado")
                val intent = Intent(this, LogotipoActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            val informacaoEmpresaRow: LinearLayout = findViewById(R.id.informacaoEmpresaRow)
            informacaoEmpresaRow.setOnClickListener {
                Log.d("DefinicoesActivity", "Ícone de Informações da Empresa clicado")
                val intent = Intent(this, InformacoesEmpresaActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            val instrucoesPagamentoRow: LinearLayout = findViewById(R.id.instrucoesPagamentoRow)
            instrucoesPagamentoRow.setOnClickListener {
                Log.d("DefinicoesActivity", "Ícone de Instruções de Pagamento clicado")
                val intent = Intent(this, InstrucoesPagamentoActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            val notaPadraoRow: LinearLayout = findViewById(R.id.notaPadraoRow)
            notaPadraoRow.setOnClickListener {
                Log.d("DefinicoesActivity", "Ícone de Nota Padrão clicado")
                val intent = Intent(this, NotasActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            val listaNegraRow: LinearLayout = findViewById(R.id.listaNegraRow)
            listaNegraRow.setOnClickListener {
                Log.d("DefinicoesActivity", "Lista Negra row clicado")
                openClientesBloqueados()
            }

            val exportButtonLayout: LinearLayout = findViewById(R.id.exportButtonLayout)
            val importButtonLayout: LinearLayout = findViewById(R.id.importButtonLayout)
            val importClientesCsvLayout: LinearLayout = findViewById(R.id.importClientesCsvLayout)

            exportButtonLayout.setOnClickListener {
                Log.d("DefinicoesActivity", "Layout Exportar Banco clicado")
                pendingAction = "export_db"
                if (checkStoragePermissions()) {
                    exportDirLauncher.launch(null)
                } else {
                    requestStoragePermissions()
                }
            }

            importButtonLayout.setOnClickListener {
                Log.d("DefinicoesActivity", "Layout Importar Banco clicado")
                pendingAction = "import_db"
                if (checkStoragePermissions()) {
                    importFileLauncher.launch(arrayOf("application/zip"))
                } else {
                    requestStoragePermissions()
                }
            }

            // Listener para o botão de importar CSV
            importClientesCsvLayout.setOnClickListener {
                Log.d("DefinicoesActivity", "Layout Importar Clientes (CSV) clicado")
                pendingAction = "import_csv"
                if (checkStoragePermissions()) {
                    pickCsvLauncher.launch("text/comma-separated-values")
                } else {
                    requestStoragePermissions()
                }
            }

        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro ao inicializar componentes: ${e.message}", e)
            showToast("Erro ao carregar componentes.")
            finish()
        }
    }

    /**
     * Analisa uma linha de CSV que lida corretamente com campos entre aspas.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        val delimiter = if (line.contains(';')) ';' else ','

        var i = 0
        while (i < line.length) {
            val char = line[i]

            when {
                inQuotes -> {
                    if (char == '"') {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            currentField.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        currentField.append(char)
                    }
                }
                else -> {
                    when (char) {
                        '"' -> inQuotes = true
                        delimiter -> {
                            result.add(currentField.toString())
                            currentField.clear()
                        }
                        else -> currentField.append(char)
                    }
                }
            }
            i++
        }
        result.add(currentField.toString())
        return result
    }

    private fun confirmarImportacaoClientesCsv(uri: Uri) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Importar Clientes de CSV")
            .setMessage("Isso adicionará os clientes do arquivo selecionado. Clientes com CPF ou CNPJ já existentes serão atualizados. Deseja continuar?")
            .setPositiveButton("Importar") { _, _ ->
                importarClientesDeCsv(uri)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun importarClientesDeCsv(uri: Uri) {
        val db = dbHelper?.writableDatabase ?: run {
            showToast("Erro: Banco de dados não acessível.")
            return
        }

        var linhasProcessadas = 0
        var clientesAdicionados = 0
        var clientesAtualizados = 0

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    db.beginTransaction()

                    reader.readLine() // Pula a linha do cabeçalho

                    var linha: String?
                    while (reader.readLine().also { linha = it } != null) {
                        if (linha.isNullOrBlank()) continue

                        val colunas = parseCsvLine(linha!!)

                        // Colunas esperadas: nome;email;telefone;numero serial;endereço;cpf;cnpj
                        if (colunas.size < 7) {
                            Log.w("DefinicoesActivity", "Linha ignorada (mal formatada, colunas < 7): $linha")
                            continue
                        }

                        val nome = colunas.getOrElse(0) { "" }.trim().removeSurrounding("\"")
                        val email = colunas.getOrElse(1) { "" }.trim().removeSurrounding("\"")
                        val telefone = colunas.getOrElse(2) { "" }.trim().removeSurrounding("\"")
                        val numeroSerial = colunas.getOrElse(3) { "" }.trim().removeSurrounding("\"")
                        val endereco = colunas.getOrElse(4) { "" }.trim().removeSurrounding("\"")
                        val cpf = colunas.getOrElse(5) { "" }.trim().removeSurrounding("\"").replace(Regex("[^0-9]"), "")
                        val cnpj = colunas.getOrElse(6) { "" }.trim().removeSurrounding("\"").replace(Regex("[^0-9]"), "")

                        if (nome.isEmpty()) continue

                        // **LÓGICA DE SALVAMENTO CORRIGIDA**
                        val values = ContentValues().apply {
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_NOME, nome)
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL, email)
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE, telefone)
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_CPF, cpf)
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ, cnpj)
                            // Salva o número serial na sua própria coluna
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO_SERIAL, numeroSerial)
                            // Salva o endereço na coluna de logradouro (endereço principal)
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_LOGRADOURO, endereco)
                            // Limpa os outros campos para consistência
                            put(ClienteContract.ClienteEntry.COLUMN_NAME_INFORMACOES_ADICIONAIS, "")
                        }

                        var clienteExistenteId: Long = -1
                        val selectionParts = mutableListOf<String>()
                        val selectionArgs = mutableListOf<String>()

                        if (cpf.isNotEmpty()) {
                            selectionParts.add("${ClienteContract.ClienteEntry.COLUMN_NAME_CPF} = ?")
                            selectionArgs.add(cpf)
                        }
                        if (cnpj.isNotEmpty()) {
                            if (selectionParts.isNotEmpty()) selectionParts.add(" OR ")
                            selectionParts.add("${ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ} = ?")
                            selectionArgs.add(cnpj)
                        }

                        // Se não houver CPF ou CNPJ, procura pelo nome como último recurso
                        if (selectionParts.isEmpty() && nome.isNotEmpty()) {
                            selectionParts.add("${ClienteContract.ClienteEntry.COLUMN_NAME_NOME} = ?")
                            selectionArgs.add(nome)
                        }


                        if (selectionParts.isNotEmpty()) {
                            val selection = selectionParts.joinToString("")
                            db.query(
                                ClienteContract.ClienteEntry.TABLE_NAME,
                                arrayOf(BaseColumns._ID),
                                selection,
                                selectionArgs.toTypedArray(),
                                null, null, null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    clienteExistenteId = cursor.getLong(0)
                                }
                            }
                        }

                        if (clienteExistenteId != -1L) {
                            val count = db.update(
                                ClienteContract.ClienteEntry.TABLE_NAME,
                                values,
                                "${BaseColumns._ID} = ?",
                                arrayOf(clienteExistenteId.toString())
                            )
                            if (count > 0) clientesAtualizados++
                        } else {
                            val newId = db.insert(ClienteContract.ClienteEntry.TABLE_NAME, null, values)
                            if (newId != -1L) clientesAdicionados++
                        }
                        linhasProcessadas++
                    }
                    db.setTransactionSuccessful()
                }
            }
            showToast("Importação concluída! $clientesAdicionados clientes adicionados, $clientesAtualizados atualizados.")
        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro ao importar CSV: ${e.message}", e)
            showToast("Erro ao ler o arquivo CSV. Verifique o formato do arquivo.")
        } finally {
            db.endTransaction()
        }
    }


    private fun openClientesBloqueados() {
        try {
            val intent = Intent(this, ClientesBloqueadosActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            Log.d("DefinicoesActivity", "Iniciando ClientesBloqueadosActivity")
        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro ao abrir ClientesBloqueadosActivity: ${e.message}", e)
            showToast("Erro ao abrir Lista Negra.")
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestStoragePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("DefinicoesActivity", "API >= 33, permissões de armazenamento via SAF picker para a ação pendente.")
            when (pendingAction) {
                "export_db" -> exportDirLauncher.launch(null)
                "import_db" -> importFileLauncher.launch(arrayOf("application/zip"))
                "import_csv" -> pickCsvLauncher.launch("text/comma-separated-values")
            }
            return
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            when (pendingAction) {
                "export_db" -> exportDirLauncher.launch(null)
                "import_db" -> importFileLauncher.launch(arrayOf("application/zip"))
                "import_csv" -> pickCsvLauncher.launch("text/comma-separated-values")
            }
            pendingAction = null
        }
    }
    private fun getRotationFromExif(imagePath: String): Int {
        return try {
            val exif = ExifInterface(imagePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            Log.e("DefinicoesActivity", "Erro ao ler dados EXIF de $imagePath para backup: ${e.message}", e)
            0
        }
    }

    private fun compressImageForBackup(inputFile: File, quality: Int = 70, targetHeight: Int = 1024): File? {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(inputFile.absolutePath, options)

            var inSampleSize = 1
            if (options.outHeight > targetHeight || options.outWidth > (targetHeight * (options.outWidth.toFloat() / options.outHeight.toFloat()))) {
                val halfHeight: Int = options.outHeight / 2
                val halfWidth: Int = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= (targetHeight * (options.outWidth.toFloat() / options.outHeight.toFloat()))) {
                    inSampleSize *= 2
                }
            }
            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false

            var initialBitmap = BitmapFactory.decodeFile(inputFile.absolutePath, options)
                ?: run {
                    Log.e("DefinicoesActivity", "Falha ao decodificar bitmap para compressão de backup: ${inputFile.absolutePath}")
                    return null
                }

            val rotationAngle = getRotationFromExif(inputFile.absolutePath)
            val finalBitmapToCompress: Bitmap
            if (rotationAngle != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationAngle.toFloat())
                finalBitmapToCompress = Bitmap.createBitmap(initialBitmap, 0, 0, initialBitmap.width, initialBitmap.height, matrix, true)
                if (finalBitmapToCompress != initialBitmap && !initialBitmap.isRecycled) {
                    initialBitmap.recycle()
                }
                Log.d("DefinicoesActivity", "Bitmap para backup rotacionado em $rotationAngle graus.")
            } else {
                finalBitmapToCompress = initialBitmap
            }

            val tempCompressedFile = File.createTempFile("bkp_comp_oriented_${inputFile.nameWithoutExtension}_", ".jpg", cacheDir)
            FileOutputStream(tempCompressedFile).use { fos ->
                finalBitmapToCompress.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
            if (!finalBitmapToCompress.isRecycled) {
                finalBitmapToCompress.recycle()
            }
            Log.d("DefinicoesActivity", "Imagem comprimida e orientada para backup: ${inputFile.name} -> ${tempCompressedFile.name}, Original: ${inputFile.length()} bytes, Comprimida: ${tempCompressedFile.length()} bytes")
            tempCompressedFile
        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro ao comprimir imagem para backup ${inputFile.name}: ${e.message}", e)
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName: String? = null
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            fileName = cursor.getString(displayNameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("DefinicoesActivity", "Erro ao obter DISPLAY_NAME do URI: $uri - ${e.message}", e)
            }
        }
        if (fileName == null) {
            fileName = uri.lastPathSegment
        }
        return fileName?.replace(Regex("[^a-zA-Z0-9._-]"), "_")?.takeIf { it.isNotBlank() } ?: "logotipo_backup.jpg"
    }

    private fun exportDatabase(directoryUri: Uri) {
        var tempZipFile: File? = null
        val tempFilesToDeleteAfterZip = mutableListOf<File>()

        Log.d("DefinicoesActivity", "Iniciando exportação para o diretório URI: $directoryUri")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !DocumentsContract.isTreeUri(directoryUri)) {
                showToast("Diretório de destino inválido.")
                Log.e("DefinicoesActivity", "URI de exportação inválido (não é um tree URI): $directoryUri")
                return
            }

            val dbFile = getDatabasePath(ClienteDbHelper.DATABASE_NAME)
            if (!dbFile.exists()) {
                showToast("Arquivo do banco de dados não encontrado.")
                Log.e("DefinicoesActivity", "Arquivo do banco de dados não existe em: ${dbFile.absolutePath}")
                return
            }

            val photoPaths = mutableListOf<String>()
            dbHelper?.readableDatabase?.query(
                FaturaContract.FaturaFotoEntry.TABLE_NAME,
                arrayOf(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH),
                null, null, null, null, null
            )?.use { cursor ->
                val pathIndex = cursor.getColumnIndex(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH)
                if (pathIndex != -1) {
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(pathIndex)
                        if (path != null && File(path).exists()) photoPaths.add(path)
                        else Log.w("DefinicoesActivity", "Caminho da foto inválido ou não existe: $path")
                    }
                }
            }
            Log.d("DefinicoesActivity", "${photoPaths.size} fotos para backup.")

            val logoPrefs = getSharedPreferences("LogotipoPrefs", MODE_PRIVATE)
            val backupFileName = "myapplication_backup_${System.currentTimeMillis()}.zip"
            tempZipFile = File.createTempFile("backup_export_temp_", ".zip", cacheDir)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile))).use { zipOut ->
                zipOut.setLevel(Deflater.BEST_COMPRESSION)

                val dbEntry = ZipEntry("myapplication.db")
                zipOut.putNextEntry(dbEntry)
                FileInputStream(dbFile).use { it.copyTo(zipOut) }
                zipOut.closeEntry()
                Log.i("DefinicoesActivity", "Banco de dados adicionado ao ZIP.")

                photoPaths.forEach { photoPath ->
                    val photoFile = File(photoPath)
                    if (photoFile.exists()) {
                        val compressedPhotoForBackup = compressImageForBackup(photoFile, quality = 60, targetHeight = 800)
                        val fileToAdd = compressedPhotoForBackup ?: photoFile

                        val originalFileNameFromDb = photoFile.name
                        val entryName = "photos/$originalFileNameFromDb"

                        zipOut.putNextEntry(ZipEntry(entryName))
                        FileInputStream(fileToAdd).use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                        Log.d("DefinicoesActivity", "Foto adicionada ao ZIP: $entryName (original: ${photoFile.name}, usado no zip: ${fileToAdd.name}, tamanho: ${fileToAdd.length()})")
                        compressedPhotoForBackup?.let { tempFilesToDeleteAfterZip.add(it) }
                    }
                }

                val sharedPrefsDir = File(applicationInfo.dataDir, "shared_prefs")
                val prefsToInclude = listOf(
                    "LogotipoPrefs.xml", "InformacoesEmpresaPrefs.xml",
                    "NotasPrefs.xml", "FaturaPrefs.xml", "InstrucoesPagamentoPrefs.xml"
                )
                prefsToInclude.forEach { prefName ->
                    val prefsFile = File(sharedPrefsDir, prefName)
                    if (prefsFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("shared_prefs/$prefName"))
                        FileInputStream(prefsFile).use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }

                val logoUriString = logoPrefs.getString("logo_uri", null)
                if (logoUriString != null) {
                    var tempLogoCopy: File? = null
                    var tempCompressedLogo: File? = null
                    try {
                        val logoUri = Uri.parse(logoUriString)
                        val originalLogoName = getFileNameFromUri(logoUri)
                        tempLogoCopy = File.createTempFile("temp_logo_orig_", ".dat", cacheDir)
                        contentResolver.openInputStream(logoUri)?.use { input ->
                            FileOutputStream(tempLogoCopy).use { output -> input.copyTo(output) }
                        }
                        if (tempLogoCopy.exists() && tempLogoCopy.length() > 0) {
                            tempCompressedLogo = compressImageForBackup(tempLogoCopy, quality = 70, targetHeight = 600)
                            val fileToAddForZip = tempCompressedLogo ?: tempLogoCopy
                            val logoEntryName = "logo/${originalLogoName.substringBeforeLast('.')}.jpg"
                            zipOut.putNextEntry(ZipEntry(logoEntryName))
                            FileInputStream(fileToAddForZip).use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                            tempCompressedLogo?.let { tempFilesToDeleteAfterZip.add(it) }
                        }
                    } catch (e: Exception) {
                        Log.e("DefinicoesActivity", "Erro ao processar logo para backup: ${e.message}", e)
                    } finally {
                        tempLogoCopy?.delete()
                    }
                }
            }

            val treeDocumentId = DocumentsContract.getTreeDocumentId(directoryUri) ?: throw IOException("ID do documento da árvore nulo.")
            val targetDirUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, treeDocumentId)
            val finalZipUri = DocumentsContract.createDocument(contentResolver, targetDirUri, "application/zip", backupFileName)
                ?: throw IOException("Falha ao criar documento ZIP no destino.")

            contentResolver.openOutputStream(finalZipUri)?.use { output ->
                FileInputStream(tempZipFile).use { input -> input.copyTo(output) }
            } ?: throw IOException("Falha ao abrir OutputStream para o ZIP final.")

            showToast("Backup exportado com sucesso: $backupFileName")
            Log.i("DefinicoesActivity", "Backup exportado para: $finalZipUri")

        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro crítico durante a exportação: ${e.message}", e)
            showToast("Erro ao exportar: ${e.message}")
        } finally {
            tempZipFile?.delete()
            tempFilesToDeleteAfterZip.forEach { it.delete() }
            Log.d("DefinicoesActivity", "Limpeza de arquivos temporários de exportação concluída.")
        }
    }


    private fun importDatabase(uri: Uri) {
        var tempZipFile: File? = null
        val tempDir = File(cacheDir, "temp_extract_${System.currentTimeMillis()}")

        try {
            Log.i("DefinicoesActivity", "Iniciando importação do URI: $uri")

            val dbFile = getDatabasePath(ClienteDbHelper.DATABASE_NAME)
            val restoredPhotosAppDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "restored_photos_from_backup")


            if (restoredPhotosAppDir.exists()) restoredPhotosAppDir.deleteRecursively()
            restoredPhotosAppDir.mkdirs()
            Log.d("DefinicoesActivity", "Diretório de fotos restauradas do backup criado/limpo: ${restoredPhotosAppDir.absolutePath}")

            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()
            Log.d("DefinicoesActivity", "Diretório temporário de extração criado: ${tempDir.absolutePath}")

            tempZipFile = File.createTempFile("backup_import_temp_", ".zip", cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZipFile).use { output -> input.copyTo(output) }
            } ?: throw IOException("Falha ao abrir stream do arquivo ZIP de importação.")

            ZipInputStream(BufferedInputStream(FileInputStream(tempZipFile))).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val outputFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { it.write(zipIn.readBytes()) }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            Log.i("DefinicoesActivity", "Extração do ZIP concluída.")

            val tempDbFile = File(tempDir, "myapplication.db")
            if (tempDbFile.exists()) {
                dbHelper?.close()
                dbHelper = null
                FileInputStream(tempDbFile).use { input ->
                    FileOutputStream(dbFile, false).use { output -> input.copyTo(output) }
                }
                dbHelper = ClienteDbHelper(this)
                Log.i("DefinicoesActivity", "Banco de dados restaurado.")
            } else {
                throw FileNotFoundException("Arquivo myapplication.db não encontrado no backup.")
            }

            val sharedPrefsDirInApp = File(applicationInfo.dataDir, "shared_prefs")
            if (!sharedPrefsDirInApp.exists()) sharedPrefsDirInApp.mkdirs()
            val prefsDirInZip = File(tempDir, "shared_prefs")
            if (prefsDirInZip.exists() && prefsDirInZip.isDirectory) {
                listOf(
                    "LogotipoPrefs.xml", "InformacoesEmpresaPrefs.xml",
                    "NotasPrefs.xml", "FaturaPrefs.xml", "InstrucoesPagamentoPrefs.xml"
                ).forEach { prefName ->
                    val sourceFile = File(prefsDirInZip, prefName)
                    if (sourceFile.exists()) {
                        sourceFile.copyTo(File(sharedPrefsDirInApp, prefName), overwrite = true)
                        Log.i("DefinicoesActivity", "SharedPreferences '$prefName' restaurado.")
                    }
                }
            }

            val logoDirInZip = File(tempDir, "logo")
            val appRestoredLogoDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "app_logo_from_backup")
            if (!appRestoredLogoDir.exists()) appRestoredLogoDir.mkdirs()

            if (logoDirInZip.exists() && logoDirInZip.isDirectory) {
                logoDirInZip.listFiles()?.firstOrNull()?.let { logoFileInZip ->
                    val newLogoFile = File(appRestoredLogoDir, logoFileInZip.name)
                    logoFileInZip.copyTo(newLogoFile, true)
                    getSharedPreferences("LogotipoPrefs", MODE_PRIVATE).edit()
                        .putString("logo_uri", Uri.fromFile(newLogoFile).toString()).apply()
                    Log.i("DefinicoesActivity", "Logo '${newLogoFile.name}' restaurado para ${newLogoFile.absolutePath}.")
                }
            }

            val photosFolderInZip = File(tempDir, "photos")
            if (photosFolderInZip.exists() && photosFolderInZip.isDirectory) {
                val dbWrite = dbHelper?.writableDatabase ?: throw SQLiteException("DB não acessível para restaurar fotos.")
                photosFolderInZip.listFiles()?.forEach { photoFileInZip ->
                    val newRestoredPhotoFile = File(restoredPhotosAppDir, photoFileInZip.name)
                    try {
                        photoFileInZip.copyTo(newRestoredPhotoFile, true)

                        val values = ContentValues().apply {
                            put(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH, newRestoredPhotoFile.absolutePath)
                        }

                        val rowsUpdated = dbWrite.update(
                            FaturaContract.FaturaFotoEntry.TABLE_NAME, values,
                            "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH} LIKE ?",
                            arrayOf("%/${photoFileInZip.name}")
                        )
                        if (rowsUpdated > 0) {
                            Log.i("DefinicoesActivity", "Foto '${photoFileInZip.name}' restaurada para '${newRestoredPhotoFile.absolutePath}'. $rowsUpdated registros no DB atualizados.")
                        } else {
                            Log.w("DefinicoesActivity", "Nenhum registro no DB atualizado para a foto '${photoFileInZip.name}'. Caminho antigo no DB pode não terminar com '/${photoFileInZip.name}'. Novo caminho seria: ${newRestoredPhotoFile.absolutePath}")
                            dbHelper?.readableDatabase?.query(
                                FaturaContract.FaturaFotoEntry.TABLE_NAME,
                                arrayOf(BaseColumns._ID, FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH),
                                "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH} LIKE ?",
                                arrayOf("%${photoFileInZip.name}%"),
                                null, null, null
                            )?.use { debugCursor ->
                                if (debugCursor.moveToFirst()) {
                                    do {
                                        val id = debugCursor.getLong(debugCursor.getColumnIndexOrThrow(BaseColumns._ID))
                                        val path = debugCursor.getString(debugCursor.getColumnIndexOrThrow(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH))
                                        Log.w("DefinicoesActivity", "DEBUG IMPORT: Encontrado no DB (ID: $id): Path='$path' para foto do zip '${photoFileInZip.name}'")
                                    } while (debugCursor.moveToNext())
                                } else {
                                    Log.w("DefinicoesActivity", "DEBUG IMPORT: Nenhum caminho no DB encontrado contendo '${photoFileInZip.name}'")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DefinicoesActivity", "Erro ao copiar ou atualizar foto '${photoFileInZip.name}' do backup: ${e.message}", e)
                    }
                }
            }


            showToast("Backup importado com sucesso! O aplicativo será reiniciado.")
            Log.i("DefinicoesActivity", "Importação concluída. Reiniciando.")
            val pm = packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
            Runtime.getRuntime().exit(0)

        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro crítico durante a importação: ${e.message}", e)
            showToast("Erro ao importar backup: ${e.message}")
        } finally {
            tempZipFile?.delete()
            tempDir.deleteRecursively()
            Log.d("DefinicoesActivity", "Limpeza de arquivos temporários de importação.")
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        try {
            dbHelper?.close()
            Log.d("DefinicoesActivity", "ClienteDbHelper fechado")
        } catch (e: Exception) {
            Log.e("DefinicoesActivity", "Erro ao fechar banco: ${e.message}", e)
        }
        super.onDestroy()
    }
}