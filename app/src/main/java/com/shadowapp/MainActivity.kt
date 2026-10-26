package com.shadowapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val fondo = Color.rgb(18, 15, 29)
    private val tarjeta = Color.rgb(35, 29, 51)
    private val morado = Color.rgb(174, 117, 255)
    private val blanco = Color.WHITE
    private val suave = Color.rgb(195, 187, 213)

    private lateinit var contenido: LinearLayout
    private lateinit var editorMensaje: EditText
    private lateinit var estado: TextView

    private val preferencias by lazy {
        getSharedPreferences("shadowapp_datos", MODE_PRIVATE)
    }

    private fun leerGrupos(): JSONArray {
        return try {
            JSONArray(preferencias.getString("grupos", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun guardarEstado(texto: String) {
        preferencias.edit().putString("estado", texto).apply()
        if (::estado.isInitialized) estado.text = texto
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarInicio()
    }

    private fun crearColumna(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 24)
        }
    }

    private fun texto(
        valor: String,
        tamano: Float = 16f,
        color: Int = blanco,
        negrita: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = valor
            textSize = tamano
            setTextColor(color)
            if (negrita) setTypeface(null, Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        }
    }

    private fun boton(
        titulo: String,
        accion: () -> Unit
    ): Button {
        return Button(this).apply {
            text = titulo
            setTextColor(blanco)
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(tarjeta)
                cornerRadius = 22f
                setStroke(2, morado)
            }
            setOnClickListener { accion() }
        }
    }

    private fun tarjeta(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 14)
            background = GradientDrawable().apply {
                setColor(tarjeta)
                cornerRadius = 24f
            }
        }
    }

    private fun espacio(vista: LinearLayout, alto: Int = 10) {
        vista.addView(View(this), LinearLayout.LayoutParams(1, alto))
    }

    private fun mostrarInicio() {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fondo)
        }

        val cabecera = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 12)
        }

        cabecera.addView(texto("SHADOWAPP", 28f, morado, true))
        cabecera.addView(
            texto("Tu espacio de campañas", 15f, suave)
        )

        raiz.addView(cabecera)

        val navegacion = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 8)
        }

        navegacion.addView(
            boton("Inicio") { mostrarInicio() },
            LinearLayout.LayoutParams(0, 52, 1f)
        )
        navegacion.addView(
            boton("Grupos") { mostrarGrupos() },
            LinearLayout.LayoutParams(0, 52, 1f)
        )
        navegacion.addView(
            boton("Historial") { mostrarHistorial() },
            LinearLayout.LayoutParams(0, 52, 1f)
        )

        raiz.addView(navegacion)

        val scroll = ScrollView(this)
        contenido = crearColumna()
        scroll.addView(contenido)
        raiz.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(raiz)

        contenido.addView(texto("✨ Panel principal", 23f, blanco, true))
        contenido.addView(
            texto(
                "Prepara tus mensajes y organiza tus grupos desde un solo lugar.",
                15f,
                suave
            )
        )

        espacio(contenido)

        val resumen = tarjeta()
        resumen.addView(texto("👥 Mis grupos", 19f, morado, true))
        resumen.addView(
            texto(
                "${leerGrupos().length()} grupos guardados",
                15f,
                suave
            )
        )
        resumen.addView(boton("Administrar grupos") { mostrarGrupos() })
        contenido.addView(resumen)

        espacio(contenido, 16)

        val campana = tarjeta()
        campana.addView(texto("💬 Nueva campaña", 20f, morado, true))
        campana.addView(
            texto("Escribe y guarda el mensaje que quieres preparar.", 14f, suave)
        )

        editorMensaje = EditText(this).apply {
            setTextColor(blanco)
            setHintTextColor(suave)
            hint = "Escribe tu mensaje aquí..."
            textSize = 16f
            gravity = Gravity.TOP
            minLines = 4
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(49, 41, 66))
            setText(preferencias.getString("mensaje", "") ?: "")
        }

        campana.addView(
            editorMensaje,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        espacio(campana)

        campana.addView(
            boton("💾 Guardar mensaje") {
                val mensaje = editorMensaje.text.toString().trim()
                if (mensaje.isEmpty()) {
                    aviso("Escribe un mensaje antes de guardarlo.")
                } else {
                    preferencias.edit().putString("mensaje", mensaje).apply()
                    guardarEstado("Mensaje guardado correctamente.")
                    aviso("¡Mensaje guardado! 💜")
                }
            }
        )

        espacio(campana)

        campana.addView(
            boton("📤 Preparar envío") {
                val mensaje = editorMensaje.text.toString().trim()
                if (mensaje.isEmpty()) {
                    aviso("Primero escribe un mensaje.")
                } else {
                    preferencias.edit().putString("mensaje", mensaje).apply()
                    val compartir = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                    }
                    startActivity(
                        Intent.createChooser(
                            compartir,
                            "Compartir mensaje con..."
                        )
                    )
                    guardarEstado("Mensaje preparado para compartir.")
                }
            }
        )

        contenido.addView(campana)

        espacio(contenido, 16)

        val tareas = tarjeta()
        tareas.addView(texto("⏱️ Programación", 19f, morado, true))
        tareas.addView(
            texto(
                "Esta versión permite preparar y guardar mensajes. " +
                    "La programación de tareas se añadirá en una etapa posterior.",
                14f,
                suave
            )
        )
        contenido.addView(tareas)

        espacio(contenido, 16)

        estado = texto(
            preferencias.getString("estado", "Todo listo para comenzar.")
                ?: "Todo listo para comenzar.",
            14f,
            suave
        )
        contenido.addView(estado)
    }

    private fun mostrarGrupos() {
        val raiz = crearPantalla("👥 Mis grupos")

        raiz.addView(
            texto(
                "Guarda los nombres de tus grupos para tenerlos organizados. " +
                    "La app no puede importar automáticamente todos tus grupos de WhatsApp.",
                14f,
                suave
            )
        )

        raiz.addView(boton("＋ Añadir grupo") { dialogoNuevoGrupo() })

        espacio(raiz, 16)

        val grupos = leerGrupos()

        if (grupos.length() == 0) {
            raiz.addView(
                texto("Todavía no has añadido grupos.", 16f, suave)
            )
        }

        for (i in 0 until grupos.length()) {
            val grupo = grupos.optJSONObject(i) ?: continue
            val nombre = grupo.optString("nombre", "Grupo")
            val enlace = grupo.optString("enlace", "")

            val tarjetaGrupo = tarjeta()
            tarjetaGrupo.addView(texto("👥 $nombre", 17f, blanco, true))

            if (enlace.isNotBlank()) {
                tarjetaGrupo.addView(texto(enlace, 13f, suave))
                tarjetaGrupo.addView(
                    boton("Abrir enlace") {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse(enlace)
                            )
                            startActivity(intent)
                        } catch (_: Exception) {
                            aviso("No se pudo abrir ese enlace.")
                        }
                    }
                )
            }

            tarjetaGrupo.addView(
                boton("Eliminar grupo") {
                    confirmarEliminarGrupo(i, nombre)
                }
            )

            espacio(raiz, 12)
            raiz.addView(tarjetaGrupo)
        }

        mostrarContenido(raiz)
    }

    private fun dialogoNuevoGrupo() {
        val formulario = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }

        val nombre = EditText(this).apply {
            hint = "Nombre del grupo"
            setSingleLine(true)
        }

        val enlace = EditText(this).apply {
            hint = "Enlace de invitación (opcional)"
            setSingleLine(true)
        }

        formulario.addView(nombre)
        formulario.addView(enlace)

        AlertDialog.Builder(this)
            .setTitle("Añadir grupo")
            .setView(formulario)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nombreGrupo = nombre.text.toString().trim()
                val enlaceGrupo = enlace.text.toString().trim()

                if (nombreGrupo.isEmpty()) {
                    aviso("Escribe el nombre del grupo.")
                    return@setPositiveButton
                }

                if (enlaceGrupo.isNotEmpty() &&
                    !enlaceGrupo.startsWith("https://") &&
                    !enlaceGrupo.startsWith("http://")
                ) {
                    aviso("El enlace debe comenzar con https:// o http://")
                    return@setPositiveButton
                }

                val grupos = leerGrupos()
                grupos.put(
                    JSONObject().apply {
                        put("nombre", nombreGrupo)
                        put("enlace", enlaceGrupo)
                    }
                )

                preferencias.edit()
                    .putString("grupos", grupos.toString())
                    .apply()

                guardarEstado("Grupo añadido: $nombreGrupo")
                mostrarGrupos()
            }
            .show()
    }

    private fun confirmarEliminarGrupo(indice: Int, nombre: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar grupo")
            .setMessage("¿Quieres quitar \"$nombre\" de tu lista?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                val antiguos = leerGrupos()
                val nuevos = JSONArray()

                for (i in 0 until antiguos.length()) {
                    if (i != indice) nuevos.put(antiguos.get(i))
                }

                preferencias.edit()
                    .putString("grupos", nuevos.toString())
                    .apply()

                guardarEstado("Grupo eliminado: $nombre")
                mostrarGrupos()
            }
            .show()
    }

    private fun mostrarHistorial() {
        val raiz = crearPantalla("📊 Historial")

        raiz.addView(
            texto("Último estado registrado", 18f, morado, true)
        )

        raiz.addView(
            texto(
                preferencias.getString("estado", "Todavía no hay actividad.")
                    ?: "Todavía no hay actividad.",
                16f,
                blanco
            )
        )

        raiz.addView(
            texto(
                "El historial de esta primera versión muestra el último estado; " +
                    "un registro detallado se añadirá después.",
                14f,
                suave
            )
        )

        raiz.addView(boton("Volver al inicio") { mostrarInicio() })
        mostrarContenido(raiz)
    }

    private fun crearPantalla(titulo: String): LinearLayout {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 24)
            setBackgroundColor(fondo)
        }

        raiz.addView(texto("SHADOWAPP", 25f, morado, true))
        raiz.addView(texto(titulo, 23f, blanco, true))
        raiz.addView(boton("← Volver al inicio") { mostrarInicio() })
        espacio(raiz, 12)

        return raiz
    }

    private fun mostrarContenido(vista: LinearLayout) {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(fondo)
            addView(vista)
        }
        setContentView(scroll)
    }

    private fun aviso(mensaje: String) {
        AlertDialog.Builder(this)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}
