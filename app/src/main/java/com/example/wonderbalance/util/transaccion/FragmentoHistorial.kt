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
import com.example.wonderbalance.databinding.FragmentoHistorialBinding
import com.example.wonderbalance.ui.dashboard.AdaptadorTransaccion
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.CategoriaViewModel
import com.example.wonderbalance.viewmodel.TransaccionViewModel
class FragmentoHistorial : Fragment() {

    private var _enlace: FragmentoHistorialBinding? = null
    private val enlace get() = _enlace!!
    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val categoriaViewModel: CategoriaViewModel by viewModels()
    private lateinit var adaptador: AdaptadorTransaccion
    private lateinit var gestorSesion: GestorSesion

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

        adaptador = AdaptadorTransaccion { transaccion ->
            val bundle = android.os.Bundle().apply {
                putInt("transaccionId", transaccion.id)
            }
            findNavController().navigate(com.example.wonderbalance.R.id.accion_historial_a_detalle, bundle)
        }

        enlace.listaHistorial.layoutManager = LinearLayoutManager(requireContext())
        enlace.listaHistorial.adapter = adaptador

        // Cargar categorías para mostrar nombres
        categoriaViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { categorias ->
            val mapa = categorias.associate { it.id to it.nombre }
            adaptador.actualizarCategorias(mapa)
        }

        // Cargar todas las transacciones
        transaccionViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { lista ->
            mostrarLista(lista.isNullOrEmpty())
            adaptador.submitList(lista)
        }

        // Búsqueda en tiempo real
        enlace.etBusqueda.addTextChangedListener { texto ->
            val busqueda = texto.toString().trim()
            if (busqueda.isBlank()) {
                transaccionViewModel.obtenerTodas(usuarioId)
                    .observe(viewLifecycleOwner) { lista ->
                        mostrarLista(lista.isNullOrEmpty())
                        adaptador.submitList(lista)
                    }
            } else {
                transaccionViewModel.buscar(usuarioId, busqueda)
                    .observe(viewLifecycleOwner) { lista ->
                        mostrarLista(lista.isNullOrEmpty())
                        adaptador.submitList(lista)
                    }
            }
        }
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