package com.example.app_autocar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica la decodificación CTC usada por ddddocr sin cargar el modelo en la JVM. */
class DdddOcrEngineTest {
    private val charset = mapOf(1 to 'A', 2 to 'B', 3 to '7')

    private fun decode(steps: Int, classes: Int, vararg rows: FloatArray): DdddOcrEngine.Result {
        val logits = FloatArray(steps * classes)
        rows.forEachIndexed { index, row -> row.copyInto(logits, index * classes) }
        return DdddOcrEngine.decodeText(logits, steps, classes, charset)
    }

    @Test
    fun collapsesRepeatedIndexesAndBlanks() {
        val result = decode(
            4,
            5,
            floatArrayOf(0f, 9f, 0f, 0f, 0f),
            floatArrayOf(0f, 8f, 0f, 0f, 0f),
            floatArrayOf(9f, 0f, 0f, 0f, 0f),
            floatArrayOf(0f, 0f, 9f, 0f, 0f)
        )
        assertEquals("AB", result.text)
        assertTrue("Confianza: ${result.confidence}", result.confidence > 0.99f)
    }

    @Test
    fun ignoresCharactersOutsideTheTargetAlphabet() {
        val result = decode(
            3,
            5,
            floatArrayOf(0f, 0f, 0f, 0f, 9f),
            floatArrayOf(0f, 9f, 0f, 0f, 0f),
            floatArrayOf(0f, 0f, 0f, 9f, 0f)
        )
        assertEquals("A7", result.text)
    }

    @Test
    fun returnsEmptyTextForBlankOutput() {
        val result = decode(2, 5, floatArrayOf(9f, 0f, 0f, 0f, 0f), floatArrayOf(9f, 0f, 0f, 0f, 0f))
        assertEquals("", result.text)
        assertTrue("Confianza: ${result.confidence}", result.confidence > 0.99f)
    }
}
