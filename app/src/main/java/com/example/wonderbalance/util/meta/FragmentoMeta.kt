package com.example.wonderbalance.ui.meta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wonderbalance.R
import com.example.wonderbalance.databinding.FragmentoMetaBinding
import com.example.wonderbalance.datos.entidad.Meta
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.MetaViewModel

import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast

class FragmentoMeta : Fragment() {

    private var _enlace: FragmentoMetaBinding? = null
    private val enlace get() = _enlace!!
    private val metaViewModel: MetaViewModel by viewModels()
    private lateinit var adaptador: AdaptadorMeta

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _enlace = FragmentoMetaBinding.inflate(inflater, container, false)
        return enlace.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usuarioId = GestorSesion(requireContext()).obtenerUsuarioId()

        adaptador = AdaptadorMeta(
            alHacerClic = { metaSeleccionada ->
                mostrarDialogoAbono(metaSeleccionada)
            },
            alMantenerPresionado = { metaSeleccionada ->
                mostrarMenuOpciones(metaSeleccionada)
            }
        )

        enlace.listaMetas.layoutManager = LinearLayoutManager(requireContext())
        enlace.listaMetas.adapter = adaptador

        metaViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { lista ->
            if (lista.isNullOrEmpty()) {
                enlace.listaMetas.visibility = View.GONE
                enlace.txtSinMetas.visibility = View.VISIBLE
            } else {
                enlace.listaMetas.visibility = View.VISIBLE
                enlace.txtSinMetas.visibility = View.GONE
                adaptador.submitList(lista)
            }
        }

        enlace.fabNuevaMeta.setOnClickListener {
            findNavController().navigate(R.id.accion_meta_a_nueva)
        }
    }

    private fun mostrarMenuOpciones(meta: Meta) {
        val opciones = arrayOf("Editar Meta", "Eliminar Meta")

        AlertDialog.Builder(requireContext())
            .setTitle("Opciones: ${meta.nombre}")
            .setItems(opciones) { _, indiceSeleccionado ->
                when (indiceSeleccionado) {
                    0 -> editarMeta(meta)
                    1 -> confirmarEliminacion(meta)
                }
            }
            .show()
    }

    private fun confirmarEliminacion(meta: Meta) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Meta")
            .setMessage("¿Estás seguro de que deseas eliminar la meta '${meta.nombre}'? Perderás el registro de este ahorro.")
            .setPositiveButton("Eliminar") { _, _ ->
                metaViewModel.eliminar(meta)
                Toast.makeText(requireContext(), "Meta eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editarMeta(meta: Meta) {
        // La forma más elegante es reutilizar tu FragmentoNuevaMeta
        // Le pasamos el ID de la meta en un Bundle
        val paquete = Bundle().apply {
            putInt("metaId", meta.id)
        }

        findNavController().navigate(R.id.accion_meta_a_nueva, paquete)
    }

    private fun mostrarDialogoAbono(meta: Meta) {
        val constructor = AlertDialog.Builder(requireContext())
        constructor.setTitle("Abonar a: ${meta.nombre}")

        val entradaDinero = EditText(requireContext())
        entradaDinero.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        entradaDinero.hint = "Ej. 500.00"

        val parametrosMárgen = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        parametrosMárgen.setMargins(50, 20, 50, 0)
        entradaDinero.layoutParams = parametrosMárgen

        // Metemos el input en un contenedor para aplicar el margen
        val contenedor = android.widget.FrameLayout(requireContext())
        contenedor.addView(entradaDinero)
        constructor.setView(contenedor)

        //boton guardar
        constructor.setPositiveButton("Abonar") { _, _ ->
            val textoIngresado = entradaDinero.text.toString()
            if (textoIngresado.isNotBlank()) {
                val cantidadAAbonar = textoIngresado.toDoubleOrNull()

                if (cantidadAAbonar != null && cantidadAAbonar > 0) {
                    metaViewModel.agregarAbono(meta.id, cantidadAAbonar)
                    Toast.makeText(requireContext(), "¡Abono registrado! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Por favor ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Botón para arrepentirse
        constructor.setNegativeButton("Cancelar", null)

        constructor.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}