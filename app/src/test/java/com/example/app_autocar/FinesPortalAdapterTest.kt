package com.example.app_autocar

import org.junit.Assert.*
import org.junit.Test

class FinesPortalAdapterTest {
    @Test fun allowsOnlyKnownSourceRedirects() {
        assertTrue(FinesPortalAdapter.sameSource("https://portal.mtc.gob.pe/reportedgtt/", "https://rec.mtc.gob.pe/Citv/ArConsultaCitv"))
        assertFalse(FinesPortalAdapter.sameSource("https://portal.mtc.gob.pe/", "https://evil.mtc.gob.pe/"))
        assertEquals("#texCaptcha",FinesPortalAdapter.forUrl("https://rec.mtc.gob.pe/Citv/ArConsultaCitv").code)
        assertTrue(FinesPortalAdapter.sameSource("https://pagopapeletascallao.pe/", "https://pagonube.municallao.gob.pe/buscar"))
        assertTrue(FinesPortalAdapter.sameSource("https://www.sat.gob.pe/VirtualSAT", "https://sat.gob.pe/VirtualSAT/modulos/papeletas.aspx"))
        assertFalse(FinesPortalAdapter.sameSource("https://sat.gob.pe/", "https://sat.gob.pe.attacker.test/"))
        assertFalse(FinesPortalAdapter.sameSource("https://sat.gob.pe/", "https://other.test/"))
        assertTrue(FinesPortalAdapter.sameSource("http://www.munipiura.gob.pe/", "http://www2.munipiura.gob.pe/institucional/transparencia/transitoxplamot.asp"))
        assertFalse(FinesPortalAdapter.sameSource("http://www.munipiura.gob.pe/", "http://www3.munipiura.gob.pe/"))
    }
    @Test fun mapsRequiredImageCaptchaFields() {
        assertEquals("#TxtCodImagen", FinesPortalAdapter.forUrl("https://webexterno.sutran.gob.pe/").code)
        assertEquals("#gd_captcha_code", FinesPortalAdapter.forUrl("https://sat-t.gob.pe/").code)
        assertEquals("#codigo", FinesPortalAdapter.forUrl("https://www.munitacna.gob.pe/").code)
        assertEquals("#captcha", FinesPortalAdapter.forUrl("https://pagopapeletascallao.pe/").code)
    }
}
