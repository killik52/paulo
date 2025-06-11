package com.example.myapplication

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.model.CnpjData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 1. Classe que representa a atividade para gerenciar informações da empresa
class InformacoesEmpresaActivity : AppCompatActivity() {

    // 2. Variáveis para os campos de entrada de texto
    private lateinit var cnpjEditText: EditText
    private lateinit var nomeEmpresaEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var telefoneEditText: EditText
    private lateinit var informacoesAdicionaisEditText: EditText
    private lateinit var cepEditText: EditText
    private lateinit var estadoEditText: EditText
    private lateinit var paisEditText: EditText
    private lateinit var cidadeEditText: EditText
    // 3. Variável para o SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences

    // 4. Chaves para armazenar os dados no SharedPreferences
    private val CNPJ_KEY = "cnpj"
    private val NOME_EMPRESA_KEY = "nome_empresa"
    private val EMAIL_KEY = "email"
    private val TELEFONE_KEY = "telefone"
    private val INFORMACOES_ADICIONAIS_KEY = "informacoes_adicionais"
    private val CEP_KEY = "cep"
    private val ESTADO_KEY = "estado"
    private val PAIS_KEY = "pais"
    private val CIDADE_KEY = "cidade"

    // 5. Método chamado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 6. Define o layout da atividade
        setContentView(R.layout.activity_informacoes_empresa)

        // 7. Inicializa o SharedPreferences
        sharedPreferences = getSharedPreferences("InformacoesEmpresaPrefs", MODE_PRIVATE)

        // 8. Inicializa os EditTexts
        cnpjEditText = findViewById(R.id.cnpjEditText)
        nomeEmpresaEditText = findViewById(R.id.nomeEmpresaEditText)
        emailEditText = findViewById(R.id.emailEditText)
        telefoneEditText = findViewById(R.id.telefoneEditText)
        informacoesAdicionaisEditText = findViewById(R.id.informacoesAdicionaisEditText)
        cepEditText = findViewById(R.id.cepEditText)
        estadoEditText = findViewById(R.id.estadoEditText)
        paisEditText = findViewById(R.id.paisEditText)
        cidadeEditText = findViewById(R.id.cidadeEditText)

        // 9. Carrega os dados salvos do SharedPreferences
        carregarDadosSalvos()

        // 10. Adiciona máscaras aos campos
        adicionarMascaras()

        // 11. Adiciona TextWatcher a cada campo para salvar alterações manuais
        adicionarTextWatchers()

