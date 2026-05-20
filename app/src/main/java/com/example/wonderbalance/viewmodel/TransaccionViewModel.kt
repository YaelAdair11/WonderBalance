package com.example.wonderbalance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.wonderbalance.datos.basededatos.BaseDeDatos
import com.example.wonderbalance.datos.entidad.Transaccion
import com.example.wonderbalance.repositorio.TransaccionRepositorio
import com.example.wonderbalance.repositorio.PresupuestoRepositorio
import com.example.wonderbalance.util.Constantes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

sealed class AlertaPresupuesto {
    data class Peligro(val porcentaje: Int) : AlertaPresupuesto()
    object Excedido : AlertaPresupuesto()
}

class TransaccionViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: TransaccionRepositorio
    private val presupuestoRepositorio: PresupuestoRepositorio
    private val _resultado = MutableLiveData<ResultadoOperacion>()
    val resultado: LiveData<ResultadoOperacion> = _resultado

    private val _alertaPresupuesto = MutableSharedFlow<AlertaPresupuesto>()
    val alertaPresupuesto = _alertaPresupuesto.asSharedFlow()

    private val _transaccionesFiltradas = MutableLiveData<List<Transaccion>?>()
    val transaccionesFiltradas: LiveData<List<Transaccion>?> = _transaccionesFiltradas

    val idsSeleccionados = mutableListOf<Int>()

    init {
        val db = BaseDeDatos.obtenerInstancia(application)
        repositorio = TransaccionRepositorio(db.transaccionDao())
        presupuestoRepositorio = PresupuestoRepositorio(db.presupuestoDao())
    }

    fun obtenerTodas(usuarioId: Int): LiveData<List<Transaccion>> =
        repositorio.obtenerTodas(usuarioId)

    fun obtenerUltimas(usuarioId: Int): LiveData<List<Transaccion>> =
        repositorio.obtenerUltimas(usuarioId)

    fun obtenerPorMes(usuarioId: Int, mes: String): LiveData<List<Transaccion>> =
        repositorio.obtenerPorMes(usuarioId, mes)

    fun obtenerBalanceGeneral(usuarioId: Int): LiveData<Double> =
        repositorio.obtenerBalanceGeneral(usuarioId)

    fun buscar(usuarioId: Int, busqueda: String): LiveData<List<Transaccion>> =
        repositorio.buscar(usuarioId, busqueda)

    fun guardar(transaccion: Transaccion) {
        viewModelScope.launch {
            if (transaccion.monto <= 0) {
                _resultado.value = ResultadoOperacion.Error("El monto debe ser mayor a cero")
                return@launch
            }
            if (transaccion.categoriaId == 0) {
                _resultado.value = ResultadoOperacion.Error("Selecciona una categoría")
                return@launch
            }
            val id = repositorio.insertar(transaccion)
            if (id > 0) {
                if (transaccion.tipo == Constantes.TIPO_GASTO) {
                    verificarLimitePresupuesto(transaccion)
                }
                _resultado.value = ResultadoOperacion.Exito("Transacción guardada")
            } else {
                _resultado.value = ResultadoOperacion.Error("Error al guardar la transacción")
            }
        }
    }

    private suspend fun verificarLimitePresupuesto(transaccion: Transaccion) {
        val cal = Calendar.getInstance()
        val mes = cal.get(Calendar.MONTH) + 1
        val anio = cal.get(Calendar.YEAR)
        val mesString = String.format(Locale.ROOT, "%04d-%02d", anio, mes)

        val presupuesto = presupuestoRepositorio.buscarPorCategoriaYMes(
            transaccion.usuarioId, transaccion.categoriaId, mes, anio
        ) ?: return

        val totalGastado = repositorio.obtenerGastoPorCategoriaYMes(
            transaccion.usuarioId, transaccion.categoriaId, mesString
        )

        val porcentaje = (totalGastado / presupuesto.montoLimite) * 100

        if (porcentaje >= 100) {
            _alertaPresupuesto.emit(AlertaPresupuesto.Excedido)
        } else if (porcentaje >= 80) {
            _alertaPresupuesto.emit(AlertaPresupuesto.Peligro(porcentaje.toInt()))
        }
    }

    fun actualizar(transaccion: Transaccion) {
        viewModelScope.launch {
            if (transaccion.monto <= 0) {
                _resultado.value = ResultadoOperacion.Error("El monto debe ser mayor a cero")
                return@launch
            }
            repositorio.actualizar(transaccion)
            _resultado.value = ResultadoOperacion.Exito("Transacción actualizada")
        }
    }

    fun eliminar(transaccion: Transaccion) {
        viewModelScope.launch {
            repositorio.eliminar(transaccion)
            _resultado.value = ResultadoOperacion.Exito("Transacción eliminada")
        }
    }

    suspend fun buscarPorId(id: Int): Transaccion? {
        return repositorio.buscarPorId(id)
    }

    fun aplicarFiltroCategorias(usuarioId: Int, ids: List<Int>) {
        viewModelScope.launch {
            try {
                idsSeleccionados.clear()
                idsSeleccionados.addAll(ids)
                
                if (ids.isEmpty()) {
                    limpiarFiltros()
                } else {
                    val lista = repositorio.obtenerPorCategorias(usuarioId, ids)
                    _transaccionesFiltradas.value = lista
                }
            } catch (e: Exception) {
                _resultado.value = ResultadoOperacion.Error("Error al filtrar: ${e.message}")
                _transaccionesFiltradas.value = null
            }
        }
    }

    fun limpiarFiltros() {
        idsSeleccionados.clear()
        _transaccionesFiltradas.value = null
    }
}
