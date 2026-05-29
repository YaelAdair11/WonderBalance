package com.example.wonderbalance.ui.meta

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.wonderbalance.databinding.FragmentoNuevaMetaBinding
import com.example.wonderbalance.datos.entidad.Meta
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.MetaViewModel
import com.example.wonderbalance.viewmodel.ResultadoOperacion
import java.util.Calendar

class FragmentoNuevaMeta : Fragment() {

    private var _enlace: FragmentoNuevaMetaBinding? = null
    private val enlace get() = _enlace!!
    private val metaViewModel: MetaViewModel by viewModels()

    // variable para guardar la meta original si estamos en modo edicion
    private var metaEnEdicion: Meta? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _enlace = FragmentoNuevaMetaBinding.inflate(inflater, container, false)
        return enlace.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usuarioId = GestorSesion(requireContext()).obtenerUsuarioId()

        // edición???
        val metaIdRecibida = arguments?.getInt("metaId") ?: -1

        if (metaIdRecibida != -1) {
            enlace.btnCrear.text = "Guardar Cambios"

            metaViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { lista ->
                val meta = lista.find { it.id == metaIdRecibida }
                if (meta != null && metaEnEdicion == null) {
                    metaEnEdicion = meta
                    enlace.etNombre.setText(meta.nombre)

                    val montoSinDecimalesInnecesarios = if (meta.montoObjetivo % 1 == 0.0)
                        meta.montoObjetivo.toInt().toString() else meta.montoObjetivo.toString()

                    enlace.etMonto.setText(montoSinDecimalesInnecesarios)
                    enlace.etFecha.setText(meta.fechaLimite)
                }
            }
        }

        enlace.campoFecha.setEndIconOnClickListener { mostrarDatePicker() }
        enlace.etFecha.setOnClickListener { mostrarDatePicker() }

        enlace.btnCrear.setOnClickListener {
            val nombre = enlace.etNombre.text.toString().trim()
            val montoTexto = enlace.etMonto.text.toString().trim()
            val fecha = enlace.etFecha.text.toString().trim()

            if (nombre.isBlank() || montoTexto.isBlank() || fecha.isBlank()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val monto = montoTexto.toDoubleOrNull()
            if (monto == null || monto <= 0) {
                enlace.campoMonto.error = "Monto no válido"
                return@setOnClickListener
            }

            if (metaIdRecibida != -1 && metaEnEdicion != null) {
                val metaActualizada = metaEnEdicion!!.copy(
                    nombre = nombre,
                    montoObjetivo = monto,
                    fechaLimite = fecha
                )
                metaViewModel.actualizar(metaActualizada)
            } else {
                val metaNueva = Meta(
                    nombre = nombre,
                    montoObjetivo = monto,
                    fechaLimite = fecha,
                    usuarioId = usuarioId
                )
                metaViewModel.guardar(metaNueva)
            }
        }

        metaViewModel.resultado.observe(viewLifecycleOwner) { resultado ->
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

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                val fechaFutura = Calendar.getInstance()
                fechaFutura.set(anio, mes, dia)
                if (fechaFutura.before(Calendar.getInstance())) {
                    Toast.makeText(requireContext(), "Selecciona una fecha futura", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }
                enlace.etFecha.setText("%04d-%02d-%02d".format(anio, mes + 1, dia))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}