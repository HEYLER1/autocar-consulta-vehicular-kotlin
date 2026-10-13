package com.example.app_autocar;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class SunarpServicesActivity extends AppCompatActivity {
    public static final String EXTRA_PLATE = "plate";

    private static final Service CONSULTA = new Service(
            "Consulta vehicular", "Titular y características del vehículo", "Gratis",
            "https://consultavehicular.sunarp.gob.pe/consulta-vehicular/", "R");

    private static final Service[] DOCUMENTS = {
            new Service("Conoce Aquí", "Antecedentes por partida registral", "Gratis",
                    "https://conoce-aqui.sunarp.gob.pe/conoce-aqui/inicio", "C"),
            new Service("Boleta informativa", "Información registral del vehículo", "Cuenta/pago",
                    "https://enlinea.sunarp.gob.pe/", "B"),
            new Service("Gravámenes y cargas", "Certificado registral vehicular", "Cuenta/pago",
                    "https://enlinea.sunarp.gob.pe/", "G"),
            new Service("Tarjeta vehicular TIVe", "Recupera tu tarjeta electrónica", "Validación",
                    "https://tivative.sunarp.gob.pe/tivative/inicio/recupera-tu-tivative", "T")
    };

    private static final Service[] PROTECTION = {
            new Service("Alerta Registral", "Avisos de movimientos en una partida", "Gratis/cuenta",
                    "https://alertaregistral.sunarp.gob.pe/alerta/", "A"),
            new Service("Alerta Robo", "Comunica el robo del vehículo", "Gratis/TIVe",
                    "https://alertarobo.sunarp.gob.pe/", "!"),
    };

    private static final Service[] PROCEDURES = {
            new Service("Síguelo Plus", "Revisa el avance de tu trámite", "Captcha",
                    "https://sigueloplus.sunarp.gob.pe/siguelo/", "S"),
            new Service("Cambio de placa", "Requisitos y trámite oficial", "Orientación",
                    "https://www.gob.pe/institucion/sunarp/pages/190-cambiar-a-nueva-placa-de-rodaje", "P")
    };

    private String plate = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sunarp_services);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        View root = findViewById(R.id.sunarp_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });

        String receivedPlate = getIntent().getStringExtra(EXTRA_PLATE);
        plate = PlateUtils.normalize(receivedPlate == null ? "" : receivedPlate);
        TextView plateView = findViewById(R.id.sunarp_plate);
        plateView.setText(plate.isEmpty() ? "SIN PLACA" : PlateUtils.display(plate));
        findViewById(R.id.sunarp_back).setOnClickListener(view -> finish());
        findViewById(R.id.sunarp_primary).setOnClickListener(view -> open(CONSULTA));

        fill(findViewById(R.id.sunarp_documents), DOCUMENTS);
        android.widget.Button identitySettings = new android.widget.Button(this);
        identitySettings.setText("Cambiar / eliminar datos de autorrelleno");
        identitySettings.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Datos locales de SUNARP").setMessage("Puedes eliminar la identidad guardada o reemplazarla. No se envía a otras fuentes.")
                .setPositiveButton("Cambiar", (d,w) -> requestIdentity(DOCUMENTS[0]))
                .setNegativeButton("Eliminar", (d,w) -> getSharedPreferences("sunarp_identity", MODE_PRIVATE).edit().clear().apply())
                .setNeutralButton("Cancelar", null).show());
        ((LinearLayout)findViewById(R.id.sunarp_documents)).addView(identitySettings);
        fill(findViewById(R.id.sunarp_protection), PROTECTION);
        fill(findViewById(R.id.sunarp_procedures), PROCEDURES);
    }

    private void fill(LinearLayout container, Service[] services) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Service service : services) {
            View row = inflater.inflate(R.layout.item_report_source, container, false);
            ((TextView) row.findViewById(R.id.report_source_symbol)).setText(service.symbol);
            ((TextView) row.findViewById(R.id.report_source_title)).setText(service.title);
            ((TextView) row.findViewById(R.id.report_source_description)).setText(service.description);
            TextView status = row.findViewById(R.id.report_source_status);
            status.setText(service.access.toUpperCase());
            styleStatus(status);
            row.setOnClickListener(view -> open(service));
            container.addView(row);
        }
    }

    private void styleStatus(TextView view) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(20));
        background.setColor(Color.parseColor("#E7EEF6"));
        view.setTextColor(Color.parseColor("#002B5C"));
        view.setBackground(background);
    }

    private void open(Service service) {
        if (service.url.contains("conoce-aqui.sunarp.gob.pe")
                && !getSharedPreferences("sunarp_identity", MODE_PRIVATE).contains("dni")) {
            requestIdentity(service);
            return;
        }
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_TITLE, service.title);
        intent.putExtra(WebViewActivity.EXTRA_URL, service.url);
        intent.putExtra(WebViewActivity.EXTRA_PLATE, plate);
        startActivity(intent);
    }

    private void requestIdentity(Service service) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(24), dp(8), dp(24), dp(8));
        android.widget.EditText dni = new android.widget.EditText(this);
        dni.setHint("DNI (8 dígitos)"); dni.setInputType(2);
        android.widget.EditText date = new android.widget.EditText(this);
        date.setHint("Fecha de emisión: dd/MM/yyyy");
        android.widget.EditText digit = new android.widget.EditText(this);
        digit.setHint("Dígito verificador"); digit.setInputType(2);
        fields.addView(dni); fields.addView(date); fields.addView(digit);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Autorrelleno de Conoce Aquí")
                .setMessage("Introduce tus datos una vez. Se guardan sólo en este dispositivo y se rellenan únicamente en Conoce Aquí. Revisa el formulario oficial antes de continuar.")
                .setView(fields).setPositiveButton("Guardar y abrir", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            String d = dni.getText().toString().trim(), f = date.getText().toString().trim(), n = digit.getText().toString().trim();
            boolean validDate;
            try { java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ROOT);
                format.setLenient(false); validDate = format.parse(f) != null && f.matches("\\d{1,2}/\\d{1,2}/\\d{4}");
            } catch (Exception error) { validDate = false; }
            if (!d.matches("\\d{8}") || !n.matches("\\d") || !validDate) {
                android.widget.Toast.makeText(this, "Revisa DNI, fecha y dígito", android.widget.Toast.LENGTH_SHORT).show(); return;
            }
            getSharedPreferences("sunarp_identity", MODE_PRIVATE).edit().putString("dni", d).putString("date", f).putString("digit", n).apply();
            dialog.dismiss(); open(service);
        }));
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class Service {
        final String title;
        final String description;
        final String access;
        final String url;
        final String symbol;

        Service(String title, String description, String access, String url, String symbol) {
            this.title = title;
            this.description = description;
            this.access = access;
            this.url = url;
            this.symbol = symbol;
        }
    }
}
