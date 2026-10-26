package com.shadowapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val fondo = Color.rgb(18, 15, 29)
    private val tarjetaColor = Color.rgb(35, 29, 51)
    private val morado = Color.rgb(174, 117, 255)
    private val blanco = Color.WHITE
    private val suave = Color.rgb(195, 187, 213)

    private val preferencias by lazy {
        getSharedPreferences("shadowapp_datos", MODE_PRIVATE)
    }

    private var archivoUri: Uri? = null
    private var archivoNombre = ""
    private var editorMensaje: EditText? = null
    private var seleccionados = mutableSetOf<String>()
    private val casillas = mutableMapOf<String, CheckBox>()

    companion object {
        private const val ELEGIR_ARCHIVO = 701
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarInicio()
    }

    private fun leerGrupos(): JSONArray {
        return try {
            JSONArray(preferencias.getString("grupos", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun guardarEstado(mensaje: String) {
        preferencias.edit().putString("estado", mensaje).apply()
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
            isAllCaps = false
            setTextColor(blanco)
            background = GradientDrawable().apply {
                setColor(tarjetaColor)
                cornerRadius = 22f
                setStroke(2, morado)
            }
            setOnClickListener { accion() }
        }
    }

    private fun columna(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 24)
            setBackgroundColor(fondo)
        }
    }

    private fun tarjeta(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 14)
            background = GradientDrawable().apply {
                setColor(tarjetaColor)
                cornerRadius = 24f
            }
        }
    }

    private fun espacio(vista: LinearLayout, alto: Int = 10) {
        vista.addView(View(this), LinearLayout.LayoutParams(1, alto))
    }

    private fun mostrar(vista: LinearLayout) {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(fondo)
        scroll.addView(vista)
        setContentView(scroll)
    }

    private fun cabecera(titulo: String): LinearLayout {
        val raiz = columna()
        raiz.addView(texto("SHADOWAPP", 25f, morado, true))
        raiz.addView(texto(titulo, 23f, blanco, true))
        raiz.addView(boton("← Inicio") { mostrarInicio() })
        espacio(raiz, 12)
        return raiz
    }

    private fun aviso(mensaje: String) {
        AlertDialog.Builder(this)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun mostrarInicio() {
        val raiz = columna()
        raiz.addView(texto("✨ ShadowApp", 28f, morado, true))
        raiz.addView(
            texto("Organiza tus grupos y prepara tus campañas.", 16f, suave)
        )

        espacio(raiz, 12)
        raiz.addView(boton("👥 Administrar grupos") { mostrarGrupos() })
        espacio(raiz, 8)
        raiz.addView(boton("📤 Nueva campaña") { mostrarCampana() })
        espacio(raiz, 8)
        raiz.addView(boton("📊 Historial") { mostrarHistorial() })

        espacio(raiz, 20)
        raiz.addView(texto("Grupos guardados: ${leerGrupos().length()}", 16f))
        raiz.addView(
            texto(
                preferencias.getString("estado", "Todo listo para comenzar.")
                    ?: "Todo listo para comenzar.",
                14f,
                suave
            )
        )

        mostrar(raiz)
    }

    private fun mostrarGrupos() {
        val raiz = cabecera("👥 Mis grupos")

        raiz.addView(
            boton("＋ Añadir grupo manualmente") {
                dialogoNuevoGrupo()
            }
        )
        espacio(raiz, 12)

        val grupos = leerGrupos()
        if (grupos.length() == 0) {
            raiz.addView(
                texto("Todavía no hay grupos guardados.", 16f, suave)
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
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(enlace))
                            )
                        } catch (_: Exception) {
                            aviso("No se pudo abrir el enlace.")
                        }
                    }
                )
            }

            tarjetaGrupo.addView(
                boton("Eliminar grupo") {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar grupo")
                        .setMessage("¿Quieres eliminar $nombre?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar") { _, _ ->
                            val nuevos = JSONArray()
                            for (j in 0 until grupos.length()) {
                                if (j != i) nuevos.put(grupos.get(j))
                            }
                            preferencias.edit()
                                .putString("grupos", nuevos.toString())
                                .apply()
                            seleccionados.remove(nombre)
                            guardarEstado("Grupo eliminado: $nombre")
                            mostrarGrupos()
                        }
                        .show()
                }
            )

            espacio(raiz, 10)
            raiz.addView(tarjetaGrupo)
        }

        mostrar(raiz)
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
                val n = nombre.text.toString().trim()
                val e = enlace.text.toString().trim()

                if (n.isBlank()) {
                    aviso("Escribe el nombre del grupo.")
                    return@setPositiveButton
                }

                if (e.isNotBlank() &&
                    !e.startsWith("https://") &&
                    !e.startsWith("http://")
                ) {
                    aviso("El enlace debe empezar con https:// o http://")
                    return@setPositiveButton
                }

                val grupos = leerGrupos()
                for (i in 0 until grupos.length()) {
                    if (grupos.optJSONObject(i)
                            ?.optString("nombre", "")
                            ?.equals(n, true) == true
                    ) {
                        aviso("Ese grupo ya está guardado.")
                        return@setPositiveButton
                    }
                }

                grupos.put(
                    JSONObject().apply {
                        put("nombre", n)
                        put("enlace", e)
                    }
                )
                preferencias.edit()
                    .putString("grupos", grupos.toString())
                    .apply()

                guardarEstado("Grupo añadido: $n")
                mostrarGrupos()
            }
            .show()
    }

    private fun mostrarCampana() {
        val raiz = cabecera("📤 Nueva campaña")
        seleccionados.clear()
        casillas.clear()

        raiz.addView(texto("1. Selecciona los grupos", 19f, morado, true))

        val grupos = leerGrupos()
        if (grupos.length() == 0) {
            raiz.addView(
                texto("Primero añade grupos en Mis grupos.", 15f, suave)
            )
            raiz.addView(
                boton("Ir a Mis grupos") { mostrarGrupos() }
            )
            mostrar(raiz)
            return
        }

        raiz.addView(
            boton("☑ Seleccionar todos") {
                casillas.forEach { (nombre, casilla) ->
                    casilla.isChecked = true
                    seleccionados.add(nombre)
                }
            }
        )
        raiz.addView(
            boton("□ Desmarcar todos") {
                casillas.forEach { (nombre, casilla) ->
                    casilla.isChecked = false
                    seleccionados.remove(nombre)
                }
            }
        )

        for (i in 0 until grupos.length()) {
            val nombre = grupos.optJSONObject(i)
                ?.optString("nombre", "Grupo") ?: continue

            val casilla = CheckBox(this).apply {
                text = nombre
                textSize = 16f
                setTextColor(blanco)
                buttonTintList = android.content.res.ColorStateList.valueOf(morado)
                setOnCheckedChangeListener { _, marcado ->
                    if (marcado) seleccionados.add(nombre)
                    else seleccionados.remove(nombre)
                }
            }

            casillas[nombre] = casilla
            raiz.addView(casilla)
        }

        espacio(raiz, 16)
        raiz.addView(texto("2. Escribe el mensaje", 19f, morado, true))

        val mensaje = EditText(this).apply {
            setTextColor(blanco)
            setHintTextColor(suave)
            hint = "Texto que acompañará al archivo (opcional)"
            textSize = 16f
            minLines = 4
            gravity = Gravity.TOP
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(49, 41, 66))
            setText(preferencias.getString("mensaje", "") ?: "")
        }
        editorMensaje = mensaje
        raiz.addView(
            mensaje,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        espacio(raiz, 12)
        raiz.addView(texto("3. Adjunta contenido (opcional)", 19f, morado, true))
        raiz.addView(
            texto(
                "Puedes elegir imágenes, vídeos, GIF, PDF y otros archivos.",
                14f,
                suave
            )
        )
        raiz.addView(boton("📎 Elegir archivo") { elegirArchivo() })
        raiz.addView(
            texto(
                if (archivoNombre.isBlank()) "Ningún archivo seleccionado"
                else "Adjunto: $archivoNombre",
                14f,
                suave
            )
        )

        espacio(raiz, 16)
        raiz.addView(
            boton("📤 Preparar envío a grupos seleccionados") {
                prepararEnvio(mensaje.text.toString().trim())
            }
        )

        espacio(raiz, 8)
        raiz.addView(
            texto(
                "Android puede abrir WhatsApp para compartir el contenido. " +
                    "Debes elegir y confirmar cada destinatario que WhatsApp permita. " +
                    "La selección de ShadowApp no selecciona automáticamente los grupos dentro de WhatsApp.",
                13f,
                suave
            )
        )

        mostrar(raiz)
    }

    private fun elegirArchivo() {
        val selector = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        try {
            startActivityForResult(selector, ELEGIR_ARCHIVO)
        } catch (_: Exception) {
            aviso("No se pudo abrir el selector de archivos.")
        }
    }

    @Deprecated("La API sigue siendo compatible con esta versión sencilla.")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != ELEGIR_ARCHIVO ||
            resultCode != RESULT_OK ||
            data?.data == null
        ) return

        archivoUri = data.data
        try {
            val flags = data.flags and
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(archivoUri!!, flags)
        } catch (_: Exception) {
            // Algunos proveedores no permiten permisos persistentes.
        }

        archivoNombre = archivoUri?.lastPathSegment ?: "Archivo seleccionado"
        mostrarCampana()
    }

    private fun prepararEnvio(mensaje: String) {
        if (seleccionados.isEmpty()) {
            aviso("Selecciona al menos un grupo.")
            return
        }

        val uri = archivoUri
        if (mensaje.isBlank() && uri == null) {
            aviso("Escribe un mensaje o selecciona un archivo.")
            return
        }

        preferencias.edit().putString("mensaje", mensaje).apply()

        val intent: Intent = if (uri != null) {
            Intent(Intent.ACTION_SEND).apply {
                setType(contentResolver.getType(uri) ?: "*/*")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (mensaje.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, mensaje)
                }
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensaje)
            }
        }

        val lista = seleccionados.joinToString(", ")
        guardarEstado(
            "Campaña preparada para ${seleccionados.size} grupo(s): $lista. " +
                "El envío aún debe confirmarse en la aplicación de destino."
        )

        try {
            startActivity(
                Intent.createChooser(intent, "Compartir contenido con...")
            )
        } catch (_: Exception) {
            aviso("No se encontró una aplicación compatible para compartir.")
        }
    }

    private fun mostrarHistorial() {
        val raiz = cabecera("📊 Historial")
        raiz.addView(texto("Último estado registrado", 18f, morado, true))
        raiz.addView(
            texto(
                preferencias.getString("estado", "Todavía no hay actividad.")
                    ?: "Todavía no hay actividad.",
                15f,
                suave
            )
        )
        raiz.addView(
            texto(
                "El estado indica que se preparó una campaña; no verifica " +
                    "que WhatsApp haya entregado los mensajes.",
                14f,
                suave
            )
        )
        mostrar(raiz)
    }
}
