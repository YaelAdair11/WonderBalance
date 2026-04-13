package com.example.wonderbalance.ui.transaccion

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.wonderbalance.R
import com.example.wonderbalance.databinding.FragmentoTransaccionBinding
import com.example.wonderbalance.datos.entidad.Categoria
import com.example.wonderbalance.datos.entidad.Transaccion
import com.example.wonderbalance.util.Constantes
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.ResultadoOperacion
import com.example.wonderbalance.viewmodel.TransaccionViewModel
import com.example.wonderbalance.viewmodel.CategoriaViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentoTransaccion : Fragment() {

    private var _enlace: FragmentoTransaccionBinding? = null
    private val enlace get() = _enlace!!
    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val categoriaViewModel: CategoriaViewModel by viewModels()
    private lateinit var gestorSesion: GestorSesion
    private var categoriaSeleccionada: Categoria? = null
    private var listaCategorias: List<Categoria> = emptyList()
    private var tipoSeleccionado = Constantes.TIPO_GASTO

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _enlace = FragmentoTransaccionBinding.inflate(inflater, container, false)
        return enlace.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gestorSesion = GestorSesion(requireContext())
        val usuarioId = gestorSesion.obtenerUsuarioId()

        // Fecha de hoy por defecto
        val formatoFecha = SimpleDateFormat(Constantes.FORMATO_FECHA, Locale.getDefault())
        enlace.etFecha.setText(formatoFecha.format(Date()))

        // Selector de tipo
        enlace.btnGasto.isChecked = true
        enlace.grupoTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                tipoSeleccionado = if (checkedId == R.id.btn_gasto)
                    Constantes.TIPO_GASTO else Constantes.TIPO_INGRESO
                cargarCategorias(usuarioId)
            }
        }

        // Cargar categorías iniciales
        cargarCategorias(usuarioId)

        // Date picker
        enlace.campoFecha.setEndIconOnClickListener {
            mostrarDatePicker()
        }
        enlace.etFecha.setOnClickListener {
            mostrarDatePicker()
        }

        // Botón guardar
        enlace.btnGuardar.setOnClickListener {
            guardarTransaccion(usuarioId)
        }

        // Observar resultado
        transaccionViewModel.resultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is ResultadoOperacion.Exito -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.accion_transaccion_a_dashboard)
                }
                is ResultadoOperacion.Error -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarCategorias(usuarioId: Int) {
        categoriaViewModel.obtenerPorTipo(usuarioId, tipoSeleccionado)
            .observe(viewLifecycleOwner) { categorias ->
                listaCategorias = categorias
                val nombres = categorias.map { it.nombre }
                val adaptador = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    nombres
                )
                enlace.dropdownCategoria.setAdapter(adaptador)
                enlace.dropdownCategoria.setOnItemClickListener { _, _, posicion, _ ->
                    categoriaSeleccionada = listaCategorias[posicion]
                }
            }
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                val fechaFormateada = "%04d-%02d-%02d".format(anio, mes + 1, dia)
                enlace.etFecha.setText(fechaFormateada)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun guardarTransaccion(usuarioId: Int) {
        val montoTexto = enlace.etMonto.text.toString().trim()
        val fecha = enlace.etFecha.text.toString().trim()
        val nota = enlace.etNota.text.toString().trim()

        if (montoTexto.isBlank()) {
            enlace.campoMonto.error = "Ingresa un monto"
            return
        }
        val monto = montoTexto.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            enlace.campoMonto.error = "Monto no válido"
            return
        }
        enlace.campoMonto.error = null

        if (categoriaSeleccionada == null) {
            Toast.makeText(requireContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        val transaccion = Transaccion(
            monto = monto,
            tipo = tipoSeleccionado,
            categoriaId = categoriaSeleccionada!!.id,
            fecha = fecha,
            nota = nota,
            usuarioId = usuarioId
        )
        transaccionViewModel.guardar(transaccion)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}