package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.BaseColumns
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class ResumoFinanceiroActivity : AppCompatActivity() {

    private lateinit var spinnerTipoResumo: Spinner
    private lateinit var spinnerPeriodoFiltro: Spinner
    private lateinit var layoutDatasCustomizadas: LinearLayout
    private lateinit var buttonDataInicio: Button
    private lateinit var buttonDataFim: Button
    private lateinit var buttonAplicarFiltroCustomizado: Button
    private lateinit var recyclerViewResumos: RecyclerView
    private lateinit var textViewTotalResumo: TextView
    private lateinit var exportPdfIcon: ImageView

    private var dbHelper: ClienteDbHelper? = null
    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))
    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateTimeFormatDisplay = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private var dataInicioSelecionada: Calendar? = null
    private var dataFimSelecionada: Calendar? = null

    private lateinit var resumoMensalAdapter: ResumoMensalAdapter
    private lateinit var resumoClienteAdapter: ResumoClienteAdapter
    private lateinit var resumoArtigoAdapter: ResumoArtigoAdapter

    private val tipoResumoFaturamentoMensal = "Faturamento Mensal"
    private val tipoResumoPorCliente = "Por Cliente"
    private val tipoResumoPorArtigo = "Por Artigo"

    private val periodoUltimoAno = "Último Ano"
    private val periodoCustomizado = "Customizado"
    private val periodoTodoPeriodo = "Todo o Período"

    private var currentResumoData: List<Any> = emptyList()
    private var currentTotalResumo: Double = 0.0
    private var currentTipoResumo: String = tipoResumoFaturamentoMensal
    private var currentPeriodoResumo: String = periodoTodoPeriodo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumo_financeiro)

        dbHelper = ClienteDbHelper(this)

        spinnerTipoResumo = findViewById(R.id.spinnerTipoResumo)
        spinnerPeriodoFiltro = findViewById(R.id.spinnerPeriodoFiltro)
        layoutDatasCustomizadas = findViewById(R.id.layoutDatasCustomizadas)
        buttonDataInicio = findViewById(R.id.buttonDataInicio)
        buttonDataFim = findViewById(R.id.buttonDataFim)
        buttonAplicarFiltroCustomizado = findViewById(R.id.buttonAplicarFiltroCustomizado)
        recyclerViewResumos = findViewById(R.id.recyclerViewResumos)
        textViewTotalResumo = findViewById(R.id.textViewTotalResumo)
        exportPdfIcon = findViewById(R.id.exportPdfIcon)

        recyclerViewResumos.layoutManager = LinearLayoutManager(this)

        setupSpinners()
        setupDatePickers()

        buttonAplicarFiltroCustomizado.setOnClickListener {
            carregarDadosResumo()
        }

        exportPdfIcon.setOnClickListener {
            generateResumoPdf()
        }

        carregarDadosResumo()
    }

    private fun setupSpinners() {
        val tiposResumo = arrayOf(tipoResumoFaturamentoMensal, tipoResumoPorCliente, tipoResumoPorArtigo)
        val tipoResumoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tiposResumo)
        spinnerTipoResumo.adapter = tipoResumoAdapter

        val periodosFiltro = arrayOf(periodoTodoPeriodo, periodoUltimoAno, periodoCustomizado)
        val periodoFiltroAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periodosFiltro)
        spinnerPeriodoFiltro.adapter = periodoFiltroAdapter

        spinnerTipoResumo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentTipoResumo = tiposResumo[position]
                carregarDadosResumo()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerPeriodoFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val periodoSelecionado = periodosFiltro[position]
                currentPeriodoResumo = periodoSelecionado
                if (periodoSelecionado == periodoCustomizado) {
                    layoutDatasCustomizadas.visibility = View.VISIBLE
                    buttonAplicarFiltroCustomizado.visibility = View.VISIBLE
                } else {
                    layoutDatasCustomizadas.visibility = View.GONE
                    buttonAplicarFiltroCustomizado.visibility = View.GONE
                    carregarDadosResumo()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDatePickers() {
        buttonDataInicio.setOnClickListener {
            val cal = dataInicioSelecionada ?: Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                dataInicioSelecionada = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
                buttonDataInicio.text = dateFormatDisplay.format(dataInicioSelecionada!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        buttonDataFim.setOnClickListener {
            val cal = dataFimSelecionada ?: Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                dataFimSelecionada = Calendar.getInstance().apply { set(year, month, dayOfMonth, 23, 59, 59) }
                buttonDataFim.text = dateFormatDisplay.format(dataFimSelecionada!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun carregarDadosResumo() {
        val tipoSelecionado = spinnerTipoResumo.selectedItem.toString()
        val periodoSelecionado = spinnerPeriodoFiltro.selectedItem.toString()

        var dataInicioFiltro: String? = null
        var dataFimFiltro: String? = null

        when (periodoSelecionado) {
            periodoUltimoAno -> {
                val calFim = Calendar.getInstance()
                val calInicio = Calendar.getInstance()
                calInicio.set(Calendar.DAY_OF_YEAR, 1)
                calInicio.set(Calendar.HOUR_OF_DAY, 0); calInicio.set(Calendar.MINUTE, 0); calInicio.set(Calendar.SECOND, 0)

                dataInicioFiltro = dateFormatApi.format(calInicio.time)
                dataFimFiltro = dateFormatApi.format(calFim.time)
            }
            periodoCustomizado -> {
                if (dataInicioSelecionada == null || dataFimSelecionada == null) {
                    Toast.makeText(this, "Por favor, selecione as datas de início e fim.", Toast.LENGTH_SHORT).show()
                    return
                }
                if (dataInicioSelecionada!!.after(dataFimSelecionada!!)) {
                    Toast.makeText(this, "Data de início não pode ser posterior à data fim.", Toast.LENGTH_SHORT).show()
                    return
                }
                dataInicioFiltro = dateFormatApi.format(dataInicioSelecionada!!.time)
                dataFimFiltro = dateFormatApi.format(dataFimSelecionada!!.time)
            }
        }

        Log.d("ResumoFinanceiro", "Carregando: Tipo=$tipoSelecionado, Período=$periodoSelecionado, Início=$dataInicioFiltro, Fim=$dataFimFiltro")

        when (tipoSelecionado) {
            tipoResumoFaturamentoMensal -> carregarFaturamentoMensal(dataInicioFiltro, dataFimFiltro)
            tipoResumoPorCliente -> carregarResumoPorCliente(dataInicioFiltro, dataFimFiltro)
            tipoResumoPorArtigo -> carregarResumoPorArtigo(dataInicioFiltro, dataFimFiltro)
        }
    }

    private fun getDateRangePredicate(inicio: String?, fim: String?): Pair<String?, Array<String>?> {
        val selectionArgs = mutableListOf<String>()
        val selectionClause = StringBuilder()

        if (inicio != null && fim != null) {
            selectionClause.append("${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} BETWEEN ? AND ?")
            selectionArgs.add(inicio)
            selectionArgs.add(fim)
        } else if (inicio != null) {
            selectionClause.append("${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} >= ?")
            selectionArgs.add(inicio)
        } else if (fim != null) {
            selectionClause.append("${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} <= ?")
            selectionArgs.add(fim)
        }
        return Pair(if (selectionClause.isNotEmpty()) selectionClause.toString() else null, if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null)
    }

    private fun carregarFaturamentoMensal(dataInicio: String?, dataFim: String?) {
        val db = dbHelper?.readableDatabase ?: return
        val resumos = mutableListOf<ResumoMensalItem>()
        var totalGeral = 0.0

        val (datePredicate, dateArgs) = getDateRangePredicate(dataInicio, dataFim)

        val query = """
            SELECT
                strftime('%m/%Y', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as mes_ano_str,
                strftime('%Y', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as ano,
                strftime('%m', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as mes,
                SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_mes
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            ${if (datePredicate != null) "WHERE $datePredicate" else ""}
            GROUP BY mes_ano_str, ano, mes
            ORDER BY ano DESC, mes DESC
        """.trimIndent()

        val cursor: Cursor? = db.rawQuery(query, dateArgs)

        cursor?.use {
            while (it.moveToNext()) {
                val mesAnoStr = it.getString(it.getColumnIndexOrThrow("mes_ano_str"))
                val valor = it.getDouble(it.getColumnIndexOrThrow("total_mes"))
                val ano = it.getInt(it.getColumnIndexOrThrow("ano"))
                val mes = it.getInt(it.getColumnIndexOrThrow("mes"))
                resumos.add(ResumoMensalItem(mesAnoStr, valor, ano, mes))
                totalGeral += valor
            }
        }
        resumoMensalAdapter = ResumoMensalAdapter(resumos) { itemClicado ->
            val intent = Intent(this, DetalhesFaturasMesActivity::class.java)
            intent.putExtra("ANO", itemClicado.ano)
            intent.putExtra("MES", itemClicado.mes)
            intent.putExtra("MES_ANO_STR", itemClicado.mesAno)
            startActivity(intent)
        }
        recyclerViewResumos.adapter = resumoMensalAdapter
        textViewTotalResumo.text = "Total Faturado: ${decimalFormat.format(totalGeral)}"

        currentResumoData = resumos
        currentTotalResumo = totalGeral
    }

    private fun carregarResumoPorCliente(dataInicio: String?, dataFim: String?) {
        val db = dbHelper?.readableDatabase ?: return
        val resumos = mutableListOf<ResumoClienteItem>()
        var totalGeral = 0.0

        val (datePredicate, dateArgs) = getDateRangePredicate(dataInicio, dataFim)

        val query = """
            SELECT
                ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE},
                SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_gasto_cliente
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            ${if (datePredicate != null) "WHERE $datePredicate" else ""}
            GROUP BY ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE}
            ORDER BY total_gasto_cliente DESC
        """.trimIndent()

        val cursor: Cursor? = db.rawQuery(query, dateArgs)
        cursor?.use {
            val nomeClienteIndex = it.getColumnIndex(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)
            val totalGastoIndex = it.getColumnIndex("total_gasto_cliente")

            if (nomeClienteIndex == -1 || totalGastoIndex == -1) {
                Log.e("ResumoFin", "Coluna não encontrada no cursor de resumo por cliente")
                return@use
            }

            while (it.moveToNext()) {
                val nomeCliente = it.getString(nomeClienteIndex) ?: "Cliente Desconhecido"
                val totalGasto = it.getDouble(totalGastoIndex)
                resumos.add(ResumoClienteItem(nomeCliente, totalGasto, null))
                totalGeral += totalGasto
            }
        }
        resumoClienteAdapter = ResumoClienteAdapter(resumos)
        recyclerViewResumos.adapter = resumoClienteAdapter
        textViewTotalResumo.text = "Total Geral Clientes: ${decimalFormat.format(totalGeral)}"

        currentResumoData = resumos
        currentTotalResumo = totalGeral
    }

    private fun carregarResumoPorArtigo(dataInicio: String?, dataFim: String?) {
        val db = dbHelper?.readableDatabase ?: return
        val artigosMap = mutableMapOf<String, ResumoArtigoItem>()
        var totalGeralVendido = 0.0

        val (datePredicate, dateArgs) = getDateRangePredicate(dataInicio, dataFim)

        val query = "SELECT ${FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS} FROM ${FaturaContract.FaturaEntry.TABLE_NAME} ${if (datePredicate != null) "WHERE $datePredicate" else ""}"
        val cursor: Cursor? = db.rawQuery(query, dateArgs)

        cursor?.use {
            val artigosStringIndex = it.getColumnIndex(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS)
            if (artigosStringIndex == -1) {
                Log.e("ResumoFin", "Coluna de artigos não encontrada no cursor de resumo por artigo.")
                return@use
            }
            while (it.moveToNext()) {
                val artigosString = it.getString(artigosStringIndex)
                if (!artigosString.isNullOrEmpty()) {
                    artigosString.split("|").forEach { artigoData ->
                        val parts = artigoData.split(",")
                        if (parts.size >= 4) {
                            val nomeArtigo = parts[1]
                            val quantidade = parts[2].toIntOrNull() ?: 0
                            val precoTotalItem = parts[3].toDoubleOrNull() ?: 0.0

                            if (nomeArtigo.isNotEmpty() && quantidade > 0) {
                                val resumoExistente = artigosMap[nomeArtigo]
                                if (resumoExistente != null) {
                                    artigosMap[nomeArtigo] = resumoExistente.copy(
                                        quantidadeTotalVendida = resumoExistente.quantidadeTotalVendida + quantidade,
                                        valorTotalVendido = resumoExistente.valorTotalVendido + precoTotalItem
                                    )
                                } else {
                                    artigosMap[nomeArtigo] = ResumoArtigoItem(nomeArtigo, quantidade, precoTotalItem, null)
                                }
                                totalGeralVendido += precoTotalItem
                            }
                        }
                    }
                }
            }
        }
        val resumos = artigosMap.values.sortedByDescending { it.valorTotalVendido }
        resumoArtigoAdapter = ResumoArtigoAdapter(resumos)
        recyclerViewResumos.adapter = resumoArtigoAdapter
        textViewTotalResumo.text = "Valor Total de Artigos Vendidos: ${decimalFormat.format(totalGeralVendido)}"

        currentResumoData = resumos
        currentTotalResumo = totalGeralVendido
    }

    private fun getFaturamentoMensalForPdf(dataInicio: String?, dataFim: String?): Pair<List<ResumoMensalItem>, Double> {
        val db = dbHelper?.readableDatabase ?: return Pair(emptyList(), 0.0)
        val resumos = mutableListOf<ResumoMensalItem>()
        var totalGeral = 0.0

        val (datePredicate, dateArgs) = getDateRangePredicate(dataInicio, dataFim)

        val query = """
            SELECT
                strftime('%m/%Y', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as mes_ano_str,
                strftime('%Y', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as ano,
                strftime('%m', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as mes,
                SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_mes
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            ${if (datePredicate != null) "WHERE $datePredicate" else ""}
            GROUP BY mes_ano_str, ano, mes
            ORDER BY ano DESC, mes DESC
        """.trimIndent()

        val cursor: Cursor? = db.rawQuery(query, dateArgs)
        cursor?.use {
            while (it.moveToNext()) {
                val mesAnoStr = it.getString(it.getColumnIndexOrThrow("mes_ano_str"))
                val valor = it.getDouble(it.getColumnIndexOrThrow("total_mes"))
                val ano = it.getInt(it.getColumnIndexOrThrow("ano"))
                val mes = it.getInt(it.getColumnIndexOrThrow("mes"))
                resumos.add(ResumoMensalItem(mesAnoStr, valor, ano, mes))
                totalGeral += valor
            }
        }
        return Pair(resumos, totalGeral)
    }

    private fun getClientTotalSpending(nomeCliente: String): Double {
        val db = dbHelper?.readableDatabase ?: return 0.0
        val query = """
            SELECT SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_gasto
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            WHERE ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE} = ?
        """.trimIndent()
        val cursor: Cursor? = db.rawQuery(query, arrayOf(nomeCliente))
        var total = 0.0
        cursor?.use {
            if (it.moveToFirst()) {
                total = it.getDouble(it.getColumnIndexOrThrow("total_gasto"))
            }
        }
        return total
    }

    private fun getAllClientsForPdf(): List<ResumoClienteItem> {
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val resumos = mutableListOf<ResumoClienteItem>()
        val query = """
            SELECT DISTINCT ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE}
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            ORDER BY ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE}
        """.trimIndent()
        val cursor: Cursor? = db.rawQuery(query, null)
        cursor?.use {
            val nomeClienteIndex = it.getColumnIndex(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)
            if (nomeClienteIndex == -1) {
                Log.e("ResumoFin", "Coluna de cliente não encontrada no cursor")
                return@use
            }
            while (it.moveToNext()) {
                val nomeCliente = it.getString(nomeClienteIndex) ?: "Cliente Desconhecido"
                resumos.add(ResumoClienteItem(nomeCliente, 0.0, null))
            }
        }
        return resumos
    }

    private fun generateResumoPdf(): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var pageNumber = 1

        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 30f
        val bottomMargin = 40f
        val contentWidth = pageWidth - 2 * margin
        var yPos: Float

        // Estilos de texto
        val companyNamePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 18f
            color = Color.LTGRAY
        }
        val dateTimePaint = TextPaint().apply {
            textSize = 12f
            color = Color.BLACK
        }
        val totalYearPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
        }
        val headerPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
        }
        val textPaint = TextPaint().apply {
            textSize = 12f
            color = Color.BLACK
        }
        val totalPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val pageNumPaint = Paint().apply {
            textSize = 8f
            color = Color.DKGRAY
        }
        val grayBoxPaint = Paint().apply {
            color = Color.parseColor("#F0F0F0")
            style = Paint.Style.FILL
        }
        val grayBoxBorderPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        // --- Função Helper para nova página ---
        fun startNewPage(): Float {
            // Desenha o número da página no rodapé da página atual
            val pageNumText = "Página $pageNumber"
            val textWidth = pageNumPaint.measureText(pageNumText)
            canvas.drawText(pageNumText, pageWidth - margin - textWidth, pageHeight - margin + 10, pageNumPaint)

            pdfDocument.finishPage(currentPage)
            pageNumber++
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            return margin // Retorna a posição Y inicial para a nova página
        }


        // --- CABEÇALHO ---
        var textBlockHeight = 0f
        var logoHeight = 0f
        var currentTextBlockY = margin

        // --- Bloco de Texto (Esquerda) ---
        val sharedPreferences = getSharedPreferences("InformacoesEmpresaPrefs", Context.MODE_PRIVATE)
        val nomeEmpresa = sharedPreferences.getString("nome_empresa", "Nome da Empresa") ?: "Nome da Empresa"
        canvas.drawText(nomeEmpresa, margin, currentTextBlockY + companyNamePaint.textSize, companyNamePaint)
        currentTextBlockY += companyNamePaint.descent() - companyNamePaint.ascent() + 10f

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        val startOfYear = SimpleDateFormat("dd MMM yy", Locale("pt", "BR")).format(calendar.time)
        val today = SimpleDateFormat("dd MMM yy", Locale("pt", "BR")).format(Date())
        val dateRangeText = "Período: $startOfYear - $today"
        canvas.drawText(dateRangeText, margin, currentTextBlockY + dateTimePaint.textSize, dateTimePaint)
        currentTextBlockY += dateTimePaint.descent() - dateTimePaint.ascent() + 10f

        val totalCurrentYear = getFaturamentoMensalForPdf(
            dateFormatApi.format(calendar.time),
            dateFormatApi.format(Date())
        ).second
        val totalYearText = "Total Acumulado ($currentYear): ${decimalFormat.format(totalCurrentYear)}"
        canvas.drawText(totalYearText, margin, currentTextBlockY + totalYearPaint.textSize, totalYearPaint)
        currentTextBlockY += totalYearPaint.descent() - totalYearPaint.ascent()
        textBlockHeight = currentTextBlockY - margin

        // --- Logo (Direita) ---
        val logoPrefs = getSharedPreferences("LogotipoPrefs", MODE_PRIVATE)
        val logoUriString = logoPrefs.getString("logo_uri", null)
        if (logoUriString != null) {
            try {
                val logoUri = Uri.parse(logoUriString)
                val file = File(logoUri.path)
                if (file.exists()){
                    val originalLogoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (originalLogoBitmap != null) {
                        val logoSizeProgress = logoPrefs.getInt("logo_size", 50)
                        val minLogoDisplaySizePdf = 50f
                        val maxLogoDisplaySizePdf = 150f
                        val actualLogoSizeForPdf = minLogoDisplaySizePdf + (logoSizeProgress * (maxLogoDisplaySizePdf - minLogoDisplaySizePdf) / 100f)

                        val aspectRatio = originalLogoBitmap.width.toFloat() / originalLogoBitmap.height.toFloat()
                        var targetWidth = actualLogoSizeForPdf
                        var targetHeight = targetWidth / aspectRatio

                        if (targetHeight > actualLogoSizeForPdf) {
                            targetHeight = actualLogoSizeForPdf
                            targetWidth = targetHeight * aspectRatio
                        }

                        logoHeight = targetHeight
                        val logoBitmap = Bitmap.createScaledBitmap(originalLogoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
                        val roundedBitmap = Bitmap.createBitmap(logoBitmap.width, logoBitmap.height, Bitmap.Config.ARGB_8888)
                        val tempCanvas = Canvas(roundedBitmap)
                        val tempPaint = Paint().apply { isAntiAlias = true }
                        val rect = RectF(0f, 0f, logoBitmap.width.toFloat(), logoBitmap.height.toFloat())
                        tempCanvas.drawRoundRect(rect, 8f, 8f, tempPaint)
                        tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        tempCanvas.drawBitmap(logoBitmap, 0f, 0f, tempPaint)

                        val logoLeft = pageWidth - margin - logoBitmap.width.toFloat()
                        canvas.drawBitmap(roundedBitmap, logoLeft, margin + 25f, null)
                        originalLogoBitmap.recycle()
                        logoBitmap.recycle()
                        roundedBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e("ResumoFinanceiro", "Erro ao carregar/processar logo: ${e.message}", e)
            }
        }

        // --- Ajusta a posição Y com base na maior altura (texto ou logo) ---
        yPos = margin + max(textBlockHeight, logoHeight) + 40f

        // --- Linha Horizontal ---
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // --- Seção "Resumo Total" ---
        val resumoTotalBoxHeight = 30f
        val resumoTotalBoxTop = yPos
        canvas.drawRoundRect(margin, resumoTotalBoxTop, pageWidth - margin, resumoTotalBoxTop + resumoTotalBoxHeight, 8f, 8f, grayBoxPaint)
        canvas.drawRoundRect(margin, resumoTotalBoxTop, pageWidth - margin, resumoTotalBoxTop + resumoTotalBoxHeight, 8f, 8f, grayBoxBorderPaint)
        val resumoTotalText = "Resumo Total"
        val resumoTotalTextWidth = headerPaint.measureText(resumoTotalText)
        val resumoTotalTextX = margin + (contentWidth - resumoTotalTextWidth) / 2
        val resumoTotalTextY = resumoTotalBoxTop + (resumoTotalBoxHeight / 2) - ((headerPaint.descent() + headerPaint.ascent()) / 2)
        canvas.drawText(resumoTotalText, resumoTotalTextX, resumoTotalTextY, headerPaint)
        yPos += resumoTotalBoxHeight + 10f

        // Valor Total do Período Selecionado (UI)
        val totalGeralFiltered = currentTotalResumo
        canvas.drawText("Fatura: ${decimalFormat.format(totalGeralFiltered)}", margin, yPos + totalPaint.textSize, totalPaint)
        yPos += totalPaint.descent() - totalPaint.ascent() + 20f

        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // --- Seção "Resumo Mensal" ---
        val resumoMensalBoxHeight = 30f
        var resumoMensalBoxTop = yPos
        canvas.drawRoundRect(margin, resumoMensalBoxTop, pageWidth - margin, resumoMensalBoxTop + resumoMensalBoxHeight, 8f, 8f, grayBoxPaint)
        canvas.drawRoundRect(margin, resumoMensalBoxTop, pageWidth - margin, resumoMensalBoxTop + resumoMensalBoxHeight, 8f, 8f, grayBoxBorderPaint)
        val resumoMensalText = "Resumo Mensal"
        val resumoMensalTextWidth = headerPaint.measureText(resumoMensalText)
        val resumoMensalTextX = margin + (contentWidth - resumoMensalTextWidth) / 2
        val resumoMensalTextY = resumoMensalBoxTop + (resumoMensalBoxHeight / 2) - ((headerPaint.descent() + headerPaint.ascent()) / 2)
        canvas.drawText(resumoMensalText, resumoMensalTextX, resumoMensalTextY, headerPaint)
        yPos += resumoMensalBoxHeight + 10f

        // Cabeçalhos "Data" e "Fatura"
        canvas.drawText("Data", margin + 5, yPos + textPaint.textSize, textPaint)
        canvas.drawText("Fatura", pageWidth - margin - textPaint.measureText("Fatura") - 5, yPos + textPaint.textSize, textPaint)
        yPos += textPaint.descent() - textPaint.ascent() + 5f

        val (mensalResumosAllTime, _) = getFaturamentoMensalForPdf(null, null)
        mensalResumosAllTime.forEach { item ->
            if (yPos + textPaint.textSize + 5f > pageHeight - bottomMargin) {
                yPos = startNewPage()
                // Redesenha o cabeçalho da seção na nova página
                resumoMensalBoxTop = yPos
                canvas.drawRoundRect(margin, resumoMensalBoxTop, pageWidth - margin, resumoMensalBoxTop + resumoMensalBoxHeight, 8f, 8f, grayBoxPaint)
                canvas.drawRoundRect(margin, resumoMensalBoxTop, pageWidth - margin, resumoMensalBoxTop + resumoMensalBoxHeight, 8f, 8f, grayBoxBorderPaint)
                canvas.drawText(resumoMensalText, resumoMensalTextX, resumoMensalBoxTop + (resumoMensalBoxHeight / 2) - ((headerPaint.descent() + headerPaint.ascent()) / 2), headerPaint)
                yPos += resumoMensalBoxHeight + 10f
                canvas.drawText("Data", margin + 5, yPos + textPaint.textSize, textPaint)
                canvas.drawText("Fatura", pageWidth - margin - textPaint.measureText("Fatura") - 5, yPos + textPaint.textSize, textPaint)
                yPos += textPaint.descent() - textPaint.ascent() + 5f
            }
            canvas.drawText(item.mesAno, margin + 5, yPos + textPaint.textSize, textPaint)
            val valueText = decimalFormat.format(item.valorTotal)
            canvas.drawText(valueText, pageWidth - margin - textPaint.measureText(valueText) - 5, yPos + textPaint.textSize, textPaint)
            yPos += textPaint.descent() - textPaint.ascent() + 10f
        }
        yPos += 10f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // --- Seção "Resumo por Cliente" ---
        if (yPos + 40f > pageHeight - bottomMargin) { // Checa se cabeçalho da próxima seção cabe
            yPos = startNewPage()
        }
        val resumoClienteBoxHeight = 30f
        var resumoClienteBoxTop = yPos
        canvas.drawRoundRect(margin, resumoClienteBoxTop, pageWidth - margin, resumoClienteBoxTop + resumoClienteBoxHeight, 8f, 8f, grayBoxPaint)
        canvas.drawRoundRect(margin, resumoClienteBoxTop, pageWidth - margin, resumoClienteBoxTop + resumoClienteBoxHeight, 8f, 8f, grayBoxBorderPaint)
        val resumoClienteText = "Resumo por Cliente"
        val resumoClienteTextWidth = headerPaint.measureText(resumoClienteText)
        val resumoClienteTextX = margin + (contentWidth - resumoClienteTextWidth) / 2
        val resumoClienteTextY = resumoClienteBoxTop + (resumoClienteBoxHeight / 2) - ((headerPaint.descent() + headerPaint.ascent()) / 2)
        canvas.drawText(resumoClienteText, resumoClienteTextX, resumoClienteTextY, headerPaint)
        yPos += resumoClienteBoxHeight + 10f

        // Cabeçalhos "Empresa" e "Fatura"
        canvas.drawText("Empresa", margin + 5, yPos + textPaint.textSize, textPaint)
        canvas.drawText("Fatura", pageWidth - margin - textPaint.measureText("Fatura") - 5, yPos + textPaint.textSize, textPaint)
        yPos += textPaint.descent() - textPaint.ascent() + 5f

        val allClients = getAllClientsForPdf()
        allClients.forEach { cliente ->
            if (yPos + textPaint.textSize + 5f > pageHeight - bottomMargin) {
                yPos = startNewPage()
                // Redesenha o cabeçalho da seção na nova página
                resumoClienteBoxTop = yPos
                canvas.drawRoundRect(margin, resumoClienteBoxTop, pageWidth - margin, resumoClienteBoxTop + resumoClienteBoxHeight, 8f, 8f, grayBoxPaint)
                canvas.drawRoundRect(margin, resumoClienteBoxTop, pageWidth - margin, resumoClienteBoxTop + resumoClienteBoxHeight, 8f, 8f, grayBoxBorderPaint)
                canvas.drawText(resumoClienteText, resumoClienteTextX, resumoClienteBoxTop + (resumoClienteBoxHeight / 2) - ((headerPaint.descent() + headerPaint.ascent()) / 2), headerPaint)
                yPos += resumoClienteBoxHeight + 10f
                canvas.drawText("Empresa", margin + 5, yPos + textPaint.textSize, textPaint)
                canvas.drawText("Fatura", pageWidth - margin - textPaint.measureText("Fatura") - 5, yPos + textPaint.textSize, textPaint)
                yPos += textPaint.descent() - textPaint.ascent() + 5f
            }
            canvas.drawText(cliente.nomeCliente, margin + 5, yPos + textPaint.textSize, textPaint)
            val totalGastoCliente = getClientTotalSpending(cliente.nomeCliente)
            val totalGastoText = decimalFormat.format(totalGastoCliente)
            canvas.drawText(totalGastoText, pageWidth - margin - textPaint.measureText(totalGastoText) - 5, yPos + textPaint.textSize, textPaint)
            yPos += textPaint.descent() - textPaint.ascent() + 10f
        }

        // Finaliza a última página
        val pageNumText = "Página $pageNumber"
        val textWidth = pageNumPaint.measureText(pageNumText)
        canvas.drawText(pageNumText, pageWidth - margin - textWidth, pageHeight - margin + 10, pageNumPaint)
        pdfDocument.finishPage(currentPage)

        val fileName = "RelatorioFinanceiro_${System.currentTimeMillis()}.pdf"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (storageDir == null) {
            Log.e("ResumoFinanceiro", "Diretório de armazenamento externo não disponível.")
            Toast.makeText(this, "Erro: Armazenamento externo não disponível para salvar PDF.", Toast.LENGTH_LONG).show()
            pdfDocument.close()
            return null
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("ResumoFinanceiro", "Não foi possível criar o diretório de documentos.")
            Toast.makeText(this, "Erro ao criar diretório para salvar PDF.", Toast.LENGTH_LONG).show()
            pdfDocument.close()
            return null
        }

        val file = File(storageDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Log.d("ResumoFinanceiro", "PDF gerado com sucesso: ${file.absolutePath}")
            Toast.makeText(this, "PDF de resumo financeiro gerado com sucesso!", Toast.LENGTH_LONG).show()
            viewPdf(file)
            return file
        } catch (e: IOException) {
            Log.e("ResumoFinanceiro", "Erro de I/O ao salvar PDF: ${e.message}", e)
            Toast.makeText(this, "Erro de I/O ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        } catch (e: Exception) {
            Log.e("ResumoFinanceiro", "Erro geral ao salvar PDF: ${e.message}", e)
            Toast.makeText(this, "Erro ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun viewPdf(file: File) {
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Nenhum visualizador de PDF encontrado.", Toast.LENGTH_LONG).show()
            Log.e("ResumoFinanceiro", "Erro ao abrir PDF: ${e.message}", e)
        }
    }
}