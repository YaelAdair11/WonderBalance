package com.example.wonderbalance.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wonderbalance.databinding.ItemTransaccionBinding
import com.example.wonderbalance.datos.entidad.Transaccion
import com.example.wonderbalance.util.Constantes

class AdaptadorTransaccion(
    private val alHacerClic: (Transaccion) -> Unit
) : ListAdapter<Transaccion, AdaptadorTransaccion.TransaccionViewHolder>(DiffTransaccion()) {

    inner class TransaccionViewHolder(
        private val enlace: ItemTransaccionBinding
    ) : RecyclerView.ViewHolder(enlace.root) {

        fun vincular(transaccion: Transaccion, nombreCategoria: String) {
            // Asignar los valores a los TextViews
            enlace.txtCategoria.text = nombreCategoria
            enlace.txtFecha.text = transaccion.fecha

            // NUEVO: Manejar la visibilidad y el texto de la nota
            if (transaccion.nota.isNullOrBlank()) {
                enlace.txtNota.visibility = android.view.View.GONE
            } else {
                enlace.txtNota.visibility = android.view.View.VISIBLE
                enlace.txtNota.text = transaccion.nota
            }

            // Lógica de colores para Gasto vs Ingreso
            if (transaccion.tipo == Constantes.TIPO_GASTO) {
                enlace.txtMonto.text = "-$%.2f".format(transaccion.monto)
                enlace.txtMonto.setTextColor(
                    itemView.context.getColor(android.R.color.holo_red_dark)
                )
            } else {
                enlace.txtMonto.text = "+$%.2f".format(transaccion.monto)
                enlace.txtMonto.setTextColor(
                    itemView.context.getColor(android.R.color.holo_green_dark)
                )
            }

            enlace.root.setOnClickListener { alHacerClic(transaccion) }
        }
    }

    private val nombresCategorias = mutableMapOf<Int, String>()

    fun actualizarCategorias(mapa: Map<Int, String>) {
        nombresCategorias.clear()
        nombresCategorias.putAll(mapa)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaccionViewHolder {
        val enlace = ItemTransaccionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransaccionViewHolder(enlace)
    }

    override fun onBindViewHolder(holder: TransaccionViewHolder, position: Int) {
        val transaccion = getItem(position)
        val nombre = nombresCategorias[transaccion.categoriaId] ?: "Sin categoría"
        holder.vincular(transaccion, nombre)
    }

    class DiffTransaccion : DiffUtil.ItemCallback<Transaccion>() {
        override fun areItemsTheSame(oldItem: Transaccion, newItem: Transaccion) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Transaccion, newItem: Transaccion) =
            oldItem == newItem
    }
}