        // 12. Adiciona um TextWatcher ao campo CNPJ para consulta automática
        cnpjEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 13. Não é necessário implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 14. Não é necessário implementar
            }

            override fun afterTextChanged(s: Editable?) {
                // 15. Remove a formatação para verificar o tamanho real do CNPJ
                val cnpj = s.toString().replace("[^0-9]".toRegex(), "")
                // 16. Verifica se o CNPJ tem 14 dígitos
                if (cnpj.length == 14) {
                    // 17. Consulta os dados do CNPJ
                    consultarCnpj(cnpj)
                }
            }
        })
    }

    // 18. Método para carregar os dados salvos do SharedPreferences
    private fun carregarDadosSalvos() {
        // 19. Carrega os dados salvos do SharedPreferences e preenche os campos
        cnpjEditText.setText(sharedPreferences.getString(CNPJ_KEY, ""))
        nomeEmpresaEditText.setText(sharedPreferences.getString(NOME_EMPRESA_KEY, ""))
        emailEditText.setText(sharedPreferences.getString(EMAIL_KEY, ""))
        telefoneEditText.setText(sharedPreferences.getString(TELEFONE_KEY, ""))
        informacoesAdicionaisEditText.setText(sharedPreferences.getString(INFORMACOES_ADICIONAIS_KEY, ""))
        cepEditText.setText(sharedPreferences.getString(CEP_KEY, ""))
        estadoEditText.setText(sharedPreferences.getString(ESTADO_KEY, ""))
        paisEditText.setText(sharedPreferences.getString(PAIS_KEY, ""))
        cidadeEditText.setText(sharedPreferences.getString(CIDADE_KEY, ""))
    }

    // 20. Método para salvar um único valor no SharedPreferences
    private fun salvarDados(chave: String, valor: String) {
        // 21. Salva um único valor no SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString(chave, valor)
        editor.apply()
    }

    // 22. Método para adicionar TextWatchers aos campos
    private fun adicionarTextWatchers() {
        // 23. Adiciona TextWatcher a cada EditText para salvar alterações manuais
        cnpjEditText.addTextChangedListener(createTextWatcher(CNPJ_KEY))
        nomeEmpresaEditText.addTextChangedListener(createTextWatcher(NOME_EMPRESA_KEY))
        emailEditText.addTextChangedListener(createTextWatcher(EMAIL_KEY))
        telefoneEditText.addTextChangedListener(createTextWatcher(TELEFONE_KEY))
        informacoesAdicionaisEditText.addTextChangedListener(createTextWatcher(INFORMACOES_ADICIONAIS_KEY))
        cepEditText.addTextChangedListener(createTextWatcher(CEP_KEY))
        estadoEditText.addTextChangedListener(createTextWatcher(ESTADO_KEY))
        paisEditText.addTextChangedListener(createTextWatcher(PAIS_KEY))
        cidadeEditText.addTextChangedListener(createTextWatcher(CIDADE_KEY))
    }

    // 24. Método para criar um TextWatcher genérico
    private fun createTextWatcher(chave: String): TextWatcher {
        // 25. Cria um TextWatcher genérico para salvar alterações em um campo
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 26. Não é necessário implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 27. Não é necessário implementar
            }

            override fun afterTextChanged(s: Editable?) {
                // 28. Salva o texto alterado no SharedPreferences
                salvarDados(chave, s.toString())
            }
        }
    }

    // 29. Método para adicionar máscaras aos campos
    private fun adicionarMascaras() {
        // 30. Máscara para CNPJ (formato: 12.345.678/0001-95)
        cnpjEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 31. Não é necessário implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 32. Não é necessário implementar
            }

            override fun afterTextChanged(s: Editable?) {
                // 33. Evita recursão infinita
                if (isUpdating) return
                isUpdating = true
                // 34. Remove caracteres não numéricos
                val unformatted = s.toString().replace("[^0-9]".toRegex(), "")
                // 35. Aplica a formatação do CNPJ
                val formatted = when {
                    unformatted.length > 12 -> "${unformatted.substring(0, 2)}.${unformatted.substring(2, 5)}.${unformatted.substring(5, 8)}/${unformatted.substring(8, 12)}-${unformatted.substring(12)}"
                    unformatted.length > 8 -> "${unformatted.substring(0, 2)}.${unformatted.substring(2, 5)}.${unformatted.substring(5, 8)}/${unformatted.substring(8)}"
                    unformatted.length > 5 -> "${unformatted.substring(0, 2)}.${unformatted.substring(2, 5)}.${unformatted.substring(5)}"
                    unformatted.length > 2 -> "${unformatted.substring(0, 2)}.${unformatted.substring(2)}"
                    else -> unformatted
                }
                // 36. Atualiza o texto no campo
                cnpjEditText.setText(formatted)
                // 37. Move o cursor para o final do texto
                cnpjEditText.setSelection(formatted.length)
                isUpdating = false
            }
        })

        // 38. Máscara para CEP (formato: 12345-678)
        cepEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 39. Não é necessário implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 40. Não é necessário implementar
            }

            override fun afterTextChanged(s: Editable?) {
                // 41. Evita recursão infinita
                if (isUpdating) return
                isUpdating = true
                // 42. Remove caracteres não numéricos
                val unformatted = s.toString().replace("[^0-9]".toRegex(), "")
                // 43. Aplica a formatação do CEP
                val formatted = when {
                    unformatted.length > 5 -> "${unformatted.substring(0, 5)}-${unformatted.substring(5)}"
                    else -> unformatted
                }
                // 44. Atualiza o texto no campo
                cepEditText.setText(formatted)
                // 45. Move o cursor para o final do texto
                cepEditText.setSelection(formatted.length)
                isUpdating = false
            }
        })

        // 46. Máscara para Telefone (formato: (12) 34567-8900)
        telefoneEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 47. Não é necessário implementar
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 48. Não é necessário implementar
            }

            override fun afterTextChanged(s: Editable?) {
                // 49. Evita recursão infinita
                if (isUpdating) return
                isUpdating = true
                // 50. Remove caracteres não numéricos
                val unformatted = s.toString().replace("[^0-9]".toRegex(), "")
                // 51. Aplica a formatação do telefone
                val formatted = when {
                    unformatted.length > 10 -> "(${unformatted.substring(0, 2)}) ${unformatted.substring(2, 7)}-${unformatted.substring(7, 11)}"
                    unformatted.length > 6 -> "(${unformatted.substring(0, 2)}) ${unformatted.substring(2, 7)}-${unformatted.substring(7)}"
                    unformatted.length > 2 -> "(${unformatted.substring(0, 2)}) ${unformatted.substring(2)}"
                    unformatted.length > 0 -> "(${unformatted}"
                    else -> unformatted
                }
                // 52. Atualiza o texto no campo
                telefoneEditText.setText(formatted)
                // 53. Move o cursor para o final do texto
                telefoneEditText.setSelection(formatted.length)
                isUpdating = false
            }
        })
    }

    // 54. Método para consultar os dados do CNPJ via API
    private fun consultarCnpj(cnpj: String) {
        // 55. Faz a chamada à API usando o Retrofit
        val call = RetrofitClient.cnpjApiService.getCnpjData(cnpj)
        call.enqueue(object : Callback<CnpjData> {
            override fun onResponse(call: Call<CnpjData>, response: Response<CnpjData>) {
                // 56. Verifica se a resposta da API foi bem-sucedida
                if (response.isSuccessful) {
                    val cnpjData = response.body()
                    // 57. Verifica se os dados são válidos e o status é OK
                    if (cnpjData != null && cnpjData.status == "OK") {
                        // 58. Preenche os campos com os dados retornados
                        nomeEmpresaEditText.setText(cnpjData.nome ?: "Nome não disponível")
                        emailEditText.setText(cnpjData.email ?: "E-mail não disponível")
                        telefoneEditText.setText(cnpjData.telefone ?: "Telefone não disponível")
                        informacoesAdicionaisEditText.setText(cnpjData.nomeFantasia ?: "Informações não disponíveis")
                        cepEditText.setText(cnpjData.cep ?: "CEP não disponível")
                        estadoEditText.setText(cnpjData.uf ?: "Estado não disponível")
                        paisEditText.setText("Brasil") // A API é brasileira, então assumimos Brasil
                        cidadeEditText.setText(cnpjData.municipio ?: "Cidade não disponível")

                        // 59. Salva os dados no SharedPreferences após preenchê-los
                        salvarDados(CNPJ_KEY, cnpjEditText.text.toString())
                        salvarDados(NOME_EMPRESA_KEY, nomeEmpresaEditText.text.toString())
                        salvarDados(EMAIL_KEY, emailEditText.text.toString())
                        salvarDados(TELEFONE_KEY, telefoneEditText.text.toString())
                        salvarDados(INFORMACOES_ADICIONAIS_KEY, informacoesAdicionaisEditText.text.toString())
                        salvarDados(CEP_KEY, cepEditText.text.toString())
                        salvarDados(ESTADO_KEY, estadoEditText.text.toString())
                        salvarDados(PAIS_KEY, paisEditText.text.toString())
                        salvarDados(CIDADE_KEY, cidadeEditText.text.toString())

                        // 60. Exibe mensagem de sucesso
                        Toast.makeText(this@InformacoesEmpresaActivity, "Dados preenchidos com sucesso", Toast.LENGTH_SHORT).show()
                    } else {
                        // 61. Trata erro retornado pela API
                        val mensagemErro = cnpjData?.mensagem ?: "Erro ao consultar CNPJ"
                        Toast.makeText(this@InformacoesEmpresaActivity, mensagemErro, Toast.LENGTH_LONG).show()
                        // 62. Limpa os campos
                        limparCampos()
                    }
                } else {
                    // 63. Trata erro de resposta HTTP (ex.: 404, 500)
                    Toast.makeText(this@InformacoesEmpresaActivity, "Erro na resposta da API: ${response.code()}", Toast.LENGTH_LONG).show()
                    // 64. Limpa os campos
                    limparCampos()
                }
            }

            override fun onFailure(call: Call<CnpjData>, t: Throwable) {
                // 65. Trata falha na chamada (ex.: sem conexão com a internet)
                Toast.makeText(this@InformacoesEmpresaActivity, "Falha ao consultar CNPJ: ${t.message}", Toast.LENGTH_LONG).show()
                // 66. Limpa os campos
                limparCampos()
            }
        })
    }

    // 67. Método para limpar os campos e o SharedPreferences
    private fun limparCampos() {
        // 68. Limpa os campos de texto
        cnpjEditText.setText("")
        nomeEmpresaEditText.setText("")
        emailEditText.setText("")
        telefoneEditText.setText("")
        informacoesAdicionaisEditText.setText("")
        cepEditText.setText("")
        estadoEditText.setText("")
        paisEditText.setText("")
        cidadeEditText.setText("")

        // 69. Limpa o SharedPreferences
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    // 70. Método chamado quando o botão de voltar do sistema é pressionado
    override fun onBackPressed() {
        // 71. Chama o método da superclasse
        super.onBackPressed()
        // 72. Aplica animação de transição
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}