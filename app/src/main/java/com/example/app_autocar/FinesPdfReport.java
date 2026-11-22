package com.example.app_autocar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Snapshot of received data, never a certificate of absence of fines. */
public final class FinesPdfReport {
  public static final class Source {
    public final String name, url, status, detail, summary;
    public final android.graphics.Bitmap image;

    public Source(String name, String url, String status, String detail) {
      this(name, url, status, detail, null);
    }

    public Source(
        String name, String url, String status, String detail, android.graphics.Bitmap image) {
      this(name, url, status, detail, image, "");
    }

    public Source(String name, String url, String status, String detail,
        android.graphics.Bitmap image, String summary) {
      this.name = name;
      this.url = url;
      this.status = status;
      this.detail = detail;
      this.image = image;
      this.summary = summary;
    }

    boolean hasEvidence() {
      return status.startsWith("Información recibida")
          && (!detail.trim().isEmpty() || (image != null && !image.isRecycled()));
    }

    int topic() {
      String host = FinesPortalAdapter.host(url);
      if (host.equals("consultavehicular.sunarp.gob.pe")) return 0;
      if (host.equals("servicios.sbs.gob.pe")) return 1;
      if (host.equals("rec.mtc.gob.pe") || host.equals("portal.mtc.gob.pe")) return 2;
      return 3;
    }
  }

  private FinesPdfReport() {}

