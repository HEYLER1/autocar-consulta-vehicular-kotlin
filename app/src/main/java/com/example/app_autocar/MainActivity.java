package com.example.app_autocar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "autocar_preferences";
    private static final String KEY_PLATE = "current_plate";
    private static final String KEY_RECENT = "recent_plates";
    private static final String CATEGORY_VEHICLE = "vehicle";
    private static final String CATEGORY_INSURANCE = "insurance";
    private static final String CATEGORY_NATIONAL = "national";
    private static final String CATEGORY_MUNICIPAL = "municipal";

    private final List<String> recentPlates = new ArrayList<>();
    private final Set<String> openedSources = new HashSet<>();
    private SharedPreferences preferences;
    private String currentPlate = "";
    private View resultsPanel;
    private LinearLayout sourceContainer;
    private TextView sourceProgress;
    private TextView sourceCategoryTitle;
    private EditText sourceSearch;
    private View filterVehicle;
    private View filterInsurance;
    private View filterNational;
    private View filterMunicipal;
    private String selectedCategory = CATEGORY_VEHICLE;
    private String sourceQuery = "";

    private final List<OfficialSource> sources = Arrays.asList(
            new OfficialSource(CATEGORY_VEHICLE, "SUNARP", "Consulta y servicios registrales", "9 servicios", "https://consultavehicular.sunarp.gob.pe/consulta-vehicular/", "R"),
            new OfficialSource(CATEGORY_VEHICLE, "Revisión técnica MTC", "Estado de la inspección vehicular", "Captcha", "https://portal.mtc.gob.pe/reportedgtt/form/frmconsultaplacaitv.aspx", "T"),
            new OfficialSource(CATEGORY_INSURANCE, "SOAT · SBS", "Pólizas, uso declarado y accidentes", "Automática", "https://servicios.sbs.gob.pe/reportesoat/", "S"),
            new OfficialSource(CATEGORY_NATIONAL, "SUTRAN", "Récord de infracciones de transporte", "Captcha", "https://webexterno.sutran.gob.pe/WebExterno/Pages/frmRecordInfracciones.aspx", "S"),
            new OfficialSource(CATEGORY_MUNICIPAL, "ATU · Lima y Callao", "Actas y multas de transporte urbano", "Placa / acta", "https://pasarela.atu.gob.pe/", "A"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Callao · Papeletas", "Consulta de infracciones por placa", "Captcha", "https://pagopapeletascallao.pe/", "C"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Arequipa", "Infracciones pendientes de tránsito y transporte", "Placa", "https://www.muniarequipa.gob.pe/oficina-virtual/c0nInfrPermisos/faltas/papeletas.php", "A"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Cusco", "Servicios municipales de consulta", "Portal", "https://cusco.gob.pe/informatica/index.php/", "C"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Tacna", "Papeletas de infracción de tránsito", "Portal", "https://www.munitacna.gob.pe/pagina/sf/servicios/papeletas", "T"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Chachapoyas", "Consulta municipal de papeletas", "Placa", "https://app.munichachapoyas.gob.pe/servicios/consulta_papeletas/app/papeletas.php", "C"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Huánuco", "Papeletas de tránsito · enlace de Peruautos", "Portal", "https://www.munihuanuco.gob.pe/wp-content/servicios/transportes/gt_papeletas.php", "H"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Pucallpa", "Consulta vehicular municipal · conexión HTTP", "HTTP", "http://consultas.municportillo.gob.pe:85/consultaVehiculo/consulta/", "P"),
            new OfficialSource(CATEGORY_MUNICIPAL, "SAT Cajamarca", "Obligaciones y papeletas", "Portal", "https://www.satcajamarca.gob.pe/#/", "C"),
            new OfficialSource(CATEGORY_MUNICIPAL, "SATCH Chiclayo", "Récord de infracciones", "Directa", "https://virtualsatch.satch.gob.pe/virtualsatch/record_infracciones/buscar_placa_", "C"),
            new OfficialSource(CATEGORY_MUNICIPAL, "SAT Ica", "Papeletas de tránsito", "Directa", "https://m.satica.gob.pe/consultapapeletas_web.php", "I"),
            new OfficialSource(CATEGORY_MUNICIPAL, "SAT Lima", "Consulta de multas y papeletas por placa", "Placa", "https://www.sat.gob.pe/VirtualSAT/principal.aspx", "L"),
            new OfficialSource(CATEGORY_MUNICIPAL, "Piura", "Multas de tránsito por placa · conexión HTTP", "HTTP", "http://www.munipiura.gob.pe/consulta-de-multas-de-transito#buscar-por-placa", "P"),
            new OfficialSource(CATEGORY_MUNICIPAL, "SAT Tarapoto", "Multas e impuesto vehicular", "Captcha", "https://sat-t.gob.pe/", "T"),
            new OfficialSource(CATEGORY_MUNICIPAL, "SATT Trujillo", "Verificación de papeletas", "Datos extra", "https://www.gob.pe/53428-verificar-informacion-de-papeletas-de-transito-y-transporte-en-trujillo", "T")
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        View mainRoot = findViewById(R.id.main_root);
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        currentPlate = PlateUtils.normalize(preferences.getString(KEY_PLATE, ""));
        loadRecentPlates();
        showHome();
        resultsPanel.setVisibility(View.GONE);
        View homeView = ((FrameLayout) findViewById(R.id.content_container)).getChildAt(0);
        ((View) homeView.findViewById(R.id.quick_soat).getParent()).setVisibility(View.GONE);
        android.widget.LinearLayout plateCard = (android.widget.LinearLayout) homeView.findViewById(R.id.quick_soat).getParent().getParent();
        for (int i = 0; i < plateCard.getChildCount(); i++) {
            View child = plateCard.getChildAt(i);
            if (child instanceof TextView && "CONSULTA INDIVIDUAL".contentEquals(((TextView) child).getText()))
                child.setVisibility(View.GONE);
        }
        homeView.findViewById(R.id.show_all_sources).setVisibility(View.GONE);
        ((View) filterVehicle.getParent().getParent().getParent()).setVisibility(View.GONE);
        sourceProgress.setVisibility(View.GONE);
        sourceCategoryTitle.setVisibility(View.GONE);
        com.google.android.material.bottomnavigation.BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        navigation.setOnItemSelectedListener(item -> {
            android.widget.ScrollView scroll = (android.widget.ScrollView) resultsPanel.getParent().getParent();
            android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(scroll.getWindowToken(), 0);
            android.widget.LinearLayout sections = (android.widget.LinearLayout) resultsPanel.getParent();
            boolean home = item.getItemId() == R.id.nav_home;
            for (int i = 0; i < sections.getChildCount(); i++) {
                View section = sections.getChildAt(i);
                if (section != resultsPanel) section.setVisibility(home ? View.VISIBLE : View.GONE);
            }
            if (home && recentPlates.isEmpty()) findViewById(R.id.recent_section).setVisibility(View.GONE);
            sourceSearch.setVisibility(item.getItemId() == R.id.nav_fines ? View.VISIBLE : View.GONE);
            TextView screenTitle = (TextView) ((android.widget.LinearLayout) sourceProgress.getParent()).getChildAt(0);
            screenTitle.setText(item.getItemId() == R.id.nav_fines ? "Multas y papeletas"
                    : item.getItemId() == R.id.nav_insurance ? "Seguros e historial" : "Datos del vehículo");
            if (item.getItemId() == R.id.nav_home) {
                resultsPanel.setVisibility(View.GONE);
                scroll.smoothScrollTo(0, 0);
            } else {
                resultsPanel.setVisibility(View.VISIBLE);
                selectCategory(item.getItemId() == R.id.nav_fines ? CATEGORY_MUNICIPAL
                        : item.getItemId() == R.id.nav_insurance ? CATEGORY_INSURANCE : CATEGORY_VEHICLE);
                resultsPanel.post(() -> scroll.smoothScrollTo(0, 0));
            }
            return true;
        });
    }

    private void showHome() {
        FrameLayout content = findViewById(R.id.content_container);
        View root = LayoutInflater.from(this).inflate(R.layout.view_home, content, false);
        content.removeAllViews();
        content.addView(root);

        resultsPanel = root.findViewById(R.id.results_panel);
        sourceContainer = root.findViewById(R.id.report_sources);
        sourceProgress = root.findViewById(R.id.source_progress);
        sourceCategoryTitle = root.findViewById(R.id.source_category_title);
        sourceSearch = root.findViewById(R.id.source_search);
        filterVehicle = root.findViewById(R.id.filter_vehicle);
        filterInsurance = root.findViewById(R.id.filter_insurance);
        filterNational = root.findViewById(R.id.filter_national);
        filterMunicipal = root.findViewById(R.id.filter_municipal);
        android.widget.LinearLayout filters = (android.widget.LinearLayout) filterVehicle.getParent();
        ((android.widget.HorizontalScrollView) filters.getParent()).setFillViewport(true);
        filters.removeAllViews();
        filters.setOrientation(android.widget.LinearLayout.VERTICAL);
        View[] choices = {filterVehicle, filterInsurance, filterNational, filterMunicipal};
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            android.widget.LinearLayout.LayoutParams rowLayout = new android.widget.LinearLayout.LayoutParams(-1, dp(52));
            rowLayout.bottomMargin = dp(8);
            filters.addView(row, rowLayout);
            for (int column = 0; column < 2; column++) {
                View choice = choices[rowIndex * 2 + column];
                choice.setMinimumWidth(0);
                android.widget.LinearLayout.LayoutParams cell = new android.widget.LinearLayout.LayoutParams(0, -1, 1f);
                if (column == 1) cell.leftMargin = dp(8);
                row.addView(choice, cell);
            }
        }
        ((TextView) filterVehicle).setText("Vehículo y revisión");
        ((TextView) filterInsurance).setText("SOAT e historial");
        ((TextView) filterNational).setText("Multas SUTRAN");
        filterVehicle.setOnClickListener(view -> selectCategory(CATEGORY_VEHICLE));
        filterInsurance.setOnClickListener(view -> selectCategory(CATEGORY_INSURANCE));
        filterNational.setOnClickListener(view -> selectCategory(CATEGORY_NATIONAL));
        filterMunicipal.setOnClickListener(view -> selectCategory(CATEGORY_MUNICIPAL));
        sourceSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable editable) {
                sourceQuery = editable.toString().trim();
                if (resultsPanel.getVisibility() == View.VISIBLE) renderSources();
            }
        });

        EditText plateInput = root.findViewById(R.id.plate_input);
        if (!currentPlate.isEmpty()) plateInput.setText(PlateUtils.display(currentPlate));
        plateInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable editable) {
                currentPlate = PlateUtils.normalize(editable.toString());
                preferences.edit().putString(KEY_PLATE, currentPlate).apply();
                ((TextView) resultsPanel.findViewById(R.id.results_plate)).setText(
                        currentPlate.isEmpty() ? "Sin placa" : PlateUtils.display(currentPlate));
            }
        });

        root.findViewById(R.id.search_plate).setOnClickListener(view -> openMultipleCheck(plateInput));
        root.findViewById(R.id.quick_soat).setOnClickListener(view -> openDirectSource(plateInput, sources.get(2)));
        root.findViewById(R.id.quick_sutran).setOnClickListener(view -> openDirectSource(plateInput, sources.get(3)));
        root.findViewById(R.id.quick_mtc).setOnClickListener(view -> openDirectSource(plateInput, sources.get(1)));
        root.findViewById(R.id.show_all_sources).setOnClickListener(view ->
                ((com.google.android.material.bottomnavigation.BottomNavigationView) findViewById(R.id.bottom_navigation))
                        .setSelectedItemId(R.id.nav_fines));

        LinearLayout recentContainer = root.findViewById(R.id.recent_container);
        renderRecentPlates(root, recentContainer, plateInput);
        root.findViewById(R.id.clear_recent).setOnClickListener(view -> {
            recentPlates.clear();
            saveRecentPlates();
            renderRecentPlates(root, recentContainer, plateInput);
        });
        selectedCategory = CATEGORY_MUNICIPAL;
        resultsPanel.setVisibility(View.VISIBLE);
        ((TextView) resultsPanel.findViewById(R.id.results_plate)).setText(
                currentPlate.isEmpty() ? "Sin placa" : PlateUtils.display(currentPlate));
        updateCategoryStyle();
        renderSources();
    }

    private boolean preparePlate(EditText plateInput) {
        String normalized = PlateUtils.normalize(plateInput.getText().toString());
        if (!PlateUtils.isValidPeruvianPlate(normalized)) {
            plateInput.setError("Ingresa una placa válida, por ejemplo ABC-123");
            plateInput.requestFocus();
            return false;
        }

        currentPlate = normalized;
        plateInput.setText(PlateUtils.display(normalized));
        plateInput.setSelection(plateInput.length());
        preferences.edit().putString(KEY_PLATE, currentPlate).apply();
        recentPlates.remove(currentPlate);
        recentPlates.add(0, currentPlate);
        while (recentPlates.size() > 5) recentPlates.remove(recentPlates.size() - 1);
        saveRecentPlates();
        return true;
    }

    private void openDirectSource(EditText plateInput, OfficialSource source) {
        if (!preparePlate(plateInput)) return;
        openedSources.add(source.title);
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_TITLE, source.title);
        intent.putExtra(WebViewActivity.EXTRA_URL, source.url);
        intent.putExtra(WebViewActivity.EXTRA_PLATE, currentPlate);
        startActivity(intent);
    }

    private void openMultipleCheck(EditText plateInput) {
        if (!preparePlate(plateInput)) return;
        Intent intent = new Intent(this, MultiCheckActivity.class);
        intent.putExtra(MultiCheckActivity.EXTRA_PLATE, currentPlate);
        startActivity(intent);
    }

    private void showAllSources(EditText plateInput) {
        if (!preparePlate(plateInput)) return;

        openedSources.clear();
        selectedCategory = CATEGORY_MUNICIPAL;
        sourceQuery = "";
        sourceSearch.setText("");
        ((TextView) resultsPanel.findViewById(R.id.results_plate)).setText(PlateUtils.display(currentPlate));
        updateCategoryStyle();
        renderSources();
        resultsPanel.setVisibility(View.VISIBLE);
        resultsPanel.post(() -> ((android.widget.ScrollView) resultsPanel.getParent().getParent())
                .smoothScrollTo(0, resultsPanel.getTop()));
    }

    private void renderSources() {
        sourceContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        String normalizedQuery = sourceQuery.toLowerCase();
        int visibleCount = 0;
        String lastGroup = "";
        List<OfficialSource> ordered = new java.util.ArrayList<>(sources);
        ordered.sort(java.util.Comparator.comparing(this::sourceGroup).thenComparing(source -> source.title));
        for (OfficialSource source : ordered) {
            boolean matchesCategory = source.category.equals(selectedCategory)
                    || (CATEGORY_MUNICIPAL.equals(selectedCategory) && CATEGORY_NATIONAL.equals(source.category));
            boolean matchesQuery = sourceQuery.isEmpty()
                    || source.title.toLowerCase().contains(normalizedQuery)
                    || source.description.toLowerCase().contains(normalizedQuery);
            if ((!sourceQuery.isEmpty() && !matchesQuery) || (sourceQuery.isEmpty() && !matchesCategory)) continue;
            if (CATEGORY_MUNICIPAL.equals(selectedCategory) && !matchesCategory) continue;
            String group = sourceGroup(source);
            if (CATEGORY_MUNICIPAL.equals(selectedCategory) && !group.equals(lastGroup)) {
                TextView heading = new TextView(this);
                heading.setText(group.substring(3));
                heading.setTextSize(13);
                heading.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
                heading.setTextColor(ContextCompat.getColor(this, R.color.navy));
                heading.setPadding(dp(4), dp(18), dp(4), dp(10));
                sourceContainer.addView(heading);
                lastGroup = group;
            }
            View row = inflater.inflate(R.layout.item_report_source, sourceContainer, false);
            ((TextView) row.findViewById(R.id.report_source_symbol)).setText(source.symbol);
            ((TextView) row.findViewById(R.id.report_source_title)).setText(source.title);
            ((TextView) row.findViewById(R.id.report_source_description)).setText(source.description);
            TextView status = row.findViewById(R.id.report_source_status);
            boolean opened = openedSources.contains(source.title);
            status.setText(opened ? "ABIERTA" : source.accessLabel.toUpperCase());
            styleStatus(status, opened);
            row.setOnClickListener(view -> openSource(source));
            sourceContainer.addView(row);
            visibleCount++;
        }
        sourceProgress.setText(openedSources.size() + " de " + sources.size() + " fuentes abiertas");
        sourceCategoryTitle.setText(sourceQuery.isEmpty() ? categoryTitle(selectedCategory) : "Resultados de búsqueda · " + visibleCount);
    }

    private String sourceGroup(OfficialSource source) {
        if (CATEGORY_NATIONAL.equals(source.category)) return "01 Nacional";
        if (CATEGORY_MUNICIPAL.equals(source.category)) {
            if (source.title.equals("SAT Lima") || source.title.startsWith("ATU") || source.title.startsWith("Callao"))
                return "02 Lima y Callao";
            return "03 Provincias · busca tu ciudad";
        }
        return CATEGORY_INSURANCE.equals(source.category) ? "06 Seguros" : "05 Registro y revisión técnica";
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        sourceQuery = "";
        sourceSearch.setText("");
        updateCategoryStyle();
        renderSources();
    }

    private void updateCategoryStyle() {
        int municipalCount = 0;
        for (OfficialSource source : sources) if (CATEGORY_MUNICIPAL.equals(source.category)) municipalCount++;
        ((TextView) filterMunicipal).setText("Multas por ciudad · " + municipalCount);
        styleFilter(filterVehicle, CATEGORY_VEHICLE.equals(selectedCategory));
        styleFilter(filterInsurance, CATEGORY_INSURANCE.equals(selectedCategory));
        styleFilter(filterNational, CATEGORY_NATIONAL.equals(selectedCategory));
        styleFilter(filterMunicipal, CATEGORY_MUNICIPAL.equals(selectedCategory));
    }

    private void styleFilter(View view, boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(16));
        background.setColor(ContextCompat.getColor(this, selected ? R.color.navy : R.color.surface));
        background.setStroke(dp(1), ContextCompat.getColor(this, selected ? R.color.navy : R.color.divider));
        view.setBackground(background);
        if (view instanceof TextView) ((TextView) view).setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.navy));
    }

    private String categoryTitle(String category) {
        if (CATEGORY_INSURANCE.equals(category)) return "Seguro obligatorio";
        if (CATEGORY_NATIONAL.equals(category)) return "Infracciones nacionales";
        if (CATEGORY_MUNICIPAL.equals(category)) return "Multas por ciudad y entidad";
        return "Datos del vehículo";
    }

    private void openSource(OfficialSource source) {
        openedSources.add(source.title);
        renderSources();
        if ("SUNARP".equals(source.title)) {
            Intent intent = new Intent(this, SunarpServicesActivity.class);
            intent.putExtra(SunarpServicesActivity.EXTRA_PLATE, currentPlate);
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_TITLE, source.title);
        intent.putExtra(WebViewActivity.EXTRA_URL, source.url);
        intent.putExtra(WebViewActivity.EXTRA_PLATE, currentPlate);
        startActivity(intent);
    }

    private void styleStatus(TextView view, boolean opened) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(20));
        background.setColor(Color.parseColor(opened ? "#E7F7EF" : "#E7EEF6"));
        view.setTextColor(Color.parseColor(opened ? "#157347" : "#002B5C"));
        view.setBackground(background);
    }

    private void renderRecentPlates(View root, LinearLayout container, EditText plateInput) {
        container.removeAllViews();
        root.findViewById(R.id.recent_section).setVisibility(recentPlates.isEmpty() ? View.GONE : View.VISIBLE);
        for (String plate : recentPlates) {
            TextView chip = new TextView(this);
            chip.setText("◷  " + PlateUtils.display(plate));
            chip.setTextColor(ContextCompat.getColor(this, R.color.navy));
            chip.setTextSize(13);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(14), 0, dp(14), 0);
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(this, R.color.surface));
            background.setCornerRadius(dp(24));
            background.setStroke(dp(1), ContextCompat.getColor(this, R.color.divider));
            chip.setBackground(background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
            params.setMarginEnd(dp(9));
            chip.setLayoutParams(params);
            chip.setOnClickListener(view -> {
                plateInput.setText(PlateUtils.display(plate));
                plateInput.setSelection(plateInput.length());
                openMultipleCheck(plateInput);
            });
            container.addView(chip);
        }
    }

    private void loadRecentPlates() {
        recentPlates.clear();
        String saved = preferences.getString(KEY_RECENT, "");
        if (saved == null || saved.isEmpty()) return;
        for (String plate : saved.split(",")) {
            String normalized = PlateUtils.normalize(plate);
            if (PlateUtils.isValidPeruvianPlate(normalized) && !recentPlates.contains(normalized)) recentPlates.add(normalized);
        }
    }

    private void saveRecentPlates() {
        preferences.edit().putString(KEY_RECENT, String.join(",", recentPlates)).apply();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class OfficialSource {
        final String category;
        final String title;
        final String description;
        final String accessLabel;
        final String url;
        final String symbol;

        OfficialSource(String category, String title, String description, String accessLabel, String url, String symbol) {
            this.category = category;
            this.title = title;
            this.description = description;
            this.accessLabel = accessLabel;
            this.url = url;
            this.symbol = symbol;
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
