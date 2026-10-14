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
  private EditText sourceSearch;
  private String selectedCategory = CATEGORY_VEHICLE;
  private String sourceQuery = "";
  private UnifiedQueryPanel inlineQuery;
  private LinearLayout inlineQuerySlot;
  private boolean queryRunning;
  private Runnable refreshHomeSelection;

  private final List<OfficialSource> sources = OfficialSourceCatalog.SOURCES;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
        .setAppearanceLightStatusBars(true);
    View mainRoot = findViewById(R.id.main_root);
    ViewCompat.setOnApplyWindowInsetsListener(
        mainRoot,
        (view, insets) -> {
          Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          boolean showingKeyboard = insets.isVisible(WindowInsetsCompat.Type.ime());
          findViewById(R.id.bottom_navigation)
              .setVisibility(showingKeyboard ? View.GONE : View.VISIBLE);
          int keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
          view.setPadding(0, bars.top, 0, Math.max(bars.bottom, keyboard));
          return insets;
        });

    preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
    currentPlate = PlateUtils.normalize(preferences.getString(KEY_PLATE, ""));
    loadRecentPlates();
    showHome();
    resultsPanel.setVisibility(View.GONE);
    com.google.android.material.bottomnavigation.BottomNavigationView navigation =
        findViewById(R.id.bottom_navigation);
    navigation.setOnItemSelectedListener(
        item -> {
          android.widget.ScrollView scroll =
              (android.widget.ScrollView) resultsPanel.getParent().getParent();
          android.view.inputmethod.InputMethodManager keyboard =
              (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
          keyboard.hideSoftInputFromWindow(scroll.getWindowToken(), 0);
          android.widget.LinearLayout sections =
              (android.widget.LinearLayout) resultsPanel.getParent();
          boolean home = item.getItemId() == R.id.nav_home;
          for (int i = 0; i < sections.getChildCount(); i++) {
            View section = sections.getChildAt(i);
            if (section != resultsPanel) section.setVisibility(home ? View.VISIBLE : View.GONE);
          }
          if (home && recentPlates.isEmpty())
            findViewById(R.id.recent_section).setVisibility(View.GONE);
          sourceSearch.setVisibility(item.getItemId() == R.id.nav_fines ? View.VISIBLE : View.GONE);
          TextView screenTitle = findViewById(R.id.directory_title);
          screenTitle.setText(
              item.getItemId() == R.id.nav_fines
                  ? "Multas y papeletas"
                  : item.getItemId() == R.id.nav_insurance
                      ? "Seguros e historial"
                      : "Datos del vehículo");
          if (item.getItemId() == R.id.nav_home) {
            resultsPanel.setVisibility(View.GONE);
            scroll.smoothScrollTo(0, 0);
          } else {
            resultsPanel.setVisibility(View.VISIBLE);
            selectCategory(
                item.getItemId() == R.id.nav_fines
                    ? CATEGORY_MUNICIPAL
                    : item.getItemId() == R.id.nav_insurance
                        ? CATEGORY_INSURANCE
                        : CATEGORY_VEHICLE);
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
    sourceSearch = root.findViewById(R.id.source_search);
    sourceSearch.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(Editable editable) {
            sourceQuery = editable.toString().trim();
            if (resultsPanel.getVisibility() == View.VISIBLE) renderSources();
          }
        });

    EditText plateInput = root.findViewById(R.id.plate_input);
    if (!currentPlate.isEmpty()) plateInput.setText(PlateUtils.display(currentPlate));
    plateInput.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(Editable editable) {
            currentPlate = PlateUtils.normalize(editable.toString());
            preferences.edit().putString(KEY_PLATE, currentPlate).apply();
            ((TextView) resultsPanel.findViewById(R.id.results_plate))
                .setText(currentPlate.isEmpty() ? "Sin placa" : PlateUtils.display(currentPlate));
          }
        });

    com.google.android.material.button.MaterialButton homeConsult =
        root.findViewById(R.id.search_plate);
    LinearLayout homeCard = (LinearLayout) homeConsult.getParent();
    addInlineSourceSelection(homeCard, homeConsult, plateInput);
    inlineQuerySlot = new LinearLayout(this);
    inlineQuerySlot.setOrientation(LinearLayout.VERTICAL);
    homeCard.addView(inlineQuerySlot, homeCard.indexOfChild(homeConsult) + 1);
    homeCard.addView(CurrencyConverter.buildView(this), homeCard.indexOfChild(homeConsult) + 1);
    LinearLayout recentContainer = root.findViewById(R.id.recent_container);
    renderRecentPlates(root, recentContainer, plateInput);
    TextView clearHistory = root.findViewById(R.id.clear_recent);
    IconActions.apply(clearHistory, R.drawable.ic_delete, "Borrar historial de placas");
    clearHistory.getLayoutParams().height = dp(48);
    root.findViewById(R.id.clear_recent)
        .setOnClickListener(
            view -> {
              recentPlates.clear();
              saveRecentPlates();
              renderRecentPlates(root, recentContainer, plateInput);
            });
    selectedCategory = CATEGORY_MUNICIPAL;
    resultsPanel.setVisibility(View.VISIBLE);
    ((TextView) resultsPanel.findViewById(R.id.results_plate))
        .setText(currentPlate.isEmpty() ? "Sin placa" : PlateUtils.display(currentPlate));
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

  private void renderSources() {
    sourceContainer.removeAllViews();
    if (CATEGORY_MUNICIPAL.equals(selectedCategory)) {
      sourceSearch.setVisibility(View.VISIBLE);
    }
    TextView help = new TextView(this);
    help.setText(
        CATEGORY_MUNICIPAL.equals(selectedCategory)
            ? "Explora por ciudad. Consulta varias a la vez desde Inicio."
            : CATEGORY_INSURANCE.equals(selectedCategory)
                ? "Pólizas, uso declarado y accidentes reportados."
                : "Registro, características y revisión técnica.");
    help.setTextSize(13);
    help.setTextColor(Color.parseColor("#6B7280"));
    help.setPadding(dp(4), dp(8), dp(4), dp(16));
    sourceContainer.addView(help);
    LayoutInflater inflater = LayoutInflater.from(this);
    String normalizedQuery = sourceQuery.toLowerCase();
    int visibleCount = 0;
    String lastGroup = "";
    List<OfficialSource> ordered = new java.util.ArrayList<>(sources);
    ordered.sort(
        java.util.Comparator.comparing(this::sourceGroup).thenComparing(source -> source.title));
    for (OfficialSource source : ordered) {
      boolean matchesCategory =
          source.category.equals(selectedCategory)
              || (CATEGORY_MUNICIPAL.equals(selectedCategory)
                  && CATEGORY_NATIONAL.equals(source.category));
      boolean matchesQuery =
          sourceQuery.isEmpty()
              || source.title.toLowerCase().contains(normalizedQuery)
              || source.description.toLowerCase().contains(normalizedQuery);
      if ((!sourceQuery.isEmpty() && !matchesQuery) || (sourceQuery.isEmpty() && !matchesCategory))
        continue;
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
      View separator = new View(this);
      separator.setBackgroundColor(Color.parseColor("#E7EAF0"));
      sourceContainer.addView(separator, new LinearLayout.LayoutParams(-1, dp(1)));
      visibleCount++;
    }
    if (visibleCount == 0) {
      TextView empty = new TextView(this);
      empty.setText("No encontramos esa ciudad o entidad.");
      empty.setTextSize(14);
      empty.setPadding(dp(12), dp(24), dp(12), dp(24));
      sourceContainer.addView(empty);
    }
  }

  private String sourceGroup(OfficialSource source) {
    if (CATEGORY_NATIONAL.equals(source.category)) return "01 Nacional";
    if (CATEGORY_MUNICIPAL.equals(source.category)) {
      if (source.title.equals("SAT Lima")
          || source.title.startsWith("ATU")
          || source.title.startsWith("Callao")) return "02 Lima y Callao";
      return "03 Provincias · busca tu ciudad";
    }
    return CATEGORY_INSURANCE.equals(source.category)
        ? "06 Seguros"
        : "05 Registro y revisión técnica";
  }

  private void selectCategory(String category) {
    selectedCategory = category;
    sourceQuery = "";
    sourceSearch.setText("");
    renderSources();
  }

  private void openSource(OfficialSource source) {
    openedSources.add(source.title);
    renderSources();
    if (CATEGORY_MUNICIPAL.equals(source.category) || CATEGORY_NATIONAL.equals(source.category)) {
      startFinesQuery(java.util.Collections.singletonList(source));
      return;
    }
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

  private void addInlineSourceSelection(
      LinearLayout parent,
      com.google.android.material.button.MaterialButton consult,
      EditText plateInput) {
    java.util.ArrayList<OfficialSource> options = new java.util.ArrayList<>();
    options.addAll(sources);
    options.sort(java.util.Comparator.comparing(s -> s.title));
    java.util.Set<String> selected =
        new java.util.HashSet<>(
            preferences.getStringSet("selected_fines_sources", java.util.Collections.emptySet()));
    if (!preferences.getBoolean("priority_defaults_v1", false)) {
      for (OfficialSource source : options)
        if (priorityIndex(source) >= 0) selected.add(source.url);
      preferences
          .edit()
          .putBoolean("priority_defaults_v1", true)
          .putStringSet("selected_fines_sources", new java.util.HashSet<>(selected))
          .apply();
    }
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(0, dp(12), 0, dp(4));
    parent.addView(panel, parent.indexOfChild(consult));
    LinearLayout heading = new LinearLayout(this);
    heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
    panel.addView(heading);
    TextView title = new TextView(this);
    title.setText("Esenciales");
    title.setTextSize(14);
    title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
    title.setTextColor(Color.parseColor("#002B5C"));
    heading.addView(title, new LinearLayout.LayoutParams(0, dp(36), 1));
    TextView count = new TextView(this);
    count.setTextSize(12);
    count.setTextColor(Color.parseColor("#6B7280"));
    heading.addView(count);
    Runnable refresh =
        () -> {
          int n = 0;
          for (OfficialSource s : options) if (selected.contains(s.url)) n++;
          count.setText(n + " seleccionadas");
          consult.setText(
              queryRunning ? "Cancelar" : n == 0 ? "Consultar" : "Consultar (" + n + ")");
          consult.setEnabled(queryRunning || n > 0);
          preferences
              .edit()
              .putStringSet("selected_fines_sources", new java.util.HashSet<>(selected))
              .apply();
        };
    refreshHomeSelection = refresh;
    TextView recommendation = new TextView(this);
    recommendation.setTextSize(12);
    recommendation.setTextColor(Color.parseColor("#6B7280"));
    recommendation.setPadding(0, 0, 0, dp(6));
    panel.addView(recommendation);
    LinearLayout grid = new LinearLayout(this);
    grid.setOrientation(LinearLayout.VERTICAL);
    panel.addView(grid);
    TextView citiesToggle = new TextView(this);
    citiesToggle.setTextSize(14);
    citiesToggle.setTextColor(Color.parseColor("#002B5C"));
    citiesToggle.setGravity(android.view.Gravity.CENTER_VERTICAL);
    citiesToggle.setMinHeight(dp(48));
    panel.addView(citiesToggle);
    LinearLayout cities = new LinearLayout(this);
    cities.setOrientation(LinearLayout.VERTICAL);
    cities.setVisibility(View.GONE);
    panel.addView(cities);
    Runnable cityLabel =
        () -> {
          int n = 0;
          for (OfficialSource s : options)
            if (priorityIndex(s) < 0 && selected.contains(s.url)) n++;
          citiesToggle.setText(
              "Multas por ciudad"
                  + (n > 0 ? " · " + n : "")
                  + (cities.getVisibility() == View.VISIBLE ? "  −" : "  +"));
        };
    citiesToggle.setOnClickListener(
        v -> {
          cities.setVisibility(cities.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
          cityLabel.run();
        });
    java.util.Map<OfficialSource, View> chips = new java.util.HashMap<>();
    // Create each option once; reorder below assigns its final row and parent.
    for (OfficialSource source : options) {
      com.google.android.material.button.MaterialButton chip =
          new com.google.android.material.button.MaterialButton(this);
      chips.put(source, chip);
      String label =
          source.title.startsWith("SOAT")
              ? "SOAT"
              : source.title.contains("MTC")
                  ? "MTC"
                  : source.title.startsWith("ATU")
                      ? "ATU"
                      : source.title.startsWith("Callao")
                          ? "Callao"
                          : source
                              .title
                              .replace("SATCH ", "")
                              .replace("SATT ", "")
                              .replace("SAT ", "");
      chip.setText(label);
      chip.setAllCaps(false);
      chip.setTextSize(13);
      chip.setCheckable(true);
      chip.setChecked(selected.contains(source.url));
      chip.setCornerRadius(dp(10));
      chip.setMinHeight(dp(48));
      chip.setMinimumWidth(0);
      chip.setMaxLines(2);
      chip.setPadding(dp(4), 0, dp(4), 0);
      chip.setInsetTop(dp(4));
      chip.setInsetBottom(dp(4));
      Runnable style =
          () -> {
            boolean on = selected.contains(source.url);
            chip.setChecked(on);
            chip.setStrokeWidth(dp(1));
            chip.setStrokeColor(
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor(on ? "#002B5C" : "#DFE5ED")));
            chip.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor(on ? "#002B5C" : "#F5F7FA")));
            chip.setTextColor(Color.parseColor(on ? "#FFFFFF" : "#002B5C"));
            chip.setIconResource(0);
            chip.setContentDescription(
                source.title + (on ? ", seleccionada" : ", no seleccionada"));
          };
      style.run();
      chip.setOnClickListener(
          v -> {
            if (queryRunning) {
              android.widget.Toast.makeText(
                      this,
                      "Cancela la consulta antes de cambiar las fuentes",
                      android.widget.Toast.LENGTH_SHORT)
                  .show();
              return;
            }
            if (selected.contains(source.url)) selected.remove(source.url);
            else selected.add(source.url);
            style.run();
            refresh.run();
            cityLabel.run();
          });
    }
    Runnable reorder =
        () -> {
          String region = PlateRegion.region(plateInput.getText().toString());
          boolean any = false;
          for (OfficialSource s : options)
            if (PlateRegion.recommended(plateInput.getText().toString(), s.title)) any = true;
          recommendation.setText(
              region.isEmpty() ? "" : any ? "Sugeridas · " + region : "Zona · " + region);
          recommendation.setVisibility(region.isEmpty() ? View.GONE : View.VISIBLE);
          recommendation.setContentDescription(
              "Zona registral orientativa: "
                  + region
                  + ". El vehículo puede tener multas en otras ciudades.");
          recommendation.setOnClickListener(
              v ->
                  new androidx.appcompat.app.AlertDialog.Builder(this)
                      .setTitle("Recomendación por placa")
                      .setMessage(
                          "El prefijo sólo orienta sobre la zona registral. No confirma dónde"
                              + " circula el vehículo: también puede tener multas en otras"
                              + " ciudades.")
                      .setPositiveButton("Entendido", null)
                      .show());
          java.util.ArrayList<OfficialSource> ordered = new java.util.ArrayList<>(options);
          ordered.sort(
              java.util.Comparator.comparingInt(
                      (OfficialSource s) ->
                          priorityIndex(s) >= 0
                              ? priorityIndex(s)
                              : PlateRegion.recommended(plateInput.getText().toString(), s.title)
                                  ? 4
                                  : 5)
                  .thenComparing(s -> s.title));
          for (View chip : chips.values())
            if (chip.getParent() != null)
              ((android.view.ViewGroup) chip.getParent()).removeView(chip);
          grid.removeAllViews();
          cities.removeAllViews();
          if (recommendation.getParent() != null)
            ((android.view.ViewGroup) recommendation.getParent()).removeView(recommendation);
          LinearLayout row = null;
          for (int i = 0; i < ordered.size(); i++) {
            if (i == 4) {
              View divider = new View(this);
              divider.setBackgroundColor(Color.parseColor("#DFE5ED"));
              LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, dp(1));
              line.topMargin = dp(10);
              grid.addView(divider, line);
              cities.addView(recommendation);
            }
            int column = i < 4 ? i % 2 : (i - 4) % 3;
            if (column == 0) {
              row = new LinearLayout(this);
              (i < 4 ? grid : cities).addView(row);
            }
            OfficialSource s = ordered.get(i);
            View chip = chips.get(s);
            LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, dp(48), 1);
            if (column > 0) cell.leftMargin = dp(4);
            row.addView(chip, cell);
            chip.setContentDescription(
                s.title
                    + (PlateRegion.recommended(plateInput.getText().toString(), s.title)
                        ? ", sugerida por placa"
                        : "")
                    + (selected.contains(s.url) ? ", seleccionada" : ", no seleccionada"));
          }
          if (row != null)
            for (int i = (ordered.size() - 4) % 3; i > 0 && i < 3; i++) {
              LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, dp(48), 1);
              cell.leftMargin = dp(4);
              row.addView(new View(this), cell);
            }
        };
    plateInput.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(Editable e) {
            reorder.run();
          }
        });
    reorder.run();
    cityLabel.run();
    refresh.run();
    consult.setOnClickListener(
        v -> {
          if (queryRunning) {
            cancelActiveQuery();
            return;
          }
          java.util.ArrayList<OfficialSource> chosen = new java.util.ArrayList<>();
          for (OfficialSource s : options) if (selected.contains(s.url)) chosen.add(s);
          if (!chosen.isEmpty() && preparePlate(plateInput)) {
            cities.setVisibility(View.GONE);
            cityLabel.run();
            startFinesQuery(chosen);
          }
        });
  }

  private void startFinesQuery(List<OfficialSource> selected) {
    if (queryRunning) return;
    if (!PlateUtils.isValidPeruvianPlate(currentPlate)) {
      EditText input = new EditText(this);
      input.setHint("Placa, por ejemplo A4D234");
      input.setSingleLine(true);
      input.setInputType(
          android.text.InputType.TYPE_CLASS_TEXT
              | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
      input.setPadding(dp(24), dp(12), dp(24), dp(12));
      androidx.appcompat.app.AlertDialog dialog =
          new androidx.appcompat.app.AlertDialog.Builder(this)
              .setTitle("Consultar multas")
              .setView(input)
              .setPositiveButton("Consultar", null)
              .setNegativeButton("Cancelar", null)
              .create();
      dialog.setOnShowListener(
          ignored ->
              dialog
                  .getButton(-1)
                  .setOnClickListener(
                      v -> {
                        String plate = PlateUtils.normalize(input.getText().toString());
                        if (!PlateUtils.isValidPeruvianPlate(plate)) {
                          input.setError("Ingresa una placa de 6 caracteres");
                          return;
                        }
                        currentPlate = plate;
                        dialog.dismiss();
                        startFinesQuery(selected);
                      }));
      dialog.show();
      return;
    }
    EditText homeInput = findViewById(R.id.plate_input);
    if (homeInput != null) {
      homeInput.setText(PlateUtils.display(currentPlate));
      preparePlate(homeInput);
    }
    java.util.ArrayList<String> names = new java.util.ArrayList<>(),
        urls = new java.util.ArrayList<>();
    for (OfficialSource s : selected) {
      names.add(s.title);
      urls.add(s.url);
    }
    if (inlineQuery != null) inlineQuery.dispose();
    inlineQuerySlot.removeAllViews();
    Intent intent =
        new Intent()
            .putExtra("plate", PlateUtils.normalize(currentPlate))
            .putExtra("names", names.toArray(new String[0]))
            .putExtra("urls", urls.toArray(new String[0]));
    queryRunning = true;
    homeInput.setEnabled(false);
    refreshHomeSelection.run();
    inlineQuery = new UnifiedQueryPanel(this, intent, this::cancelActiveQuery);
    inlineQuery.setCompletionListener(
        () -> {
          queryRunning = false;
          homeInput.setEnabled(true);
          refreshHomeSelection.run();
        });
    inlineQuery.start();
    inlineQuerySlot.addView(inlineQuery.view(), new LinearLayout.LayoutParams(-1, -2));
    ((com.google.android.material.bottomnavigation.BottomNavigationView)
            findViewById(R.id.bottom_navigation))
        .setSelectedItemId(R.id.nav_home);
    inlineQuerySlot.post(
        () -> {
          android.widget.ScrollView scroll =
              (android.widget.ScrollView)
                  ((FrameLayout) findViewById(R.id.content_container)).getChildAt(0);
          android.graphics.Rect rect = new android.graphics.Rect();
          inlineQuerySlot.getDrawingRect(rect);
          scroll.offsetDescendantRectToMyCoords(inlineQuerySlot, rect);
          scroll.smoothScrollTo(0, rect.top);
        });
  }

  private int priorityIndex(OfficialSource s) {
    return s.title.equals("SUNARP")
        ? 0
        : s.title.startsWith("SOAT")
            ? 1
            : s.title.equals("SUTRAN") ? 2 : s.title.contains("MTC") ? 3 : -1;
  }

  private void cancelActiveQuery() {
    if (inlineQuery != null) {
      inlineQuery.dispose();
      inlineQuery = null;
    }
    inlineQuerySlot.removeAllViews();
    queryRunning = false;
    EditText input = findViewById(R.id.plate_input);
    if (input != null) input.setEnabled(true);
    if (refreshHomeSelection != null) refreshHomeSelection.run();
  }

  @Override
  protected void onDestroy() {
    if (inlineQuery != null) inlineQuery.dispose();
    super.onDestroy();
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
    root.findViewById(R.id.recent_section)
        .setVisibility(recentPlates.isEmpty() ? View.GONE : View.VISIBLE);
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
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
      params.setMarginEnd(dp(9));
      chip.setLayoutParams(params);
      chip.setOnClickListener(
          view -> {
            if (queryRunning) {
              android.widget.Toast.makeText(
                      this,
                      "Cancela la consulta antes de cambiar la placa",
                      android.widget.Toast.LENGTH_SHORT)
                  .show();
              return;
            }
            plateInput.setText(PlateUtils.display(plate));
            plateInput.setSelection(plateInput.length());
            plateInput.clearFocus();
            ((com.google.android.material.bottomnavigation.BottomNavigationView)
                    findViewById(R.id.bottom_navigation))
                .setSelectedItemId(R.id.nav_home);
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
      if (PlateUtils.isValidPeruvianPlate(normalized) && !recentPlates.contains(normalized))
        recentPlates.add(normalized);
    }
  }

  private void saveRecentPlates() {
    preferences.edit().putString(KEY_RECENT, String.join(",", recentPlates)).apply();
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private abstract static class SimpleTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}
  }
}
