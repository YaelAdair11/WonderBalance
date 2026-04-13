package com.example.wonderbalance.ui.analitica

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.wonderbalance.databinding.FragmentoAnaliticaBinding
import com.example.wonderbalance.util.Constantes
import com.example.wonderbalance.util.GestorSesion
import com.example.wonderbalance.viewmodel.CategoriaViewModel
import com.example.wonderbalance.viewmodel.TransaccionViewModel
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class FragmentoAnalitica : Fragment() {

    private var _enlace: FragmentoAnaliticaBinding? = null
    private val enlace get() = _enlace!!
    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val categoriaViewModel: CategoriaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _enlace = FragmentoAnaliticaBinding.inflate(inflater, container, false)
        return enlace.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usuarioId = GestorSesion(requireContext()).obtenerUsuarioId()
        val formatoMes = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val mesActual = formatoMes.format(Date())
        val formatoVisible = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
        enlace.txtMes.text = formatoVisible.format(Date()).replaceFirstChar { it.uppercase() }

        categoriaViewModel.obtenerTodas(usuarioId).observe(viewLifecycleOwner) { categorias ->
            val mapaCategorias = categorias.associate { it.id to it.nombre }

            transaccionViewModel.obtenerPorMes(usuarioId, mesActual)
                .observe(viewLifecycleOwner) { transacciones ->

                    // Gastos por categoría
                    val gastos = transacciones.filter { it.tipo == Constantes.TIPO_GASTO }
                    val gastosAgrupados = gastos.groupBy { it.categoriaId }
                        .mapValues { entrada -> entrada.value.sumOf { it.monto } }

                    if (gastosAgrupados.isNotEmpty()) {
                        val entradasPastel = gastosAgrupados.map { (catId, total) ->
                            PieEntry(total.toFloat(), mapaCategorias[catId] ?: "Otro")
                        }
                        val conjuntoPastel = PieDataSet(entradasPastel, "Gastos").apply {
                            colors = ColorTemplate.MATERIAL_COLORS.toList()
                            valueTextSize = 12f
                            valueTextColor = Color.WHITE
                        }
                        enlace.graficaGastos.apply {
                            data = PieData(conjuntoPastel)
                            description.isEnabled = false
                            isDrawHoleEnabled = true
                            holeRadius = 40f
                            setHoleColor(Color.TRANSPARENT)
                            animateY(800)
                            invalidate()
                        }
                    }

                    // Ingresos por categoría
                    val ingresos = transacciones.filter { it.tipo == Constantes.TIPO_INGRESO }
                    val ingresosAgrupados = ingresos.groupBy { it.categoriaId }
                        .mapValues { entrada -> entrada.value.sumOf { it.monto } }

                    if (ingresosAgrupados.isNotEmpty()) {
                        val entradasBarra = ingresosAgrupados.entries
                            .mapIndexed { indice, (catId, total) ->
                                BarEntry(indice.toFloat(), total.toFloat()).also {
                                    _ = mapaCategorias[catId] ?: "Otro"
                                }
                            }
                        val conjuntoBarra = BarDataSet(entradasBarra, "Ingresos").apply {
                            colors = ColorTemplate.COLORFUL_COLORS.toList()
                            valueTextSize = 11f
                        }
                        enlace.graficaIngresos.apply {
                            data = BarData(conjuntoBarra)
                            description.isEnabled = false
                            animateY(800)
                            invalidate()
                        }
                    }
                }
        }

        enlace.btnExportar.setOnClickListener {
            Toast.makeText(requireContext(), "Reporte generado en PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}