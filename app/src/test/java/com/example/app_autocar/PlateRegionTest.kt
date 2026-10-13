package com.example.app_autocar

import org.junit.Assert.*
import org.junit.Test

class PlateRegionTest {
    @Test fun ordinaryPrefixesRecommendMatchingSources() {
        assertEquals("Lima y Callao",PlateRegion.region("A4D-234"))
        for(prefix in listOf("A","B","C","D","F"))assertTrue(PlateRegion.recommended(prefix+"1A234","SAT Lima"))
        assertTrue(PlateRegion.recommended("V1A234","Arequipa"))
        assertFalse(PlateRegion.recommended("V1A234","SAT Lima"))
        assertTrue(PlateRegion.recommended("M1A234","SATCH Chiclayo"))
        assertTrue(PlateRegion.recommended("Z1A234","Tacna"))
        assertEquals("",PlateRegion.region("1234AB"))
        assertEquals("",PlateRegion.region("EAA123"))
        assertEquals("",PlateRegion.region("A"))
    }
}
