package com.example.wonderbalance.ui.transaccion

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.wonderbalance.databinding.FragmentoDetalleTransaccionBinding
import com.example.wonderbalance.datos.entidad.Transaccion
import com.example.wonderbalance.util.Constantes
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.CategoriaViewModel
import com.example.wonderbalance.viewmodel.ResultadoOperacion
import com.example.wonderbalance.viewmodel.TransaccionViewModel

class FragmentoDetalleTransaccion : Fragment() {

    private var _enlace: FragmentoDetalleTransaccionBinding? = null
    private val enlace get() = _enlace!!
    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val categoriaViewModel: CategoriaViewModel by viewModels()
    private val args: FragmentoDetalleTransaccionArgs by navArgs()
    private var transaccionActual: Transaccion? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _enlace = FragmentoDetalleTransaccionBinding.inflate(inflater, container, false)
        return enlace.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usuarioId = GestorSesion(requireContext()).obtenerUsuarioId()

        // Cargar transacción
        transaccionViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { lista ->
            val transaccion = lista.find { it.id == args.transaccionId }
            transaccion?.let {
                transaccionActual = it
                mostrarDetalle(it, usuarioId)
            }
        }

        // Botón eliminar
        enlace.btnEliminar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar transacción")
                .setMessage("¿Estás seguro? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    transaccionActual?.let { transaccionViewModel.eliminar(it) }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Botón editar (navegar de regreso a transacción con datos)
        enlace.btnEditar.setOnClickListener {
            Toast.makeText(requireContext(), "Función de edición próximamente", Toast.LENGTH_SHORT).show()
        }

        // Observar resultado
        transaccionViewModel.resultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is ResultadoOperacion.Exito -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is ResultadoOperacion.Error -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDetalle(transaccion: Transaccion, usuarioId: Int) {
        val simbolo = GestorSesion(requireContext()).obtenerMoneda()

        if (transaccion.tipo == Constantes.TIPO_GASTO) {
            enlace.txtMonto.text = "-$simbolo %.2f".format(transaccion.monto)
            enlace.txtMonto.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            enlace.txtTipo.text = "GASTO"
            enlace.txtTipo.setBackgroundColor(
                requireContext().getColor(android.R.color.holo_red_light)
            )
        } else {
            enlace.txtMonto.text = "+$simbolo %.2f".format(transaccion.monto)
            enlace.txtMonto.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            enlace.txtTipo.text = "INGRESO"
            enlace.txtTipo.setBackgroundColor(
                requireContext().getColor(android.R.color.holo_green_light)
            )
        }

        enlace.txtFecha.text = transaccion.fecha
        enlace.txtNota.text = transaccion.nota.ifBlank { "—" }

        categoriaViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { categorias ->
            val nombre = categorias.find { it.id == transaccion.categoriaId }?.nombre ?: "Sin categoría"
            enlace.txtCategoria.text = nombre
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}