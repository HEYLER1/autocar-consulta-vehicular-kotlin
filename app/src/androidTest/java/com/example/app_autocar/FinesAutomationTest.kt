package com.example.app_autocar

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Offline WebView checks use real selector shapes, not real fines or captcha solutions. */
@RunWith(AndroidJUnit4::class)
class FinesAutomationTest {
    @Test fun historyOnlyFillsHomeAndKeyboardKeepsLayoutStable(){
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext;val preferences=context.getSharedPreferences("autocar_preferences",0)
        val oldHistory=preferences.getString("recent_plates","");val oldPlate=preferences.getString("current_plate","")
        preferences.edit().putString("recent_plates","ABC123").putString("current_plate","").commit()
        val activity=instrumentation.startActivitySync(android.content.Intent(context,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val input=activity.findViewById<android.widget.EditText>(R.id.plate_input)
        val root=activity.findViewById<android.view.View>(R.id.main_root)
        fun waitForKeyboard(visible:Boolean){var found=false;repeat(40){instrumentation.runOnMainSync{found=androidx.core.view.ViewCompat.getRootWindowInsets(root)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==visible};if(!found)Thread.sleep(100)};assertTrue("Keyboard visibility: $visible",found)}
        try{
            instrumentation.runOnMainSync{
                activity.findViewById<android.widget.LinearLayout>(R.id.recent_container).getChildAt(0).performClick()
                assertEquals("ABC-123",input.text.toString())
                assertNull(MainActivity::class.java.getDeclaredField("inlineQuery").apply{isAccessible=true}.get(activity))
                assertFalse(activity.isFinishing)
                assertEquals(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,activity.window.attributes.softInputMode and android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                input.requestFocus();(activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            waitForKeyboard(true)
            instrumentation.runOnMainSync{assertEquals(android.view.View.GONE,activity.findViewById<android.view.View>(R.id.bottom_navigation).visibility);assertSame(input,activity.findViewById<android.widget.EditText>(R.id.plate_input));(activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(input.windowToken,0)}
            waitForKeyboard(false)
            instrumentation.runOnMainSync{assertEquals(android.view.View.VISIBLE,activity.findViewById<android.view.View>(R.id.bottom_navigation).visibility);assertEquals("ABC-123",input.text.toString())}
        }finally{instrumentation.runOnMainSync{activity.finish()};preferences.edit().putString("recent_plates",oldHistory).putString("current_plate",oldPlate).commit()}
    }
    @Test fun secondaryScreensHaveReadableSourcesAndSearch(){
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val activity=instrumentation.startActivitySync(android.content.Intent(context,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try{instrumentation.runOnMainSync{
            val navigation=activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            navigation.selectedItemId=R.id.nav_fines
            assertEquals(android.view.View.VISIBLE,activity.findViewById<android.view.View>(R.id.source_search).visibility)
            val search=activity.findViewById<android.widget.EditText>(R.id.source_search);search.setText("ciudad inexistente")
            val sources=activity.findViewById<android.widget.LinearLayout>(R.id.report_sources)
            assertTrue((0 until sources.childCount).map{sources.getChildAt(it)}.filterIsInstance<android.widget.TextView>().any{it.text.toString().contains("No encontramos")})
            navigation.selectedItemId=R.id.nav_vehicle
            assertEquals(android.view.View.GONE,search.visibility)
            assertTrue(sources.childCount>=3)
            navigation.selectedItemId=R.id.nav_insurance
            assertTrue(sources.childCount>=2)
        }}finally{instrumentation.runOnMainSync{activity.finish()}}
        val sunarp=instrumentation.startActivitySync(android.content.Intent(context,SunarpServicesActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("plate","ABC123")) as SunarpServicesActivity
        try{instrumentation.runOnMainSync{assertEquals("",sunarp.findViewById<android.widget.TextView>(R.id.sunarp_back).text.toString());assertTrue(sunarp.findViewById<android.widget.LinearLayout>(R.id.sunarp_documents).childCount>=4)}}finally{instrumentation.runOnMainSync{sunarp.finish()}}
    }
    @Test fun readmeHomeScreenshotUsesOnlyExamplePlate(){
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val preferences=context.getSharedPreferences("autocar_preferences",0)
        val oldPlate=preferences.getString("current_plate","")
        val oldSelected=preferences.getStringSet("selected_fines_sources",emptySet())!!.toSet()
        val oldDefaults=preferences.getBoolean("priority_defaults_v1",false)
        preferences.edit().putString("current_plate","ABC123").putStringSet("selected_fines_sources",emptySet()).putBoolean("priority_defaults_v1",false).commit()
        val activity=instrumentation.startActivitySync(android.content.Intent(context,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try{
            instrumentation.runOnMainSync{activity.findViewById<android.view.View>(R.id.recent_section).visibility=android.view.View.GONE}
            instrumentation.waitForIdleSync()
            Thread.sleep(800) // Let layout and the Activity enter animation finish before capture.
            val bitmap=instrumentation.uiAutomation.takeScreenshot()
            try{java.io.FileOutputStream(java.io.File(context.cacheDir,"readme_home.png")).use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}finally{bitmap.recycle()}
        }finally{instrumentation.runOnMainSync{activity.finish()};preferences.edit().putString("current_plate",oldPlate).putStringSet("selected_fines_sources",oldSelected).putBoolean("priority_defaults_v1",oldDefaults).commit()}
    }
    @Test fun fullScreenGeneralReportRendersSnapshot(){
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val folder=java.io.File(context.cacheDir,"reports");folder.mkdirs();val file=java.io.File(folder,"general_${java.util.UUID.randomUUID()}.json")
        val data=org.json.JSONObject().put("plate","ABC123").put("progress","100% · 4 con datos · Finalizado").put("created",System.currentTimeMillis()).put("summary","RESUMEN DEL VEHÍCULO · ABC-123\n\nDatos del vehículo\n• SUNARP: información ficticia\n\nSeguro e historial\n• SOAT: póliza de prueba\n\nRevisión técnica\n• MTC: resultado ficticio\n\nMultas y papeletas\n• SUTRAN: respuesta de prueba").put("detail","DATOS FICTICIOS PARA COMPROBAR EL DISEÑO").put("pdf","")
        java.io.FileOutputStream(file).use{it.write(data.toString().toByteArray())}
        val activity=instrumentation.startActivitySync(android.content.Intent(context,GeneralReportActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("snapshot",file.name)) as GeneralReportActivity
        try{instrumentation.waitForIdleSync();Thread.sleep(800);assertFalse(activity.isFinishing);val bitmap=instrumentation.uiAutomation.takeScreenshot();try{java.io.FileOutputStream(java.io.File(context.cacheDir,"general_report_ui_test.png")).use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}finally{bitmap.recycle()}}
        finally{instrumentation.runOnMainSync{activity.finish()}}
    }
    @Test fun queryStaysInsideHome(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val activity=instrumentation.startActivitySync(android.content.Intent(instrumentation.targetContext,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try{instrumentation.runOnMainSync{
            activity.findViewById<android.widget.EditText>(R.id.plate_input).setText("ABC123")
            val sourceClass=OfficialSource::class.java
            val constructor=sourceClass.declaredConstructors.first().apply{isAccessible=true}
            val source=constructor.newInstance("municipal","Prueba ficticia","","","about:blank","P")
            MainActivity::class.java.getDeclaredMethod("startFinesQuery",List::class.java).apply{isAccessible=true}.invoke(activity,listOf(source))
            val panel=MainActivity::class.java.getDeclaredField("inlineQuery").apply{isAccessible=true}.get(activity) as UnifiedQueryPanel
            val slot=MainActivity::class.java.getDeclaredField("inlineQuerySlot").apply{isAccessible=true}.get(activity) as android.widget.LinearLayout
            assertSame(slot,panel.view().parent)
            panel.view().measure(android.view.View.MeasureSpec.makeMeasureSpec(1080,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED))
            assertTrue("Collapsed query must not reserve the hidden browser viewport",panel.view().measuredHeight<180*activity.resources.displayMetrics.density)
            assertNotNull(activity.findViewById<android.view.View>(R.id.plate_input))
            assertEquals(R.id.nav_home,activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
            assertFalse(activity.isFinishing)
            assertTrue(activity.findViewById<android.view.View>(R.id.search_plate).isEnabled)
            assertEquals("Cancelar",activity.findViewById<android.widget.TextView>(R.id.search_plate).text.toString())
            MainActivity::class.java.getDeclaredMethod("startFinesQuery",List::class.java).apply{isAccessible=true}.invoke(activity,listOf(source))
            assertSame("Repeated requests must not replace the active session",panel,MainActivity::class.java.getDeclaredField("inlineQuery").apply{isAccessible=true}.get(activity))
            activity.findViewById<android.view.View>(R.id.search_plate).performClick()
            assertTrue(activity.findViewById<android.view.View>(R.id.search_plate).isEnabled)
            assertEquals(0,slot.childCount)
        }}finally{instrumentation.runOnMainSync{activity.finish()}}
    }
    @Test fun progressDistinguishesErrorsAndPreservesFullDetail(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val intent=android.content.Intent(instrumentation.targetContext,FinesBatchActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("plate","ABC123").putExtra("names",arrayOf("Prueba 1","Prueba 2")).putExtra("urls",arrayOf("about:blank","about:blank"))
        val activity=instrumentation.startActivitySync(intent) as FinesBatchActivity
        fun field(name:String):Any=UnifiedQueryPanel::class.java.getDeclaredField(name).apply{isAccessible=true}.get(activity.panel)!!
        try{instrumentation.runOnMainSync{
            (field("handler") as android.os.Handler).removeCallbacksAndMessages(null)
            @Suppress("UNCHECKED_CAST") val portals=field("portals") as List<WebView>
            @Suppress("UNCHECKED_CAST") val summaries=field("summaries") as Map<WebView,android.widget.TextView>
            @Suppress("UNCHECKED_CAST") val raw=field("rawDetails") as MutableMap<WebView,String>
            @Suppress("UNCHECKED_CAST") val failed=field("failed") as MutableSet<WebView>
            val complete=(1..25).joinToString("\n"){"Dato ficticio $it"};raw[portals[0]]=complete;summaries[portals[0]]!!.text="Resumen de prueba"
            assertTrue((field("queryProgressText") as android.widget.TextView).text.startsWith("50%"))
            failed.add(portals[1]);UnifiedQueryPanel::class.java.getDeclaredMethod("updateCounts").apply{isAccessible=true}.invoke(activity.panel)
            assertTrue((field("queryProgressText") as android.widget.TextView).text.toString().contains("100% · 1 con datos · 1 sin completar"))
            assertEquals(android.view.View.GONE,(field("resultsScroll") as android.view.View).visibility)
            (field("resultTab") as android.view.View).performClick()
            assertEquals(android.view.View.VISIBLE,(field("resultsScroll") as android.view.View).visibility)
            @Suppress("UNCHECKED_CAST") val buttons=field("detailButtons") as Map<WebView,android.widget.Button>
            @Suppress("UNCHECKED_CAST") val details=field("fullDetails") as Map<WebView,android.widget.TextView>
            buttons[portals[0]]!!.performClick();assertEquals(complete,details[portals[0]]!!.text.toString());assertEquals(android.view.View.VISIBLE,details[portals[0]]!!.visibility)
            assertEquals(android.view.View.GONE,(field("verificationTab") as android.view.View).visibility)
        }}finally{instrumentation.runOnMainSync{activity.finish()}}
    }
    @Test fun homeOffersSourceSelectionWithoutStartingQueries() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as MainActivity
        try {
            instrumentation.runOnMainSync {
                val button = activity.findViewById<android.widget.TextView>(R.id.search_plate)
                assertTrue(button.text.toString().startsWith("Consultar"))
                val labels=mutableListOf<String>()
                fun collect(view:android.view.View){
                    if(view is com.google.android.material.button.MaterialButton && view.isCheckable)labels.add(view.text.toString())
                    if(view is android.view.ViewGroup)for(i in 0 until view.childCount)collect(view.getChildAt(i))
                }
                collect(activity.window.decorView)
                assertEquals(listOf("SUNARP","SOAT","SUTRAN","MTC"),labels.take(4))
                fun findOption(view: android.view.View): android.widget.TextView? {
                    if(view is com.google.android.material.button.MaterialButton && view.text.toString()=="Cusco")return view
                    if(view is android.view.ViewGroup)for(i in 0 until view.childCount){val found=findOption(view.getChildAt(i));if(found!=null)return found}
                    return null
                }
                val option=findOption(activity.window.decorView)
                assertNotNull("Inline city option",option)
                fun cityToggle(view: android.view.View): android.widget.TextView? {
                    if(view is android.widget.TextView && view.text.toString().startsWith("Multas por ciudad"))return view
                    if(view is android.view.ViewGroup)for(i in 0 until view.childCount){val found=cityToggle(view.getChildAt(i));if(found!=null)return found}
                    return null
                }
                val toggle=cityToggle(activity.window.decorView)!!
                val cityContainer=option!!.parent.parent as android.view.View
                assertEquals(android.view.View.GONE,cityContainer.visibility)
                toggle.performClick()
                assertEquals(android.view.View.VISIBLE,cityContainer.visibility)
                val before=button.text.toString();option!!.performClick()
                assertNotEquals(before,button.text.toString())
                option.performClick() // Restore saved selection; no real query is made.
                assertEquals(before,button.text.toString())
                toggle.performClick()
                assertEquals(android.view.View.GONE,cityContainer.visibility)
                assertFalse(activity.isFinishing)
            }
            instrumentation.waitForIdleSync()
            val bitmap=instrumentation.uiAutomation.takeScreenshot()
            try {
                java.io.FileOutputStream(java.io.File(instrumentation.targetContext.cacheDir,"home_compact.png")).use { output -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output) }
            } finally { bitmap.recycle() }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
    private fun evaluate(web: WebView, script: String): String {
        val latch = CountDownLatch(1)
        var value = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            web.evaluateJavascript(script) { value = it; latch.countDown() }
        }
        assertTrue("JavaScript callback", latch.await(8, TimeUnit.SECONDS))
        return value
    }

    private fun check(url: String, html: String, expected: String, expectedClicks: Int = if(expected == "sent") 1 else 0) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val loaded = CountDownLatch(1)
        lateinit var web: WebView
        instrumentation.runOnMainSync {
            web = WebView(instrumentation.targetContext)
            web.settings.javaScriptEnabled = true
            web.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) { loaded.countDown() }
            }
            web.loadDataWithBaseURL(url, "<html><body>$html<script>window.clicks=0;document.querySelectorAll('button,input[type=button],input[type=submit]').forEach(function(b){b.addEventListener('click',function(e){e.preventDefault();window.clicks++;});});document.querySelectorAll('a').forEach(function(a){a.addEventListener('click',function(e){e.preventDefault();});});</script></body></html>", "text/html", "UTF-8", null)
        }
        try {
            assertTrue("Page loaded: $url", loaded.await(8, TimeUnit.SECONDS))
            val adapter = FinesPortalAdapter.forUrl(url)
            var result = evaluate(web, adapter.fillScript("xd4-345"))
            if (result == "\"waiting\"") result = evaluate(web, adapter.fillScript("xd4345"))
            assertEquals(url, "\"$expected\"", result)
            assertEquals("\"XD4345\"", evaluate(web, "document.querySelector(${FinesPortalAdapter.quote(adapter.plate)}).value"))
            evaluate(web, adapter.fillScript("xd4345"))
            assertEquals("Only one legitimate query or challenge request: $url", expectedClicks.toString(), evaluate(web, "window.clicks"))
        } finally { instrumentation.runOnMainSync { web.destroy() } }
    }

    @Test fun submitsSourcesWithoutCaptchaOnce() {
        check("https://consultavehicular.sunarp.gob.pe/consulta-vehicular/", "<form><input id=placa><button>Consultar vehículo</button></form>", "sent")
        check("https://servicios.sbs.gob.pe/reportesoat/", "<form><input id=ctl00_MainBodyContent_txtPlaca><input type=hidden id=ctl00_MainBodyContent_hdnReCaptchaV3 value=official-test-token><button id=ctl00_MainBodyContent_btnIngresarPla>Consultar</button></form>", "sent")
        check("https://m.satica.gob.pe/consultapapeletas_web.php", "<form name=buscar><input name=placa required><button type=submit>Buscar papeletas</button></form>", "sent")
        check("https://virtualsatch.satch.gob.pe/virtualsatch/record_infracciones/buscar_placa_", "<form id=frm><input name=search><button type=submit>Buscar</button></form>", "sent")
        check("https://cusco.gob.pe/informatica/", "<select id=sl_tipo><option value=P selected>Placa</option></select><input id=tx_numero><button id=bt_consultar>Consultar</button>", "sent")
        check("https://www.munihuanuco.gob.pe/consulta", "<form id=consulta_placa><input id=placa required><button type=submit>Buscar</button></form>", "sent")
        check("https://app.munichachapoyas.gob.pe/consulta", "<input id=placa_cnt><button id=btnConsulta>Consultar</button>", "sent")
        check("http://consultas.municportillo.gob.pe:85/consultaVehiculo/consulta/", "<form name=f1><input id=placa><button type=submit>Consultar</button></form>", "sent")
        check("https://www.muniarequipa.gob.pe/consulta", "<form><input id=placa><input type=button id=btnConsultar value=CONSULTAR></form>", "sent")
        check("https://www.satcajamarca.gob.pe/consultas", "<a href='#menu1'>Tránsito</a><select id=opcion_busqueda_record><option value=1>Infractor</option><option value=2>Placa</option></select><div id=menu1><input class=form-control></div><div id=field_1><button class=next>Buscar</button></div>", "sent")
        check("http://www.munipiura.gob.pe/consulta-de-multas-de-transito", "<a id=tab-buscar-por-placa href='#buscar-por-placa'>Placa</a><div id=buscar-por-placa><form target=popup><input name=PlaMot><input type=submit value=Buscar></form></div>", "sent")
    }

    @Test fun fillsPlateButWaitsForImageCaptcha() {
        check("https://rec.mtc.gob.pe/Citv/ArConsultaCitv", "<select id=selBUS_Filtro><option value=2>Certificado</option><option value=1>Placa</option></select><form><input id=texFiltro><img id=imgCaptcha><input id=texCaptcha><button id=btnBuscar>Buscar</button></form>", "captcha")
        check("https://portal.mtc.gob.pe/reportedgtt/form/frmconsultaplacaitv.aspx", "<form><input id=texFiltro><img id=imgCaptcha><input id=texCaptcha><button id=btnBuscar>Buscar</button></form>", "captcha")
        check("https://webexterno.sutran.gob.pe/consulta", "<form><input id=txtPlaca><input id=TxtCodImagen maxlength=4><button id=BtnBuscar>Buscar</button></form>", "captcha")
        check("https://pagopapeletascallao.pe/", "<select id=tipo_busqueda><option value=1>Placa</option></select><form><input id=valor_busqueda><input id=captcha required><button id=idBuscar>Buscar</button></form>", "captcha")
        check("https://www.munitacna.gob.pe/consulta", "<select id=opcion><option value=dni>DNI</option><option value=placa>Placa</option></select><form class=form-mpt><input id=busca><input id=codigo><button class=btn-danger>Buscar</button></form>", "captcha")
        check("https://sat-t.gob.pe/", "<form><input id=placa_vehiculo><input id=gd_captcha_code><button id=btn_buscar_papeleta title=Buscar></button></form>", "captcha")
    }

    @Test fun leavesInteractiveVerificationToUser() {
        check("https://www.sat.gob.pe/VirtualSAT/modulos/papeletas.aspx", "<form><input id=ctl00_cplPrincipal_txtPlaca><div class=g-recaptcha></div><button id=ctl00_cplPrincipal_CaptchaContinue>Buscar</button></form>", "interactive")
        check("https://pasarela.atu.gob.pe/", "<select id=TipoBusquedaselectElemento><option value=1>Acta</option><option value=2>Placa</option></select><form id=formBusqueda><input id=PlacaBusquedainputElemento><div class=g-recaptcha></div><button type=submit>search BUSCAR</button></form>", "interactive", expectedClicks=1)
    }

    @Test fun unifiedVerificationUiKeepsCaptchaSeparateFromResults() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = android.content.Intent(instrumentation.targetContext, FinesBatchActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("plate", "ABC123")
            .putExtra("names", arrayOf("Chiclayo · prueba ficticia", "SUTRAN · prueba ficticia"))
            .putExtra("urls", arrayOf("about:blank", "about:blank"))
        val activity = instrumentation.startActivitySync(intent) as FinesBatchActivity
        fun field(name: String): Any = requireNotNull(UnifiedQueryPanel::class.java.getDeclaredField(name).apply { isAccessible = true }.get(activity.panel))
        @Suppress("UNCHECKED_CAST") val views = field("portals") as ArrayList<WebView>
        @Suppress("UNCHECKED_CAST") val statuses = field("reportStatuses") as ArrayList<android.widget.TextView>
        val fixture = "<form onsubmit='return false'><input id=txtPlaca><input id=TxtCodImagen maxlength=4><button id=BtnBuscar>Buscar</button><img id=imgCaptcha></form><canvas id=c width=170 height=52></canvas><script>var c=document.getElementById('c'),x=c.getContext('2d');x.fillStyle='#ffffff';x.fillRect(0,0,170,52);x.fillStyle='#002B5C';x.font='24px sans-serif';x.fillText('PRUEBA',20,35);document.getElementById('imgCaptcha').src=c.toDataURL();c.remove();</script>"
        try {
            instrumentation.runOnMainSync {
                (field("handler") as android.os.Handler).removeCallbacksAndMessages(null)
                views[1].loadDataWithBaseURL("https://webexterno.sutran.gob.pe/test", "<html><body>$fixture</body></html>", "text/html", "UTF-8", null)
            }
            for (attempt in 0 until 20) { if (evaluate(views[1], "Boolean(document.querySelector('#imgCaptcha')&&document.querySelector('#imgCaptcha').naturalWidth>0)") == "true") break else Thread.sleep(100) }
            val fill = UnifiedQueryPanel::class.java.getDeclaredMethod("fillAndConsult", WebView::class.java, String::class.java, android.widget.TextView::class.java, String::class.java).apply { isAccessible = true }
            instrumentation.runOnMainSync { fill.invoke(activity.panel, views[1], "ABC123", statuses[1], "SUTRAN · prueba ficticia") }
            @Suppress("UNCHECKED_CAST") val panels = field("verificationPanels") as Map<WebView, android.widget.LinearLayout>
            var visible = false
            repeat(25) {
                instrumentation.runOnMainSync { visible = panels[views[1]]!!.visibility == android.view.View.VISIBLE && panels[views[1]]!!.childCount >= 2 }
                if (!visible) Thread.sleep(100)
            }
            assertTrue("Native captcha image and input visible; status=${statuses[1].text}; DOM=" + evaluate(views[1], "JSON.stringify({url:location.href,image:!!document.querySelector('#imgCaptcha'),input:!!document.querySelector('#TxtCodImagen')})"), visible)
            assertEquals("Plate filled without sending captcha", "\"ABC123\"", evaluate(views[1], "document.querySelector('#txtPlaca').value"))
            instrumentation.runOnMainSync {
                val bitmap = instrumentation.uiAutomation.takeScreenshot()
                java.io.FileOutputStream(java.io.File(activity.cacheDir, "fines_verification_ui_test.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
            @Suppress("UNCHECKED_CAST") val codes = field("codes") as Map<WebView, android.widget.EditText>
            @Suppress("UNCHECKED_CAST") val sends = field("codeButtons") as Map<WebView, android.widget.Button>
            val before = evaluate(views[1], "location.href")
            instrumentation.runOnMainSync { codes[views[1]]!!.setText("ABCD"); sends[views[1]]!!.performClick() }
            for (attempt in 0 until 20) { if (evaluate(views[1], "document.querySelector('#TxtCodImagen').value") == "\"ABCD\"") break else Thread.sleep(100) }
            assertEquals("Manual code passed to the original input", "\"ABCD\"", evaluate(views[1], "document.querySelector('#TxtCodImagen').value"))
            assertEquals("No page reload when continuing", before, evaluate(views[1], "location.href"))
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
