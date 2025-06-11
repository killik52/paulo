package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Classe adaptadora para o RecyclerView que exibe uma lista de notas
class NotaAdapter(
    private val notas: MutableList<String>,
    private val onLongPress: (Int) -> Unit = {} // 2. Callback para long press, com função vazia como padrão
) : RecyclerView.Adapter<NotaAdapter.NotaViewHolder>() {

    // 3. Classe ViewHolder que armazena a referência ao componente da view
    class NotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notaTextView: TextView = itemView.findViewById(R.id.notaTextView) // 4. Referência ao TextView que exibe a nota
    }

    // 5. Método chamado para criar uma nova view para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        // 6. Infla o layout do item da nota
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        // 7. Retorna uma nova instância do ViewHolder com a view inflada
        return NotaViewHolder(view)
    }

    // 8. Método chamado para vincular o texto da nota à view
    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        // 9. Define o texto da nota no TextView
        holder.notaTextView.text = notas[position]
        // 10. Configura o listener para clique longo no item
        holder.itemView.setOnLongClickListener {
            // 11. Chama a função de callback para clique longo
            onLongPress(position)
            true
        }
    }

    // 12. Método que retorna o número total de notas na lista
    override fun getItemCount(): Int {
        return notas.size
    }

    // 13. Método para adicionar uma nova nota à lista
    fun addNota(nota: String) {
        // 14. Adiciona a nota à lista
        notas.add(nota)
        // 15. Notifica o adaptador sobre a inserção do novo item
        notifyItemInserted(notas.size - 1)
    }

    // 16. Método para remover uma nota da lista
    fun removeNota(position: Int) {
        // 17. Verifica se a posição é válida
        if (position in 0 until notas.size) {
            // 18. Remove a nota na posição especificada
            notas.removeAt(position)
            // 19. Notifica o adaptador sobre a remoção do item
            notifyItemRemoved(position)
        }
    }
}