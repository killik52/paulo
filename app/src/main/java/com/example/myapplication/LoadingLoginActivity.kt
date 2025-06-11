package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LoadingLoginActivity : AppCompatActivity() {

    private lateinit var imageViewLogo: ImageView
    private lateinit var progressBarHorizontal: ProgressBar
    private lateinit var textViewPercentage: TextView

    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)

    private val TOTAL_INITIALIZATION_TASKS = 2 // Ajuste conforme suas tarefas reais
    private var completedTasks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_login) // Define o layout da atividade

        imageViewLogo = findViewById(R.id.imageViewLogoLoading) // Inicializa a ImageView para o logo
        progressBarHorizontal = findViewById(R.id.progressBarHorizontalLoading) // Inicializa a ProgressBar
        textViewPercentage = findViewById(R.id.textViewPercentage) // Inicializa o TextView para a porcentagem

        val rotateAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely) // Carrega a animação de rotação
        imageViewLogo.startAnimation(rotateAnimation) // Inicia a animação do logo

        startLoadingProcess() // Inicia o processo de carregamento
    }

    private fun startLoadingProcess() {
        completedTasks = 0 // Reinicia o contador de tarefas completas
        updateProgressUI() // Atualiza a UI para 0% no início

        uiScope.launch {
            // Pequeno atraso inicial (opcional) para permitir que a animação comece e seja visível.
            delay(300L)

            // --- Tarefas de Inicialização Reais ---

            // Tarefa 1: Inicialização do Banco de Dados
            var initializationFailed = false
            withContext(Dispatchers.IO) { // Garante que a operação de DB seja executada em um thread de I/O
                try {
                    val dbHelper = ClienteDbHelper(applicationContext) // Inicializa o DB Helper
                    dbHelper.readableDatabase // Acessa o DB para garantir que ele está pronto (onCreate/onUpgrade são chamados aqui)
                } catch (e: Exception) {
                    initializationFailed = true // Define a flag de falha
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoadingLoginActivity, "Erro crítico ao inicializar o banco de dados. O aplicativo será fechado.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            // Verifica a flag de falha após o bloco withContext
            if (initializationFailed) {
                delay(2000L) // Atraso para o Toast ser visível
                finishAffinity() // Fecha a atividade e todas as atividades pais
                return@launch // Interrompe a coroutine
            }

            completedTasks++ // Marca a tarefa como concluída
            updateProgressUI() // Atualiza a barra de progresso após a conclusão da tarefa 1

            // Tarefa 2: Carregamento de Preferências Iniciais (ex: SharedPreferences)
            withContext(Dispatchers.IO) { // Garante que a operação seja executada em um thread de I/O
                try {
                    val prefs = applicationContext.getSharedPreferences("InformacoesEmpresaPrefs", MODE_PRIVATE) // Exemplo de acesso a SharedPreferences
                    prefs.getString("nome_empresa", "") // Exemplo de leitura
                    val logoPrefs = applicationContext.getSharedPreferences("LogotipoPrefs", MODE_PRIVATE) // Exemplo de acesso a SharedPreferences
                    logoPrefs.getString("logo_uri", null) // Exemplo de leitura

                } catch (e: Exception) {
                    // Captura exceções gerais ao carregar SharedPreferences.
                    // Isso pode ocorrer por corrupção de arquivo ou outros problemas inesperados.
                    // Se for um erro recuperável (não crítico para o app funcionar), apenas avise o usuário.
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoadingLoginActivity, "Erro ao carregar configurações iniciais. Alguns dados podem estar faltando.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            completedTasks++ // Marca a tarefa como concluída
            updateProgressUI() // Atualiza a barra de progresso após a conclusão da tarefa 2

            // Adicione mais tarefas reais de inicialização aqui, seguindo o padrão:
            // 1. Chamar a tarefa (preferencialmente em `withContext(Dispatchers.IO)`)
            // 2. Incrementar `completedTasks`
            // 3. Chamar `updateProgressUI()`

            // Quando todas as tarefas de inicialização estiverem concluídas:
            allTasksCompleted()
        }
    }

    private fun updateProgressUI() {
        val progressPercentage = (completedTasks.toFloat() / TOTAL_INITIALIZATION_TASKS.toFloat() * 100).toInt()
        progressBarHorizontal.progress = progressPercentage // Atualiza o progresso da barra
        textViewPercentage.text = String.format(Locale.getDefault(), "%d%%", progressPercentage) // Atualiza o texto da porcentagem
    }

    private fun allTasksCompleted() {
        imageViewLogo.clearAnimation() // Para a animação do logo
        navigateToNextScreen() // Navega para a próxima tela
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, MainActivity::class.java) // Cria uma Intent para iniciar a MainActivity
        startActivity(intent) // Inicia a MainActivity
        finish() // Fecha esta activity (LoadingLoginActivity)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel() // Cancela todas as coroutines quando a activity é destruída para evitar vazamentos de memória.
    }
}