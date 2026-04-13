package com.example.wonderbalance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.wonderbalance.databinding.ActivityMainBinding
import com.example.wonderbalance.util.GestorSesion

class MainActivity : AppCompatActivity() {

    private lateinit var enlace: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enlace = ActivityMainBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        val gestorSesion = GestorSesion(this)
        val fragmentoContenedor = supportFragmentManager
            .findFragmentById(R.id.contenedor_nav) as NavHostFragment
        val controladorNav = fragmentoContenedor.navController

        // Si ya hay sesión activa, ir directo al dashboard
        if (gestorSesion.haySesionActiva()) {
            controladorNav.navigate(R.id.fragmentoDashboard)
        }

        // Conectar barra inferior con navegación
        enlace.navInferior.setupWithNavController(controladorNav)

        // Ocultar barra inferior en pantallas de autenticación
        controladorNav.addOnDestinationChangedListener { _, destino, _ ->
            when (destino.id) {
                R.id.fragmentoAcceso, R.id.fragmentoRegistro -> {
                    enlace.navInferior.visibility =
                        android.view.View.GONE
                }
                else -> {
                    enlace.navInferior.visibility =
                        android.view.View.VISIBLE
                }
            }
        }
    }
}