package com.example.wonderbalance.ui.transaccion

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.wonderbalance.R
import com.example.wonderbalance.databinding.DialogoNuevaCategoriaBinding
import com.example.wonderbalance.databinding.FragmentoTransaccionBinding
import com.example.wonderbalance.datos.entidad.Categoria
import com.example.wonderbalance.datos.entidad.Transaccion
import com.example.wonderbalance.util.Constantes
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.AlertaPresupuesto
import com.example.wonderbalance.viewmodel.CategoriaViewModel
import com.example.wonderbalance.viewmodel.ResultadoOperacion
import com.example.wonderbalance.viewmodel.TransaccionViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    private var idTransaccionEdicion: Int = -1

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

        idTransaccionEdicion = arguments?.getInt("transaccionId", -1) ?: -1

        if (idTransaccionEdicion != -1) {
            enlace.txtTitulo.text = "Editar Transacción"
            enlace.btnGuardar.text = "Actualizar"

            viewLifecycleOwner.lifecycleScope.launch {
                val transaccion = transaccionViewModel.buscarPorId(idTransaccionEdicion)

                if (transaccion != null) {
                    enlace.etMonto.setText(transaccion.monto.toString())
                    enlace.etFecha.setText(transaccion.fecha)
                    enlace.etNota.setText(transaccion.nota ?: "")

                    tipoSeleccionado = transaccion.tipo
                    if (tipoSeleccionado == Constantes.TIPO_GASTO) {
                        enlace.btnGasto.isChecked = true
                    } else {
                        enlace.btnIngreso.isChecked = true
                    }
                    actualizarColorBotonTipo(tipoSeleccionado)

                    cargarCategorias(usuarioId, transaccion.categoriaId)
                }
            }
        } else {
            val formatoFecha = SimpleDateFormat(Constantes.FORMATO_FECHA, Locale.getDefault())
            enlace.etFecha.setText(formatoFecha.format(Date()))

            cargarCategorias(usuarioId, null)
            enlace.btnGasto.isChecked = true
            actualizarColorBotonTipo(Constantes.TIPO_GASTO)
        }

        enlace.btnRegresar.setOnClickListener {
            findNavController().popBackStack()
        }

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

        enlace.btnAgregarCategoria.setOnClickListener {
            mostrarDialogoNuevaCategoria(usuarioId)
        }

        enlace.campoFecha.setEndIconOnClickListener { mostrarDatePicker() }
        enlace.etFecha.setOnClickListener { mostrarDatePicker() }

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

        enlace.btnGuardar.setOnClickListener {
            guardarTransaccion(usuarioId)
        }

        transaccionViewModel.resultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is ResultadoOperacion.Exito -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
                    if (findNavController().currentDestination?.id == R.id.fragmentoTransaccion) {
                        if(idTransaccionEdicion != -1) {
                            findNavController().popBackStack()
                        } else {
                            findNavController().navigate(R.id.accion_transaccion_a_dashboard)
                        }
                    }
                }
                is ResultadoOperacion.Error -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }

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

        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.alertaPresupuesto.collectLatest { alerta ->
                manejarAlertaPresupuesto(alerta)
            }
        }
    }

    private fun manejarAlertaPresupuesto(alerta: AlertaPresupuesto) {
        val titulo = "Alerta de Presupuesto"
        val mensaje = when (alerta) {
            is AlertaPresupuesto.Excedido -> "Has superado el 100% de tu presupuesto."
            is AlertaPresupuesto.Peligro -> "Has alcanzado el ${alerta.porcentaje}% de tu presupuesto."
        }

        if (tienePermisoNotificaciones()) {
            mostrarNotificacionLocal(titulo, mensaje)
        } else {
            Snackbar.make(enlace.root, mensaje, Snackbar.LENGTH_LONG)
                .setBackgroundTint(android.graphics.Color.RED)
                .setTextColor(android.graphics.Color.WHITE)
                .show()
        }
    }

    private fun tienePermisoNotificaciones(): Boolean {
        val manager = NotificationManagerCompat.from(requireContext())
        if (!manager.areNotificationsEnabled()) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun mostrarNotificacionLocal(titulo: String, mensaje: String) {
        val canalId = "alerta_presupuesto"
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Alertas de Gasto", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(canal)
        }

        val builder = NotificationCompat.Builder(requireContext(), canalId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun actualizarColorBotonTipo(tipo: String) {
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

    private fun cargarCategorias(usuarioId: Int, categoriaIdPreseleccionada: Int? = null) {
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

                if (categoriaIdPreseleccionada != null) {
                    categoriaSeleccionada = categorias.find { it.id == categoriaIdPreseleccionada }
                    categoriaSeleccionada?.let { cat ->
                        enlace.dropdownCategoria.setText(cat.nombre, false)
                        enlace.txtErrorCategoria.visibility = View.GONE
                        enlace.campoCategoria.error = null
                    }
                } else {
                    val textoActual = enlace.dropdownCategoria.text.toString().trim()
                    if (textoActual.isNotBlank()) {
                        categoriaSeleccionada = listaCategorias.find { it.nombre.equals(textoActual, ignoreCase = true) }
                        if (categoriaSeleccionada != null) {
                            enlace.txtErrorCategoria.visibility = View.GONE
                            enlace.campoCategoria.error = null
                        }
                    }
                }

                enlace.dropdownCategoria.setOnItemClickListener { parent, _, posicion, _ ->
                    val nombreSeleccionado = parent.getItemAtPosition(posicion) as String
                    categoriaSeleccionada = listaCategorias.find { it.nombre == nombreSeleccionado }
                    enlace.txtErrorCategoria.visibility = View.GONE
                    enlace.campoCategoria.error = null
                }
            }
    }

    private fun mostrarDialogoNuevaCategoria(usuarioId: Int) {
        val dialogoEnlace = DialogoNuevaCategoriaBinding.inflate(layoutInflater)

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

            if (tipoCat == tipoSeleccionado) {
                enlace.dropdownCategoria.setText(nombre, false)
            }
        }

        dialogo.show()
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                enlace.etFecha.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mes + 1, dia))
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
            enlace.campoMonto.error = "El monto debe ser mayor a cero"
            enlace.btnGuardar.isEnabled = false
            return
        }
        enlace.campoMonto.error = null

        val textoCategoria = enlace.dropdownCategoria.text.toString().trim()
        if (categoriaSeleccionada == null && textoCategoria.isNotBlank()) {
            categoriaSeleccionada =
                listaCategorias.find { it.nombre.equals(textoCategoria, ignoreCase = true) }
        }

        if (categoriaSeleccionada == null) {
            enlace.txtErrorCategoria.visibility = View.VISIBLE
            enlace.campoCategoria.error = "Selecciona una categoría"
            return
        }
        enlace.txtErrorCategoria.visibility = View.GONE
        enlace.campoCategoria.error = null

        val transaccion = Transaccion(
            id = if (idTransaccionEdicion != -1) idTransaccionEdicion else 0,
            monto = monto,
            tipo = tipoSeleccionado,
            categoriaId = categoriaSeleccionada!!.id,
            fecha = fecha,
            nota = nota,
            usuarioId = usuarioId
        )

        if (idTransaccionEdicion != -1) {
            transaccionViewModel.actualizar(transaccion)
        } else {
            transaccionViewModel.guardar(transaccion)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}
