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
    private val groupCheckboxes = mutableMapOf<String, CheckBox>()

    private var editingCampaignId: String? = null
    private var messageInput: EditText? = null
    private var campaignNameInput: EditText? = null
    private var attachmentLabel: TextView? = null

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

    // =========================================================
    // INTERFAZ GENERAL
    // =========================================================

    private fun construirPantalla(titulo: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        texto(titulo, 25f, true)
        texto("ShadowApp", 14f)
        espacio(12)
    }

    private fun texto(
        contenido: String,
        tamano: Float = 16f,
        negrita: Boolean = false
    ): TextView {
        val vista = TextView(this).apply {
            text = contenido
            textSize = tamano
            if (negrita) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            setPadding(0, dp(5), 0, dp(5))
        }

        root.addView(vista)
        return vista
    }

    private fun boton(
        contenido: String,
        accion: () -> Unit
    ) {
        val vista = Button(this).apply {
            text = contenido
            isAllCaps = false
            setOnClickListener { accion() }
        }

        root.addView(
            vista,
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
        val vista = EditText(this).apply {
            hint = pista
            setText(valor)
            textSize = 16f

            if (multilinea) {
                minLines = 3
                gravity = Gravity.TOP
            }
        }

        root.addView(
            vista,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return vista
    }

    private fun espacio(valorDp: Int) {
        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(valorDp))
        )
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

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

    // =========================================================
    // INICIO
    // =========================================================

    private fun mostrarInicio() {
        construirPantalla("Inicio")

        texto(
            "Administra tus grupos, prepara campañas y consulta su estado."
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
            "Los grupos y las campañas se guardan en el dispositivo."
        )

        texto(
            "Estado actual: ${prefs.getString(keyStatus, "Listo")}"
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
        } catch (_: Exception) {
            aviso("Ocurrió un error al abrir la aplicación.")
        }
    }

    // =========================================================
    // GRUPOS
    // =========================================================

    private fun leerGrupos(): JSONArray {
        return try {
            JSONArray(prefs.getString(keyGroups, "[]") ?: "[]")
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
            val grupo = lista.optJSONObject(i) ?: continue
            val nombre = grupo.optString("nombre", "").trim()

            if (
                nombre.isNotEmpty() &&
                resultado.none { it.equals(nombre, ignoreCase = true) }
            ) {
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

        if (nombre.isBlank()) return false

        val lista = leerGrupos()

        for (i in 0 until lista.length()) {
            val grupo = lista.optJSONObject(i) ?: continue

            if (
                grupo.optString("nombre")
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
                .setTitle("No hay texto detectado")
                .setMessage(
                    "Abre el grupo en WhatsApp, verifica que el servicio " +
                        "de accesibilidad esté activado y vuelve a intentarlo."
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
            aviso("No se encontró un nombre de grupo.")
            return
        }

        val entrada = EditText(this).apply {
            setText(candidato)
            hint = "Nombre del grupo"
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar grupo detectado")
            .setMessage("Comprueba el nombre antes de guardarlo.")
            .setView(entrada)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = entrada.text.toString().trim()

                if (guardarGrupo(nombre)) {
                    aviso("Grupo guardado correctamente.")
                    mostrarGrupos()
                } else {
                    aviso("El nombre está vacío o el grupo ya existe.")
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

        boton("Abrir WhatsApp Business") {
            abrirWhatsApp("com.whatsapp.w4b")
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

            boton("Quitar grupo") {
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

            if (
                !grupo.optString("nombre")
                    .equals(nombre, ignoreCase = true)
            ) {
                nueva.put(grupo)
            }
        }

        guardarGrupos(nueva)
        aviso("Grupo eliminado de la lista.")
    }

    // =========================================================
    // CREAR Y EDITAR CAMPAÑAS
    // =========================================================

    private fun mostrarCampana(idExistente: String? = null) {
        selectedGroups.clear()
        selectedFiles.clear()
        groupCheckboxes.clear()
        editingCampaignId = idExistente

        val existente = if (idExistente != null) {
            buscarCampana(idExistente)
        } else {
            null
        }

        if (existente != null) {
            val grupos = existente.optJSONArray("grupos") ?: JSONArray()

            for (i in 0 until grupos.length()) {
                selectedGroups.add(grupos.optString(i))
            }

            val archivos =
                existente.optJSONArray("archivos") ?: JSONArray()

            for (i in 0 until archivos.length()) {
                val valor = archivos.optString(i)

                if (valor.isNotBlank()) {
                    selectedFiles.add(Uri.parse(valor))
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
            existente?.optString("mensaje", "")
                ?: prefs.getString(keyMessage, "").orEmpty(),
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

                    setOnCheckedChangeListener { _, marcado ->
                        if (marcado) {
                            selectedGroups.add(nombre)
                        } else {
                            selectedGroups.remove(nombre)
                        }
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
                    "Se abrirá la hoja de compartir de Android. " +
                        "Comprueba el destinatario y confirma el envío " +
                        "en la aplicación correspondiente."
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
        } catch (_: Exception) {
            aviso("No se pudo abrir el selector de archivos.")
        }
    }

    @Deprecated("Se mantiene por compatibilidad con el proyecto")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode != REQUEST_FILES ||
            resultCode != RESULT_OK ||
            data == null
        ) {
            return
        }

        val obtenidos = mutableListOf<Uri>()
        val clip = data.clipData

        if (clip != null) {
            for (i in 0 until clip.itemCount) {
                obtenidos.add(clip.getItemAt(i).uri)
            }
        }

        if (obtenidos.isEmpty()) {
            data.data?.let { obtenidos.add(it) }
        }

        val flags = data.flags and
            Intent.FLAG_GRANT_READ_URI_PERMISSION

        for (uri in obtenidos) {
            if (!selectedFiles.contains(uri)) {
                selectedFiles.add(uri)
            }

            try {
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
                // Algunos proveedores no admiten permisos persistentes.
            }
        }

        actualizarListaArchivos()
    }

    private fun actualizarListaArchivos() {
        val etiqueta = attachmentLabel ?: return

        etiqueta.text = if (selectedFiles.isEmpty()) {
            "No hay archivos adjuntos."
        } else {
            selectedFiles.mapIndexed { index, uri ->
                "${index + 1}. ${uri.lastPathSegment ?: uri}"
            }.joinToString("\n")
        }
    }

    // =========================================================
    // GUARDAR CAMPAÑAS
    // =========================================================

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

        val id = editingCampaignId
            ?: System.currentTimeMillis().toString()

        val lista = leerCampanas()
        val nuevaLista = JSONArray()
        var reemplazada = false

        val anterior = buscarCampana(id)
        val estadoAnterior =
            anterior?.optString("estado", "Pendiente") ?: "Pendiente"

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

        val fechaCreacion = anterior?.optLong(
            "creada",
            System.currentTimeMillis()
        ) ?: System.currentTimeMillis()
        }
    }

    // =========================================================
    // LEER Y ACTUALIZAR CAMPAÑAS
    // =========================================================

    private fun leerCampanas(): JSONArray {
        return try {
            JSONArray(
                prefs.getString(keyCampaigns, "[]") ?: "[]"
            )
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun buscarCampana(id: String): JSONObject? {
        val lista = leerCampanas()

        for (i in 0 until lista.length()) {
            val item = lista.optJSONObject(i) ?: continue

            if (item.optString("id") == id) {
                return item
            }
        }
        return null
    }

    private fun actualizarEstadoCampana(
        id: String,
        estado: String
    ) {
        val lista = leerCampanas()

        for (i in 0 until lista.length()) {
            val item = lista.optJSONObject(i) ?: continue

            if (item.optString("id") == id) {
                item.put("estado", estado)
                item.put("actualizada", System.currentTimeMillis())
                break
            }
        }

        prefs.edit()
            .putString(keyCampaigns, lista.toString())
            .putString(keyStatus, estado)
            .apply()
    }

    // =========================================================
    // CAMPAÑAS GUARDADAS
    // =========================================================
    private fun mostrarCampanas() {
        construirPantalla("Campañas guardadas")

        boton("Volver al inicio") {
            mostrarInicio()
        }

        val lista = leerCampanas()

        if (lista.length() == 0) {
            texto("Todavía no hay campañas guardadas.")
            return
        }

        for (i in lista.length() - 1 downTo 0) {
            val item = lista.optJSONObject(i) ?: continue

            val id = item.optString("id")
            val nombre = item.optString("nombre", "Sin nombre")
            val estado = item.optString("estado", "Pendiente")
            val mensaje = item.optString("mensaje", "")
            val grupos = item.optJSONArray("grupos") ?: JSONArray()

            texto(
                "$nombre\n" +
                    "Estado: $estado\n" +
                    "Grupos seleccionados: ${grupos.length()}\n" +
                    "Mensaje: ${mensaje.take(100)}",
                16f,
                true
                )

            boton("Editar campaña") {
                mostrarCampana(id)
            }

            boton("Preparar para compartir") {
                compartirCampanaGuardada(id)
            }

            boton("Eliminar campaña") {
                confirmar(
                    "Eliminar campaña",
                    "¿Quieres eliminar \"$nombre\"?"
                ) {
                    eliminarCampana(id)
                    mostrarCampanas()
                }
            }

            espacio(8)
        }
    }

    private fun eliminarCampana(id: String) {
        val original = leerCampanas()
        val nueva = JSONArray()

        for (i in 0 until original.length()) {
            val item =
            original.optJSONObject(i) ?: continue

            if (item.optString("id") != id) {
                nueva.put(item)
            }
        }

        prefs.edit()
            .putString(keyCampaigns, nueva.toString())
            .apply()

        aviso("Campaña eliminada.")
    }

    // =========================================================
    // COMPARTIR CONTENIDO
    // =========================================================
    private fun compartirCampana() {
        val id = editingCampaignId

        if (id.isNullOrBlank()) {
            aviso("Guarda primero la campaña.")
            return
        }

        compartirCampanaGuardada(id)
    }

    private fun compartirCampanaGuardada(id: String) {
        val campana = buscarCampana(id)

        if (campana == null) {
            aviso("No se encontró la campaña.")
            return
        }

        val mensaje = campana.optString("mensaje", "")
        val archivos = campana.optJSONArray("archivos") ?: JSONArray()
        val uris = mutableListOf<Uri>()

        for (i in 0 until archivos.length()) {
            val valor =
            archivos.optString(i)

            if (valor.isBlank()) continue

            val uri = Uri.parse(valor)

            try {
                val descriptor =
                    contentResolver.openFileDescriptor(uri, "r")

                if (descriptor == null) {
                    aviso("No se pudo abrir uno de los archivos.")
                    actualizarEstadoCampana(
                        id,
                        "Error: archivo inaccesible"
                    )
                    return
                }

                descriptor.close()
                uris.add(uri)
            } catch (_: Exception) {
                aviso(
                    "Un archivo ya no está disponible. " +
                        "Edita la campaña y selecciónalo otra vez."
                )
                actualizarEstadoCampana(
                    id,
                    "Error: archivo inaccesible"
                )
                return
            }
        }

        if (mensaje.isBlank() && uris.isEmpty()) {
            aviso("La campaña no contiene texto ni archivos.")
            return
        }

        val intent = when {
            uris.size > 1 -> {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"

                    putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList(uris)
                    )

                    if (mensaje.isNotBlank()) {
                        if (mensaje.isNotBlank()) {
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                    }

                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            uris.size == 1 -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = contentResolver.getType(uris[0]) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uris[0])

                    if (mensaje.isNotBlank()) {
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                    }

                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            else -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensaje)
                }
            }
        }

        try {
            actualizarEstadoCampana(id, "Preparada para compartir")

            startActivity(
                Intent.createChooser(intent, "Compartir campaña")
            )
        } catch (_: ActivityNotFoundException) {
            actualizarEstadoCampana(id, "Error al compartir")
            aviso("No hay una aplicación compatible para compartir.")
        } catch (e: Exception) {
            actualizarEstadoCampana(id, "Error al compartir")
            aviso(
                "No se pudo compartir: " +
                    (e.localizedMessage ?: "error desconocido")
            )
        }
    }

    //
    =========================================================
    // INTERVALO PARA FUTURAS TAREAS DE COLA
    // =========================================================

    private fun intervaloEntreTareas(): Long {
        return Random.nextLong(2000L, 3001L)
    }

    // =========================================================
    // HISTORIAL
    // =========================================================

    private fun mostrarHistorial() {
        construirPantalla("Historial")

        boton("Volver al inicio") {
            mostrarInicio()
        }

        val lista = leerCampanas()
        if (lista.length() == 0) {
            texto("Todavía no hay campañas en el historial.")
            return
        }

        for (i in lista.length() - 1 downTo 0) {
            val item = lista.optJSONObject(i) ?: continue

            val id = item.optString("id")
            val nombre = item.optString("nombre", "Sin nombre")
            val estado = item.optString("estado", "Pendiente")
            val grupos = item.optJSONArray("grupos") ?: JSONArray()
            val archivos = item.optJSONArray("archivos") ?: JSONArray()

            texto(
                "$nombre\n" +
                    "Estado: $estado\n" +
                    "Grupos: ${grupos.length()}\n" +
                    "Archivos: ${archivos.length()}",
                16f,
                true
            )

            boton("Ver detalles") {
                val detalle = buscarCampana(id)

                if (detalle != null) {
                    AlertDialog.Builder(this)
                        .setTitle(nombre)
                        .setMessage(
                            "Estado: ${detalle.optString("estado")}\n\n" +
                                "Mensaje:\n" +
                                "${detalle.optString("mensaje")}\n\n" +
                                "Grupos: ${grupos.length()}\n" +
                                "Archivos: ${archivos.length()}"
                        )
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
            }

            boton("Preparar para compartir") {
                compartirCampanaGuardada(id)
            }

            espacio(8)
        }
    }
    }
    
