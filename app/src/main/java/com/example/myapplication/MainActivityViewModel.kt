package com.example.myapplication

import android.app.Application
import android.provider.BaseColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = ClienteDbHelper(application)

    // LiveData para expor a lista de faturas para a UI.
    // A Activity vai "observar" esta variável para receber atualizações.
    private val _faturas = MutableLiveData<List<FaturaResumidaItem>>()
    val faturas: LiveData<List<FaturaResumidaItem>> get() = _faturas

    init {
        // Carrega as faturas assim que o ViewModel é criado.
        carregarFaturas()
    }

    fun carregarFaturas() {
        // viewModelScope é um CoroutineScope que é automaticamente cancelado quando o ViewModel é destruído.
        viewModelScope.launch {
            try {
                // Executa a operação de banco de dados em uma thread de I/O
                val faturasCarregadas = withContext(Dispatchers.IO) {
                    val tempFaturasList = mutableListOf<FaturaResumidaItem>()
                    val db = dbHelper.readableDatabase
                    val cursor = db.rawQuery(
                        "SELECT ${BaseColumns._ID}, " +
                                "${FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA}, " +
                                "${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE}, " +
                                "${FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS}, " +
                                "${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}, " +
                                "${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}, " +
                                "${FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA} " +
                                "FROM ${FaturaContract.FaturaEntry.TABLE_NAME} " +
                                "ORDER BY ${BaseColumns._ID} DESC",
                        null
                    )

                    cursor?.use {
                        while (it.moveToNext()) {
                            val id = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
                            val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA))
                            val cliente = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE))
                            val artigosString = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS))
                            val saldoDevedor = it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR))
                            val dataFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA))
                            val foiEnviada = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1

                            val serialNumbers = mutableListOf<String?>()
                            artigosString?.split("|")?.forEach { artigoData ->
                                val parts = artigoData.split(",")
                                if (parts.size >= 5) {
                                    val serial = parts[4].takeIf { s -> s.isNotEmpty() && s.lowercase(Locale.ROOT) != "null" }
                                    serialNumbers.add(serial)
                                }
                            }

                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val outputFormat = SimpleDateFormat("dd MMM yy", Locale("pt", "BR"))
                            val formattedData = try {
                                val date = inputFormat.parse(dataFatura)
                                date?.let { d -> outputFormat.format(d) } ?: dataFatura
                            } catch (e: Exception) {
                                dataFatura
                            }
                            tempFaturasList.add(FaturaResumidaItem(id, numeroFatura, cliente, serialNumbers, saldoDevedor, formattedData, foiEnviada))
                        }
                    }
                    tempFaturasList // Retorna a lista da corrotina
                }

                // Atualiza o LiveData com a nova lista. Isso vai notificar a Activity.
                _faturas.value = faturasCarregadas

            } catch (e: Exception) {
                Log.e("MainActivityViewModel", "Erro ao carregar faturas: ${e.message}", e)
                // Você pode expor um LiveData de erro para a UI também, se desejar.
            }
        }
    }

    // É importante fechar o dbHelper quando o ViewModel for limpo
    override fun onCleared() {
        super.onCleared()
        dbHelper.close()
    }
}