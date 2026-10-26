package com.shadowapp

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
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
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class MainActivity : Activity() {

    private val prefsName = "shadowapp_datos"
    private val keyGroups = "grupos"
    private val keyMessage = "mensaje"
    private val keyStatus = "estado"
    private val keyDetected = "texto_detectado"
    private val keyCampaigns = "campanas_guardadas"

    private lateinit var root: LinearLayout
    private val selectedGroups = mutableSetOf<String>()
    private val selectedFiles = mutableListOf<Uri>()

    private var editingCampaignId: String? = null
    private var messageInput: EditText? = null
    private var campaignNameInput: EditText? = null
    private var attachmentLabel: TextView? = null
    private var groupCheckboxes = mutableMapOf<String, CheckBox>()

    private val prefs by lazy {
        getSharedPreferences(prefsName, MODE_PRIVATE)
    }

    companion object {
        private const val REQUEST_FILES = 301
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarInicio()
    }

    // ---------------------------------------------------------
    // INTERFAZ GENERAL
    // ---------------------------------------------------------

    private fun construirPantalla(titulo: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        setContentView(scroll)

        texto(titulo, 25f, true)
        texto("ShadowApp", 14f, false)
        espacio(12)
    }

    private fun texto(
        contenido: String,
        tamano: Float = 16f,
        negrita: Boolean = false
    ): TextView {
        val v = TextView(this).apply {
            text = contenido
            textSize = tamano
            if (negrita) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            setPadding(0, dp(5), 0, dp(5))
        }
        root.addView(v)
        return v
    }

    private fun boton(
        contenido: String,
        accion: () -> Unit
    ) {
        val b = Button(this).apply {
            text = contenido
            isAllCaps = false
            setOnClickListener { accion() }
        }
        root.addView(
            b,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(5)
            }
        )
    }

    private fun campo(
        pista: String,
        valor: String = "",
        multilinea: Boolean = false
    ): EditText {
        val e = EditText(this).apply {
            hint = pista
            setText(valor)
            textSize = 16f
            if (multilinea) {
                minLines = 3
                gravity = Gravity.TOP
            }
        }

        root.addView(
            e,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return e
    }

    private fun espacio(dpValue: Int) {
        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(dpValue))
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun aviso(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun confirmar(
        titulo: String,
        mensaje: String,
        accion: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ -> accion() }
            .show()
    }

    // ---------------------------------------------------------
    // INICIO
    // ---------------------------------------------------------

    private fun mostrarInicio() {
        construirPantalla("Inicio")

        texto(
            "Administra tus grupos, prepara campañas y consulta el progreso."
        )

        boton("Abrir WhatsApp") {
            abrirWhatsApp("com.whatsapp")
        }

        boton("Abrir WhatsApp Business") {
            abrirWhatsApp("com.whatsapp.w4b")
        }

        boton("Detectar grupo actual") {
            detectarGrupo()
        }

        boton("Mis grupos") {
            mostrarGrupos()
        }

        boton("Crear campaña") {
            mostrarCampana()
        }

        boton("Campañas guardadas") {
            mostrarCampanas()
        }

        boton("Historial") {
            mostrarHistorial()
        }

        espacio(12)
        texto(
            "Las campañas y los grupos se guardan en el dispositivo."
        )
    }

    private fun abrirWhatsApp(paquete: String) {
        val intent = packageManager.getLaunchIntentForPackage(paquete)

        if (intent == null) {
            aviso(
                if (paquete == "com.whatsapp") {
                    "WhatsApp no está instalado."
                } else {
                    "WhatsApp Business no está instalado."
                }
            )
            return
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            aviso("No se pudo abrir la aplicación.")
        }
    }

    // ---------------------------------------------------------
    // GRUPOS
    // Formato compatible: [{"nombre":"...", "enlace":"..."}]
    // ---------------------------------------------------------

    private fun leerGrupos(): JSONArray {
        return try {
            JSONArray(prefs.getString(keyGroups, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun guardarGrupos(lista: JSONArray) {
        prefs.edit()
            .putString(keyGroups, lista.toString())
            .apply()
    }

    private fun nombresGrupos(): List<String> {
        val lista = leerGrupos()
        val resultado = mutableListOf<String>()

        for (i in 0 until lista.length()) {
            val nombre = lista.optJSONObject(i)
                ?.optString("nombre", "")
                ?.trim()
                .orEmpty()

            if (nombre.isNotEmpty() && !resultado.contains(nombre)) {
                resultado.add(nombre)
            }
        }

        return resultado
    }

    private fun guardarGrupo(
        nombreOriginal: String,
        enlace: String = ""
    ): Boolean {
        val nombre = nombreOriginal.trim()

        if (nombre.isEmpty()) return false

        val lista = leerGrupos()

        for (i in 0 until lista.length()) {
            val actual = lista.optJSONObject(i) ?: continue

            if (actual.optString("nombre")
                    .equals(nombre, ignoreCase = true)
            ) {
                return false
            }
        }

        lista.put(
            JSONObject().apply {
                put("nombre", nombre)
                put("enlace", enlace)
            }
        )

        guardarGrupos(lista)
        return true
    }

    private fun detectarGrupo() {
        val detectado = prefs.getString(keyDetected, "")
            .orEmpty()
            .trim()

        if (detectado.isBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Todavía no hay texto detectado")
                .setMessage(
                    "Abre el grupo en WhatsApp y comprueba que el " +
                    "servicio de accesibilidad de ShadowApp esté " +
                    "activado. Después vuelve e inténtalo de nuevo."
                )
                .setPositiveButton("Abrir ajustes") { _, _ ->
                    try {
                        startActivity(
                            Intent(
                                android.provider.Settings
                                    .ACTION_ACCESSIBILITY_SETTINGS
                            )
                        )
                    } catch (_: Exception) {
                        aviso("No se pudieron abrir los ajustes.")
                    }
                }
                .setNegativeButton("Volver", null)
                .show()
            return
        }

        val lineas = detectado.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val candidato = lineas.firstOrNull()

        if (candidato.isNullOrBlank()) {
            aviso("No se encontró un nombre de grupo reconocible.")
            return
        }

        val entrada = EditText(this).apply {
            setText(candidato)
            hint = "Nombre del grupo"
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar grupo detectado")
            .setMessage(
                "Comprueba que el texto corresponda al nombre " +
                "del grupo que quieres guardar."
            )
            .setView(entrada)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = entrada.text.toString().trim()

                if (guardarGrupo(nombre)) {
                    aviso("Grupo guardado.")
                    mostrarGrupos()
                } else {
                    aviso(
                        "El nombre está vacío o el grupo ya existe."
                    )
                }
            }
            .show()
    }

    private fun mostrarGrupos() {
        construirPantalla("Mis grupos")

        boton("Volver al inicio") {
            mostrarInicio()
        }

        boton("Abrir WhatsApp") {
            abrirWhatsApp("com.whatsapp")
        }

        boton("Detectar grupo actual") {
            detectarGrupo()
        }

        val lista = leerGrupos()

        if (lista.length() == 0) {
            texto("Todavía no hay grupos guardados.")
            return
        }

        for (i in 0 until lista.length()) {
            val grupo = lista.optJSONObject(i) ?: continue
            val nombre = grupo.optString("nombre", "")
            val enlace = grupo.optString("enlace", "")

            texto(
                if (enlace.isBlank()) nombre
                else "$nombre\n$enlace",
                16f,
                true
            )

            boton("Quitar $nombre") {
                confirmar(
                    "Eliminar grupo",
                    "¿Quieres quitar \"$nombre\" de ShadowApp?"
                ) {
                    eliminarGrupo(nombre)
                    mostrarGrupos()
                }
            }

            espacio(4)
        }
    }

    private fun eliminarGrupo(nombre: String) {
        val original = leerGrupos()
        val nueva = JSONArray()

        for (i in 0 until original.length()) {
            val grupo = original.optJSONObject(i) ?: continue

            if (!grupo.optString("nombre")
                    .equals(nombre, ignoreCase = true)
            ) {
                nueva.put(grupo)
            }
        }

        guardarGrupos(nueva)
        aviso("Grupo eliminado de la lista.")
    }

    // ---------------------------------------------------------
    // CREAR Y EDITAR CAMPAÑAS
    // ---------------------------------------------------------

    private fun mostrarCampana(idExistente: String? = null) {
        selectedGroups.clear()
        selectedFiles.clear()
        groupCheckboxes.clear()
        editingCampaignId = idExistente

        val existente = idExistente?.let { buscarCampana(it) }

        if (existente != null) {
            val grupos = existente.optJSONArray("grupos") ?: JSONArray()
            for (i in 0 until grupos.length()) {
                selectedGroups.add(grupos.optString(i))
            }

            val archivos = existente.optJSONArray("archivos")
                ?: JSONArray()

            for (i in 0 until archivos.length()) {
                try {
                    selectedFiles.add(Uri.parse(archivos.getString(i)))
                } catch (_: Exception) {
                    // Se validará el acceso al archivo al compartir.
                }
            }
        }

        construirPantalla(
            if (idExistente == null) "Crear campaña"
            else "Editar campaña"
        )

        boton("Volver al inicio") {
            mostrarInicio()
        }

        campaignNameInput = campo(
            "Nombre de la campaña",
            existente?.optString("nombre", "").orEmpty()
        )

        messageInput = campo(
            "Escribe el mensaje",
            existente?.optString(
                "mensaje",
                prefs.getString(keyMessage, "").orEmpty()
            ).orEmpty(),
            true
        )

        espacio(10)
        texto("Selecciona los grupos", 18f, true)

        val gruposDisponibles = nombresGrupos()

        if (gruposDisponibles.isEmpty()) {
            texto(
                "No hay grupos guardados. Usa Detectar grupo actual " +
                "desde la pantalla de inicio."
            )
        } else {
            for (nombre in gruposDisponibles) {
                val check = CheckBox(this).apply {
                    text = nombre
                    isChecked = selectedGroups.contains(nombre)
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selectedGroups.add(nombre)
                        else selectedGroups.remove(nombre)
                    }
                }

                groupCheckboxes[nombre] = check
                root.addView(check)
            }
        }

        espacio(10)
        texto("Archivos adjuntos", 18f, true)

        attachmentLabel = texto("", 14f)

        actualizarListaArchivos()

        boton("Seleccionar imágenes, vídeos y archivos") {
            seleccionarArchivos()
        }

        boton("Quitar todos los adjuntos") {
            selectedFiles.clear()
            actualizarListaArchivos()
        }

        boton("Guardar campaña") {
            guardarCampanaActual()
        }

        boton("Guardar y preparar para compartir") {
            if (guardarCampanaActual()) {
                confirmar(
                    "Preparar campaña",
                    "La campaña se guardó. ShadowApp abrirá " +
                    "el sistema de compartir para que confirmes " +
                    "el destinatario y el envío."
                ) {
                    compartirCampana()
                }
            }
        }
    }

    private fun seleccionarArchivos() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        try {
            startActivityForResult(intent, REQUEST_FILES)
        } catch (_: ActivityNotFoundException) {
            aviso("No se encontró un selector de archivos.")
        }
    }

    @Deprecated("Usado para mantener compatibilidad con el proyecto actual")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_FILES ||
            resultCode != RESULT_OK ||
            data == null
        ) return

        val obtenidos = mutableListOf<Uri>()

        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                obtenidos.add(clip.getItemAt(i).uri)
            }
        }

        if (obtenidos.isEmpty()) {
            data.data?.let { obtenidos.add(it) }
        }

        val flags = data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION)

        for (uri in obtenidos) {
            if (!selectedFiles.contains(uri)) {
                selectedFiles.add(uri)
            }

            try {
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
                // Algunos proveedores no permiten permisos persistentes.
            }
        }

        actualizarListaArchivos()
    }

    private fun actualizarListaArchivos() {
        attachmentLabel?.text =
            if (selectedFiles.isEmpty()) {
                "No hay archivos adjuntos."
            } else {
                selectedFiles.mapIndexed { index, uri ->
                    "${index + 1}. ${uri.lastPathSegment ?: uri}"
                }.joinToString("\n")
            }
    }

    private fun guardarCampanaActual(): Boolean {
        val nombre = campaignNameInput?.text
            ?.toString()?.trim().orEmpty()

        val mensaje = messageInput?.text
            ?.toString()?.trim().orEmpty()

        if (nombre.isBlank()) {
            aviso("Escribe un nombre para la campaña.")
            return false
        }

        if (mensaje.isBlank() && selectedFiles.isEmpty()) {
            aviso("Añade un mensaje o al menos un archivo.")
            return false
        }

        if (selectedGroups.isEmpty()) {
            aviso("Selecciona al menos un grupo.")
            return false
        }

        val id = editingCampaignId ?: System.currentTimeMillis().toString()
        val lista = leerCampanas()
        val nuevaLista = JSONArray()
        var reemplazada = false

        val anterior = buscarCampana(id)
        val estadoAnterior = anterior?.optString("estado", "Pendiente")
            ?: "Pendiente"

        for (i in 0 until lista.length()) {
            val item = lista.optJSONObject(i) ?: continue

            if (item.optString("id") == id) {
                nuevaLista.put(
                    crearObjetoCampana(
                        id,
                        nombre,
                        mensaje,
                        selectedGroups.toList(),
                        selectedFiles.map { it.toString() },
                        estadoAnterior
                    )
                )
                reemplazada = true
            } else {
                nuevaLista.put(item)
            }
        }

        if (!reemplazada) {
            nuevaLista.put(
                crearObjetoCampana(
                    id,
                    nombre,
                    mensaje,
                    selectedGroups.toList(),
                    selectedFiles.map { it.toString() },
                    "Pendiente"
                )
            )
        }

        prefs.edit()
            .putString(keyCampaigns, nuevaLista.toString())
            .putString(keyMessage, mensaje)
            .putString(keyStatus, "Campaña guardada")
            .apply()

        editingCampaignId = id
        aviso("Campaña guardada correctamente.")
        return true
    }

    private fun crearObjetoCampana(
        id: String,
        nombre: String,
        mensaje: String,
        grupos: List<String>,
        archivos: List<String>,
        estado: String
    ): JSONObject {
        val anterior = buscarCampana(id)

        return JSONObject().apply {
            put("id", id)
            put("nombre", nombre)
            put("mensaje", mensaje)
            put("grupos", JSONArray(grupos))
            put("archivos", JSONArray(archivos))
            put("es
