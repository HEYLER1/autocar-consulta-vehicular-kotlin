package com.example.app_autocar

import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba el contrato de los formularios asistidos de tránsito: se rellenan los datos pedidos, se
 * coloca el código reconocido en el campo propio del portal y se pulsa su botón, sin alterar
 * verificaciones interactivas. El OCR se prueba aparte; aquí se envía el código directamente.
 */
@RunWith(AndroidJUnit4::class)
class TrafficFormAutomationInstrumentedTest {
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var web: WebView

    private val satUrl = "https://www.sat.gob.pe/VirtualSAT/modulos/Capturas.aspx?tri=C"
    private val sutranUrl =
        "https://webexterno.sutran.gob.pe/WebExterno/Pages/frmPapeletasCinemometro.aspx"

    private val captchaCanvas =
        """
        <canvas id=c width=190 height=60></canvas>
        <script>
        var c=document.getElementById('c'),x=c.getContext('2d');
        x.fillStyle='#ffffff';x.fillRect(0,0,190,60);
        x.fillStyle='#14145a';x.font='bold 38px monospace';x.fillText('A7K2',16,44);
        window.__captchaData=c.toDataURL();c.remove();
        </script>
        """

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            activity.getSharedPreferences("traffic_identity", Context.MODE_PRIVATE).edit()
                .clear().apply()
            web = WebView(activity)
            web.settings.javaScriptEnabled = true
        }
    }

    @After
    fun tearDown() {
        scenario.onActivity {
            TrafficFormAutomation.clear(web)
            web.destroy()
        }
        scenario.close()
    }

    @Test
    fun fillsSatPlateAndSubmitsTheRecognisedCode() {
        val fixture =
            """
            <html><body>
            <form onsubmit='return false'>
              <input id='ctl00_cplPrincipal_txtPlaca'>
              <img class='captcha_class' alt='Visual verification'>
              <input id='ctl00_cplPrincipal_txtCaptcha' maxlength='10'>
              <button type='button' id='ctl00_cplPrincipal_CaptchaContinue'>Buscar</button>
            </form>
            $captchaCanvas
            <script>
            document.querySelector('img.captcha_class').src=window.__captchaData;
            document.getElementById('ctl00_cplPrincipal_CaptchaContinue')
              .addEventListener('click',function(){window.__clicks=(window.__clicks||0)+1;});
            </script>
            </body></html>
            """
        load(satUrl, fixture)
        scenario.onActivity { TrafficFormAutomation.prepare(web, satUrl, "ABC123", false) }
        waitFor("\"ABC123\"", "document.getElementById('ctl00_cplPrincipal_txtPlaca').value")
        waitFor("true", "Boolean(window.__autocarTrafficInput)")
        assertEquals(
            "La placa se rellena sola",
            "\"ABC123\"",
            evaluate("document.getElementById('ctl00_cplPrincipal_txtPlaca').value"),
        )
        scenario.onActivity { TrafficFormAutomation.submit(web, "A7K2") }
        waitFor("\"A7K2\"", "document.getElementById('ctl00_cplPrincipal_txtCaptcha').value")
        assertEquals(
            "El código va al campo del portal",
            "\"A7K2\"",
            evaluate("document.getElementById('ctl00_cplPrincipal_txtCaptcha').value"),
        )
        assertEquals(
            "Se pulsa el botón del portal",
            "true",
            evaluate("Boolean(window.__clicks)"),
        )
    }

    @Test
    fun fillsSutranDocumentAndSkipsTheHardcodedClientCheck() {
        val fixture =
            """
            <html><body>
            <form onsubmit='return false'>
              <input type='radio' name='rbtListMovimiento' value='A' checked>
              <input type='radio' name='rbtListMovimiento' value='P'>
              <select id='ddlTipoBusqueda'><option value='2' selected>DNI</option></select>
              <input id='TxtBuscar'>
              <input id='txtCodigoVerificador'>
              <input id='txtPlaca'>
              <iframe id='iimage'></iframe>
              <input id='TxtCodImagen' maxlength='4'>
              <input type='submit' id='BtnBuscar' value='Buscar' onclick='return false;'>
            </form>
            $captchaCanvas
            <script>
            var f=document.getElementById('iimage'),d=f.contentDocument;
            d.open();d.write('<img id=cap>');d.close();
            d.getElementById('cap').src=window.__captchaData;
            document.getElementById('BtnBuscar')
              .addEventListener('click',function(){window.__clicks=(window.__clicks||0)+1;});
            </script>
            </body></html>
            """
        scenario.onActivity {
            web.context.getSharedPreferences("traffic_identity", Context.MODE_PRIVATE).edit()
                .putString("dni", "4100027").putString("digit", "V").apply()
        }
        load(sutranUrl, fixture)
        scenario.onActivity { TrafficFormAutomation.prepare(web, sutranUrl, "ABC123", false) }
        waitFor("\"4100027\"", "document.getElementById('TxtBuscar').value")
        waitFor("true", "Boolean(window.__autocarTrafficInput)")
        assertEquals("Se rellena el documento", "\"4100027\"", evaluate("document.getElementById('TxtBuscar').value"))
        assertEquals("Se rellena el verificador", "\"V\"", evaluate("document.getElementById('txtCodigoVerificador').value"))
        scenario.onActivity { TrafficFormAutomation.submit(web, "A7K2") }
        waitFor("\"A7K2\"", "document.getElementById('TxtCodImagen').value")
        assertEquals("El código va al campo del portal", "\"A7K2\"", evaluate("document.getElementById('TxtCodImagen').value"))
        assertEquals("Se anula la comprobación fija del cliente", "true", evaluate("document.getElementById('BtnBuscar').onclick===null"))
        assertEquals("Se pulsa el botón del portal", "true", evaluate("Boolean(window.__clicks)"))
    }

    @Test
    fun solvesSatCaptchaWithTheLocalOcr() {
        val fixture =
            """
            <html><body>
            <form onsubmit='return false'>
              <input id='ctl00_cplPrincipal_txtPlaca'>
              <img class='captcha_class' alt='Visual verification'>
              <input id='ctl00_cplPrincipal_txtCaptcha' maxlength='10'>
              <button type='button' id='ctl00_cplPrincipal_CaptchaContinue'>Buscar</button>
            </form>
            $captchaCanvas
            <script>
            document.querySelector('img.captcha_class').src=window.__captchaData;
            document.getElementById('ctl00_cplPrincipal_CaptchaContinue')
              .addEventListener('click',function(){window.__clicks=(window.__clicks||0)+1;});
            </script>
            </body></html>
            """
        load(satUrl, fixture)
        scenario.onActivity { TrafficFormAutomation.fill(web, satUrl, "ABC123") }
        waitFor("true", "Boolean(window.__clicks)")
        val submitted = evaluate("document.getElementById('ctl00_cplPrincipal_txtCaptcha').value")
        assertTrue(
            "El OCR reconoce y envía un código: $submitted",
            submitted.trim('"').length >= 3,
        )
    }

    @Test
    fun fallsBackToPlateModeWhenNoDocumentIsSaved() {
        val fixture =
            """
            <html><body>
            <form onsubmit='return false'>
              <input type='radio' name='rbtListMovimiento' value='A' checked>
              <input type='radio' name='rbtListMovimiento' value='P'>
              <input id='txtPlaca'>
              <input id='TxtCodImagen' maxlength='4'>
              <input type='submit' id='BtnBuscar' value='Buscar'>
            </form>
            </body></html>
            """
        load(sutranUrl, fixture)
        scenario.onActivity { TrafficFormAutomation.prepare(web, sutranUrl, "ABC123", false) }
        waitFor("true", "document.querySelector('input[value=P]').checked")
        // El portal cambia de modo con un postback; tras él, un nuevo intento rellena la placa.
        scenario.onActivity { TrafficFormAutomation.prepare(web, sutranUrl, "ABC123", false) }
        waitFor("\"ABC123\"", "document.getElementById('txtPlaca').value")
        assertEquals(
            "Sin documento guardado se consulta por placa",
            "\"ABC123\"",
            evaluate("document.getElementById('txtPlaca').value"),
        )
    }

    private fun load(url: String, html: String) {
        val latch = CountDownLatch(1)
        scenario.onActivity {
            web.webViewClient =
                object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, loaded: String?) {
                        latch.countDown()
                    }
                }
            web.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
        }
        assertTrue("La página de prueba carga", latch.await(10, TimeUnit.SECONDS))
    }

    private fun waitFor(expected: String, script: String) {
        repeat(60) {
            if (evaluate(script) == expected) return
            Thread.sleep(100)
        }
    }

    private fun evaluate(script: String): String {
        val latch = CountDownLatch(1)
        var value = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            web.evaluateJavascript(script) {
                value = it
                latch.countDown()
            }
        }
        assertTrue("JavaScript responde", latch.await(8, TimeUnit.SECONDS))
        return value
    }
}
