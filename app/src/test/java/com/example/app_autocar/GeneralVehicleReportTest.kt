package com.example.app_autocar
import org.junit.Assert.*
import org.junit.Test
class GeneralVehicleReportTest {
    @Test fun oneReportPreservesDataAndPendingSources(){
        val entries=listOf(GeneralVehicleReport.Entry("MTC","https://rec.mtc.gob.pe/Citv/ArConsultaCitv","Recibido","VENCIDO · APROBADO","Placa: ABC123\nHasta: 26/08/2025"),GeneralVehicleReport.Entry("Cusco","https://cusco.gob.pe/","Captcha pendiente","",""))
        val summary=GeneralVehicleReport.summary("ABC123",entries)
        assertTrue(summary.contains("Revisión técnica"));assertTrue(summary.contains("VENCIDO"));assertTrue(summary.contains("Captcha pendiente"))
        val detail=GeneralVehicleReport.detail("ABC123",entries)
        assertTrue(detail.contains("26/08/2025"));assertTrue(detail.contains("Sin datos verificados todavía"));assertTrue(detail.contains("https://cusco.gob.pe/"))
    }
}
