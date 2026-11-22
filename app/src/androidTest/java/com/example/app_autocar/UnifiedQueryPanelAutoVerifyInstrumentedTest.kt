package com.example.app_autocar

import android.content.Intent
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba el contrato del envío automático del código reconocido: se rellena una sola vez, se
 * pulsa el botón oculto y se detiene al alcanzar el límite de intentos, dejando el campo editable.
 */
@RunWith(AndroidJUnit4::class)
class UnifiedQueryPanelAutoVerifyInstrumentedTest {
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var panel: UnifiedQueryPanel
    private lateinit var web: WebView
    private lateinit var code: EditText
    private lateinit var send: Button
    private lateinit var status: TextView
    private val clicks = AtomicInteger()

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            panel = UnifiedQueryPanel(activity, Intent(), Runnable {})
            web = WebView(activity)
            code = EditText(activity)
            send = Button(activity)
            status = TextView(activity)
            send.setOnClickListener { clicks.incrementAndGet() }
            put("codes", web, code)
            put("codeButtons", web, send)
        }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun fillsSubmittedTextAndStopsAtTheAutomaticAttemptCap() {
        val cap = maxAttempts()
        val latch = CountDownLatch(cap)
        send.setOnClickListener {
            clicks.incrementAndGet()
            latch.countDown()
        }
        invoke(cap, recognized = "A7K2")
        latch.await(5, TimeUnit.SECONDS)
        assertEquals("A7K2", code.text.toString())
        assertEquals("No debe superar el límite automático", cap, clicks.get())
        // Un intento más ya no vuelve a pulsar: pide recargar la fuente.
        mapField("portalUrls")[web] = "about:blank"
        invoke(1, recognized = "A7K2")
        Thread.sleep(1200)
        assertEquals("No debe superar el límite automático", cap, clicks.get())
        assertEquals("Debe recargar en lugar de repetir", 1, reloads())
    }

    @Test
    fun keepsTheCodeTypedByTheUserAndStillSubmitsIt() {
        val latch = CountDownLatch(1)
        send.setOnClickListener {
            clicks.incrementAndGet()
            latch.countDown()
        }
        code.setText("BXX9")
        invoke(1, recognized = "A7K2")
        latch.await(3, TimeUnit.SECONDS)
        assertEquals("BXX9", code.text.toString())
        assertEquals(1, clicks.get())
    }

    @Test
    fun replacesThePreviousAutomaticCodeWhenANewCaptchaIsRecognised() {
        val latch = CountDownLatch(2)
        send.setOnClickListener {
            clicks.incrementAndGet()
            latch.countDown()
        }
        invoke(1, recognized = "A7K2")
        latch.await(3, TimeUnit.SECONDS)
        invoke(1, recognized = "B9Q4")
        latch.await(3, TimeUnit.SECONDS)
        assertEquals("Debe usar el código de la imagen nueva", "B9Q4", code.text.toString())
        assertEquals(2, clicks.get())
    }

    @Test
    fun doesNotSubmitWhenThereIsNoRecognisedText() {
        invoke(1, recognized = null)
        Thread.sleep(1200)
        assertEquals(0, clicks.get())
        assertEquals("", code.text.toString())
    }

    @Test
    fun reloadsTheSourceWhenTheCaptchaIsRejected() {
        mapField("portalUrls")[web] = "about:blank"
        reload()
        assertEquals("Debe contar una recarga", 1, reloads())
        assertTrue(
            "Debe avisar de la recarga",
            status.text.toString().contains("recargando"),
        )
        assertFalse("El código rechazado no se reutiliza", codes().containsKey(web))
    }

    @Test
    fun stopsReloadingAfterTheCapAndKeepsThePanelForManualEntry() {
        mapField("portalUrls")[web] = "about:blank"
        mapField("captchaReloads")[web] = maxReloads()
        reload()
        assertEquals("No debe recargar más allá del límite", maxReloads(), reloads())
        assertTrue(
            "Debe pedir el código a mano",
            status.text.toString().contains("pulsa Continuar"),
        )
    }

    @Test
    fun neverReloadsAPrioritySource() {
        mapField("priorityIndices")[web] = 1
        mapField("portalUrls")[web] = "about:blank"
        reload()
        assertEquals("El motor prioritario gestiona su propia recarga", 0, reloads())
    }

    private fun invoke(times: Int, recognized: String?) {
        val method =
            UnifiedQueryPanel::class
                .java
                .getDeclaredMethod(
                    "autoVerifyCaptcha",
                    WebView::class.java,
                    EditText::class.java,
                    Button::class.java,
                    TextView::class.java,
                    String::class.java,
                    String::class.java,
                )
        method.isAccessible = true
        scenario.onActivity {
            repeat(times) {
                method.invoke(panel, web, code, send, status, "Prueba", recognized)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun put(field: String, key: WebView, value: android.view.View) {
        val declared = UnifiedQueryPanel::class.java.getDeclaredField(field)
        declared.isAccessible = true
        (declared.get(panel) as MutableMap<WebView, android.view.View>)[key] = value
    }

    private fun reload() {
        val method =
            UnifiedQueryPanel::class
                .java
                .getDeclaredMethod(
                    "reloadSource",
                    WebView::class.java,
                    TextView::class.java,
                    String::class.java,
                )
        method.isAccessible = true
        scenario.onActivity { method.invoke(panel, web, status, "Prueba") }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapField(name: String): MutableMap<WebView, Any> {
        val declared = UnifiedQueryPanel::class.java.getDeclaredField(name)
        declared.isAccessible = true
        return declared.get(panel) as MutableMap<WebView, Any>
    }

    private fun codes(): MutableMap<WebView, Any> = mapField("codes")

    private fun reloads(): Int = (mapField("captchaReloads")[web] as? Int) ?: 0

    private fun maxReloads(): Int {
        val field = UnifiedQueryPanel::class.java.getDeclaredField("MAX_CAPTCHA_RELOADS")
        field.isAccessible = true
        return field.getInt(null)
    }

    private fun maxAttempts(): Int {
        val field =
            UnifiedQueryPanel::class.java.getDeclaredField("MAX_AUTO_CAPTCHA_ATTEMPTS")
        field.isAccessible = true
        return field.getInt(null)
    }
}
