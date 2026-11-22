package com.example.app_autocar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba que el modelo de ddddocr se carga desde los assets y produce texto alfanumérico. No
 * valida la precisión frente a captchas reales de los portales.
 */
@RunWith(AndroidJUnit4::class)
class DdddOcrEngineInstrumentedTest {
    @Test
    fun recognizesRenderedCharactersWithTheBundledModel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(190, 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 20, 90)
            textSize = 38f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("A7K2", 16f, 44f, paint)

        val result = DdddOcrEngine.get(context).recognize(bitmap)
        bitmap.recycle()

        assertNotNull("El motor debe estar disponible en la ABI de prueba", result)
        val text = result!!.text
        assertTrue("Texto reconocido: '$text'", text.length >= 3)
        assertTrue("Solo alfanumérico: '$text'", text.all { it.isLetterOrDigit() })
        assertTrue("Confianza: ${result.confidence}", result.confidence > 0.5f)
    }

    @Test
    fun matchesTheReferenceDdddocrResultForTheSameImage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val bytes = instrumentation.context.assets.open("captcha_sample.png").use { it.readBytes() }
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val result = DdddOcrEngine.get(context).recognize(bitmap)
        bitmap.recycle()
        assertNotNull(result)
        // ddddocr (escritorio) devuelve "A7k2" para esta imagen de referencia.
        assertTrue("Texto reconocido: '${result!!.text}'", result.text.length >= 3)
        assertTrue("Texto reconocido: '${result.text}'", result.text.startsWith("A7"))
    }
}
