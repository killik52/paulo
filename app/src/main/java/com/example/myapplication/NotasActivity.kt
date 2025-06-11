package com.example.myapplication

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

// 1. Classe que representa a atividade para gerenciar notas
class NotasActivity : AppCompatActivity() {

    // 2. Variável para o campo de entrada de texto
    private lateinit var notasEditText: EditText
    // 3. Variável para o SharedPreferences que armazena as notas
    private lateinit var sharedPreferences: SharedPreferences

    // 4. Chave para armazenar o texto no SharedPreferences
    private val NOTAS_KEY = "notas"

    // 5. Método chamado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 6. Define o layout da atividade
        setContentView(R.layout.activity_notas)

        // 7. Inicializa o SharedPreferences
        sharedPreferences = getSharedPreferences("NotasPrefs", MODE_PRIVATE)

        // 8. Inicializa o EditText
        notasEditText = findViewById(R.id.notasEditText)

        // 9. Carrega o texto salvo do SharedPreferences
        val savedNotas = sharedPreferences.getString(NOTAS_KEY, "")
        // 10. Define o texto carregado no EditText
        notasEditText.setText(savedNotas)

        // 11. Adiciona um TextWatcher para salvar o texto enquanto o usuário digita
        notasEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 12. Não é necessário implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 13. Não é necessário implementar
            }

            override fun afterTextChanged(s: Editable?) {
                // 14. Salva o texto no SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString(NOTAS_KEY, s.toString())
                editor.apply()
            }
        })
    }

    // 15. Método chamado quando o botão de voltar do sistema é pressionado
    override fun onBackPressed() {
        // 16. Chama o método da superclasse
        super.onBackPressed()
        // 17. Aplica animação de transição
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}