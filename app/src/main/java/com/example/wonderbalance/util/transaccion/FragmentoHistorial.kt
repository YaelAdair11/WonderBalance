package com.example.wonderbalance.ui.transaccion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wonderbalance.R
import com.example.wonderbalance.databinding.FragmentoHistorialBinding
import com.example.wonderbalance.ui.dashboard.AdaptadorTransaccion
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.CategoriaViewModel
import com.example.wonderbalance.viewmodel.TransaccionViewModel
import com.example.wonderbalance.datos.entidad.Transaccion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FragmentoHistorial : Fragment() {

    private var _enlace: FragmentoHistorialBinding? = null
    private val enlace get() = _enlace!!
    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val categoriaViewModel: CategoriaViewModel by viewModels()
    private lateinit var adaptador: AdaptadorTransaccion
    private lateinit var gestorSesion: GestorSesion

    private var listaCompletaTransacciones = listOf<Transaccion>()

    private var mapaCategoriasActual = mapOf<Int, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _enlace = FragmentoHistorialBinding.inflate(inflater, container, false)
        return enlace.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gestorSesion = GestorSesion(requireContext())
        val usuarioId = gestorSesion.obtenerUsuarioId()

        // 1. Configuración del adaptador y envío del ID
        adaptador = AdaptadorTransaccion { transaccion ->
            val paquete = android.os.Bundle().apply {
                putInt("transaccionId", transaccion.id)
            }
            // Asegúrate de usar la acción correcta de tu archivo de navegación
            findNavController().navigate(R.id.accion_historial_a_detalle, paquete)
        }

        enlace.listaHistorial.layoutManager = LinearLayoutManager(requireContext())
        enlace.listaHistorial.adapter = adaptador

        // 2. Cargar categorías y guardarlas en memoria para el buscador
        categoriaViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { categorias ->
            mapaCategoriasActual = categorias.associate { it.id to it.nombre }
            adaptador.actualizarCategorias(mapaCategoriasActual)
            filtrarLista(enlace.etBusqueda.text.toString())
        }

        // 3. Cargar todas las transacciones en memoria
        transaccionViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { lista ->
            listaCompletaTransacciones = lista ?: emptyList()
            filtrarLista(enlace.etBusqueda.text.toString())
        }

        // 4. Búsqueda en tiempo real desde la memoria
        enlace.etBusqueda.addTextChangedListener { texto ->
            filtrarLista(texto.toString())
        }

        // --- ADICIÓN CU-16: Listener para el botón de filtros ---
        enlace.btnFiltrar.setOnClickListener {
            mostrarDialogoCategorias()
        }

        // --- ADICIÓN CU-16: Observar cambios en el filtro de categorías ---
        transaccionViewModel.transaccionesFiltradas.observe(viewLifecycleOwner) {
            filtrarLista(enlace.etBusqueda.text.toString())
        }
    }

    // 5. Función que filtra por nota o por nombre de categoría (Lógica Unificada)
    private fun filtrarLista(textoConsulta: String) {
        val busqueda = textoConsulta.trim()
        
        // Usamos el filtro de categorías como base si existe
        val listaBase = transaccionViewModel.transaccionesFiltradas.value ?: listaCompletaTransacciones

        if (busqueda.isBlank()) {
            mostrarLista(listaBase.isEmpty())
            adaptador.submitList(listaBase)
            return
        }

        val listaFiltrada = listaBase.filter { transaccion ->
            val nombreCategoria = mapaCategoriasActual[transaccion.categoriaId] ?: ""
            nombreCategoria.contains(busqueda, ignoreCase = true) ||
                    (transaccion.nota?.contains(busqueda, ignoreCase = true) == true)
        }

        mostrarLista(listaFiltrada.isEmpty())
        adaptador.submitList(listaFiltrada)
    }

    // --- ADICIÓN CU-16: Menú de selección de categorías ---
    private fun mostrarDialogoCategorias() {
        val categoriasArr = mapaCategoriasActual.values.toTypedArray()
        val idsArr = mapaCategoriasActual.keys.toIntArray()
        
        val seleccionados = BooleanArray(categoriasArr.size) { i ->
            transaccionViewModel.idsSeleccionados.contains(idsArr[i])
        }

        val temporalSeleccion = transaccionViewModel.idsSeleccionados.toMutableList()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtrar por categoría")
            .setMultiChoiceItems(categoriasArr, seleccionados) { _, index, isChecked ->
                if (isChecked) temporalSeleccion.add(idsArr[index])
                else temporalSeleccion.remove(idsArr[index])
            }
            .setPositiveButton("Aplicar") { _, _ ->
                val usuarioId = gestorSesion.obtenerUsuarioId()
                transaccionViewModel.aplicarFiltroCategorias(usuarioId, temporalSeleccion)
            }
            .setNeutralButton("Limpiar") { _, _ ->
                transaccionViewModel.limpiarFiltros()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarLista(estaVacia: Boolean) {
        enlace.listaHistorial.visibility = if (estaVacia) View.GONE else View.VISIBLE
        enlace.txtSinResultados.visibility = if (estaVacia) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}