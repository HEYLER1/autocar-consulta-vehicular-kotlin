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
  private final List<android.webkit.WebView> assistedForms = new ArrayList<>();
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
          findViewById(R.id.top_navigation)
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
    com.google.android.material.tabs.TabLayout navigation = findViewById(R.id.top_navigation);
    navigation.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
      @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
        showDestination(NAV_DESTINATIONS[tab.getPosition()]);
      }
      @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
      @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
    });
  }

  private static final int[] NAV_DESTINATIONS = {
    R.id.nav_home, R.id.nav_vehicle, R.id.nav_insurance
  };

  private void showDestination(int destination) {
    android.widget.ScrollView scroll =
        (android.widget.ScrollView) resultsPanel.getParent().getParent();
    android.view.inputmethod.InputMethodManager keyboard =
        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    keyboard.hideSoftInputFromWindow(scroll.getWindowToken(), 0);
    boolean home = destination == R.id.nav_home;
    LinearLayout sections = (LinearLayout) resultsPanel.getParent();
    for (int i = 0; i < sections.getChildCount(); i++) {
      View section = sections.getChildAt(i);
      if (section != resultsPanel) section.setVisibility(home ? View.VISIBLE : View.GONE);
    }
    if (recentPlates.isEmpty()) findViewById(R.id.recent_section).setVisibility(View.GONE);
    sourceSearch.setVisibility(View.GONE);
    TextView screenTitle = findViewById(R.id.directory_title);
    screenTitle.setText(
        destination == R.id.nav_insurance
                ? "Seguros e historial"
                : "Datos del vehículo");
    if (destination == R.id.nav_home) {
      resultsPanel.setVisibility(View.GONE);
      scroll.smoothScrollTo(0, 0);
    } else {
      resultsPanel.setVisibility(View.VISIBLE);
      selectCategory(
          destination == R.id.nav_insurance
                  ? CATEGORY_INSURANCE
                  : CATEGORY_VEHICLE);
      resultsPanel.post(() -> scroll.smoothScrollTo(0, 0));
    }
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
    LinearLayout extras = root.findViewById(R.id.home_extras);
    inlineQuerySlot = new LinearLayout(this);
    inlineQuerySlot.setOrientation(LinearLayout.VERTICAL);
    extras.addView(inlineQuerySlot);
    addInlineSourceSelection(extras, homeConsult, plateInput);
    LinearLayout converter = CurrencyConverter.buildView(this);
    converter.setBackgroundResource(R.drawable.bg_round_card);
    TextView currencyTitle = new TextView(this);
    currencyTitle.setText("Conversor de moneda");
    currencyTitle.setTextColor(Color.rgb(16, 60, 53));
    currencyTitle.setTypeface(null, android.graphics.Typeface.BOLD);
    currencyTitle.setPadding(dp(4), 0, 0, dp(4));
    converter.addView(currencyTitle, 0);
    LinearLayout.LayoutParams converterLayout = new LinearLayout.LayoutParams(-1, -2);
    converterLayout.topMargin = dp(12);
    extras.addView(converter, converterLayout);
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

  private void addPreparedChecks(List<OfficialSource> checks, String plate) {
    if (checks.isEmpty()) return;
    TextView heading = new TextView(this);
    heading.setText("Verificar formularios (" + checks.size() + ")  ▾");
    heading.setTextColor(ContextCompat.getColor(this, R.color.navy));
    heading.setPadding(dp(12), 0, dp(12), 0);
    heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
    heading.setMinHeight(dp(48));
    inlineQuerySlot.addView(heading);
    LinearLayout forms = new LinearLayout(this);
    forms.setOrientation(LinearLayout.VERTICAL);
    forms.setVisibility(View.GONE);
    inlineQuerySlot.addView(forms);
    heading.setOnClickListener(v -> {
      boolean open = forms.getVisibility() != View.VISIBLE;
      forms.setVisibility(open ? View.VISIBLE : View.GONE);
      heading.setText("Verificar formularios (" + checks.size() + ")" + (open ? "  ▴" : "  ▾"));
    });
    for (OfficialSource check : checks) {
      com.google.android.material.button.MaterialButton button =
          new com.google.android.material.button.MaterialButton(this);
      button.setAllCaps(false);
      button.setText(check.title + " ›");
      forms.addView(button);
      FrameLayout container = new FrameLayout(this);
      container.setVisibility(View.GONE);
      forms.addView(container, new LinearLayout.LayoutParams(-1, -2));
      button.setOnClickListener(v -> {
        if (check == ManualVerificationCatalog.TRAFFIC.get(3)) {
          openTrafficCheck(check, plate);
          return;
        }
        boolean open = container.getVisibility() != View.VISIBLE;
        container.setVisibility(open ? View.VISIBLE : View.GONE);
        if (open && container.getChildCount() == 0) prepareEmbeddedForm(container, check, plate);
      });
    }
  }

  @android.annotation.SuppressLint("SetJavaScriptEnabled")
  private void prepareEmbeddedForm(FrameLayout container, OfficialSource check, String plate) {
    android.webkit.WebView form = new android.webkit.WebView(this);
    android.net.Uri endpoint = android.net.Uri.parse(check.url);
    LinearLayout surface = new LinearLayout(this);
    surface.setOrientation(LinearLayout.VERTICAL);
    container.addView(surface, new FrameLayout.LayoutParams(-1, -2));
    TextView toggle = new TextView(this);
    toggle.setText("Ver formulario completo");
    toggle.setTextColor(ContextCompat.getColor(this, R.color.navy));
    toggle.setMinHeight(dp(48));
    toggle.setGravity(android.view.Gravity.CENTER_VERTICAL);
    surface.addView(toggle);
    LinearLayout.LayoutParams viewport = new LinearLayout.LayoutParams(-1, dp(250));
    surface.addView(form, viewport);
    boolean[] expanded = {false};
    boolean[] switched = {false};
    toggle.setOnClickListener(v -> {
      expanded[0] = !expanded[0];
      viewport.height = dp(expanded[0] ? 520 : 250);
      form.setLayoutParams(viewport);
      toggle.setText(expanded[0] ? "Volver al código de seguridad" : "Ver formulario completo");
      if (expanded[0]) form.scrollTo(0, 0);
      else prepareManualCaptchaView(form, endpoint, form.getUrl(), plate, switched, false);
    });
    form.getSettings().setJavaScriptEnabled(true);
    form.getSettings().setDomStorageEnabled(true);
    form.getSettings().setAllowFileAccess(false);
    form.getSettings().setAllowContentAccess(false);
    form.setWebViewClient(new android.webkit.WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(android.webkit.WebView view,
          android.webkit.WebResourceRequest request) {
        android.net.Uri target = request.getUrl();
        return request.isForMainFrame() && (!"https".equals(target.getScheme())
            || !endpoint.getHost().equals(target.getHost())
            || (target.getPort() != -1 && target.getPort() != 443));
      }
      @Override public void onPageFinished(android.webkit.WebView view, String url) {
        if (AutoLoginAutomation.tryAutoLogin(view, url)) return;
        prepareManualCaptchaView(view, endpoint, url, plate, switched, expanded[0]);
      }
    });
    assistedForms.add(form);
    form.loadUrl(check.url);
  }

  private void prepareManualCaptchaView(android.webkit.WebView view, android.net.Uri endpoint,
      String url, String plate, boolean[] switched, boolean expanded) {
    if (url == null) return;
    android.net.Uri page = android.net.Uri.parse(url);
    if (!"https".equals(page.getScheme()) || !endpoint.getHost().equals(page.getHost())
        || !java.util.Objects.equals(endpoint.getPath(), page.getPath())
        || (page.getPort() != -1 && page.getPort() != 443)) return;
    boolean sutran = "webexterno.sutran.gob.pe".equals(endpoint.getHost());
    SharedPreferences profile = getSharedPreferences("traffic_identity", MODE_PRIVATE);
    String script = "(function(){if(location.origin!=="
        + org.json.JSONObject.quote("https://" + endpoint.getHost()) + "||location.pathname!=="
        + org.json.JSONObject.quote(endpoint.getPath()) + ")return;"
        + (sutran && !switched[0]
            ? "var mode=document.getElementById('rbtListMovimiento_1');"
                + "if(mode&&!mode.checked){mode.click();return 'switching';}" : "")
        + "function fill(id,value){var e=document.getElementById(id);"
        + "if(!e||e.disabled||!e.getClientRects().length)return false;"
        + "if(value&&!e.value&&!e.readOnly)e.value=value;return !!e.value;}"
        + "var ready=fill(" + org.json.JSONObject.quote(sutran ? "txtPlaca" : "ctl00_cplPrincipal_txtPlaca")
        + "," + org.json.JSONObject.quote(plate) + ");"
        + (sutran ? "var t=document.getElementById('ddlTipoBusqueda');"
            + "if(t&&t.value==='2'){var dni=fill('TxtBuscar',"
            + org.json.JSONObject.quote(profile.getString("dni", "")) + ");"
            + "var digit=fill('txtCodigoVerificador',"
            + org.json.JSONObject.quote(profile.getString("digit", "")) + ");ready=ready&&dni&&digit;}"
            + "else ready=false;" : "")
        + (!expanded ? "if(ready){var code=document.getElementById("
            + org.json.JSONObject.quote(sutran ? "TxtCodImagen" : "ctl00_cplPrincipal_txtCaptcha")
            + ");if(code&&code.getClientRects().length){var area=code.closest('tr')||code;"
            + "area.scrollIntoView({block:'center'});}}" : "")
        + "})();";
    view.evaluateJavascript(script, result -> {
      if ("\"switching\"".equals(result)) switched[0] = true;
    });
  }

  private void disposeAssistedForms() {
    for (android.webkit.WebView form : assistedForms) {
      TrafficFormAutomation.clear(form);
      if (form.getParent() instanceof android.view.ViewGroup)
        ((android.view.ViewGroup) form.getParent()).removeView(form);
      form.stopLoading();
      form.destroy();
    }
    assistedForms.clear();
  }

  private void openTrafficCheck(OfficialSource check, String plate) {
    startActivity(new Intent(this, WebViewActivity.class)
        .putExtra(WebViewActivity.EXTRA_URL, check.url)
        .putExtra(WebViewActivity.EXTRA_TITLE, check.title)
        .putExtra(WebViewActivity.EXTRA_PLATE, plate)
        .putExtra(WebViewActivity.EXTRA_TRAFFIC_AUTOFILL, true));
  }

  private void editTrafficIdentity() {
    SharedPreferences data = getSharedPreferences("traffic_identity", MODE_PRIVATE);
    LinearLayout fields = new LinearLayout(this);
    fields.setOrientation(LinearLayout.VERTICAL);
    fields.setPadding(dp(24), 0, dp(24), 0);
    EditText dni = new EditText(this);
    dni.setHint("Documento (7 a 11 dígitos)");
    dni.setInputType(2);
    dni.setText(data.getString("dni", ""));
    EditText digit = new EditText(this);
    digit.setHint("Código verificador");
    digit.setInputType(2);
    digit.setText(data.getString("digit", ""));
    fields.addView(dni);
    fields.addView(digit);
    androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Datos de Cinemómetro")
        .setMessage("Se guardan solo en este dispositivo y se rellenan únicamente en Cinemómetro de SUTRAN. El CAPTCHA se resuelve con reconocimiento local y la consulta se envía automáticamente.")
        .setView(fields)
        .setPositiveButton("Guardar", null)
        .setNeutralButton("Borrar datos", (d, w) -> data.edit().clear().apply())
        .setNegativeButton("Cancelar", null).create();
    dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
      String number = dni.getText().toString().trim();
      String verifier = digit.getText().toString().trim();
      if (!number.matches("[0-9]{6,11}")) { dni.setError("Ingresa de 6 a 11 dígitos"); return; }
      if (!verifier.matches("[A-Za-z0-9]{1,2}")) { digit.setError("Ingresa 1 o 2 caracteres"); return; }
      data.edit().putString("dni", number).putString("digit", verifier).apply();
      dialog.dismiss();
    }));
    dialog.show();
  }

  private void managePortalCredentials() {
    java.util.List<String> hosts = CredentialVault.hosts(this);
    StringBuilder list = new StringBuilder();
    if (hosts.isEmpty()) list.append("No hay accesos guardados.\n");
    else for (String h : hosts) list.append("• ").append(h).append("\n");
    LinearLayout fields = new LinearLayout(this);
    fields.setOrientation(LinearLayout.VERTICAL);
    fields.setPadding(dp(24), 0, dp(24), 0);
    EditText hostInput = new EditText(this);
    hostInput.setHint("Host del portal (ej. www.sunarp.gob.pe)");
    EditText userInput = new EditText(this);
    userInput.setHint("Usuario");
    EditText passInput = new EditText(this);
    passInput.setHint("Contraseña");
    passInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    fields.addView(hostInput);
    fields.addView(userInput);
    fields.addView(passInput);
    TextView info = new TextView(this);
    info.setText(list.toString() + "\nLas credenciales se cifran con la clave del dispositivo (AndroidKeyStore) y solo se usan para iniciar sesión automáticamente con tu consentimiento.");
    info.setTextSize(12);
    fields.addView(info);
    androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Accesos a portales")
        .setView(fields)
        .setPositiveButton("Guardar", null)
        .setNeutralButton("Borrar todo", (d, w) -> { CredentialVault.clearAll(this); android.widget.Toast.makeText(this, "Accesos borrados", android.widget.Toast.LENGTH_SHORT).show(); })
        .setNegativeButton("Cerrar", null).create();
    dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
      String h = hostInput.getText().toString().trim();
      String u = userInput.getText().toString().trim();
      String p = passInput.getText().toString();
      if (h.isEmpty() || u.isEmpty() || p.isEmpty()) { hostInput.setError("Completa todos los campos"); return; }
      if (CredentialVault.save(this, h, u, p)) {
        android.widget.Toast.makeText(this, "Acceso guardado para " + h, android.widget.Toast.LENGTH_SHORT).show();
        hostInput.setText(""); userInput.setText(""); passInput.setText("");
      } else {
        hostInput.setError("No se pudo guardar");
      }
    }));
    dialog.show();
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
    boolean fines = CATEGORY_MUNICIPAL.equals(selectedCategory);
    if (fines) sourceSearch.setVisibility(View.GONE);
    TextView help = new TextView(this);
    help.setText(
        CATEGORY_MUNICIPAL.equals(selectedCategory)
            ? "Verificaciones adicionales. Las papeletas por ciudad se consultan desde Inicio → Elegir fuentes."
            : CATEGORY_INSURANCE.equals(selectedCategory)
                ? "Consultas complementarias. El reporte SOAT de SBS se consulta desde Inicio."
                : "Trámites y verificaciones adicionales. La ficha vehicular y la revisión técnica se consultan desde Inicio.");
    help.setTextSize(13);
    help.setTextColor(Color.parseColor("#6B7280"));
    help.setPadding(dp(4), dp(8), dp(4), dp(16));
    sourceContainer.addView(help);
    LayoutInflater inflater = LayoutInflater.from(this);
    String normalizedQuery = sourceQuery.toLowerCase();
    int visibleCount = 0;
    String lastGroup = "";
    List<OfficialSource> ordered = new java.util.ArrayList<>();
    if (!fines) ordered.addAll(sources);
    ordered.addAll(ManualVerificationCatalog.SOURCES);
    ordered.sort(
        java.util.Comparator.comparing(this::sourceGroup).thenComparing(source -> source.title));
    for (OfficialSource source : ordered) {
      if (CATEGORY_INSURANCE.equals(selectedCategory) && sources.contains(source)) continue;
      if (CATEGORY_VEHICLE.equals(selectedCategory) && sources.contains(source)
          && !"SUNARP".equals(source.title)) continue;
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
      boolean registryServices = CATEGORY_VEHICLE.equals(selectedCategory) && "SUNARP".equals(source.title);
      ((TextView) row.findViewById(R.id.report_source_title))
          .setText(registryServices ? "Trámites SUNARP" : source.title);
      ((TextView) row.findViewById(R.id.report_source_description))
          .setText(registryServices ? "Documentos, alertas y seguimiento registral" : source.description);
      TextView status = row.findViewById(R.id.report_source_status);
      boolean opened = openedSources.contains(source.title);
      status.setText(opened ? "ABIERTA" : source.accessLabel.toUpperCase());
      styleStatus(status, opened);
      row.setOnClickListener(view -> {
        if (ManualVerificationCatalog.SOURCES.contains(source)) {
          try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(source.url)));
          } catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, "No hay navegador disponible", android.widget.Toast.LENGTH_SHORT).show();
          }
        } else {
          openSource(source);
        }
      });
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
    options.addAll(ManualVerificationCatalog.TRAFFIC);
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
    panel.setPadding(dp(14), dp(4), dp(14), dp(4));
    panel.setBackgroundResource(R.drawable.bg_round_card);
    parent.addView(panel);
    LinearLayout heading = new LinearLayout(this);
    heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
    panel.addView(heading);
    TextView title = new TextView(this);
    title.setText("Elegir fuentes  ▾");
    title.setGravity(android.view.Gravity.CENTER_VERTICAL);
    title.setTextSize(14);
    title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
    title.setTextColor(Color.parseColor("#002B5C"));
    heading.addView(title, new LinearLayout.LayoutParams(0, dp(36), 1));
    TextView count = new TextView(this);
    count.setTextSize(12);
    count.setTextColor(Color.parseColor("#6B7280"));
    heading.addView(count);
    List<Runnable> groupLabels = new ArrayList<>();
    Runnable refresh =
        () -> {
          int n = 0;
          for (OfficialSource s : options) if (selected.contains(s.url)) n++;
          count.setText(n == 1 ? "1 fuente" : n + " fuentes");
          groupLabels.forEach(Runnable::run);
          consult.setText(queryRunning ? "Cancelar consulta" : "Consultar");
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
    LinearLayout cities = new LinearLayout(this);
    cities.setOrientation(LinearLayout.VERTICAL);
    TextView profile = new TextView(this);
    profile.setText("Editar datos de SUTRAN ›");
    profile.setTextColor(ContextCompat.getColor(this, R.color.navy));
    profile.setMinHeight(dp(48));
    profile.setGravity(android.view.Gravity.CENTER_VERTICAL);
    profile.setVisibility(View.GONE);
    profile.setOnClickListener(v -> editTrafficIdentity());

    TextView creds = new TextView(this);
    creds.setText("Gestionar accesos a portales ›");
    creds.setTextColor(ContextCompat.getColor(this, R.color.navy));
    creds.setMinHeight(dp(48));
    creds.setGravity(android.view.Gravity.CENTER_VERTICAL);
    creds.setOnClickListener(v -> managePortalCredentials());
    cities.addView(profile);
    cities.setVisibility(View.GONE);
    panel.addView(cities);
    panel.addView(creds);
    grid.setVisibility(View.GONE);
    heading.setMinimumHeight(dp(48));
    java.util.function.Consumer<Boolean> showSources = expand -> {
      title.setText(expand ? "Elegir fuentes  ▴" : "Elegir fuentes  ▾");
      grid.setVisibility(expand ? View.VISIBLE : View.GONE);
      cities.setVisibility(expand ? View.VISIBLE : View.GONE);
    };
    heading.setOnClickListener(v -> showSources.accept(grid.getVisibility() != View.VISIBLE));
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
      int assisted = ManualVerificationCatalog.TRAFFIC.indexOf(source);
      chip.setText(assisted < 0 ? label :
          new String[] {"Cinemómetro", "Captura SAT", "Depósito SAT", "Impuesto"}[assisted] + " ↗");
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
              style.run();
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
          groupLabels.clear();
          if (recommendation.getParent() != null)
            ((android.view.ViewGroup) recommendation.getParent()).removeView(recommendation);
          addSourceGroup(grid, "Esenciales", null, ordered.stream()
              .filter(s -> priorityIndex(s) >= 0).collect(java.util.stream.Collectors.toList()), chips, 2, selected, groupLabels, null);
          addSourceGroup(cities, "Multas por ciudad", recommendation, ordered.stream()
              .filter(s -> priorityIndex(s) < 0 && !ManualVerificationCatalog.TRAFFIC.contains(s))
              .collect(java.util.stream.Collectors.toList()), chips, 3, selected, groupLabels, null);
          if (profile.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) profile.getParent()).removeView(profile);
          profile.setVisibility(View.VISIBLE);
          addSourceGroup(cities, "Verificación en el portal", null, ordered.stream()
              .filter(ManualVerificationCatalog.TRAFFIC::contains)
              .collect(java.util.stream.Collectors.toList()), chips, 2, selected, groupLabels, profile);
        };
    plateInput.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(Editable e) {
            reorder.run();
          }
        });
    reorder.run();
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
            grid.setVisibility(View.GONE);
            title.setText("Elegir fuentes  ▾");
            cities.setVisibility(View.GONE);
            startFinesQuery(chosen);
          }
        });
  }


  private void addSourceGroup(LinearLayout parent, String title, View hint,
      List<OfficialSource> group, java.util.Map<OfficialSource, View> chips, int columns,
      java.util.Set<String> selected, List<Runnable> labels, View footer) {
    if (group.isEmpty()) return;
    TextView heading = new TextView(this);
    heading.setTextColor(Color.rgb(16, 60, 53));
    heading.setTypeface(null, android.graphics.Typeface.BOLD);
    heading.setMinHeight(dp(48));
    heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
    parent.addView(heading);
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setVisibility(View.GONE);
    parent.addView(body);
    Runnable updateLabel = () -> {
      long count = group.stream().filter(s -> selected.contains(s.url)).count();
      heading.setText(title + " · " + count + " de " + group.size()
          + (body.getVisibility() == View.VISIBLE ? "  ▴" : "  ▾"));
    };
    labels.add(updateLabel);
    updateLabel.run();
    heading.setOnClickListener(v -> {
      body.setVisibility(body.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
      updateLabel.run();
    });
    if (hint != null) body.addView(hint);
    for (int i = 0; i < group.size(); i += columns) {
      LinearLayout row = new LinearLayout(this);
      body.addView(row);
      for (int j = 0; j < columns; j++) {
        View cell = i + j < group.size() ? chips.get(group.get(i + j)) : new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
        if (j > 0) params.setMarginStart(dp(4));
        row.addView(cell, params);
      }
    }
    if (footer != null) body.addView(footer);
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
    List<OfficialSource> assisted = new ArrayList<>();
    for (OfficialSource s : selected) {
      if (ManualVerificationCatalog.TRAFFIC.contains(s)) {
        assisted.add(s);
        continue;
      }
      names.add(s.title);
      urls.add(s.url);
    }
    if (inlineQuery != null) inlineQuery.dispose();
    inlineQuery = null;
    disposeAssistedForms();
    inlineQuerySlot.removeAllViews();
    addPreparedChecks(assisted, currentPlate);
    if (names.isEmpty()) {
      selectHomeTab();
      inlineQuerySlot.post(() -> inlineQuerySlot.requestRectangleOnScreen(
          new android.graphics.Rect(0, 0, inlineQuerySlot.getWidth(), dp(80)), false));
      return;
    }
    Intent intent =
        new Intent()
            .putExtra("plate", PlateUtils.normalize(currentPlate))
            .putExtra("names", names.toArray(new String[0]))
            .putExtra("urls", urls.toArray(new String[0]));
    queryRunning = true;
    ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(homeInput.getWindowToken(), 0);
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
    inlineQuerySlot.addView(inlineQuery.view(), 0, new LinearLayout.LayoutParams(-1, -2));
    selectHomeTab();
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
    disposeAssistedForms();
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
    disposeAssistedForms();
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
            selectHomeTab();
          });
      container.addView(chip);
    }
  }

  private void selectHomeTab() {
    com.google.android.material.tabs.TabLayout navigation = findViewById(R.id.top_navigation);
    navigation.selectTab(navigation.getTabAt(0));
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
