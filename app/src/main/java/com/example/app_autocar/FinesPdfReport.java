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
    public final String name, url, status, detail;
    public final android.graphics.Bitmap image;

    public Source(String name, String url, String status, String detail) {
      this(name, url, status, detail, null);
    }

    public Source(
        String name, String url, String status, String detail, android.graphics.Bitmap image) {
      this.name = name;
      this.url = url;
      this.status = status;
      this.detail = detail;
      this.image = image;
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
            "Multas_" + PlateUtils.normalize(plate) + "_" + System.currentTimeMillis() + ".pdf");
    java.text.SimpleDateFormat format =
        new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.ROOT);
    format.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
    String date = format.format(new java.util.Date());
    int received = 0;
    for (Source source : sources) if (!source.detail.trim().isEmpty()) received++;
    Layout layout = new Layout(PlateUtils.display(plate), date);
    try {
      layout.line("INFORME VEHICULAR", 16, true, Color.rgb(0, 43, 92));
      layout.line(
          received + " / " + sources.size() + " fuentes con información extraída",
          12,
          true,
          Color.rgb(0, 43, 92));
      layout.line(
          received < sources.size()
              ? "INFORME PARCIAL - Hay fuentes pendientes o no verificadas."
              : "Datos recibidos - Verifica su exactitud en las fuentes oficiales.",
          11,
          false,
          Color.rgb(120, 70, 0));
      layout.line(
          "Este documento resume la consulta desde AUTOCAR. No acredita ausencia de multas ni"
              + " reemplaza certificados oficiales. Los errores y captchas pendientes no equivalen"
              + " a cero deudas.",
          10,
          false,
          Color.DKGRAY);
      for (Source source : sources) {
        layout.ensure(100);
        layout.y += 14;
        layout.line(source.name, 14, true, Color.rgb(0, 43, 92));
        layout.line("Estado: " + source.status, 10, true, Color.DKGRAY);
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
      canvas.drawText("Consulta: " + date + " (America/Lima)", 36, 78, header);
      y = 120;
    }

    void ensure(float height) {
      if (y + height > 785) {
        finishPage();
        newPage();
      }
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
