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
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.MetaViewModel

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
        adaptador = AdaptadorMeta()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}