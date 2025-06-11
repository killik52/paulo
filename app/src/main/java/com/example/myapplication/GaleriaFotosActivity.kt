package com.example.myapplication

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.myapplication.BuildConfig
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GaleriaFotosActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GaleriaFotosActivity"
        private const val COMPRESS_IMAGE_TAG = "CompressImage"
        private const val TAKE_PICTURE_CALLBACK_TAG = "TakePictureCallback"
        private const val HANDLE_GALLERY_TAG = "HandleGalleryImage"
        private const val KEY_CURRENT_PHOTO_PATH = "currentPhotoPath"
        private const val KEY_CURRENT_PHOTO_URI_STRING = "currentPhotoUriString"
    }

    private lateinit var fotosRecyclerView: RecyclerView
    private lateinit var fotoAdapter: FotoAdapter
    private val fotosList = mutableListOf<String>()
    private var faturaId: Long = -1
    private var dbHelper: ClienteDbHelper? = null

    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null

    private var pendingAction: (() -> Unit)? = null


    private val pickImageFromGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            Log.d(TAG, "Imagem selecionada da galeria: $imageUri")
            handleImageFromGallery(imageUri)
        } ?: run {
            Log.w(TAG, "Nenhuma imagem selecionada da galeria.")
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val logTag = TAKE_PICTURE_CALLBACK_TAG
        Log.d(logTag, "Retorno da câmera. Sucesso: $success")

        val photoPathToProcess = currentPhotoPath

        if (success && photoPathToProcess != null) {
            Log.d(logTag, "Foto capturada com sucesso. Caminho do arquivo temporário: $photoPathToProcess")

            try {
                val originalFile = File(photoPathToProcess)
                if (originalFile.exists() && originalFile.length() > 0) {
                    Log.d(logTag, "Arquivo original encontrado: ${originalFile.absolutePath}, Tamanho: ${originalFile.length()} bytes")

                    val compressedAndOrientedFile = compressImageFile(originalFile)
                    Log.d(logTag, "Arquivo comprimido e orientado: ${compressedAndOrientedFile.absolutePath}, Tamanho: ${compressedAndOrientedFile.length()} bytes")

                    if (compressedAndOrientedFile.exists() && compressedAndOrientedFile.length() > 0) {
                        // Adicionar à lista e notificar o adapter DENTRO do thread principal
                        // ActivityResultLauncher já retorna no thread principal, mas para garantir:
                        runOnUiThread {
                            fotosList.add(compressedAndOrientedFile.absolutePath)
                            val newPosition = fotosList.size - 1
                            fotoAdapter.notifyItemInserted(newPosition)
                            fotosRecyclerView.scrollToPosition(newPosition)
                            Log.i(logTag, "Foto ADICIONADA à lista UI e rolada para posição: $newPosition. Caminho: ${compressedAndOrientedFile.absolutePath}")
                        }

                        savePhotoToDatabase(faturaId, compressedAndOrientedFile.absolutePath) // Pode ser feito em background se demorar
                        Toast.makeText(this, "Foto salva!", Toast.LENGTH_SHORT).show() // Movido para após sucesso de processamento

                        if (originalFile.absolutePath != compressedAndOrientedFile.absolutePath && originalFile.exists()) {
                            if (originalFile.delete()) {
                                Log.d(logTag, "Arquivo original temporário deletado: ${originalFile.absolutePath}")
                            } else {
                                Log.w(logTag, "Falha ao deletar arquivo original temporário: ${originalFile.absolutePath}")
                            }
                        }
                    } else {
                        Log.e(logTag, "Arquivo comprimido NÃO EXISTE ou está VAZIO: ${compressedAndOrientedFile.absolutePath}")
                        Toast.makeText(this, "Erro: Falha ao processar a foto (arquivo final inválido).", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e(logTag, "Arquivo original da foto capturada NÃO EXISTE ou está VAZIO: $photoPathToProcess")
                    Toast.makeText(this, "Erro: Arquivo da foto não encontrado ou vazio após captura.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(logTag, "Erro ao comprimir, orientar ou salvar foto: ${e.message}", e)
                Toast.makeText(this, "Erro ao processar foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w(logTag, "Captura cancelada pelo usuário ou falhou. Sucesso: $success, Caminho: $photoPathToProcess")
            if (photoPathToProcess != null) {
                File(photoPathToProcess).delete()
                Log.d(logTag, "Arquivo temporário $photoPathToProcess deletado devido a falha/cancelamento.")
            }
            Toast.makeText(this, "Captura de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
        currentPhotoUri = null
        currentPhotoPath = null
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Permissão da câmera concedida via launcher.")
            pendingAction?.invoke()
        } else {
            Log.w(TAG, "Permissão da câmera negada via launcher.")
            Toast.makeText(this, "Permissão da câmera é necessária para tirar fotos.", Toast.LENGTH_LONG).show()
        }
        pendingAction = null
    }

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        var allGranted = true
        permissions.entries.forEach {
            if (!it.value) {
                allGranted = false
            }
        }
        if (allGranted) {
            Log.d(TAG, "Permissões de armazenamento concedidas via launcher.")
            pendingAction?.invoke()
        } else {
            Log.w(TAG, "Uma ou mais permissões de armazenamento foram negadas via launcher.")
            Toast.makeText(this, "Permissão de armazenamento é necessária para acessar ou adicionar fotos.", Toast.LENGTH_LONG).show()
        }
        pendingAction = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria_fotos)
        Log.d(TAG, "onCreate - Início")

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(KEY_CURRENT_PHOTO_PATH)
            savedInstanceState.getString(KEY_CURRENT_PHOTO_URI_STRING)?.let {
                currentPhotoUri = Uri.parse(it)
            }
            Log.i(TAG, "Estado restaurado de onSaveInstanceState: currentPhotoPath=$currentPhotoPath, currentPhotoUri=$currentPhotoUri")
        }


        try {
            dbHelper = ClienteDbHelper(this)
            Log.d(TAG, "ClienteDbHelper inicializado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar banco: ${e.message}", e)
            Toast.makeText(this, "Erro no banco.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        fotosRecyclerView = findViewById(R.id.fotosRecyclerView)
        val backButton: ImageButton = findViewById(R.id.backButtonGaleria)
        val grayCircleButton: ImageButton = findViewById(R.id.grayCircleButton)
        val pickFromGalleryButton: ImageButton = findViewById(R.id.pickFromGalleryButton)

        fotoAdapter = FotoAdapter(
            this,
            fotosList,
            onPhotoClick = { photoPath -> viewPhotoInExternalViewer(photoPath) },
            onPhotoLongClick = { position -> removePhoto(position) }
        )
        fotosRecyclerView.adapter = fotoAdapter
        fotosRecyclerView.layoutManager = GridLayoutManager(this, 3)

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.page_margin) / 4
        fotosRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.set(spacingInPixels, spacingInPixels, spacingInPixels, spacingInPixels)
            }
        })

        faturaId = intent.getLongExtra("fatura_id", -1)
        val photosFromIntent = intent.getStringArrayListExtra("photos")
        if (photosFromIntent != null && fotosList.isEmpty() && savedInstanceState == null) { // Apenas na criação inicial e se não restaurado
            fotosList.clear()
            fotosList.addAll(photosFromIntent)
            Log.d(TAG, "Fotos iniciais recebidas da intent: ${fotosList.size}")
        }
        Log.d(TAG, "fatura_id: $faturaId")

        if (faturaId != -1L && fotosList.isEmpty() && savedInstanceState == null) {
            pendingAction = { loadPhotosFromDatabase() }
            if (checkStoragePermission()) {
                pendingAction?.invoke()
                pendingAction = null
            } else {
                requestStoragePermission()
            }
        } else if (fotosList.isNotEmpty() && fotoAdapter.itemCount != fotosList.size) {
            // Se a lista foi preenchida pela intent e o adapter ainda não reflete isso
            fotoAdapter.notifyDataSetChanged()
        }


        backButton.setOnClickListener {
            val resultIntent = Intent().apply {
                putStringArrayListExtra("photos", ArrayList(fotosList))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
            Log.d(TAG, "Botão voltar pressionado, retornando ${fotosList.size} fotos.")
        }

        grayCircleButton.setOnClickListener {
            Log.d(TAG, "Botão 'Câmera' (grayCircleButton) clicado.")
            pendingAction = { dispatchTakePictureIntent() }
            if (checkCameraPermission()) {
                pendingAction?.invoke()
                pendingAction = null
            } else {
                requestCameraPermission()
            }
        }

        pickFromGalleryButton.setOnClickListener {
            Log.d(TAG, "Botão 'Escolher da Galeria' (pickFromGalleryButton) clicado.")
            pendingAction = { pickImageFromGalleryLauncher.launch("image/*") }
            if (checkStoragePermission()) {
                pendingAction?.invoke()
                pendingAction = null
            } else {
                requestStoragePermission()
            }
        }
        Log.d(TAG, "onCreate - Fim")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentPhotoPath?.let {
            outState.putString(KEY_CURRENT_PHOTO_PATH, it)
            Log.i(TAG, "Salvando estado em onSaveInstanceState: currentPhotoPath=$it")
        }
        currentPhotoUri?.let {
            outState.putString(KEY_CURRENT_PHOTO_URI_STRING, it.toString())
            Log.i(TAG, "Salvando estado em onSaveInstanceState: currentPhotoUriString=${it.toString()}")
        }
        // Salvar a lista de fotos atual para restaurar se a activity for recriada
        // e não vier da intent (ex: rotação durante a visualização da galeria)
        if (fotosList.isNotEmpty()) {
            outState.putStringArrayList("fotosListState", ArrayList(fotosList))
            Log.i(TAG, "Salvando estado de fotosList: ${fotosList.size} itens")
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restaurar currentPhotoPath e currentPhotoUri é feito no onCreate
        // Restaurar a lista de fotos se necessário
        savedInstanceState.getStringArrayList("fotosListState")?.let {
            if (fotosList.isEmpty()) { // Só restaura se a lista estiver vazia (para não duplicar com a intent)
                fotosList.addAll(it)
                fotoAdapter.notifyDataSetChanged()
                Log.i(TAG, "Restaurado fotosListState: ${fotosList.size} itens")
            }
        }
    }


    private fun handleImageFromGallery(imageUri: Uri) {
        val logTag = HANDLE_GALLERY_TAG
        try {
            val copiedFile = copyUriToAppFile(imageUri, "picked_image")
            if (copiedFile.exists() && copiedFile.length() > 0) {
                Log.d(logTag, "Imagem da galeria copiada para: ${copiedFile.absolutePath}, Tamanho: ${copiedFile.length()}")
                val compressedAndOrientedFile = compressImageFile(copiedFile)
                Log.d(logTag, "Imagem da galeria comprimida e orientada: ${compressedAndOrientedFile.absolutePath}, Tamanho: ${compressedAndOrientedFile.length()}")

                if (compressedAndOrientedFile.exists() && compressedAndOrientedFile.length() > 0) {
                    runOnUiThread {
                        fotosList.add(compressedAndOrientedFile.absolutePath)
                        val newPosition = fotosList.size -1
                        fotoAdapter.notifyItemInserted(newPosition)
                        fotosRecyclerView.scrollToPosition(newPosition)
                        Log.i(logTag, "Imagem da galeria processada e ADICIONADA à UI: ${compressedAndOrientedFile.absolutePath}")
                    }
                    savePhotoToDatabase(faturaId, compressedAndOrientedFile.absolutePath)
                    Toast.makeText(this, "Foto da galeria adicionada!", Toast.LENGTH_SHORT).show()


                    if (copiedFile.absolutePath != compressedAndOrientedFile.absolutePath && copiedFile.exists()) {
                        if(copiedFile.delete()){
                            Log.d(logTag, "Arquivo intermediário copiado da galeria deletado: ${copiedFile.absolutePath}")
                        } else {
                            Log.w(logTag, "Falha ao deletar arquivo intermediário copiado da galeria: ${copiedFile.absolutePath}")
                        }
                    }
                } else {
                    Log.e(logTag, "Arquivo comprimido da galeria NÃO EXISTE ou está VAZIO: ${compressedAndOrientedFile.absolutePath}")
                    Toast.makeText(this, "Erro: Falha ao processar a foto da galeria (arquivo final inválido).", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e(logTag, "Arquivo copiado da galeria NÃO EXISTE ou está VAZIO: ${copiedFile.absolutePath}")
                Toast.makeText(this, "Erro ao copiar imagem da galeria.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(logTag, "Erro ao manusear imagem da galeria: ${e.message}", e)
            Toast.makeText(this, "Erro ao processar imagem da galeria: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(IOException::class)
    private fun copyUriToAppFile(uri: Uri, fileNamePrefix: String): File {
        val inputStream: InputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Não foi possível abrir InputStream para o URI: $uri")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val originalFileName = getFileNameFromContentUri(uri) ?: "${fileNamePrefix}_${timeStamp}"
        val safeBaseName = originalFileName.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9._-]"), "_")

        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: filesDir.resolve("pictures_fallback").apply { mkdirs() }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw IOException("Não foi possível criar o diretório de armazenamento: ${storageDir.absolutePath}")
        }

        val newFile = File(storageDir, "GAL_${System.currentTimeMillis()}_${safeBaseName}.jpg")

        FileOutputStream(newFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        Log.d(TAG, "Imagem da galeria copiada para: ${newFile.absolutePath} (Tamanho: ${newFile.length()})")
        if (newFile.length() == 0L) {
            Log.e(TAG, "ATENÇÃO: Arquivo copiado da galeria está VAZIO: ${newFile.absolutePath}")
        }
        return newFile
    }

    private fun getFileNameFromContentUri(uri: Uri): String? {
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
                Log.w(TAG, "Erro ao obter DISPLAY_NAME do URI de conteúdo: $uri - ${e.message}", e)
            }
        }
        if (fileName == null) {
            fileName = uri.lastPathSegment
        }
        return fileName
    }

    private fun getRotationFromExif(imagePath: String): Int {
        try {
            val file = File(imagePath)
            if (!file.exists() || file.length() == 0L) {
                Log.w(TAG, "Arquivo não existe ou está vazio ao tentar ler EXIF: $imagePath")
                return 0
            }
            FileInputStream(file).use { fis ->
                val exif = ExifInterface(fis)
                return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "IOException ao ler dados EXIF de $imagePath: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exceção geral ao ler EXIF de $imagePath: ${e.message}", e)
        }
        return 0
    }

    private fun compressImageFile(inputFile: File, quality: Int = 75, targetHeight: Int = 1024): File {
        val logTag = COMPRESS_IMAGE_TAG
        Log.d(logTag, "Iniciando compressão para: ${inputFile.absolutePath}, Tamanho: ${inputFile.length()}")
        if (!inputFile.exists() || inputFile.length() == 0L) {
            Log.e(logTag, "Arquivo de entrada para compressão não existe ou está vazio: ${inputFile.absolutePath}")
            throw IOException("Arquivo de entrada para compressão não existe ou está vazio: ${inputFile.absolutePath}")
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(inputFile.absolutePath, options)
        Log.d(logTag, "Dimensões originais (bounds): ${options.outWidth}x${options.outHeight}")

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            try {
                FileInputStream(inputFile).use { fis ->
                    val exif = ExifInterface(fis)
                    var exifWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    var exifHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        val temp = exifWidth
                        exifWidth = exifHeight
                        exifHeight = temp
                    }
                    options.outWidth = exifWidth
                    options.outHeight = exifHeight
                }
                Log.d(logTag, "Dimensões via EXIF (fallback): ${options.outWidth}x${options.outHeight}")
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    throw IOException("Não foi possível obter as dimensões do arquivo de imagem (BitmapFactory e EXIF falharam): ${inputFile.absolutePath}")
                }
            } catch (exifError: Exception) {
                Log.e(logTag, "Erro ao tentar ler dimensões via EXIF: ${exifError.message}")
                throw IOException("Não foi possível obter as dimensões do arquivo de imagem (BitmapFactory e EXIF falharam): ${inputFile.absolutePath}", exifError)
            }
        }

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
        Log.d(logTag, "inSampleSize calculado: $inSampleSize")

        val initialBitmap = BitmapFactory.decodeFile(inputFile.absolutePath, options)
            ?: throw IOException("Não foi possível decodificar o arquivo de imagem (após inSampleSize): ${inputFile.absolutePath}")

        Log.d(logTag, "Bitmap inicial decodificado: ${initialBitmap.width}x${initialBitmap.height}")

        val rotationAngle = getRotationFromExif(inputFile.absolutePath)
        Log.d(logTag, "Ângulo de rotação EXIF lido: $rotationAngle")

        val finalBitmapToCompress: Bitmap = if (rotationAngle != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationAngle.toFloat())
            try {
                val rotatedBitmap = Bitmap.createBitmap(initialBitmap, 0, 0, initialBitmap.width, initialBitmap.height, matrix, true)
                if (rotatedBitmap != initialBitmap && !initialBitmap.isRecycled) {
                    initialBitmap.recycle()
                    Log.d(logTag, "Bitmap inicial reciclado após rotação bem-sucedida.")
                }
                Log.d(logTag, "Bitmap rotacionado: ${rotatedBitmap.width}x${rotatedBitmap.height}")
                rotatedBitmap
            } catch (e: OutOfMemoryError) {
                Log.e(logTag, "OutOfMemoryError ao rotacionar bitmap. Tentando reciclar e relançando.", e)
                if (!initialBitmap.isRecycled) {
                    initialBitmap.recycle()
                }
                throw IOException("Memória insuficiente para rotacionar a imagem. Tente uma imagem menor.", e)
            } catch (e: Exception) {
                Log.e(logTag, "Erro desconhecido ao rotacionar bitmap. Tentando reciclar e relançando.", e)
                if (!initialBitmap.isRecycled) {
                    initialBitmap.recycle()
                }
                throw IOException("Erro desconhecido ao processar a rotação da imagem.", e)
            }
        } else {
            Log.d(logTag, "Nenhuma rotação EXIF necessária.")
            initialBitmap
        }

        val outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: filesDir.resolve("pictures_fallback").apply { mkdirs() }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw IOException("Não foi possível criar o diretório de armazenamento para compressão: ${outputDir.absolutePath}")
        }

        val compressedFileName = "COMP_ORIENTED_${inputFile.nameWithoutExtension}_${System.currentTimeMillis()}.jpg"
        val compressedFile = File(outputDir, compressedFileName)
        Log.d(logTag, "Arquivo de saída para compressão: ${compressedFile.absolutePath}")

        try {
            FileOutputStream(compressedFile).use { fos ->
                finalBitmapToCompress.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                Log.d(logTag, "Bitmap comprimido e salvo.")
            }
        } catch (e: Exception) {
            Log.e(logTag, "Erro ao salvar bitmap comprimido: ${e.message}", e)
            throw IOException("Erro ao salvar imagem comprimida: ${e.message}", e)
        } finally {
            if (!finalBitmapToCompress.isRecycled) {
                finalBitmapToCompress.recycle()
                Log.d(logTag, "Bitmap final (finalBitmapToCompress) reciclado no finally.")
            }
        }

        Log.i(logTag, "Compressão concluída: ${inputFile.name} (${inputFile.length()} bytes) -> ${compressedFile.name} (${compressedFile.length()} bytes)")
        if (compressedFile.length() == 0L) {
            Log.e(logTag, "ATENÇÃO: Arquivo comprimido está VAZIO: ${compressedFile.absolutePath}")
        }
        return compressedFile
    }


    private fun viewPhotoInExternalViewer(photoPath: String) {
        try {
            val file = File(photoPath)
            if (!file.exists()) {
                Log.w(TAG, "Tentativa de visualizar foto não encontrada: $photoPath")
                Toast.makeText(this, "Arquivo da foto não encontrado.", Toast.LENGTH_LONG).show()
                return
            }
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val uri = FileProvider.getUriForFile(this, authority, file)
            Log.d(TAG, "Visualizando foto com URI: $uri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Log.w(TAG, "Nenhum aplicativo encontrado para visualizar a imagem.")
                Toast.makeText(this, "Nenhum aplicativo para visualizar imagens encontrado.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao tentar visualizar foto: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir a foto.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadPhotosFromDatabase() {
        if (faturaId == -1L) {
            Log.w(TAG, "ID da fatura inválido (-1), não carregando fotos do DB.")
            return
        }
        val db = dbHelper?.readableDatabase ?: run {
            Log.e(TAG, "Erro ao acessar o banco de dados para carregar fotos.")
            Toast.makeText(this, "Erro no banco de dados.", Toast.LENGTH_LONG).show()
            return
        }

        val loadedPhotosFromDb = mutableListOf<String>()
        try {
            db.query(
                FaturaContract.FaturaFotoEntry.TABLE_NAME,
                arrayOf(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH),
                "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ?",
                arrayOf(faturaId.toString()),
                null, null, null
            ).use { cursor ->
                val pathIndex = cursor.getColumnIndex(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH)
                if (pathIndex == -1) {
                    Log.e(TAG, "Coluna ${FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH} não encontrada no cursor ao carregar do DB.")
                    return@use
                }
                while (cursor.moveToNext()) {
                    val path = cursor.getString(pathIndex)
                    if (path != null && File(path).exists()) {
                        loadedPhotosFromDb.add(path)
                    } else {
                        Log.w(TAG, "Foto do DB não encontrada no caminho físico: $path (ou caminho nulo).")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao executar query para carregar fotos do DB: ${e.message}", e)
        }

        fotosList.clear()
        fotosList.addAll(loadedPhotosFromDb)
        fotoAdapter.notifyDataSetChanged()
        Log.d(TAG, "Total de fotos carregadas do DB: ${fotosList.size} para fatura ID $faturaId")
    }

    private fun savePhotoToDatabase(faturaIdParaSalvar: Long, photoPath: String) {
        if (faturaIdParaSalvar == -1L) {
            Log.d(TAG, "Fatura ID é -1. A foto '$photoPath' será vinculada quando a fatura principal for salva.")
            return
        }

        val db = dbHelper?.writableDatabase ?: run {
            Log.e(TAG, "Erro ao acessar o banco de dados para salvar foto.")
            Toast.makeText(this, "Erro no banco de dados.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            db.query(
                FaturaContract.FaturaFotoEntry.TABLE_NAME,
                arrayOf(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH),
                "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ? AND ${FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH} = ?",
                arrayOf(faturaIdParaSalvar.toString(), photoPath),
                null, null, null
            ).use { existingCursor ->
                if (existingCursor.moveToFirst()) {
                    Log.d(TAG, "Foto '$photoPath' já existe no banco para fatura ID $faturaIdParaSalvar. Não salvando novamente.")
                    return
                }
            }

            val values = ContentValues().apply {
                put(FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID, faturaIdParaSalvar)
                put(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH, photoPath)
            }
            val newRowId = db.insert(FaturaContract.FaturaFotoEntry.TABLE_NAME, null, values)
            if (newRowId != -1L) {
                Log.d(TAG, "Foto salva no DB: $photoPath, faturaId=$faturaIdParaSalvar, rowId=$newRowId")
            } else {
                Log.e(TAG, "Erro ao salvar foto no DB: $photoPath, faturaId=$faturaIdParaSalvar")
                Toast.makeText(this, "Erro ao salvar a foto no banco de dados.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao salvar foto no DB: ${e.message}", e)
            Toast.makeText(this, "Erro ao salvar a foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun removePhoto(position: Int) {
        if (position < 0 || position >= fotosList.size) {
            Log.e(TAG, "Posição inválida para remoção: $position, tamanho da lista: ${fotosList.size}")
            Toast.makeText(this, "Posição inválida para remoção.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val photoPath = fotosList[position]
            Log.d(TAG, "Iniciando remoção da foto: $photoPath")

            val file = File(photoPath)
            if (file.exists()) {
                if (file.delete()) {
                    Log.d(TAG, "Arquivo físico deletado: $photoPath")
                } else {
                    Log.w(TAG, "Falha ao deletar arquivo físico: $photoPath")
                }
            } else {
                Log.w(TAG, "Arquivo físico não encontrado para deletar: $photoPath")
            }

            if (faturaId != -1L) {
                val db = dbHelper?.writableDatabase ?: run {
                    Log.e(TAG, "Erro ao acessar banco para remover foto do DB")
                    fotosList.removeAt(position)
                    fotoAdapter.notifyItemRemoved(position)
                    if (position < fotosList.size) {
                        fotoAdapter.notifyItemRangeChanged(position, fotosList.size - position)
                    }
                    return
                }
                val rowsDeleted = db.delete(
                    FaturaContract.FaturaFotoEntry.TABLE_NAME,
                    "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ? AND ${FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH} = ?",
                    arrayOf(faturaId.toString(), photoPath)
                )
                if (rowsDeleted > 0) {
                    Log.d(TAG, "Foto removida do banco: $photoPath, faturaId=$faturaId")
                } else {
                    Log.w(TAG, "Nenhuma foto removida do banco (faturaId=$faturaId, path=$photoPath).")
                }
            }

            fotosList.removeAt(position)
            fotoAdapter.notifyItemRemoved(position)
            if (position < fotosList.size) {
                fotoAdapter.notifyItemRangeChanged(position, fotosList.size - position)
            }
            Log.d(TAG, "Foto removida da lista local. Novo tamanho: ${fotosList.size}")
            Toast.makeText(this, "Foto removida.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao remover foto: ${e.message}", e)
            Toast.makeText(this, "Erro ao remover foto.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("A permissão da câmera é necessária para tirar fotos.")
                .setPositiveButton("OK") { _, _ ->
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestStoragePermission() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        var showRationale = false
        for (permission in permissionsToRequest) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showRationale = true
                break
            }
        }

        if (showRationale) {
            AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("A permissão de armazenamento é necessária para carregar e salvar fotos.")
                .setPositiveButton("OK") { _, _ ->
                    storagePermissionLauncher.launch(permissionsToRequest)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            storagePermissionLauncher.launch(permissionsToRequest)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val appSpecificDirectory: File
        if (storageDir == null) {
            Log.w(TAG, "Diretório externo (Pictures) não disponível, usando diretório interno de fallback.")
            appSpecificDirectory = filesDir.resolve("images_camera_fallback")
        } else {
            appSpecificDirectory = storageDir
        }

        if (!appSpecificDirectory.exists() && !appSpecificDirectory.mkdirs()) {
            Log.e(TAG, "Falha ao criar diretório de armazenamento de imagens: ${appSpecificDirectory.absolutePath}")
            throw IOException("Não foi possível criar o diretório de armazenamento: ${appSpecificDirectory.absolutePath}")
        }

        return File.createTempFile("JPEG_CAM_${timeStamp}_", ".jpg", appSpecificDirectory).also {
            currentPhotoPath = it.absolutePath
            Log.d(TAG, "Arquivo temporário para foto da câmera criado: ${it.absolutePath}")
        }
    }

    private fun dispatchTakePictureIntent() {
        try {
            val photoFileForIntent = createImageFile()
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            currentPhotoUri = FileProvider.getUriForFile(this, authority, photoFileForIntent)

            currentPhotoUri?.let { uri ->
                Log.d(TAG, "Iniciando câmera com URI: $uri para arquivo: ${currentPhotoPath}")
                takePictureLauncher.launch(uri)
            } ?: run {
                Log.e(TAG, "currentPhotoUri é nulo após createImageFile. Não é possível iniciar a câmera.")
                Toast.makeText(this, "Erro ao preparar a câmera: URI da foto é nula.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Nenhum app de câmera disponível: ${e.message}", e)
            Toast.makeText(this, "Nenhum app de câmera disponível.", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e(TAG, "IOException ao criar arquivo para foto: ${e.message}", e)
            Toast.makeText(this, "Erro ao criar arquivo para foto: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Erro desconhecido ao tentar abrir a câmera: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir a câmera.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}