  public static File write(Context context, String plate, List<Source> sources) throws IOException {
    File folder = new File(context.getCacheDir(), "reports");
    if (!folder.exists() && !folder.mkdirs())
      throw new IOException("No se pudo crear el directorio del informe");
    File file =
        new File(
            folder,
            "AUTOCAR_" + PlateUtils.normalize(plate) + "_" + System.currentTimeMillis() + ".pdf");
    java.text.SimpleDateFormat format =
        new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.ROOT);
    format.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
    String date = format.format(new java.util.Date());
    int received = 0;
    for (Source source : sources) if (source.hasEvidence()) received++;
    Layout layout = new Layout(PlateUtils.display(plate), date);
    try {
      String[] topics = {"Registro vehicular", "Seguro y siniestros", "Revisión técnica", "Multas e infracciones"};
      layout.cover(sources, received, topics);
      layout.finishPage();
      layout.newPage();
      layout.line("Detalle y evidencia por fuente", 21, true, Color.rgb(0, 43, 92));
      for (int topic = 0; topic < topics.length; topic++) {
        boolean showHeading = true;
        for (Source source : sources) {
        if (source.topic() != topic) continue;
        if (showHeading) {
          layout.ensure(130);
          layout.y += 12;
          layout.line(topics[topic], 13, true, Color.rgb(0, 43, 92));
          showHeading = false;
        }
        layout.ensure(100);
        layout.y += 14;
        layout.line(source.name, 14, true, Color.rgb(0, 43, 92));
        layout.line((source.hasEvidence() ? "Estado: " : "Sin verificar: ") + source.status,
            10, true, Color.DKGRAY);
        layout.line(
            source.detail.trim().isEmpty()
                ? "Sin información extraída. Completa la verificación o revisa el portal oficial."
                : source.detail,
            11,
            false,
            Color.rgb(26, 26, 46));
        layout.line("Fuente: " + source.url, 9, false, Color.rgb(90, 100, 120));
        if (source.image != null && !source.image.isRecycled()) {
          layout.ensure(700);
          layout.line("SUNARP - Imagen oficial recibida", 12, true, Color.rgb(0, 43, 92));
          float scale = Math.min(523f / source.image.getWidth(), 620f / source.image.getHeight());
          float height = source.image.getHeight() * scale;
          layout.canvas.drawBitmap(
              source.image,
              null,
              new android.graphics.RectF(
                  36, layout.y, 36 + source.image.getWidth() * scale, layout.y + height),
              null);
          layout.y += height + 12;
        }
        }
      }
      layout.ensure(55);
      layout.y += 12;
      layout.line(
          "Alcance: se presenta el texto extraído disponible; puede estar limitado por tablas,"
              + " paginación o cambios del portal. No se suman importes sin verificar estado y"
              + " duplicados.",
          10,
          false,
          Color.DKGRAY);
      layout.finishPage();
      try (FileOutputStream out = new FileOutputStream(file)) {
        layout.document.writeTo(out);
      }
      return file;
    } finally {
      layout.document.close();
    }
  }

  private static final class Layout {
    final PdfDocument document = new PdfDocument();
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final String plate, date;
    PdfDocument.Page page;
    Canvas canvas;
    int number;
    float y;

    Layout(String plate, String date) {
      this.plate = plate;
      this.date = date;
      newPage();
    }

    void newPage() {
      page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++number).create());
      canvas = page.getCanvas();
      Paint header = new Paint(Paint.ANTI_ALIAS_FLAG);
      header.setColor(Color.rgb(0, 43, 92));
      canvas.drawRect(0, 0, 595, 92, header);
      header.setColor(Color.rgb(255, 214, 0));
      header.setTextSize(20);
      header.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
      canvas.drawText("AUTOCAR", 36, 35, header);
      header.setColor(Color.WHITE);
      header.setTextSize(14);
      canvas.drawText("PLACA  " + plate, 36, 60, header);
      header.setTextSize(9);
      header.setTypeface(Typeface.DEFAULT);
      canvas.drawText("Informe generado: " + date + " (hora de Perú)", 36, 78, header);
      y = 120;
    }

    void ensure(float height) {
      if (y + height > 785) {
        finishPage();
        newPage();
      }
    }

    private static final int INK = 0xff10253b, TEAL = 0xff00695f, MUTED = 0xff495d70;

    void block(float x, float y, float w, float h, int color) {
      paint.setColor(color);
      canvas.drawRoundRect(x, y, x + w, y + h, 12, 12, paint);
    }

    void label(String text, float x, float baseline, int size, boolean bold, int color) {
      paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
      paint.setTextSize(size);
      paint.setColor(color);
      canvas.drawText(text, x, baseline, paint);
    }

    void paragraph(String text, float x, float top, int width, int size, int lines, int color) {
      android.text.TextPaint brush = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
      brush.setTextSize(size);
      brush.setColor(color);
      android.text.StaticLayout paragraph = android.text.StaticLayout.Builder
          .obtain(text, 0, text.length(), brush, width)
          .setMaxLines(lines).setEllipsize(android.text.TextUtils.TruncateAt.END)
          .setIncludePad(false).setLineSpacing(3, 1).build();
      canvas.save();
      canvas.translate(x, top);
      paragraph.draw(canvas);
      canvas.restore();
    }

    void cover(List<Source> sources, int received, String[] topics) {
      canvas.drawColor(0xfff1f5f7);
      block(24, 24, 547, 198, INK);
      label("AUTOCAR  /  INFORME VEHICULAR", 44, 57, 13, true, 0xff8de0cd);
      label("Antes de comprar", 44, 104, 38, true, Color.WHITE);
      block(44, 125, 205, 54, 0xffffdc65);
      label(plate, 60, 162, 29, true, INK);
      label("Generado: " + date + " (Perú)", 44, 202, 10, false, 0xffd6e5ee);

      String[] metrics = {"Fuentes seleccionadas", "Con evidencia", "Sin verificar"};
      int[] counts = {sources.size(), received, sources.size() - received};
      for (int i = 0; i < metrics.length; i++) {
        int x = 24 + 185 * i;
        block(x, 240, 177, 70, Color.WHITE);
        label(Integer.toString(counts[i]), x + 15, 276, 28, true, i == 2 ? 0xff984800 : TEAL);
        label(metrics[i], x + 15, 295, 10, false, MUTED);
      }
      label("Resumen por tema", 26, 345, 21, true, INK);
      for (int i = 0; i < topics.length; i++) {
        int x = 24 + (i % 2) * 279, top = 363 + (i / 2) * 139;
        block(x, top, 268, 127, Color.WHITE);
        label("0" + (i + 1), x + 15, top + 24, 10, true, TEAL);
        label(topics[i], x + 38, top + 25, 14, true, INK);
        int total = 0, verified = 0;
        String brief = "";
        for (Source source : sources) {
          if (source.topic() != i) continue;
          total++;
          if (!source.hasEvidence()) continue;
          verified++;
          if (brief.isEmpty() && !source.summary.isBlank())
            brief = source.name + ": " + source.summary.replaceAll("\\s+", " ").trim();
        }
        label(total == 0 ? "No consultado" : verified + " con evidencia  /  " + (total - verified)
            + " sin verificar", x + 15, top + 49, 10, true, TEAL);
        paragraph(brief.isEmpty() ? (verified == 0 ? "No hay evidencia de este tema en la consulta."
            : "Consulta el detalle y los importes disponibles en el anexo.") : brief,
            x + 15, top + 63, 238, 11, 4, MUTED);
      }
      block(24, 648, 547, 126, 0xfffff1d9);
      label("Lo que este informe no confirma", 41, 676, 14, true, 0xff783d05);
      paragraph("Una fuente sin verificar no equivale a cero multas ni confirma vigencias. "
          + "Las consultas manuales (gravámenes, captura, impuesto, lunas y GNV) no están incluidas. "
          + "Revisa el anexo y los documentos oficiales antes de pagar.", 41, 691, 507, 11, 5, INK);
    }

    void finishPage() {
      if (page == null) return;
      paint.setColor(Color.rgb(107, 114, 128));
      paint.setTypeface(Typeface.DEFAULT);
      paint.setTextSize(9);
      canvas.drawText("Consulta informativa - Fuentes oficiales indicadas", 36, 815, paint);
      canvas.drawText("Página " + number, 505, 815, paint);
      document.finishPage(page);
      page = null;
    }

    void line(String text, float size, boolean bold, int color) {
      for (String paragraph : text.replace("\r", "").split("\n", -1)) {
        String remaining = paragraph.trim();
        if (remaining.isEmpty()) {
          ensure(7);
          y += 7;
          continue;
        }
        while (!remaining.isEmpty()) {
          paint.setTextSize(size);
          paint.setTypeface(
              Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
          int count = paint.breakText(remaining, true, 523, null);
          if (count < 1) count = 1;
          if (count < remaining.length()) {
            int space = remaining.lastIndexOf(' ', count);
            if (space > 0) count = space;
          }
          ensure(size + 8);
          paint.setTextSize(size);
          paint.setTypeface(
              Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
          paint.setColor(color);
          canvas.drawText(remaining.substring(0, count), 36, y, paint);
          y += size + 5;
          remaining = remaining.substring(count).trim();
        }
      }
    }
  }
}
