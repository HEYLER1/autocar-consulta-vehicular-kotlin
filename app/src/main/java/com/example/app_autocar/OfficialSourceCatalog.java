package com.example.app_autocar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Official endpoints and display metadata; query behavior lives in the engines. */
public final class OfficialSourceCatalog {
  private OfficialSourceCatalog() {}

  public static final List<OfficialSource> SOURCES =
      Collections.unmodifiableList(
          Arrays.asList(
              new OfficialSource(
                  "vehicle",
                  "SUNARP",
                  "Consulta y servicios registrales",
                  "9 servicios",
                  "https://consultavehicular.sunarp.gob.pe/consulta-vehicular/",
                  "R"),
              new OfficialSource(
                  "vehicle",
                  "Revisión técnica MTC",
                  "Estado de la inspección vehicular",
                  "Captcha",
                  "https://rec.mtc.gob.pe/Citv/ArConsultaCitv",
                  "T"),
              new OfficialSource(
                  "insurance",
                  "SOAT · SBS",
                  "Pólizas, uso declarado y accidentes",
                  "Automática",
                  "https://servicios.sbs.gob.pe/reportesoat/",
                  "S"),
              new OfficialSource(
                  "national",
                  "SUTRAN",
                  "Récord de infracciones de transporte",
                  "Captcha",
                  "https://webexterno.sutran.gob.pe/WebExterno/Pages/frmRecordInfracciones.aspx",
                  "S"),
              new OfficialSource(
                  "municipal",
                  "ATU · Lima y Callao",
                  "Actas y multas de transporte urbano",
                  "Placa / acta",
                  "https://pasarela.atu.gob.pe/",
                  "A"),
              new OfficialSource(
                  "municipal",
                  "Callao · Papeletas",
                  "Consulta de infracciones por placa",
                  "Captcha",
                  "https://pagopapeletascallao.pe/",
                  "C"),
              new OfficialSource(
                  "municipal",
                  "Arequipa",
                  "Infracciones pendientes de tránsito y transporte",
                  "Placa",
                  "https://www.muniarequipa.gob.pe/oficina-virtual/c0nInfrPermisos/faltas/papeletas.php",
                  "A"),
              new OfficialSource(
                  "municipal",
                  "Cusco",
                  "Servicios municipales de consulta",
                  "Portal",
                  "https://cusco.gob.pe/informatica/index.php/",
                  "C"),
              new OfficialSource(
                  "municipal",
                  "Tacna",
                  "Papeletas de infracción de tránsito",
                  "Portal",
                  "https://www.munitacna.gob.pe/pagina/sf/servicios/papeletas",
                  "T"),
              new OfficialSource(
                  "municipal",
                  "Chachapoyas",
                  "Consulta municipal de papeletas",
                  "Placa",
                  "https://app.munichachapoyas.gob.pe/servicios/consulta_papeletas/app/papeletas.php",
                  "C"),
              new OfficialSource(
                  "municipal",
                  "Huánuco",
                  "Papeletas de tránsito · enlace de Peruautos",
                  "Portal",
                  "https://www.munihuanuco.gob.pe/wp-content/servicios/transportes/gt_papeletas.php",
                  "H"),
              new OfficialSource(
                  "municipal",
                  "Pucallpa",
                  "Consulta vehicular municipal · conexión HTTP",
                  "HTTP",
                  "http://consultas.municportillo.gob.pe:85/consultaVehiculo/consulta/",
                  "P"),
              new OfficialSource(
                  "municipal",
                  "SAT Cajamarca",
                  "Récord de infracciones por placa",
                  "Placa",
                  "https://www.satcajamarca.gob.pe/consultas",
                  "C"),
              new OfficialSource(
                  "municipal",
                  "SATCH Chiclayo",
                  "Récord de infracciones",
                  "Directa",
                  "https://virtualsatch.satch.gob.pe/virtualsatch/record_infracciones/buscar_placa_",
                  "C"),
              new OfficialSource(
                  "municipal",
                  "SAT Ica",
                  "Papeletas de tránsito",
                  "Directa",
                  "https://m.satica.gob.pe/consultapapeletas_web.php",
                  "I"),
              new OfficialSource(
                  "municipal",
                  "SAT Lima",
                  "Consulta de multas y papeletas por placa",
                  "Placa",
                  "https://www.sat.gob.pe/VirtualSAT/principal.aspx",
                  "L"),
              new OfficialSource(
                  "municipal",
                  "Piura",
                  "Multas de tránsito por placa · conexión HTTP",
                  "HTTP",
                  "http://www.munipiura.gob.pe/consulta-de-multas-de-transito#buscar-por-placa",
                  "P"),
              new OfficialSource(
                  "municipal",
                  "SAT Tarapoto",
                  "Multas e impuesto vehicular",
                  "Captcha",
                  "https://sat-t.gob.pe/",
                  "T"),
              new OfficialSource(
                  "municipal",
                  "SATT Trujillo",
                  "Verificación de papeletas",
                  "Datos extra",
                  "https://www.gob.pe/53428-verificar-informacion-de-papeletas-de-transito-y-transporte-en-trujillo",
                  "T")));
}
