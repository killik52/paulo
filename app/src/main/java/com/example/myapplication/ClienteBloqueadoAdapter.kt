package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Define a classe adaptadora para o RecyclerView, que gerencia a lista de clientes bloqueados
class ClienteBloqueadoAdapter(
    private val clientes: List<ClienteBloqueado>
) : RecyclerView.Adapter<ClienteBloqueadoAdapter.ClienteViewHolder>() {

    // 2. Define a classe ViewHolder que armazena a view de cada item da lista
    class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 3. Referência ao TextView que exibirá o nome do cliente
        val nomeTextView: TextView = itemView.findViewById(android.R.id.text1)
    }

    // 4. Método chamado para criar uma nova view quando necessário
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        // 5. Infla o layout do item da lista usando um layout padrão do Android
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        // 6. Retorna uma nova instância do ViewHolder com a view inflada
        return ClienteViewHolder(view)
    }

    // 7. Método chamado para vincular os dados do cliente à view
    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        // 8. Obtém o cliente na posição especificada
        val cliente = clientes[position]
        // 9. Define o texto do TextView como o nome do cliente
        holder.nomeTextView.text = cliente.nome
    }

    // 10. Método que retorna o número total de itens na lista
    override fun getItemCount(): Int = clientes.size
}