package com.example.myapplication

import android.app.Activity // Adicionado para RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Adicionado para OnFocusChangeListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// 1. Classe que representa a atividade para configurar descontos e taxas de entrega
class ThirdScreenActivity : AppCompatActivity() {

    // 2. Variáveis para os elementos da interface - Declaradas como lateinit
    private lateinit var backButton: ImageButton
    private lateinit var valorEditText: EditText
    private lateinit var descontoTypeSpinner: Spinner
    private lateinit var valorRemessaEditText: EditText
    private lateinit var buttonSave: Button

    // 3. Formato para valores numéricos no padrão brasileiro
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    })

    // 4. Método chamado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 5. Define o layout da atividade
        setContentView(R.layout.activity_third_screen)

        // 6. Inicializa os elementos da interface (ANTES DE QUALQUER USO)
        backButton = findViewById(R.id.backButton)
        valorEditText = findViewById(R.id.valorEditText) // ID correto do seu XML
        descontoTypeSpinner = findViewById(R.id.descontoTypeSpinner) // ID correto do seu XML
        valorRemessaEditText = findViewById(R.id.valorRemessaEditText) // ID correto do seu XML
        buttonSave = findViewById(R.id.buttonSave) // ID correto do seu XML

        // --- A PARTIR DAQUI, AS VIEWS ESTÃO INICIALIZADAS E PODEM SER USADAS ---

        // 7. Recebe os dados da fatura do Intent
        val desconto = intent.getDoubleExtra("desconto", 0.0)
        val isPercentDesconto = intent.getBooleanExtra("isPercentDesconto", false)
        val taxaEntrega = intent.getDoubleExtra("taxaEntrega", 0.0)

        // 8. Registra os dados recebidos
        Log.d("ThirdScreen", "Dados recebidos: Desconto=$desconto, isPercent=$isPercentDesconto, TaxaEntrega=$taxaEntrega")

        // 9. Preenche os campos com os dados recebidos
        valorEditText.setText(decimalFormat.format(desconto))
        valorRemessaEditText.setText(decimalFormat.format(taxaEntrega))

        // 10. Configura o Spinner com base no tipo de desconto
        val descontoType = if (isPercentDesconto) 1 else 0 // 0 para "R$", 1 para "%"
        descontoTypeSpinner.setSelection(descontoType)

        // Listener para limpar o campo de DESCONTO (valorEditText) ao ganhar foco
        valorEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Limpa o texto se for "0,00" ou o valor padrão formatado
                val currentFormattedZero = decimalFormat.format(0.0)
                if (valorEditText.text.toString() == currentFormattedZero || valorEditText.text.toString() == "0") {
                    valorEditText.setText("")
                }
            }
        }

        // Listener para limpar o campo de TAXA DE ENTREGA (valorRemessaEditText) ao ganhar foco
        valorRemessaEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Limpa o texto se for "0,00" ou o valor padrão formatado
                val currentFormattedZero = decimalFormat.format(0.0)
                if (valorRemessaEditText.text.toString() == currentFormattedZero || valorRemessaEditText.text.toString() == "0") {
                    valorRemessaEditText.setText("")
                }
            }
        }

        // 11. Configura o listener para o botão de voltar
        backButton.setOnClickListener {
            // 12. Salva os dados e retorna à atividade anterior
            saveAndReturn()
        }

        // 13. Configura o listener para o botão de salvar
        buttonSave.setOnClickListener {
            // 14. Salva os dados e retorna à atividade anterior
            saveAndReturn()
        }
    }

    // 15. Método para salvar os dados e retornar à atividade chamadora
    private fun saveAndReturn() {
        try {
            // 16. Converte o valor do desconto, substituindo vírgula por ponto
            // Lida com texto vazio ou não numérico, retornando 0.0 nesses casos.
            val descontoText = valorEditText.text.toString().replace("R$", "").trim().replace(".", "").replace(",", ".")
            val descontoValue = if (descontoText.isEmpty()) 0.0 else descontoText.toDoubleOrNull() ?: 0.0

            // 17. Converte o valor da taxa de entrega
            val taxaEntregaText = valorRemessaEditText.text.toString().replace("R$", "").trim().replace(".", "").replace(",", ".")
            val taxaEntregaValue = if (taxaEntregaText.isEmpty()) 0.0 else taxaEntregaText.toDoubleOrNull() ?: 0.0

            // 18. Determina se o desconto é percentual (1 para "%", 0 para "R$")
            val isPercent = descontoTypeSpinner.selectedItemPosition == 1

            // 19. Cria um Intent com os dados a serem retornados
            val resultIntent = Intent().apply {
                // Se for porcentagem, não divida por 100 aqui, o valor já deve ser a porcentagem direta
                // A lógica de formatação e parse deve garantir que o valor em 'descontoValue' seja correto
                // para o tipo selecionado.
                if (isPercent) {
                    putExtra("desconto", descontoValue) // Passa o valor como está (ex: 10 para 10%)
                } else {
                    putExtra("desconto", descontoValue) // Passa o valor como está (ex: 10.50 para R$10,50)
                }
                putExtra("isPercentDesconto", isPercent)
                putExtra("taxaEntrega", taxaEntregaValue) // Passa o valor como está (ex: 5.20 para R$5,20)
            }
            // 20. Define o resultado como bem-sucedido e retorna
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            // 21. Registra e exibe erro se houver falha ao salvar
            Log.e("ThirdScreen", "Erro ao salvar dados: ${e.message}")
            showToast("Erro ao salvar dados: ${e.message}")
        }
    }

    // 22. Método para exibir mensagens toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}