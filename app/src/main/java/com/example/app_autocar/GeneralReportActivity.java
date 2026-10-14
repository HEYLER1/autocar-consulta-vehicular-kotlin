package com.example.app_autocar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONObject;

/** Full-screen, frozen report snapshot. Going back preserves the ongoing query in Home. */
public class GeneralReportActivity extends AppCompatActivity {
  private java.io.File reportFile;
  private static final int SAVE_PDF = 301;

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private TextView text(String value, int size, int color) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setLineSpacing(dp(4), 1);
    return t;
  }

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
        .setAppearanceLightStatusBars(true);
    try {
      java.io.File folder = new java.io.File(getCacheDir(), "reports");
      String name = getIntent().getStringExtra("snapshot");
      if (name == null || !name.matches("general_[a-f0-9-]+\\.json"))
        throw new IllegalArgumentException();
      java.io.File file = new java.io.File(folder, name);
      JSONObject data;
      try (java.io.FileInputStream input = new java.io.FileInputStream(file)) {
        data =
            new JSONObject(
                new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
      }
      LinearLayout root = new LinearLayout(this);
      root.setOrientation(LinearLayout.VERTICAL);
      root.setBackgroundColor(Color.rgb(244, 247, 251));
      ViewCompat.setOnApplyWindowInsetsListener(
          root,
          (v, insets) -> {
            androidx.core.graphics.Insets b =
                insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, b.top, 0, b.bottom);
            return insets;
          });
      ScrollView scroll = new ScrollView(this);
      LinearLayout body = new LinearLayout(this);
      body.setOrientation(LinearLayout.VERTICAL);
      scroll.addView(body);
      root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
      LinearLayout hero = new LinearLayout(this);
      hero.setOrientation(LinearLayout.VERTICAL);
      hero.setPadding(dp(22), dp(8), dp(22), dp(22));
      GradientDrawable gradient =
          new GradientDrawable(
              GradientDrawable.Orientation.TL_BR,
              new int[] {Color.rgb(0, 43, 92), Color.rgb(20, 83, 130)});
      hero.setBackground(gradient);
      body.addView(hero);
      TextView back = text("", 14, Color.WHITE);
      IconActions.apply(back, R.drawable.ic_back, "Volver a Inicio");
      back.getCompoundDrawables()[0].setTint(Color.WHITE);
      back.setMinHeight(dp(48));
      back.setGravity(android.view.Gravity.CENTER_VERTICAL);
      back.setOnClickListener(v -> finish());
      LinearLayout navigation = new LinearLayout(this);
      navigation.setGravity(android.view.Gravity.CENTER_VERTICAL);
      hero.addView(navigation, new LinearLayout.LayoutParams(-1, dp(48)));
      navigation.addView(back, new LinearLayout.LayoutParams(0, dp(48), 1));
      TextView title = text("Informe del vehículo", 21, Color.WHITE);
      title.setTypeface(null, Typeface.BOLD);
      hero.addView(title);
      TextView plate =
          text(PlateUtils.display(data.optString("plate")), 34, Color.rgb(255, 214, 0));
      plate.setTypeface(null, Typeface.BOLD);
      plate.setPadding(0, dp(10), 0, dp(8));
      hero.addView(plate);
      hero.addView(text(data.optString("progress"), 13, Color.WHITE));
      java.text.SimpleDateFormat date =
          new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.ROOT);
      date.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
      hero.addView(
          text(
              "Captura del informe: "
                  + date.format(new java.util.Date(data.optLong("created")))
                  + " · Perú",
              11,
              Color.rgb(214, 228, 242)));
      LinearLayout card = new LinearLayout(this);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(dp(18), dp(18), dp(18), dp(18));
      GradientDrawable bg = new GradientDrawable();
      bg.setColor(Color.WHITE);
      bg.setCornerRadius(dp(16));
      card.setBackground(bg);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
      p.setMargins(dp(16), dp(16), dp(16), dp(12));
      body.addView(card, p);
      String summary = data.optString("summary");
      int first = summary.indexOf('\n');
      String content = first >= 0 ? summary.substring(first + 1) : summary;
      for (String part : content.trim().split("\\n\\n")) {
        if (card.getChildCount() > 0) {
          View divider = new View(this);
          divider.setBackgroundColor(Color.rgb(231, 234, 240));
          LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(-1, dp(1));
          gap.topMargin = dp(18);
          gap.bottomMargin = dp(10);
          card.addView(divider, gap);
        }
        String[] lines = part.split("\\n", 2);
        TextView heading = text(lines[0], 15, Color.rgb(0, 43, 92));
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, dp(6), 0, dp(8));
        card.addView(heading);
        if (lines.length > 1) {
          TextView details = text(lines[1], 15, Color.rgb(31, 41, 55));
          details.setTextIsSelectable(true);
          card.addView(details);
        }
      }
      TextView complete = text(data.optString("detail"), 14, Color.rgb(31, 41, 55));
      complete.setTextIsSelectable(true);
      complete.setPadding(dp(18), 0, dp(18), dp(20));
      complete.setVisibility(View.GONE);
      TextView expand = text("Información completa", 13, Color.rgb(0, 43, 92));
      expand.setContentDescription("Mostrar u ocultar toda la información consultada");
      expand.setMinHeight(dp(48));
      expand.setPadding(dp(16), 0, dp(16), 0);
      expand.setGravity(android.view.Gravity.CENTER);
      LinearLayout.LayoutParams reveal = new LinearLayout.LayoutParams(-2, dp(48));
      reveal.gravity = android.view.Gravity.CENTER_HORIZONTAL;
      body.addView(expand, reveal);
      body.addView(complete);
      expand.setOnClickListener(
          v -> {
            boolean open = complete.getVisibility() != View.VISIBLE;
            complete.setVisibility(open ? View.VISIBLE : View.GONE);
            expand.setText(open ? "Ocultar información" : "Información completa");
          });
      String pdfName = data.optString("pdf");
      if (!pdfName.isEmpty()) {
        java.io.File candidate = new java.io.File(folder, pdfName);
        if (candidate.getCanonicalFile().getParentFile().equals(folder.getCanonicalFile())
            && candidate.isFile()) reportFile = candidate;
      }
      LinearLayout actions = new LinearLayout(this);
      actions.setGravity(android.view.Gravity.CENTER_VERTICAL);
      navigation.addView(actions, new LinearLayout.LayoutParams(-2, dp(48)));
      Button download = IconActions.button(this, R.drawable.ic_download, "Guardar PDF");
      Button share = IconActions.button(this, R.drawable.ic_share, "Compartir PDF");
      ((com.google.android.material.button.MaterialButton) download)
          .setIconTint(android.content.res.ColorStateList.valueOf(Color.WHITE));
      ((com.google.android.material.button.MaterialButton) share)
          .setIconTint(android.content.res.ColorStateList.valueOf(Color.WHITE));
      actions.addView(download, new LinearLayout.LayoutParams(dp(48), dp(48)));
      actions.addView(share, new LinearLayout.LayoutParams(dp(48), dp(48)));
      download.setEnabled(reportFile != null);
      share.setEnabled(reportFile != null);
      download.setAlpha(reportFile != null ? 1f : 0.4f);
      share.setAlpha(reportFile != null ? 1f : 0.4f);
      download.setOnClickListener(
          v -> {
            if (reportFile == null) return;
            android.content.Intent save =
                new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT)
                    .setType("application/pdf")
                    .addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    .putExtra(
                        android.content.Intent.EXTRA_TITLE,
                        "AUTOCAR_" + PlateUtils.normalize(data.optString("plate")) + ".pdf");
            startActivityForResult(save, SAVE_PDF);
          });
      share.setOnClickListener(
          v -> {
            if (reportFile == null) return;
            android.net.Uri uri =
                androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".reports", reportFile);
            android.content.Intent send =
                new android.content.Intent(android.content.Intent.ACTION_SEND)
                    .setType("application/pdf")
                    .putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.setClipData(android.content.ClipData.newRawUri("Informe AUTOCAR", uri));
            startActivity(android.content.Intent.createChooser(send, "Compartir informe"));
          });
      setContentView(root);
    } catch (Exception e) {
      Toast.makeText(this, "No se pudo cargar el informe", Toast.LENGTH_SHORT).show();
      finish();
    }
  }

  @Override
  protected void onActivityResult(int request, int result, android.content.Intent data) {
    super.onActivityResult(request, result, data);
    if (request != SAVE_PDF
        || result != RESULT_OK
        || data == null
        || data.getData() == null
        || reportFile == null) return;
    try (java.io.InputStream input = new java.io.FileInputStream(reportFile);
        java.io.OutputStream output = getContentResolver().openOutputStream(data.getData())) {
      if (output == null) throw new java.io.IOException();
      byte[] buffer = new byte[8192];
      int size;
      while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
      Toast.makeText(this, "PDF guardado", Toast.LENGTH_SHORT).show();
    } catch (Exception error) {
      Toast.makeText(this, "No se pudo guardar el PDF", Toast.LENGTH_SHORT).show();
    }
  }
}
