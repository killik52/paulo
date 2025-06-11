package com.example.myapplication

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// 1. Classe que adiciona espaçamento vertical entre itens em um RecyclerView
class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {

    // 2. Método chamado para definir os offsets (espaçamentos) para cada item no RecyclerView
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        // 3. Define o espaçamento na parte inferior de cada item
        outRect.bottom = verticalSpaceHeight
        // 4. Adiciona espaçamento na parte superior apenas para o primeiro item
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = verticalSpaceHeight
        }
    }
}