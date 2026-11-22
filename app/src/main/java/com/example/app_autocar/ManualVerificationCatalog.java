package com.example.app_autocar;

import java.util.List;

/** Supplemental checks opened by the user, never counted as batch results. */
final class ManualVerificationCatalog {
  private ManualVerificationCatalog() {}

  static final List<OfficialSource> SOURCES = List.of(
      new OfficialSource(
          "vehicle",
          "Historial registral y gravámenes",
          "Solicita publicidad registral; puede requerir cuenta y pago",
          "Cuenta/pago",
          "https://enlinea.sunarp.gob.pe/",
          "R"),
      new OfficialSource(
          "vehicle",
          "TIVe · SUNARP",
          "Recupera la tarjeta con los datos solicitados por SUNARP",
          "Datos extra",
          "https://tivative.sunarp.gob.pe/tivative/inicio/recupera-tu-tivative",
          "T"),
      new OfficialSource(
          "vehicle",
          "Síguelo Plus · SUNARP",
          "Seguimiento de títulos; no equivale al historial de dueños",
          "Datos extra",
          "https://www.gob.pe/10574",
          "S"),
      new OfficialSource(
          "vehicle",
          "Garantías mobiliarias · SIGM",
          "Información oficial y criterios de búsqueda del SIGM",
          "Guía",
          "https://cursosigm.sunarp.gob.pe/preguntas-frecuentes/",
          "G"),
      new OfficialSource(
          "vehicle",
          "Lunas oscurecidas · PNP",
          "Verificación de autorización por placa",
          "Por placa",
          "https://lunas.portaldeconsultaspnp.net.pe/",
          "L"),
      new OfficialSource(
          "vehicle",
          "GNV · Infogas",
          "Consulta del registro de gas natural vehicular",
          "Por placa",
          "https://infogas.com.pe/consulta-placa/",
          "G"),
      new OfficialSource(
          "vehicle",
          "Financiamiento GNV · FISE",
          "Consulta de pagos y saldo con documento del titular y placa",
          "Datos extra",
          "https://fise.pe/ahorro-gnv/",
          "F"),
      new OfficialSource(
          "insurance",
          "SOAT · APESEG",
          "Consulta complementaria de vigencia del SOAT",
          "Portal",
          "https://www.apeseg.org.pe/consultas-soat/",
          "A"),
      new OfficialSource(
          "insurance",
          "SOAT, CAT y seguro · SBS",
          "Acceso al reporte oficial de pólizas y siniestros",
          "Por placa",
          "https://www.sbs.gob.pe/usuarios/nuestros-servicios/reporte-vehicular/",
          "S"));

  static final List<OfficialSource> TRAFFIC = List.of(
      new OfficialSource(
          "national",
          "Cinemómetro · SUTRAN",
          "Placa, DNI y verificación oficial",
          "Portal",
          "https://webexterno.sutran.gob.pe/WebExterno/Pages/frmPapeletasCinemometro.aspx",
          "S"),
      new OfficialSource(
          "municipal",
          "SAT Lima · Orden de captura",
          "Placa y código de seguridad",
          "Portal",
          "https://www.sat.gob.pe/VirtualSAT/modulos/Capturas.aspx?tri=C",
          "L"),
      new OfficialSource(
          "municipal",
          "SAT Lima · Depósito",
          "Placa y código de seguridad",
          "Portal",
          "https://www.sat.gob.pe/VirtualSAT/modulos/ConsultaDeposito.aspx?tri=I",
          "L"),
      new OfficialSource("municipal", "SAT Lima · Impuesto vehicular",
          "Elige la consulta tributaria en Virtual SAT", "Portal",
          "https://www.sat.gob.pe/VirtualSAT/principal.aspx", "L"));
}
