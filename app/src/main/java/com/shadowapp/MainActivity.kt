package com.shadowapp

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var campaignInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(0xFF101014.toInt())
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(content)

        content.addView(
            TextView(this).apply {
                text = "SHADOWAPP"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 12, 0, 8)
            }
        )

        content.addView(
            TextView(this).apply {
                text = "Tu asistente de campañas"
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(0xFFCAC2D4.toInt())
                setPadding(0, 0, 0, 24)
            }
        )

        content.addView(label("Nombre de la campaña"))

        campaignInput = EditText(this).apply {
            hint = "Ej. Novedades de la semana"
            setSingleLine(true)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF96909F.toInt())
        }
        content.addView(campaignInput)

        content.addView(label("Tu mensaje"))

        messageInput = EditText(this).apply {
            hint = "Escribe aquí tu mensaje..."
            minLines = 4
            gravity = Gravity.TOP
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF96909F.toInt())
        }
        content.addView(messageInput)

        content.addView(makeButton("Guardar campaña") {
            saveCampaign()
        })

        content.addView(makeButton("Copiar mensaje") {
            copyMessage()
        })

        content.addView(makeButton("Abrir WhatsApp Business") {
            openWhatsAppBusiness()
        })

        content.addView(
            TextView(this).apply {
                text = "MIS GRUPOS"
                textSize = 20f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 28, 0, 8)
            }
        )

        content.addView(
            TextView(this).apply {
                text = "Esta versión permite preparar y copiar mensajes. "
                    + "Todavía no sincroniza automáticamente tus grupos "
                    + "de WhatsApp. Puedes abrir WhatsApp Business y "
                    + "elegir manualmente dónde enviar tu mensaje."
                textSize = 14f
                setTextColor(0xFFCAC2D4.toInt())
                setPadding(0, 0, 0, 16)
            }
        )

        status = TextView(this).apply {
            text = "Tus campañas se guardan en este teléfono."
            textSize = 14f
            setTextColor(0xFFBFA8FF.toInt())
            setPadding(0, 20, 0, 12)
        }
        content.addView(status)

        page.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(page)
        loadCampaign()
    }

    private fun label(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(0xFFEFE6FF.toInt())
            setPadding(0, 14, 0, 4)
        }
    }

    private fun makeButton(
        title: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = title
            setOnClickListener { action() }

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 4)
            layoutParams = params
        }
    }

    private fun saveCampaign() {
        val message = messageInput.text.toString().trim()

        if (message.isBlank()) {
            Toast.makeText(
                this,
                "Escribe un mensaje primero.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        getSharedPreferences("shadowapp", MODE_PRIVATE)
            .edit()
            .putString("title", campaignInput.text.toString())
            .putString("message", message)
            .apply()

        status.text = "Campaña guardada correctamente."

        Toast.makeText(
            this,
            "Campaña guardada",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun loadCampaign() {
        val prefs = getSharedPreferences("shadowapp", MODE_PRIVATE)
        campaignInput.setText(prefs.getString("title", ""))
        messageInput.setText(prefs.getString("message", ""))
    }

    private fun copyMessage() {
        val message = messageInput.text.toString().trim()

        if (message.isBlank()) {
            Toast.makeText(
                this,
                "No hay ningún mensaje para copiar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val clipboard = getSystemService(CLIPBOARD_SERVICE)
                as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("Mensaje de ShadowApp", message)
        )

        Toast.makeText(
            this,
            "Mensaje copiado",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openWhatsAppBusiness() {
        val intent = packageManager
            .getLaunchIntentForPackage("com.whatsapp.w4b")

        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "No se encontró WhatsApp Business instalado.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

