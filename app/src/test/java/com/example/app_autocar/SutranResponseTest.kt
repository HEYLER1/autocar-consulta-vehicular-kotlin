package com.example.app_autocar
import org.junit.Assert.assertEquals
import org.junit.Test
class SutranResponseTest {
    @Test fun distinguishesEmptyResultFromRejectedCaptcha(){
        for(message in listOf("No tiene papeletas","La placa no registra infracciones","No se encontraron resultados","No se encontró información","Sin papeletas pendientes"))assertEquals(message,"empty",SutranResponse.classify(message,true))
        assertEquals("captcha",SutranResponse.classify("El código ingresado es incorrecto",true))
        assertEquals("pending",SutranResponse.classify("Ingrese el código de la imagen",true))
        assertEquals("pending",SutranResponse.classify("No tiene papeletas",false))
        assertEquals("pending",SutranResponse.classify("",true))
        assertEquals("pending",SutranResponse.classify("Ocurrió un error. No se encontraron resultados",true))
    }
}
