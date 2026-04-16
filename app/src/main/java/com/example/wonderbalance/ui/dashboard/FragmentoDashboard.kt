package com.example.wonderbalance.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wonderbalance.R
import com.example.wonderbalance.databinding.FragmentoDashboardBinding
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.TransaccionViewModel
import com.example.wonderbalance.viewmodel.CategoriaViewModel

class FragmentoDashboard : Fragment() {

    private var _enlace: FragmentoDashboardBinding? = null
    private val enlace get() = _enlace!!

    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val categoriaViewModel: CategoriaViewModel by viewModels() // <-- LÍNEA NUEVA

    private lateinit var adaptador: AdaptadorTransaccion
    private lateinit var gestorSesion: GestorSesion

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _enlace = FragmentoDashboardBinding.inflate(inflater, container, false)
        return enlace.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gestorSesion = GestorSesion(requireContext())
        val usuarioId = gestorSesion.obtenerUsuarioId()

        // Saludo
        enlace.txtSaludo.text = "Hola, ${gestorSesion.obtenerUsuarioNombre()}"

        // RecyclerView
        adaptador = AdaptadorTransaccion { transaccion ->
            // navegar al detalle (lo implementaremos después)
        }
        enlace.listaTransacciones.layoutManager = LinearLayoutManager(requireContext())
        enlace.listaTransacciones.adapter = adaptador

        // NUEVO: Observar categorías y pasarlas al adaptador
        categoriaViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { categorias ->
            val mapaCategorias = categorias.associate { it.id to it.nombre }
            adaptador.actualizarCategorias(mapaCategorias)
        }

        // Observar balance
        transaccionViewModel.obtenerBalanceGeneral(usuarioId)
            .observe(viewLifecycleOwner) { balance ->
                val simbolo = gestorSesion.obtenerMoneda()
                enlace.txtBalance.text = "$simbolo %.2f".format(balance ?: 0.0)
                enlace.txtBalance.setTextColor(
                    if ((balance ?: 0.0) < 0)
                        requireContext().getColor(android.R.color.holo_red_dark)
                    else
                        requireContext().getColor(android.R.color.white)
                )
            }

        // Observar últimas transacciones
        transaccionViewModel.obtenerUltimas(usuarioId)
            .observe(viewLifecycleOwner) { lista ->
                if (lista.isNullOrEmpty()) {
                    enlace.listaTransacciones.visibility = View.GONE
                    enlace.txtSinTransacciones.visibility = View.VISIBLE
                } else {
                    enlace.listaTransacciones.visibility = View.VISIBLE
                    enlace.txtSinTransacciones.visibility = View.GONE
                    adaptador.submitList(lista)
                }
            }

        // Botones
        enlace.fabNuevaTransaccion.setOnClickListener {
            findNavController().navigate(R.id.accion_dashboard_a_transaccion)
        }

        enlace.fabVerHistorial.setOnClickListener {
            findNavController().navigate(R.id.accion_dashboard_a_historial)
        }

        enlace.fabAnalitica.setOnClickListener {
            findNavController().navigate(R.id.fragmentoAnalitica)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}