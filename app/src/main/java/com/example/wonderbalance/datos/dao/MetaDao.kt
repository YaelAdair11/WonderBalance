package com.example.wonderbalance.datos.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.wonderbalance.datos.entidad.Meta

@Dao
interface MetaDao {

    @Insert
    suspend fun insertar(meta: Meta): Long

    @Update
    suspend fun actualizar(meta: Meta)

    @Delete
    suspend fun eliminar(meta: Meta)

    @Query("SELECT * FROM metas WHERE usuarioId = :usuarioId ORDER BY fechaLimite ASC")
    fun obtenerTodas(usuarioId: Int): LiveData<List<Meta>>

    @Query("SELECT * FROM metas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Meta?
}