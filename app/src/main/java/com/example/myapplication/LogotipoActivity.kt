package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class LogotipoActivity : AppCompatActivity() {

    private lateinit var logoImageView: ImageView
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var sharedPreferences: SharedPreferences

    private val LOGO_URI_KEY = "logo_uri"
    private val LOGO_SIZE_KEY = "logo_size"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                try {
                    // Copia a imagem para o armazenamento interno e obtém o novo URI
                    val internalFileUri = copyImageToInternalStorage(selectedImageUri)

                    if (internalFileUri != null) {
                        logoImageView.setImageURI(internalFileUri)
                        logoImageView.setBackgroundResource(0)

                        // Salva o URI do arquivo interno no SharedPreferences
                        val editor = sharedPreferences.edit()
                        editor.putString(LOGO_URI_KEY, internalFileUri.toString())
                        editor.apply()

                        Toast.makeText(this, "Logotipo salvo com sucesso", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Erro ao salvar o logotipo.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao carregar a imagem: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Nenhuma imagem selecionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logotipo)

        sharedPreferences = getSharedPreferences("LogotipoPrefs", MODE_PRIVATE)

        logoImageView = findViewById(R.id.logoImageView)
        sizeSeekBar = findViewById(R.id.sizeSeekBar)

        val savedLogoUriString = sharedPreferences.getString(LOGO_URI_KEY, null)
        if (savedLogoUriString != null) {
            try {
                val savedLogoUri = Uri.parse(savedLogoUriString)
                // Verifica se o arquivo ainda existe antes de tentar carregar
                val file = File(savedLogoUri.path)
                if (file.exists()) {
                    logoImageView.setImageURI(savedLogoUri)
                    logoImageView.setBackgroundResource(0)
                } else {
                    // Se o arquivo foi apagado, remove a referência
                    sharedPreferences.edit().remove(LOGO_URI_KEY).apply()
                    Toast.makeText(this, "Arquivo de logotipo não encontrado, por favor selecione novamente.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao carregar o logotipo salvo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        val savedSize = sharedPreferences.getInt(LOGO_SIZE_KEY, 50)
        sizeSeekBar.progress = savedSize

        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minSize = 40
                val maxSize = 200
                val size = minSize + (progress * (maxSize - minSize) / 100)

                val density = resources.displayMetrics.density
                val sizeInPixels = (size * density).toInt()

                val layoutParams = logoImageView.layoutParams
                layoutParams.width = sizeInPixels
                layoutParams.height = sizeInPixels
                logoImageView.layoutParams = layoutParams

                val editor = sharedPreferences.edit()
                editor.putInt(LOGO_SIZE_KEY, progress)
                editor.apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val trashButton: ImageView = findViewById(R.id.trashButton)
        trashButton.setOnClickListener {
            // Remove a imagem do ImageView
            logoImageView.setImageResource(0)
            logoImageView.setBackgroundColor(android.graphics.Color.parseColor("#FF00FF"))

            // Remove o URI e o arquivo do armazenamento interno
            val savedUriString = sharedPreferences.getString(LOGO_URI_KEY, null)
            if (savedUriString != null) {
                val fileToDelete = File(Uri.parse(savedUriString).path)
                if (fileToDelete.exists()) {
                    fileToDelete.delete()
                }
            }
            sharedPreferences.edit().remove(LOGO_URI_KEY).apply()

            Toast.makeText(this, "Logotipo removido", Toast.LENGTH_SHORT).show()
        }

        val insertLogoButton: Button = findViewById(R.id.insertLogoButton)
        insertLogoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(filesDir, "logo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}