package com.example.wonderbalance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.wonderbalance.datos.basededatos.BaseDeDatos
import com.example.wonderbalance.datos.entidad.Transaccion
import com.example.wonderbalance.repositorio.TransaccionRepositorio
import kotlinx.coroutines.launch

class TransaccionViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: TransaccionRepositorio
    private val _resultado = MutableLiveData<ResultadoOperacion>()
    val resultado: LiveData<ResultadoOperacion> = _resultado

    // --- ADICIÓN PARA CU-16 ---
    private val _transaccionesFiltradas = MutableLiveData<List<Transaccion>?>()
    val transaccionesFiltradas: LiveData<List<Transaccion>?> = _transaccionesFiltradas

    // Guarda los IDs seleccionados para mantener el estado del diálogo
    val idsSeleccionados = mutableListOf<Int>()

    init {
        val db = BaseDeDatos.obtenerInstancia(application)
        repositorio = TransaccionRepositorio(db.transaccionDao())
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
                _resultado.value = ResultadoOperacion.Exito("Transacción guardada")
            } else {
                _resultado.value = ResultadoOperacion.Error("Error al guardar la transacción")
            }
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

    // --- ADICIÓN PARA CU-16 ---
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
                // Ex-01: Error de actualización de UI
                _resultado.value = ResultadoOperacion.Error("Error al filtrar: ${e.message}")
                _transaccionesFiltradas.value = null // Dispara recarga completa en el Fragment
            }
        }
    }

    fun limpiarFiltros() {
        idsSeleccionados.clear()
        _transaccionesFiltradas.value = null // Indica al Fragment que use la lista completa
    }
}
