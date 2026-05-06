package com.example.wonderbalance.datos.entidad

import kotlinx.serialization.Serializable

@Serializable
data class UsuarioSupabase(
    val id: Int? = null,
    val auth_id: String,
    val nombre: String,
    val correo: String,
    val moneda: String = "MXN"
)