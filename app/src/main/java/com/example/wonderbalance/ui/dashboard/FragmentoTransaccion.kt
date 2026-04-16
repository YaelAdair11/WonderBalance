package com.example.wonderbalance.ui.transaccion

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.wonderbalance.R
import com.example.wonderbalance.databinding.DialogoNuevaCategoriaBinding
import com.example.wonderbalance.databinding.FragmentoTransaccionBinding
import com.example.wonderbalance.datos.entidad.Categoria
import com.example.wonderbalance.datos.entidad.Transaccion
import com.example.wonderbalance.util.Constantes
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.CategoriaViewModel
import com.example.wonderbalance.viewmodel.ResultadoOperacion
import com.example.wonderbalance.viewmodel.TransaccionViewModel
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

        // Botón regresar
        enlace.btnRegresar.setOnClickListener {
            findNavController().popBackStack()
        }

        // Selector de tipo: Gasto activo por defecto con color
        enlace.btnGasto.isChecked = true
        actualizarColorBotonTipo(Constantes.TIPO_GASTO)

        enlace.grupoTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                tipoSeleccionado = if (checkedId == R.id.btn_gasto)
                    Constantes.TIPO_GASTO else Constantes.TIPO_INGRESO
                actualizarColorBotonTipo(tipoSeleccionado)
                categoriaSeleccionada = null
                enlace.dropdownCategoria.setText("")
                cargarCategorias(usuarioId)
            }
        }

        // Cargar categorías iniciales
        cargarCategorias(usuarioId)

        // Botón agregar nueva categoría
        enlace.btnAgregarCategoria.setOnClickListener {
            mostrarDialogoNuevaCategoria(usuarioId)
        }

        // Date picker
        enlace.campoFecha.setEndIconOnClickListener { mostrarDatePicker() }
        enlace.etFecha.setOnClickListener { mostrarDatePicker() }

        // Validación en tiempo real del monto — deshabilitar guardar si es inválido
        enlace.etMonto.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val texto = s.toString().trim()
                val monto = texto.toDoubleOrNull()
                if (texto.isNotBlank() && (monto == null || monto <= 0)) {
                    enlace.campoMonto.error = "El monto debe ser mayor a cero"
                    enlace.btnGuardar.isEnabled = false
                } else {
                    enlace.campoMonto.error = null
                    enlace.btnGuardar.isEnabled = true
                }
            }
        })

        // Botón guardar
        enlace.btnGuardar.setOnClickListener {
            guardarTransaccion(usuarioId)
        }

        // Observar resultado de guardar transacción
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

        // Observar resultado de crear categoría
        categoriaViewModel.resultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is ResultadoOperacion.Exito -> {
                    Toast.makeText(requireContext(), "Categoría creada", Toast.LENGTH_SHORT).show()
                }
                is ResultadoOperacion.Error -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarColorBotonTipo(tipo: String) {
        // Borramos la línea de 'colorActivo' que causaba el error
        if (tipo == Constantes.TIPO_GASTO) {
            enlace.btnGasto.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#7F77DD")
                )
            enlace.btnGasto.setTextColor(android.graphics.Color.WHITE)
            enlace.btnIngreso.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            enlace.btnIngreso.setTextColor(android.graphics.Color.parseColor("#7F77DD"))
        } else {
            enlace.btnIngreso.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#5DCAA5")
                )
            enlace.btnIngreso.setTextColor(android.graphics.Color.WHITE)
            enlace.btnGasto.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            enlace.btnGasto.setTextColor(android.graphics.Color.parseColor("#7F77DD"))
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
                    enlace.txtErrorCategoria.visibility = View.GONE
                }
            }
    }

    private fun mostrarDialogoNuevaCategoria(usuarioId: Int) {
        val dialogoEnlace = DialogoNuevaCategoriaBinding.inflate(layoutInflater)

        // Pre-seleccionar el tipo actual
        if (tipoSeleccionado == Constantes.TIPO_GASTO) {
            dialogoEnlace.btnTipoGasto.isChecked = true
        } else {
            dialogoEnlace.btnTipoIngreso.isChecked = true
        }

        val dialogo = AlertDialog.Builder(requireContext())
            .setView(dialogoEnlace.root)
            .create()

        dialogo.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogoEnlace.btnCancelarCat.setOnClickListener {
            dialogo.dismiss()
        }

        dialogoEnlace.btnGuardarCat.setOnClickListener {
            val nombre = dialogoEnlace.etNombreCat.text.toString().trim()
            if (nombre.isBlank()) {
                dialogoEnlace.campoNombreCat.error = "Escribe un nombre"
                return@setOnClickListener
            }
            val tipoCat = if (dialogoEnlace.btnTipoGasto.isChecked)
                Constantes.TIPO_GASTO else Constantes.TIPO_INGRESO

            val nuevaCategoria = Categoria(
                nombre = nombre,
                tipo = tipoCat,
                usuarioId = usuarioId
            )
            categoriaViewModel.guardar(nuevaCategoria)
            dialogo.dismiss()

            // Si el tipo coincide con el seleccionado, autoseleccionar la nueva
            if (tipoCat == tipoSeleccionado) {
                enlace.dropdownCategoria.setText(nombre)
                // Se actualizará automáticamente por el observer de cargarCategorias
            }
        }

        dialogo.show()
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                enlace.etFecha.setText("%04d-%02d-%02d".format(anio, mes + 1, dia))
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

        // CP-01 / CP-03: Validar monto
        if (montoTexto.isBlank()) {
            enlace.campoMonto.error = "Ingresa un monto"
            return
        }
        val monto = montoTexto.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            // CP-02 / CU-04-CP-03: monto inválido o negativo
            enlace.campoMonto.error = "El monto debe ser mayor a cero"
            enlace.btnGuardar.isEnabled = false
            return
        }
        enlace.campoMonto.error = null

        // CP-03 / CU-04-CP-02: Categoría obligatoria con indicador visual
        if (categoriaSeleccionada == null) {
            enlace.txtErrorCategoria.visibility = View.VISIBLE
            enlace.campoCategoria.error = "Selecciona una categoría"
            return
        }
        enlace.txtErrorCategoria.visibility = View.GONE
        enlace.campoCategoria.error = null

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