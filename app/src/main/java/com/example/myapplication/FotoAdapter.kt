package com.example.myapplication

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

// 1. Classe adaptadora para o RecyclerView que exibe uma lista de fotos
class FotoAdapter(
    private val context: Context,
    private val fotos: MutableList<String>,
    private val onPhotoClick: (String) -> Unit,
    private val onPhotoLongClick: (Int) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    // 2. Método chamado para criar uma nova view para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        // 3. Infla o layout do item da foto
        val view = LayoutInflater.from(context).inflate(R.layout.item_foto, parent, false)
        // 4. Retorna uma nova instância do ViewHolder com a view inflada
        return FotoViewHolder(view)
    }

    // 5. Método chamado para vincular o caminho da foto à view
    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        // 6. Obtém o caminho da foto na posição especificada
        val fotoPath = fotos[position]
        // 7. Cria um objeto File a partir do caminho
        val file = File(fotoPath)
        try {
            // 8. Verifica se o arquivo da foto existe
            if (file.exists()) {
                // 9. Configura opções para reduzir o tamanho da imagem
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                // 10. Decodifica a imagem do arquivo
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                if (bitmap != null) {
                    // 11. Define a imagem no ImageView se decodificada com sucesso
                    holder.fotoImageView.setImageBitmap(bitmap)
                    Log.d("FotoAdapter", "Imagem carregada: $fotoPath")
                } else {
                    // 12. Define uma imagem padrão se a decodificação falhar
                    Log.w("FotoAdapter", "Falha ao decodificar: $fotoPath")
                    holder.fotoImageView.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            } else {
                // 13. Define uma imagem padrão se o arquivo não existir
                Log.w("FotoAdapter", "Arquivo não encontrado: $fotoPath")
                holder.fotoImageView.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        } catch (e: OutOfMemoryError) {
            // 14. Trata erro de memória insuficiente
            Log.e("FotoAdapter", "Erro memória: $fotoPath, ${e.message}", e)
            holder.fotoImageView.setImageResource(android.R.drawable.ic_menu_report_image)
        } catch (e: Exception) {
            // 15. Trata outros erros durante o carregamento da imagem
            Log.e("FotoAdapter", "Erro ao carregar: $fotoPath, ${e.message}", e)
            holder.fotoImageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        // 16. Configura o listener para clique simples na imagem
        holder.fotoImageView.setOnClickListener {
            // 17. Chama a função de callback para clique simples
            onPhotoClick(fotoPath)
        }

        // 18. Configura o listener para clique longo na imagem
        holder.fotoImageView.setOnLongClickListener {
            // 19. Chama a função de callback para clique longo
            onPhotoLongClick(position)
            true
        }
    }

    // 20. Método que retorna o número total de fotos na lista
    override fun getItemCount(): Int = fotos.size

    // 21. Classe ViewHolder que armazena a referência ao componente da view
    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 22. Referência ao ImageView que exibe a foto
        val fotoImageView: ImageView = itemView.findViewById(R.id.fotoImageView)
    }
}