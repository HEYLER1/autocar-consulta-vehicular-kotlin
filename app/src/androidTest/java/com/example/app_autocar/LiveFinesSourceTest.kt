package com.example.app_autocar

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.webkit.WebView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in only. Never runs real queries without explicit runtime arguments. */
@RunWith(AndroidJUnit4::class)
class LiveFinesSourceTest {
    @Test fun observesRealAutomaticSources() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveSources") == "true")
        val plate = args.getString("testPlate") ?: ""
        assumeTrue(PlateUtils.isValidPeruvianPlate(plate))
        val allNames = arrayOf("Arequipa", "Cusco", "Chachapoyas", "Huánuco", "Pucallpa", "Cajamarca", "Chiclayo", "Ica", "Piura", "SUTRAN", "Callao", "Tacna", "Tarapoto", "SAT Lima", "ATU", "Trujillo", "MTC")
        val allUrls = arrayOf(
            "https://www.muniarequipa.gob.pe/oficina-virtual/c0nInfrPermisos/faltas/papeletas.php",
            "https://cusco.gob.pe/informatica/index.php/",
            "https://app.munichachapoyas.gob.pe/servicios/consulta_papeletas/app/papeletas.php",
            "https://www.munihuanuco.gob.pe/wp-content/servicios/transportes/gt_papeletas.php",
            "http://consultas.municportillo.gob.pe:85/consultaVehiculo/consulta/",
            "https://www.satcajamarca.gob.pe/consultas",
            "https://virtualsatch.satch.gob.pe/virtualsatch/record_infracciones/buscar_placa_",
            "https://m.satica.gob.pe/consultapapeletas_web.php",
            "http://www.munipiura.gob.pe/consulta-de-multas-de-transito#buscar-por-placa",
            "https://webexterno.sutran.gob.pe/WebExterno/Pages/frmRecordInfracciones.aspx",
            "https://pagopapeletascallao.pe/",
            "https://www.munitacna.gob.pe/pagina/sf/servicios/papeletas",
            "https://sat-t.gob.pe/",
            "https://www.sat.gob.pe/VirtualSAT/principal.aspx",
            "https://pasarela.atu.gob.pe/",
            "https://www.gob.pe/53428-verificar-informacion-de-papeletas-de-transito-y-transporte-en-trujillo",
            "https://rec.mtc.gob.pe/Citv/ArConsultaCitv"
        )
        val selected = args.getString("onlySources", "").split(',').filter { it.isNotBlank() }
        val indices = allNames.indices.filter { selected.isEmpty() || allNames[it] in selected }
        val names = indices.map { allNames[it] }.toTypedArray()
        val urls = indices.map { allUrls[it] }.toTypedArray()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, FinesBatchActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("plate", plate).putExtra("names", names).putExtra("urls", urls)
        val activity = instrumentation.startActivitySync(intent) as FinesBatchActivity
        fun field(name: String): Any = requireNotNull(UnifiedQueryPanel::class.java.getDeclaredField(name).apply { isAccessible = true }.get(activity.panel))
        @Suppress("UNCHECKED_CAST") val portals = field("portals") as List<WebView>
        @Suppress("UNCHECKED_CAST") val submitted = field("submitted") as Set<WebView>
        @Suppress("UNCHECKED_CAST") val failed = field("failed") as Set<WebView>
        @Suppress("UNCHECKED_CAST") val details = field("reportDetails") as List<TextView>
        @Suppress("UNCHECKED_CAST") val codes = field("codes") as Map<WebView, *>
        @Suppress("UNCHECKED_CAST") val verificationCards = field("verificationCards") as Map<WebView, android.view.View>
        var received = 0
        try {
            val waitSeconds = args.getString("waitSeconds")?.toLongOrNull()?.coerceIn(25,110) ?: 105
            val deadline = SystemClock.elapsedRealtime() + waitSeconds*1000
            while (SystemClock.elapsedRealtime() < deadline) {
                Thread.sleep(1000)
                instrumentation.runOnMainSync { received = details.count { it.text.isNotBlank() } }
            }
            instrumentation.runOnMainSync {
                val phases = portals.mapIndexed { index, web ->
                    val state = when {
                        details[index].text.isNotBlank() -> "RECEIVED"
                        failed.contains(web) -> "UNAVAILABLE"
                        codes.containsKey(web) -> "CAPTCHA_IMAGE"
                        verificationCards[web]?.visibility == android.view.View.VISIBLE -> "INTERACTIVE_VERIFICATION"
                        submitted.contains(web) -> "SENT_NO_EXTRACTED_RESULT"
                        else -> "NOT_COMPLETED"
                    }
                    "${names[index]}=$state"
                }
                instrumentation.sendStatus(0, Bundle().apply { putString("stream", "\nLIVE_SOURCE_PHASES: ${phases.joinToString("; ")}\n") })
            }
            for ((index, web) in portals.withIndex()) {
                val latch = java.util.concurrent.CountDownLatch(1)
                val adapter = FinesPortalAdapter.forUrl(urls[index])
                val control = if(adapter.plate.isBlank()) "null" else "document.querySelector(${FinesPortalAdapter.quote(adapter.plate)})"
                val button = if(adapter.button.isBlank()) "null" else "document.querySelector(${FinesPortalAdapter.quote(adapter.button)})"
                var metadata = ""
                instrumentation.runOnMainSync {
                    web.evaluateJavascript("(function(){var e=$control,b=$button;return JSON.stringify({path:location.hostname+location.pathname,plateControl:!!e,plateMatches:e?e.value.replace(/[^A-Z0-9]/gi,'').toUpperCase()===${FinesPortalAdapter.quote(PlateUtils.normalize(plate))}:false,button:!!b,formValid:e&&e.form?e.form.checkValidity():null,queryFlag:!!window.__autocarSubmitted,rows:document.querySelectorAll('tbody tr').length,negativeMessage:/libre de infracciones|su b[uú]squeda no produjo resultados/i.test(document.body.innerText)});})();") { metadata = it; latch.countDown() }
                }
                latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                instrumentation.sendStatus(0, Bundle().apply { putString("stream", "\nFORM_METADATA ${names[index]}: $metadata\n") })
            }
            if(names.contentEquals(arrayOf("MTC")))assertTrue("MTC must expose its original human verification or a result",received>0||codes.isNotEmpty())
            else assertTrue("At least one real source returned extractable information; this is not full source validation", received > 0)
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
