package com.shadowapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

object CampanasShadow {

    private const val PREFS = "shadowapp_datos"
    private const val KEY_CAMPANAS = "campanas_guardadas"

    /**
     * Guarda una campaña con su mensaje, grupos y archivos.
     * Los archivos se guardan como URI en formato texto.
     */
    fun guardar(
        context: Context,
        nombre: String,
        mensaje: String,
        grupos: List<String>,
        archivos: List<String>
    ): String {
        val prefs = context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        )

        val lista = try {
            JSONArray(prefs.getString(KEY_CAMPANAS, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        val id = System.currentTimeMillis().toString()

        val campana = JSONObject().apply {
            put("id", id)
            put("nombre", nombre)
            put("mensaje", mensaje)
            put("grupos", JSONArray(grupos))
            put("archivos", JSONArray(archivos))
            put("estado", "Pendiente")
            put("creada", System.currentTimeMillis())
        }

        lista.put(campana)

        prefs.edit()
            .putString(KEY_CAMPANAS, lista.toString())
            .apply()

        return id
    }

    fun listar(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        )

        return try {
            JSONArray(prefs.getString(KEY_CAMPANAS, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
    }

    fun actualizarEstado(
        context: Context,
        id: String,
        estado: String
    ) {
        val prefs = context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        )

        val lista = listar(context)

        for (i in 0 until lista.length()) {
            val item = lista.optJSONObject(i) ?: continue

            if (item.optString("id") == id) {
                item.put("estado", estado)
                break
            }
        }

        prefs.edit()
            .putString(KEY_CAMPANAS, lista.toString())
            .apply()
    }

    /**
     * Intervalo aleatorio entre 2 y 3 segundos.
     * Úsalo entre tareas de la cola.
     */
    fun intervaloEntreTareas(): Long {
        return Random.nextLong(2000L, 3001L)
    }
}
