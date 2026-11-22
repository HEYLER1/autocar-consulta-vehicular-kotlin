package com.example.app_autocar

import org.junit.Assert.*
import org.junit.Test

class VehicleFindingsTest {
    @Test fun includesAllSourcesAndKeepsPendingSeparate() {
        val entries = (1..5).map {
            GeneralVehicleReport.Entry("Municipio $it", "", "Información recibida", "Papeleta pendiente", "")
        } + GeneralVehicleReport.Entry("SOAT", "", "Verificación pendiente", "Vigente", "")
        val findings = GeneralVehicleReport.findings(entries)
        assertTrue(findings.contains("Municipio 5: Papeleta pendiente"))
        assertTrue(findings.contains("Sin verificar:\n• SOAT: Verificación pendiente"))
        assertFalse(findings.contains("Vigente"))
    }
}
