package com.example.myapplication

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class DetalhesFaturasMesActivity : AppCompatActivity() {

    private lateinit var textViewDetalhesMesTitle: TextView
    private lateinit var recyclerViewDetalhesFaturas: RecyclerView
    private lateinit var faturaAdapter: FaturaResumidaAdapter // Reutilizando o adapter existente
    private var dbHelper: ClienteDbHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_faturas_mes)

        dbHelper = ClienteDbHelper(this)

        textViewDetalhesMesTitle = findViewById(R.id.textViewDetalhesMesTitle)
        recyclerViewDetalhesFaturas = findViewById(R.id.recyclerViewDetalhesFaturas)
        recyclerViewDetalhesFaturas.layoutManager = LinearLayoutManager(this)

        val ano = intent.getIntExtra("ANO", -1)
        val mes = intent.getIntExtra("MES", -1) // 1-12
        val mesAnoStr = intent.getStringExtra("MES_ANO_STR") ?: "Mês/Ano Desconhecido"

        textViewDetalhesMesTitle.text = "Faturas de $mesAnoStr"

        faturaAdapter = FaturaResumidaAdapter(this,
            onItemClick = { fatura ->
                val intent = Intent(this, SecondScreenActivity::class.java)
                intent.putExtra("fatura_id", fatura.id)
                // Você pode querer passar o status 'foiEnviada' aqui também, se necessário
                val foiEnviada = dbHelper?.readableDatabase?.let { db ->
                    val cursorEnvio = db.query(
                        FaturaContract.FaturaEntry.TABLE_NAME,
                        arrayOf(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA),
                        "${BaseColumns._ID} = ?",
                        arrayOf(fatura.id.toString()), null, null, null
                    )
                    var status = false
                    cursorEnvio?.use { if(it.moveToFirst()) status = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1 }
                    status
                } ?: false
                intent.putExtra("foi_enviada", foiEnviada)
                startActivity(intent) // Você pode querer usar startActivityForResult se precisar de um resultado
            },
            onItemLongClick = { fatura ->
                // Implemente a lógica para clique longo se necessário
                // Por exemplo, mostrar opções como excluir ou marcar como paga/não paga
                Toast.makeText(this, "Clique longo na fatura: ${fatura.numeroFatura}", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerViewDetalhesFaturas.adapter = faturaAdapter

        if (ano != -1 && mes != -1) {
            carregarFaturasDoMes(ano, mes)
        } else {
            Log.e("DetalhesFaturas", "Ano ou Mês inválido recebido. Ano: $ano, Mês: $mes")
            Toast.makeText(this, "Erro ao carregar detalhes: período inválido.", Toast.LENGTH_LONG).show()
        }
    }

    private fun carregarFaturasDoMes(ano: Int, mes: Int) {
        val db = dbHelper?.readableDatabase ?: return
        val faturasDoMes = mutableListOf<FaturaResumidaItem>()

        val mesFormatado = String.format(Locale.US, "%02d", mes)
        // Ajustado para '%Y-%m-%' para pegar todas as datas do mês, se o formato da data no DB for 'YYYY-MM-DD HH:MM:SS'
        val anoMesLike = "$ano-$mesFormatado%"

        val selection = "${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} LIKE ?"
        val selectionArgs = arrayOf(anoMesLike)

        Log.d("DetalhesFaturas", "Carregando faturas para o período: $anoMesLike")

        val cursor: Cursor? = db.query(
            FaturaContract.FaturaEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} DESC"
        )

        cursor?.use {
            Log.d("DetalhesFaturas", "Número de faturas encontradas: ${it.count}")
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
                val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA)) ?: "N/A"
                val cliente = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)) ?: "N/A"
                val artigosString = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS))
                val saldoDevedor = it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR))
                val dataFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA)) ?: ""
                val foiEnviada = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1

                val serialNumbers = mutableListOf<String?>()
                artigosString?.split("|")?.forEach { artigoData ->
                    val parts = artigoData.split(",")
                    if (parts.size >= 5) {
                        val serial = parts[4].takeIf { it.isNotEmpty() && it.lowercase(Locale.ROOT) != "null" }
                        serialNumbers.add(serial)
                    }
                }

                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yy", Locale("pt", "BR"))
                val formattedData = try {
                    val date = inputFormat.parse(dataFatura)
                    if (date != null) outputFormat.format(date) else dataFatura
                } catch (e: Exception) {
                    Log.w("DetalhesFaturas", "Erro ao formatar data: $dataFatura", e)
                    dataFatura
                }
                faturasDoMes.add(FaturaResumidaItem(id, numeroFatura, cliente, serialNumbers, saldoDevedor, formattedData, foiEnviada))
            }
        } ?: Log.e("DetalhesFaturas", "Cursor nulo ao carregar faturas do mês.")

        faturaAdapter.updateFaturas(faturasDoMes)
        if (faturasDoMes.isEmpty()) {
            Toast.makeText(this, "Nenhuma fatura encontrada para este mês.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}