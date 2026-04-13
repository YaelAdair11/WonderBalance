package com.example.wonderbalance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.wonderbalance.datos.basededatos.BaseDeDatos
import com.example.wonderbalance.datos.entidad.Usuario
import com.example.wonderbalance.repositorio.UsuarioRepositorio
import kotlinx.coroutines.launch

class UsuarioViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: UsuarioRepositorio
    private val _resultadoRegistro = MutableLiveData<ResultadoOperacion>()
    val resultadoRegistro: LiveData<ResultadoOperacion> = _resultadoRegistro

    private val _resultadoSesion = MutableLiveData<ResultadoOperacion>()
    val resultadoSesion: LiveData<ResultadoOperacion> = _resultadoSesion

    private val _usuarioActual = MutableLiveData<Usuario?>()
    val usuarioActual: LiveData<Usuario?> = _usuarioActual

    init {
        val db = BaseDeDatos.obtenerInstancia(application)
        repositorio = UsuarioRepositorio(db.usuarioDao())
    }

    fun registrar(nombre: String, correo: String, contrasena: String) {
        viewModelScope.launch {
            val existente = repositorio.buscarPorCorreo(correo)
            if (existente != null) {
                _resultadoRegistro.value = ResultadoOperacion.Error("El correo ya está registrado")
                return@launch
            }
            val nuevoUsuario = Usuario(nombre = nombre, correo = correo, contrasena = contrasena)
            val id = repositorio.registrar(nuevoUsuario)
            if (id > 0) {
                val usuario = repositorio.buscarPorId(id.toInt())
                _usuarioActual.value = usuario
                _resultadoRegistro.value = ResultadoOperacion.Exito("Registro exitoso")
            } else {
                _resultadoRegistro.value = ResultadoOperacion.Error("Error al registrar")
            }
        }
    }

    fun iniciarSesion(correo: String, contrasena: String) {
        viewModelScope.launch {
            if (correo.isBlank() || contrasena.isBlank()) {
                _resultadoSesion.value = ResultadoOperacion.Error("Campos vacíos")
                return@launch
            }
            val usuario = repositorio.iniciarSesion(correo, contrasena)
            if (usuario != null) {
                _usuarioActual.value = usuario
                _resultadoSesion.value = ResultadoOperacion.Exito("Sesión iniciada")
            } else {
                _resultadoSesion.value = ResultadoOperacion.Error("Correo o contraseña incorrectos")
            }
        }
    }

    fun cargarUsuario(id: Int) {
        viewModelScope.launch {
            _usuarioActual.value = repositorio.buscarPorId(id)
        }
    }
}