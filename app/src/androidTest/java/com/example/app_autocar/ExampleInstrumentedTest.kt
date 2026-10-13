package com.example.app_autocar

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun finesPdfContainsReadablePages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sources = listOf(
            FinesPdfReport.Source("Fuente de prueba", "https://example.test/consulta", "Información recibida", "Papeleta: EJEMPLO-001\nEstado: pendiente\nImporte: dato de prueba, no deuda real"),
            FinesPdfReport.Source("Verificación pendiente", "https://example.test/captcha", "Captcha pendiente", ""),
            FinesPdfReport.Source("Prueba de paginación", "https://example.test/resultado", "Información recibida", (1..55).joinToString("\n") { "Campo de prueba $it: información ficticia para comprobar saltos de página y márgenes." })
        )
        val report = FinesPdfReport.write(context, "ABC123", sources)
        assertTrue(report.length() > 1000)
        report.copyTo(java.io.File(context.cacheDir, "reports/pdf_layout_test.pdf"), overwrite = true)
        android.os.ParcelFileDescriptor.open(report, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            android.graphics.pdf.PdfRenderer(descriptor).use { renderer ->
                assertTrue(renderer.pageCount >= 2)
                for (index in 0 until renderer.pageCount) renderer.openPage(index).use { page ->
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                    val bitmap=android.graphics.Bitmap.createBitmap(595,842,android.graphics.Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    java.io.FileOutputStream(java.io.File(context.cacheDir,"pdf_unified_$index.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                    bitmap.recycle()
                }
            }
        }
    }
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.app_autocar", appContext.packageName)
    }
}
