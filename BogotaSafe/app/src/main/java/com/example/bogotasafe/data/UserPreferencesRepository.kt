package com.example.bogotasafe.data

import android.content.Context
import android.content.SharedPreferences

data class UserProfile(
    val sexo: String,
    val edad: Int
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bogotasafe_user_prefs", Context.MODE_PRIVATE)

    fun obtenerPerfilGuardado(): UserProfile? {
        val sexo = prefs.getString("user_sexo", null)
        val edad = prefs.getInt("user_edad", -1)
        return if (sexo != null && edad > 0) {
            UserProfile(sexo, edad)
        } else {
            null // No asume nada: retorna nulo si no se ha configurado
        }
    }

    fun guardarPerfil(sexo: String, edad: Int) {
        prefs.edit()
            .putString("user_sexo", sexo)
            .putInt("user_edad", edad)
            .apply()
    }

    fun borrarPerfil() {
        prefs.edit().clear().apply()
    }
}