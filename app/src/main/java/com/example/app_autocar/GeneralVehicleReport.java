package com.example.app_autocar;

import java.util.List;

/** One consolidated report; unreturned data is never interpreted as zero debt. */
public final class GeneralVehicleReport {
  private GeneralVehicleReport() {}

  public static class Entry {
    final String name, url, status, summary, detail;

    public Entry(String name, String url, String status, String summary, String detail) {
      this.name = name;
      this.url = url;
      this.status = status;
      this.summary = summary;
      this.detail = detail;
    }
  }

  public static String summary(String plate, List<Entry> entries) {
    StringBuilder out =
        new StringBuilder("RESUMEN DEL VEHÍCULO · " + PlateUtils.display(plate) + "\n");
    String[] sections = {
      "Datos del vehículo", "Seguro e historial", "Revisión técnica", "Multas y papeletas"
    };
    for (int group = 0; group < sections.length; group++) {
      StringBuilder content = new StringBuilder();
      for (Entry entry : entries)
        if (group(entry) == group) {
          String value = entry.summary.trim().isEmpty() ? entry.status : entry.summary;
          content.append("• ").append(entry.name).append(": ").append(value).append("\n");
        }
      if (content.length() > 0)
        out.append("\n").append(sections[group]).append("\n").append(content);
    }
    out.append(
        "\nLo pendiente o fallido no equivale a ausencia de multas ni a documentos vigentes.");
    return out.toString();
  }

  public static String detail(String plate, List<Entry> entries) {
    StringBuilder out = new StringBuilder("INFORME GENERAL · " + PlateUtils.display(plate) + "\n");
    for (Entry entry : entries)
      out.append("\n")
          .append(entry.name)
          .append("\nEstado: ")
          .append(entry.status)
          .append("\n")
          .append(entry.detail.trim().isEmpty() ? "Sin datos verificados todavía." : entry.detail)
          .append("\nFuente: ")
          .append(entry.url)
          .append("\n");
    return out.toString();
  }

  private static int group(Entry entry) {
    String host = FinesPortalAdapter.host(entry.url);
    return host.equals("consultavehicular.sunarp.gob.pe")
        ? 0
        : host.equals("servicios.sbs.gob.pe")
            ? 1
            : host.equals("rec.mtc.gob.pe") || host.equals("portal.mtc.gob.pe") ? 2 : 3;
  }
}
