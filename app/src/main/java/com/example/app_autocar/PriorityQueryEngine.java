package com.example.app_autocar;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PriorityQueryEngine extends android.view.ContextThemeWrapper {
  private final AppCompatActivity host;
  private final android.content.Intent intent;
  private View rootView;
  private boolean disposed;
  private final boolean[] errors = new boolean[4];

  public PriorityQueryEngine(AppCompatActivity host, android.content.Intent intent) {
    super(host, host.getTheme());
    this.host = host;
    this.intent = intent;
  }

  public View view() {
    return rootView;
  }

  private <T extends View> T findViewById(int id) {
    return rootView.findViewById(id);
  }

  private android.content.Intent getIntent() {
    return intent;
  }

  private boolean isFinishing() {
    return disposed || host.isFinishing() || host.isDestroyed();
  }

  private void runOnUiThread(Runnable task) {
    host.runOnUiThread(
        () -> {
          if (!disposed) task.run();
        });
  }

  private void finish() {
    host.finish();
  }

  public WebView portal(int index) {
    return webViews.get(index);
  }

  public String summary(int index) {
    return reportSummaries[index];
  }

  public String detail(int index) {
    return reportDetails[index];
  }

  public boolean complete(int index) {
    return resultComplete[index];
  }

  public boolean error(int index) {
    return errors[index];
  }

  public Bitmap captcha(int index) {
    return captchaReady[index] && !validationPending[index] && !resultComplete[index]
        ? captchaBitmaps[index]
        : null;
  }

  public Bitmap officialImage() {
    return sunarpOfficialImage;
  }

  public void submit(int index, String answer) {
    if (!enabled[index] || resultComplete[index] || validationPending[index]) return;
    String code = answer.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    if (code.isEmpty()) return;
    validationPending[index] = true;
    captchaReady[index] = false;
    submitCaptchaToPortal(
        index,
        index == 2 ? "#TxtCodImagen" : "#texCaptcha",
        index == 2 ? "#BtnBuscar" : "#btnBuscar",
        code);
    setResult(index, "Validando código…", false);
    handler.postDelayed(
        () -> {
          if (validationPending[index] && !resultComplete[index]) {
            validationPending[index] = false;
            captureCaptcha(index);
            setResult(index, "Escribe el código pendiente", false);
          }
        },
        25000);
  }

  public static final String EXTRA_PLATE = "plate";
  private final boolean[] enabled = {true, true, true, true};
  private int selectedCount = 4;

  private static final Portal[] PORTALS = {
    new Portal(
        "SUNARP",
        "https://consultavehicular.sunarp.gob.pe/consulta-vehicular/",
        R.id.tab_sunarp,
        R.id.result_sunarp,
        null),
    new Portal(
        "SOAT",
        "https://servicios.sbs.gob.pe/reportesoat/",
        R.id.tab_soat,
        R.id.result_soat,
        "#ctl00_MainBodyContent_txtPlaca"),
    new Portal(
        "SUTRAN",
        "https://webexterno.sutran.gob.pe/WebExterno/Pages/frmRecordInfracciones.aspx",
        R.id.tab_sutran,
        R.id.result_sutran,
        "#txtPlaca"),
    new Portal(
        "MTC",
        "https://portal.mtc.gob.pe/reportedgtt/form/frmconsultaplacaitv.aspx",
        R.id.tab_mtc,
        R.id.result_mtc,
        "#texFiltro")
  };

  private final List<WebView> webViews = new ArrayList<>();
  private final boolean[] loaded = new boolean[PORTALS.length];
  private final boolean[] imageProcessed = new boolean[PORTALS.length];
  private final boolean[] ocrInFlight = new boolean[PORTALS.length];
  private final boolean[] apiFallback = new boolean[PORTALS.length];
  private final boolean[] captchaReady = new boolean[PORTALS.length];
  private final boolean[] validationPending = new boolean[PORTALS.length];
  private final boolean[] resultComplete = new boolean[PORTALS.length];
  private final boolean[] sourceAlert = new boolean[PORTALS.length];
  private final Bitmap[] captchaBitmaps = new Bitmap[PORTALS.length];
  private final String[] reportDetails = new String[PORTALS.length];
  private final String[] reportSummaries = new String[PORTALS.length];
  private final List<String> insuranceHistory = new ArrayList<>();
  private Bitmap sunarpOfficialImage;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private FrameLayout container;
  private ProgressBar progress;
  private TextView progressText;
  private View captchaPanel;
  private TextView captchaHint;
  private TextView purchaseVerdict;
  private EditText soatCaptchaInput;
  private EditText sutranCaptchaInput;
  private EditText mtcCaptchaInput;
  private String plate = "";
  private int selectedIndex;
  private boolean captchaSubmitted;
  private boolean sutranQuerySent;

  @SuppressLint("SetJavaScriptEnabled")
  public void start() {
    rootView =
        android.view.LayoutInflater.from(host).inflate(R.layout.activity_multi_check, null, false);
    plate = PlateUtils.normalize(getIntent().getStringExtra(EXTRA_PLATE));
    String[] requested = getIntent().getStringArrayExtra("selected_portals");
    if (requested != null) {
      java.util.Set<String> names = new java.util.HashSet<>(java.util.Arrays.asList(requested));
      selectedCount = 0;
      for (int i = 0; i < PORTALS.length; i++) {
        enabled[i] = names.contains(PORTALS[i].name);
        if (enabled[i]) selectedCount++;
      }
      if (selectedCount == 0) {
        finish();
        return;
      }
    }
    ((TextView) findViewById(R.id.multi_plate)).setText(PlateUtils.display(plate));
    findViewById(R.id.multi_back).setOnClickListener(view -> finish());
    container = findViewById(R.id.multi_web_container);
    progress = findViewById(R.id.multi_progress);
    progressText = findViewById(R.id.multi_progress_text);
    captchaPanel = findViewById(R.id.captcha_panel);
    captchaHint = findViewById(R.id.captcha_hint);
    purchaseVerdict = findViewById(R.id.purchase_verdict);
    soatCaptchaInput = findViewById(R.id.captcha_soat_input);
    sutranCaptchaInput = findViewById(R.id.captcha_sutran_input);
    mtcCaptchaInput = findViewById(R.id.captcha_mtc_input);
    ((Button) findViewById(R.id.captcha_submit))
        .setOnClickListener(view -> submitUnifiedCaptchas());
    findViewById(R.id.open_soat_source).setOnClickListener(view -> selectPortal(1));

    for (int index = 0; index < PORTALS.length; index++) createPortal(index);
    updateProgress();
    updatePurchaseVerdict();
    for (int i = 0; i < enabled.length; i++)
      if (enabled[i]) {
        selectPortal(i);
        break;
      }
    findViewById(R.id.open_soat_source).setVisibility(enabled[1] ? View.VISIBLE : View.GONE);
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void createPortal(int index) {
    Portal portal = PORTALS[index];
    TextView tab = findViewById(portal.tabId);
    tab.setOnClickListener(view -> selectPortal(index));

    WebView webView = new WebView(this);
    webView.setLayoutParams(
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    webView.setVisibility(index == 0 ? View.VISIBLE : View.INVISIBLE);
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    android.webkit.CookieManager.getInstance().setAcceptCookie(true);
    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    webView.addJavascriptInterface(new ResultBridge(index), "AutocarResults");
    webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onReceivedSslError(
              WebView view,
              android.webkit.SslErrorHandler sslHandler,
              android.net.http.SslError error) {
            sslHandler.cancel();
            android.util.Log.e(
                "AutocarPortal", portal.name + " TLS error " + error.getPrimaryError());
            loaded[index] = true;
            updateProgress();
            setResult(
                index,
                "No se pudo validar la conexión segura del portal ("
                    + error.getPrimaryError()
                    + ")",
                true);
            view.loadData(
                "<html><body><h3>Conexión segura no disponible</h3><p>El portal "
                    + portal.name
                    + " no pudo cargar por un problema de certificado. No se recibió su"
                    + " captcha.</p></body></html>",
                "text/html",
                "UTF-8");
          }

          @Override
          public void onPageStarted(WebView view, String url, Bitmap favicon) {
            tab.setText(portal.name + "  ···");
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            if (isPlacApiPortal(index)) {
              if (!loaded[index]) {
                loaded[index] = true;
                updateProgress();
              }
              styleTab(tab, index == selectedIndex, false);
              return;
            }
            injectPlate(view, index);
            if (index == 1) prepareSbsQuery(view);
            if (!loaded[index]) {
              loaded[index] = true;
              updateProgress();
            }
            tab.setText(portal.name + "  ···");
            styleTab(tab, index == selectedIndex, false);
            scheduleResultReads(index);
            if (index >= 2 && index <= 3) {
              handler.postDelayed(() -> captureCaptcha(index), 1200);
              handler.postDelayed(() -> captureCaptcha(index), 3200);
              if (index == 1) {
                String watch =
                    "(function(){if(window.__autocarCaptchaWatch)return;window.__autocarCaptchaWatch=true;var"
                        + " last='';function send(){var"
                        + " img=document.querySelector('img.captcha-img, img[alt=\"captcha\"]');"
                        + "if(!img||!img.complete||!img.naturalWidth||img.src===last)return;"
                        + "if(img.src.indexOf('data:image/png')===0||img.src.indexOf('data:image/jpeg')===0){last=img.src;AutocarResults.reportCaptcha(img.src);return;}try{var"
                        + " c=document.createElement('canvas');c.width=img.naturalWidth;c.height=img.naturalHeight;c.getContext('2d').drawImage(img,0,0);var"
                        + " data=c.toDataURL('image/png');last=img.src;AutocarResults.reportCaptcha(data);}catch(e){}}new"
                        + " MutationObserver(send).observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['src']});"
                        + "document.addEventListener('load',send,true);send();})();";
                view.evaluateJavascript(watch, null);
              }
            }
          }

          @Override
          public void onReceivedError(
              WebView view, WebResourceRequest request, WebResourceError error) {
            if (!request.isForMainFrame()) return;
            loaded[index] = true;
            updateProgress();
            tab.setText(portal.name + "  !");
            styleTab(tab, index == selectedIndex, true);
            setResult(index, "No se pudo conectar", true);
          }
        });
    webViews.add(webView);
    container.addView(webView);
    if (!enabled[index]) {
      loaded[index] = true;
      resultComplete[index] = true;
      tab.setVisibility(View.GONE);
      findViewById(portal.resultId).setVisibility(View.GONE);
      webView.setVisibility(View.INVISIBLE);
      return;
    }
    if (isPlacApiPortal(index)) {
      setResult(index, "Consultando por API…", false);
      webView.loadData(
          "<html><body style='font-family:sans-serif;padding:32px;color:#334155'><h2>Consulta"
              + " automática</h2><p>Este resultado se obtiene mediante API. No necesitas completar"
              + " un captcha.</p></body></html>",
          "text/html",
          "UTF-8");
      queryPlacApi(index);
    } else {
      webView.loadUrl(portal.url);
    }
  }

  private void selectPortal(int index) {
    if (!enabled[index]) return;
    selectedIndex = index;
    for (int i = 0; i < webViews.size(); i++) {
      webViews.get(i).setVisibility(i == index ? View.VISIBLE : View.INVISIBLE);
      TextView tab = findViewById(PORTALS[i].tabId);
      styleTab(tab, i == index, tab.getText().toString().endsWith("!"));
    }
    if (!isPlacApiPortal(index)) handler.postDelayed(() -> readResult(index), 700);
  }

  private void prepareSbsQuery(WebView view) {
    String script =
        "(function(){if(window.__autocarSbsSubmit)return;window.__autocarSbsSubmit=true;var"
            + " tries=0,t=setInterval(function(){tries++;var"
            + " p=document.querySelector('#ctl00_MainBodyContent_txtPlaca'),"
            + "token=document.querySelector('#ctl00_MainBodyContent_hdnReCaptchaV3'),b=document.querySelector('#ctl00_MainBodyContent_btnIngresarPla');"
            + "if(p&&p.value&&token&&token.value&&b&&!b.disabled){clearInterval(t);b.click();return;}if(tries>=60){clearInterval(t);AutocarResults.reportPortalMessage('SBS:"
            + " no terminó la validación de seguridad. Abre Ver SOAT para revisar el"
            + " portal.');}},500);})();";
    view.evaluateJavascript(script, null);
  }

  private boolean isPlacApiPortal(int index) {
    return !BuildConfig.PLACAPI_KEY.trim().isEmpty()
        && !apiFallback[index]
        && (index == 2 || index == 3);
  }

  private void updateProgress() {
    int count = 0;
    for (int i = 0; i < loaded.length; i++) if (enabled[i] && loaded[i]) count++;
    progressText.setText(
        count == selectedCount
            ? selectedCount + " fuentes preparadas"
            : "Preparando " + count + " de " + selectedCount + "…");
    progress.setVisibility(count == selectedCount ? View.GONE : View.VISIBLE);
  }

  private void injectPlate(WebView webView, int index) {
    if (plate.isEmpty()) return;
    String safePlate = plate.replace("\\", "\\\\").replace("'", "\\'");
    String selector = PORTALS[index].inputSelector == null ? "" : PORTALS[index].inputSelector;
    String script =
        "(function(){if(window.__autocarPrepared)return;window.__autocarPrepared=true;if(!window.__autocarNetworkHook){window.__autocarNetworkHook=true;var"
            + " ox=XMLHttpRequest.prototype.open,os=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.open=function(m,u){this.__autocarUrl=u||'';return"
            + " ox.apply(this,arguments);};XMLHttpRequest.prototype.send=function(){this.addEventListener('load',function(){var"
            + " u=this.__autocarUrl||'';if(u.indexOf('getDatosVehiculo')>=0||u.indexOf('/certificados/placa/')>=0){try{AutocarResults.reportApi(u+'\\n"
            + "'+this.responseText);}catch(e){}}});return os.apply(this,arguments);};var"
            + " of=window.fetch;if(of)window.fetch=function(){var a=arguments;return"
            + " of.apply(this,a).then(function(r){var"
            + " u=(a[0]&&a[0].url)||a[0]||'';if(String(u).indexOf('getDatosVehiculo')>=0||String(u).indexOf('/certificados/placa/')>=0)r.clone().text().then(function(t){AutocarResults.reportApi(String(u)+'\\n"
            + "'+t);});return r;});};}var fillAttempts=0;var"
            + " fillTimer=setInterval(function(){fillAttempts++;var"
            + " inputs=document.querySelectorAll('input'),found=false;var preferred='"
            + selector
            + "'?document.querySelector('"
            + selector
            + "'):null;for(var i=0;i<inputs.length;i++){var e=preferred||inputs[i],k=((e.id||'')+'"
            + " '+(e.name||'')+' '+(e.placeholder||'')+'"
            + " '+(e.getAttribute('aria-label')||'')).toLowerCase();if(preferred||k.indexOf('placa')>=0||k.indexOf('patente')>=0){var"
            + " d=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');"
            + "if(d&&d.set)d.set.call(e,'"
            + safePlate
            + "');else e.value='"
            + safePlate
            + "';e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new"
            + " Event('change',{bubbles:true}));e.dispatchEvent(new"
            + " Event('blur',{bubbles:true}));found=true;break;}}"
            + "if(!found&&fillAttempts<40)return;clearInterval(fillTimer);if(!found)return;if("
            + (index >= 1 && index <= 3)
            + ")return;setTimeout(function(){var"
            + " attempts=0,timer=setInterval(function(){attempts++;var"
            + " buttons=document.querySelectorAll('button,input[type=submit]');for(var"
            + " j=0;j<buttons.length;j++){var b=buttons[j],label=((b.innerText||'')+'"
            + " '+(b.value||'')+' '+(b.getAttribute('aria-label')||'')).toLowerCase();"
            + "if((label.indexOf('consult')>=0||label.indexOf('buscar')>=0||label.indexOf('búsqueda')>=0||label.indexOf('busqueda')>=0||label.indexOf('realizar')>=0||label.indexOf('verificar')>=0)&&!b.disabled&&b.offsetParent!==null){clearInterval(timer);b.click();return;}}"
            + "if(attempts>=180)clearInterval(timer);},1000);},5000);},500);})();";
    webView.evaluateJavascript(script, null);
  }

  private void captureCaptcha(int index) {
    if (isFinishing()
        || index < 2
        || index > 3
        || index >= webViews.size()
        || isPlacApiPortal(index)
        || resultComplete[index]) return;
    String findImage;
    if (index == 1) {
      findImage = "var img=document.querySelector('img.captcha-img, img[alt=\"captcha\"]');";
    } else if (index == 2) {
      findImage =
          "var f=document.querySelector('#iimage'),d=f&&f.contentDocument,img=d&&d.querySelector('img');";
    } else {
      findImage = "var img=document.querySelector('#imgCaptcha');";
    }
    String script =
        "(function(){try{"
            + findImage
            + "if(!img||!img.complete||!img.naturalWidth)return;if(img.src.indexOf('data:image/png')===0||img.src.indexOf('data:image/jpeg')===0){AutocarResults.reportCaptcha(img.src);return;}var"
            + " c=document.createElement('canvas');c.width=img.naturalWidth;c.height=img.naturalHeight;var"
            + " x=c.getContext('2d');x.drawImage(img,0,0);"
            + "AutocarResults.reportCaptcha(c.toDataURL('image/png'));}catch(e){}})();";
    webViews.get(index).evaluateJavascript(script, null);
  }

  private void showCaptchaPanelIfReady() {
    if (resultComplete[2] && resultComplete[3]) {
      captchaPanel.setVisibility(View.GONE);
      return;
    }
    boolean soatAvailable = false;
    boolean sutranAvailable = resultComplete[2] || (!validationPending[2] && captchaReady[2]);
    boolean mtcAvailable = resultComplete[3] || (!validationPending[3] && captchaReady[3]);
    if ((!resultComplete[1] && soatAvailable)
        || (!resultComplete[2] && sutranAvailable)
        || (!resultComplete[3] && mtcAvailable)) {
      captchaHint.setText(
          resultComplete[2] || resultComplete[3]
              ? "Escribe únicamente los códigos pendientes."
              : "Escribe los códigos de SUTRAN y MTC y continúa una sola vez.");
      soatCaptchaInput.setEnabled(!resultComplete[1] && soatAvailable);
      sutranCaptchaInput.setEnabled(!resultComplete[2] && sutranAvailable);
      mtcCaptchaInput.setEnabled(!resultComplete[3] && mtcAvailable);
      ((View) soatCaptchaInput.getParent())
          .setVisibility(!resultComplete[1] && soatAvailable ? View.VISIBLE : View.GONE);
      ((View) sutranCaptchaInput.getParent())
          .setVisibility(!resultComplete[2] && sutranAvailable ? View.VISIBLE : View.GONE);
      ((View) mtcCaptchaInput.getParent())
          .setVisibility(!resultComplete[3] && mtcAvailable ? View.VISIBLE : View.GONE);
      captchaPanel.setVisibility(View.VISIBLE);
    }
  }

  private void submitUnifiedCaptchas() {
    String soatCode =
        soatCaptchaInput
            .getText()
            .toString()
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-ZÁÉÍÓÚÑ0-9]", "");
    String sutranCode =
        sutranCaptchaInput
            .getText()
            .toString()
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-ZÁÉÍÓÚÑ0-9]", "");
    String mtcCode =
        mtcCaptchaInput
            .getText()
            .toString()
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]", "");
    if ((soatCaptchaInput.isEnabled() && soatCode.length() < 4)
        || (sutranCaptchaInput.isEnabled() && sutranCode.length() != 4)
        || (mtcCaptchaInput.isEnabled() && mtcCode.length() != 6)) {
      captchaHint.setText("Revisa los códigos pendientes antes de continuar.");
      return;
    }
    captchaSubmitted = true;
    captchaPanel.setVisibility(View.GONE);
    InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    keyboard.hideSoftInputFromWindow(mtcCaptchaInput.getWindowToken(), 0);
    if (!resultComplete[1] && soatCaptchaInput.isEnabled()) {
      validationPending[1] = true;
      submitCaptchaToPortal(1, "#captcha", "button[type=submit]", soatCode);
      setResult(1, "Validando código y seguridad…", false);
      scheduleValidationTimeout(1);
    }
    if (!resultComplete[2] && sutranCaptchaInput.isEnabled()) {
      validationPending[2] = true;
      submitCaptchaToPortal(2, "#TxtCodImagen", "#BtnBuscar", sutranCode);
      setResult(2, "Validando código…", false);
      scheduleValidationTimeout(2);
    }
    if (!resultComplete[3] && mtcCaptchaInput.isEnabled()) {
      validationPending[3] = true;
      submitCaptchaToPortal(3, "#texCaptcha", "#btnBuscar", mtcCode);
      setResult(3, "Validando código…", false);
      scheduleValidationTimeout(3);
    }
  }

  private void scheduleValidationTimeout(int index) {
    handler.postDelayed(
        () -> {
          if (!validationPending[index] || resultComplete[index]) return;
          validationPending[index] = false;
          captchaSubmitted = false;
          captchaReady[index] = false;
          setResult(index, "El portal tardó demasiado; verifica el código otra vez", true);
          captureCaptcha(index);
          handler.postDelayed(() -> captureCaptcha(index), 1500);
        },
        index == 1 ? 55000 : 20000);
  }

  private void handleInvalidCaptcha(int index) {
    if (index == 2) sutranQuerySent = false;
    validationPending[index] = false;
    captchaSubmitted = false;
    captchaReady[index] = false;
    EditText input =
        index == 1 ? soatCaptchaInput : index == 2 ? sutranCaptchaInput : mtcCaptchaInput;
    input.setText("");
    setResult(index, "Escribe el nuevo código mostrado", false);
    handler.postDelayed(() -> captureCaptcha(index), 700);
    handler.postDelayed(() -> captureCaptcha(index), 1800);
  }

  private void submitCaptchaToPortal(
      int index, String inputSelector, String buttonSelector, String code) {
    if (index >= webViews.size() || resultComplete[index]) return;
    if (index == 2) sutranQuerySent = true;
    String submitAction =
        index == 1
            ? "var tries=0,t=setInterval(function(){tries++;var"
                  + " token=document.querySelector('[name=\"cf-turnstile-response\"]');if(token&&token.value&&token.value.length>10&&!b.disabled){clearInterval(t);b.click();setTimeout(function(){var"
                  + " m=document.querySelector('.form-error,[class*=\"error\"]');if(m&&m.innerText)AutocarResults.reportPortalMessage(m.innerText);},1000);return;}if(tries>=100){clearInterval(t);AutocarResults.reportPortalMessage('La"
                  + " validación de seguridad aún no termina');}},500);"
            : "b.click();setTimeout(function(){var"
                  + " m=document.querySelector('#LblMensaje');if(m&&m.innerText)AutocarResults.reportPortalMessage(m.innerText);},500);";
    String script =
        "(function(){if(!window.__autocarAlert){window.__autocarAlert=true;window.alert=function(m){try{AutocarResults.reportPortalMessage(String(m));}catch(e){}};}var"
            + " e=document.querySelector('"
            + inputSelector
            + "'),b=document.querySelector('"
            + buttonSelector
            + "');if(!e||!b)return;var"
            + " d=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');"
            + "if(d&&d.set)d.set.call(e,'"
            + code
            + "');else e.value='"
            + code
            + "';e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new"
            + " Event('change',{bubbles:true}));"
            + submitAction
            + "})();";
    webViews.get(index).evaluateJavascript(script, null);
    scheduleResultReads(index);
  }

  private void queryPlacApi(int index) {
    String endpoint = index == 2 ? "/api/multas-pe" : "/api/revision-tecnica-pe";
    new Thread(
            () -> {
              HttpURLConnection connection = null;
              try {
                connection =
                    (HttpURLConnection) new URL("https://placapi.com" + endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(45000);
                connection.setDoOutput(true);
                connection.setRequestProperty("x-api-key", BuildConfig.PLACAPI_KEY);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] request =
                    new JSONObject()
                        .put("placa", plate)
                        .toString()
                        .getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                  output.write(request);
                }

                int status = connection.getResponseCode();
                InputStream stream =
                    status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String response = readStream(stream);
                if (status < 200 || status >= 300) {
                  throw new IllegalStateException(
                      "HTTP " + status + (response.isEmpty() ? "" : ": " + response));
                }
                JSONObject root = new JSONObject(response);
                String summary = index == 2 ? summarizeFines(root) : summarizeInspection(root);
                boolean alert = index == 2 ? hasPendingFines(root) : !hasCurrentInspection(root);
                runOnUiThread(
                    () -> {
                      resultComplete[index] = true;
                      validationPending[index] = false;
                      sourceAlert[index] = alert;
                      setResult(index, summary, false);
                      updatePurchaseVerdict();
                    });
              } catch (Exception error) {
                runOnUiThread(() -> fallbackToOfficialPortal(index));
              } finally {
                if (connection != null) connection.disconnect();
              }
            },
            "placapi-" + PORTALS[index].name.toLowerCase(Locale.ROOT))
        .start();
  }

  private String readStream(InputStream stream) throws Exception {
    if (stream == null) return "";
    StringBuilder result = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) result.append(line);
    }
    return result.toString();
  }

  private String summarizeFines(JSONObject root) {
    JSONObject data = root.optJSONObject("data");
    if (data == null) return "La API no devolvió datos";
    int pending = data.optInt("pendientes", data.optInt("total", 0));
    if (pending == 0) return "Sin papeletas pendientes · SUTRAN, SAT Lima y Callao";
    double amount = data.optDouble("montoPendiente", 0);
    String summary = pending + (pending == 1 ? " papeleta pendiente" : " papeletas pendientes");
    if (amount > 0) summary += String.format(Locale.US, " · S/ %.2f disponible", amount);
    return summary;
  }

  private boolean hasPendingFines(JSONObject root) {
    JSONObject data = root.optJSONObject("data");
    return data == null || data.optInt("pendientes", data.optInt("total", 0)) > 0;
  }

  private boolean hasCurrentInspection(JSONObject root) {
    JSONObject data = root.optJSONObject("data");
    return data != null && data.optBoolean("vigente", false);
  }

  private String summarizeInspection(JSONObject root) {
    JSONObject data = root.optJSONObject("data");
    if (data == null) return "La API no devolvió datos";
    boolean current = data.optBoolean("vigente", false);
    String until = data.optString("vigenteHasta", "").trim();
    if (current) return until.isEmpty() ? "Revisión técnica vigente" : "Vigente hasta " + until;
    JSONArray certificates = data.optJSONArray("certificados");
    if (certificates == null || certificates.length() == 0)
      return "Sin certificados de revisión técnica";
    JSONObject latest = certificates.optJSONObject(0);
    String state = latest == null ? "" : latest.optString("estado", "").trim();
    String end = latest == null ? "" : latest.optString("vigenciaFin", "").trim();
    if (!state.isEmpty() && !end.isEmpty()) return state + " desde " + end;
    return state.isEmpty() ? "Sin revisión técnica vigente" : state;
  }

  private void fallbackToOfficialPortal(int index) {
    if (isFinishing() || index >= webViews.size()) return;
    apiFallback[index] = true;
    loaded[index] = false;
    setResult(index, "API no disponible; usando el portal oficial", true);
    webViews.get(index).loadUrl(PORTALS[index].url);
  }

  private void scheduleResultReads(int index) {
    long[] delays = {1500, 4500, 8500, 14000, 22000, 32000, 48000, 70000, 100000, 150000};
    for (long delay : delays) handler.postDelayed(() -> readResult(index), delay);
  }

  private void readResult(int index) {
    if (isFinishing() || index >= webViews.size()) return;
    if (index == 2) {
      WebView source = webViews.get(index);
      source.evaluateJavascript(
          "(function(){var"
              + " messages=Array.from(document.querySelectorAll('#LblMensaje,#LblMensaje2,#LblMensajeNet')).filter(function(n){return"
              + " n.getClientRects().length;}).map(function(n){return"
              + " n.innerText||n.textContent||''}).join('\\n"
              + "');var table=document.querySelector('#gvDeudas');return"
              + " JSON.stringify({message:messages,table:table&&table.getClientRects().length?table.innerText:'',rows:table?Array.from(table.querySelectorAll('tr')).filter(function(r){return"
              + " r.getClientRects().length&&r.querySelectorAll('td').length>1}).length:0});})();",
          value -> {
            try {
              Object decoded = new JSONTokener(value).nextValue();
              JSONObject response = new JSONObject((String) decoded);
              String message = response.optString("message");
              if (interpretSutranMessage(message)) return;
              if (sutranQuerySent && response.optInt("rows") > 0) {
                reportDetails[2] = response.optString("table");
                resultComplete[2] = true;
                validationPending[2] = false;
                captchaReady[2] = false;
                sourceAlert[2] = true;
                setResult(2, "SUTRAN registra infracciones · revisa su estado", false);
                updatePurchaseVerdict();
                showCaptchaPanelIfReady();
                return;
              }
              if (!validationPending[2]) captureCaptcha(2);
            } catch (Exception ignored) {
              setResult(2, "Esperando respuesta verificable de SUTRAN", false);
            }
          });
      return;
    }
    if (reportSummaries[index] != null && reportSummaries[index].startsWith("No se pudo")) return;
    if (index >= 1 && index <= 3 && !captchaReady[index] && !validationPending[index])
      captureCaptcha(index);
    WebView webView = webViews.get(index);
    String reader =
        "(function(){var out=[];function"
            + " read(root){if(!root)return;if(root.innerText)out.push(root.innerText);var"
            + " all=root.querySelectorAll?root.querySelectorAll('*'):[];for(var"
            + " i=0;i<all.length;i++){if(all[i].shadowRoot)read(all[i].shadowRoot);}var"
            + " frames=root.querySelectorAll?root.querySelectorAll('iframe'):[];for(var"
            + " j=0;j<frames.length;j++){try{read(frames[j].contentDocument.body);}catch(e){}}}read(document.body);var"
            + " c=document.querySelector('[name=\"cf-turnstile-response\"]');if(c&&c.value)out.push('AUTOCAR_CAPTCHA_OK');var"
            + " imgs=document.querySelectorAll('img[src^=\"data:image\"]'),best='';for(var"
            + " z=0;z<imgs.length;z++){if(imgs[z].src.length>best.length)best=imgs[z].src;}if(best.length>50000)AutocarResults.reportImage(best);return"
            + " out.join('\\n"
            + "').slice(0,50000);})()";
    webView.evaluateJavascript(
        reader,
        value -> {
          try {
            Object decoded = new JSONTokener(value).nextValue();
            interpretResult(index, decoded instanceof String ? (String) decoded : "");
          } catch (Exception ignored) {
            setResult(index, "Esperando respuesta del portal", false);
          }
        });
    String pdfReader =
        "(async function(){try{var wins=[window],fs=document.querySelectorAll('iframe');for(var"
            + " i=0;i<fs.length;i++){try{wins.push(fs[i].contentWindow);}catch(e){}}var"
            + " out=[];for(var w=0;w<wins.length;w++){var"
            + " app=wins[w].PDFViewerApplication;if(!app||!app.pdfDocument)continue;for(var"
            + " p=1;p<=Math.min(app.pdfDocument.numPages,3);p++){var page=await"
            + " app.pdfDocument.getPage(p),tc=await"
            + " page.getTextContent();out.push(tc.items.map(function(x){return x.str||'';}).join('"
            + " '));}}if(out.length)AutocarResults.report(out.join('\\n"
            + "').slice(0,50000));}catch(e){}})();";
    webView.evaluateJavascript(pdfReader, null);
    if (index == 3) {
      String mtcReader =
          "(function(){try{var"
              + " p=document.querySelector('#Spv1_3');if(!p||!p.innerText.trim())return;var"
              + " o={type:'mtc',plate:p.innerText.trim(),start:(document.querySelector('#Spv1_5')||{}).innerText||'',"
              + "end:(document.querySelector('#Spv1_6')||{}).innerText||'',result:(document.querySelector('#Spv1_7')||{}).innerText||'',"
              + "state:(document.querySelector('#Spv1_8')||{}).innerText||''};AutocarResults.reportStructured(JSON.stringify(o));}catch(e){}})();";
      webView.evaluateJavascript(mtcReader, null);
    }
    if (index == 0 && webView.getVisibility() == View.VISIBLE)
      recognizeRenderedPage(index, webView);
  }

  private void recognizeRenderedPage(int index, WebView webView) {
    if (ocrInFlight[index] || webView.getWidth() <= 0 || webView.getHeight() <= 0) return;
    ocrInFlight[index] = true;
    Bitmap bitmap =
        Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
    webView.draw(new Canvas(bitmap));
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener(
            text -> {
              String recognized = text.getText();
              String lower = recognized.toLowerCase(Locale.ROOT);
              if (lower.contains("marca")
                  && lower.contains("modelo")
                  && (lower.contains("placa")
                      || lower.contains("motor")
                      || lower.contains("serie"))) {
                interpretResult(index, recognized);
              }
            })
        .addOnCompleteListener(
            task -> {
              ocrInFlight[index] = false;
              bitmap.recycle();
            });
  }

  private void interpretResult(int index, String body) {
    if (resultComplete[index]) return;
    if (index == 2) {
      interpretSutranMessage(body);
      return;
    }
    Portal portal = PORTALS[index];
    String lower = body.toLowerCase(Locale.ROOT);
    String negative =
        firstPhrase(
            lower,
            "no se encontraron resultados",
            "no se encontró información",
            "no se encontro información",
            "no se encontro informacion",
            "no registra infracciones",
            "no registra",
            "sin resultados",
            "no existe información");
    if (negative != null) {
      resultComplete[index] = true;
      validationPending[index] = false;
      sourceAlert[index] = index == 0 || index == 1 || index == 3;
      String message =
          index == 1
              ? "Sin SOAT registrado"
              : index == 3 ? "Sin revisión técnica registrada" : "Sin registros encontrados";
      setResult(index, message, false);
      updatePurchaseVerdict();
      showCaptchaPanelIfReady();
      return;
    }

    boolean hasAnswer;
    if ("SUNARP".equals(portal.name)) {
      String summary = extractSunarpSummary(body);
      hasAnswer =
          lower.contains(plate.toLowerCase(Locale.ROOT))
              && !summary.isEmpty()
              && !summary.toLowerCase(Locale.ROOT).contains("sunarp");
    } else if ("SOAT".equals(portal.name)) {
      hasAnswer =
          lower.contains(plate.toLowerCase(Locale.ROOT))
              && (lower.contains("listado de pólizas")
                  || lower.contains("resultado de consulta")
                  || lower.contains("inicio de vigencia"));
    } else if ("SUTRAN".equals(portal.name)) {
      hasAnswer =
          lower.contains("infracci")
              && (lower.contains("acta") || lower.contains("resultado"))
              && lower.contains(plate.toLowerCase(Locale.ROOT));
    } else {
      hasAnswer =
          lower.contains("resultado inspecci")
              && lower.contains("vigente hasta")
              && lower.contains(plate.toLowerCase(Locale.ROOT));
    }

    if (hasAnswer) {
      reportDetails[index] = index == 0 ? extractSunarpSummary(body) : body.trim();
      resultComplete[index] = true;
      validationPending[index] = false;
      sourceAlert[index] = sourceAlert[index] || hasRiskWords(index, lower);
      String detail =
          index == 1
              ? summarizeSbsReport(body)
              : "SUNARP".equals(portal.name)
                  ? extractSunarpSummary(body)
                  : extractUsefulLines(body);
      if (index == 2) reportDetails[index] = null;
      String fallback =
          "SUNARP".equals(portal.name)
              ? "Vehículo encontrado en el registro oficial"
              : "Respuesta recibida";
      setResult(index, detail.isEmpty() ? fallback : detail, false);
      updatePurchaseVerdict();
      showCaptchaPanelIfReady();
      return;
    }

    if (resultComplete[index]) return;
    boolean captcha =
        (lower.contains("captcha")
                || lower.contains("código de seguridad")
                || lower.contains("verificación de seguridad")
                || lower.contains("ingrese el código"))
            && !lower.contains("success!")
            && !lower.contains("autocar_captcha_ok");
    String pending =
        index == 1 ? "SBS · esperando reporte y validación automática" : "Consultando…";
    setResult(
        index, captcha && index != 1 ? "Escribe el código en Verificación única" : pending, false);
    if (captcha && index >= 1 && index <= 3) showCaptchaPanelIfReady();
  }

  private boolean interpretSutranMessage(String message) {
    if (resultComplete[2]) return true;
    String kind = SutranResponse.classify(message, sutranQuerySent);
    if (kind.equals("captcha")) {
      sutranQuerySent = false;
      handleInvalidCaptcha(2);
      return true;
    }
    if (!kind.equals("empty")) return false;
    reportDetails[2] =
        "Placa consultada: "
            + PlateUtils.display(plate)
            + "\nRespuesta oficial SUTRAN:\n"
            + message.trim()
            + "\n"
            + "Alcance: sólo esta consulta SUTRAN; no acredita ausencia de multas en otras"
            + " entidades.";
    resultComplete[2] = true;
    validationPending[2] = false;
    captchaReady[2] = false;
    sourceAlert[2] = false;
    setResult(2, "Sin papeletas encontradas en esta consulta SUTRAN", false);
    updatePurchaseVerdict();
    showCaptchaPanelIfReady();
    return true;
  }

  private boolean hasRiskWords(int index, String lower) {
    if (index == 1) {
      return lower.contains("vencido")
          || lower.contains("no vigente")
          || lower.contains("anulado")
          || lower.contains("cancelado");
    }
    if (index == 2) return true;
    if (index == 3) {
      return lower.contains("vencido")
          || lower.contains("no vigente")
          || lower.contains("desaprobado")
          || lower.contains("rechazado");
    }
    return lower.contains("robo") && !lower.contains("sin alerta");
  }

  private String summarizeSbsReport(String body) {
    insuranceHistory.clear();
    String accidents = capture(body, "últimos\\s+5\\s+años\\s*:\\s*(\\d+)");
    String updated = capture(body, "Información actualizada a\\s*:\\s*([^\\n\\r]+)");
    java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
    format.setLenient(false);
    format.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
    java.util.Date latest = null;
    String insurer = "", start = "", end = "";
    for (String line : body.split("\\n")) {
      Matcher dates =
          Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4})").matcher(line);
      if (!dates.find()) continue;
      String[] columns = line.trim().split("\\t+");
      if (columns.length >= 3) {
        insuranceHistory.add(
            columns[0].trim()
                + " · "
                + columns[1].trim()
                + " · uso: "
                + columns[2].trim()
                + " · "
                + dates.group(1)
                + " al "
                + dates.group(2));
      } else {
        insuranceHistory.add(line.trim().replaceAll("\\s+", " "));
      }
      try {
        java.util.Date candidate = format.parse(dates.group(2));
        if (candidate == null || (latest != null && !candidate.after(latest))) continue;
        latest = candidate;
        start = dates.group(1);
        end = dates.group(2);
        insurer = line.split("\\t")[0].trim();
        if (insurer.length() > 60) insurer = "Aseguradora indicada en SBS";
      } catch (java.text.ParseException ignored) {
      }
    }
    boolean expired = false;
    if (latest != null) {
      try {
        expired = latest.before(format.parse(format.format(new java.util.Date())));
      } catch (java.text.ParseException ignored) {
      }
    }
    sourceAlert[1] = latest == null || expired || (!accidents.isEmpty() && !accidents.equals("0"));
    String status =
        latest == null
            ? "Vigencia no identificada"
            : expired ? "Última póliza reportada vencida" : "Póliza reportada vigente";
    List<String> details = new ArrayList<>();
    details.add("Estado: " + status);
    if (!insurer.isEmpty()) details.add("Aseguradora: " + insurer);
    if (!end.isEmpty()) details.add("Vigencia: " + start + " al " + end);
    details.add(
        "Accidentes cubiertos en 5 años: " + (accidents.isEmpty() ? "No identificado" : accidents));
    if (!updated.isEmpty()) details.add("SBS actualizada a: " + updated);
    if (!insuranceHistory.isEmpty()) {
      details.add("\nHISTORIAL DE PÓLIZAS Y USO DECLARADO");
      details.addAll(insuranceHistory);
    }
    details.add("Confirma la póliza actual con la aseguradora: SBS tiene actualización mensual.");
    reportDetails[1] = String.join("\n", details);
    return status
        + (end.isEmpty() ? "" : " · hasta " + end)
        + (accidents.isEmpty() ? "" : " · " + accidents + " accidentes en 5 años");
  }

  private String firstPhrase(String body, String... phrases) {
    for (String phrase : phrases) if (body.contains(phrase)) return phrase;
    return null;
  }

  private String extractUsefulLines(String body) {
    List<String> keys =
        Arrays.asList(
            "estado",
            "vigente",
            "vencido",
            "compañía",
            "aseguradora",
            "fecha fin",
            "fecha de vencimiento",
            "marca",
            "modelo",
            "propietario",
            "titular",
            "infracción");
    Set<String> chosen = new HashSet<>();
    for (String raw : body.split("\\n")) {
      String line = raw.trim().replaceAll("\\s+", " ");
      String lower = line.toLowerCase(Locale.ROOT);
      if (line.length() < 3 || line.length() > 110) continue;
      for (String key : keys) {
        if (lower.contains(key)) {
          chosen.add(line);
          break;
        }
      }
      if (chosen.size() >= 2) break;
    }
    return String.join(" · ", chosen);
  }

  private String extractSunarpSummary(String body) {
    String compact = body.replaceAll("\\s+", " ").trim();
    String brand = capture(compact, "MARCA\\s*:?\\s*([A-ZÁÉÍÓÚÑ0-9 .-]{2,28}?)\\s+MODELO");
    String model =
        capture(
            compact,
            "MODELO\\s*:?\\s*([A-ZÁÉÍÓÚÑ0-9"
                + " .-]{2,32}?)(?=\\s+(?:ESTADO|SEDE|AÑO|TITULAR|CARROCERÍA|CARROCERIA|N[°º])|$)");
    String color = capture(compact, "COLOR\\s*:?\\s*([A-ZÁÉÍÓÚÑ0-9 /.-]{2,28}?)\\s+MARCA");
    List<String> parts = new ArrayList<>();
    if (!brand.isEmpty() || !model.isEmpty()) parts.add((brand + " " + model).trim());
    if (!color.isEmpty()) parts.add("Color " + color);
    return String.join(" · ", parts);
  }

  private String capture(String text, String expression) {
    Matcher matcher =
        Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
    if (!matcher.find()) return "";
    String value = matcher.group(1);
    return value == null ? "" : value.trim();
  }

  private void setResult(int index, String message, boolean error) {
    errors[index] = error;
    reportSummaries[index] = message;
    Portal portal = PORTALS[index];
    boolean needsAction = message.startsWith("Completa") || message.startsWith("Escribe");
    TextView result = findViewById(portal.resultId);
    result.setText(portal.name + " · " + message);
    result.setTextColor(Color.parseColor(error ? "#A50E25" : needsAction ? "#9A6700" : "#334155"));
    TextView tab = findViewById(portal.tabId);
    String indicator = error || needsAction ? "  !" : resultComplete[index] ? "  ✓" : "  ···";
    tab.setText(portal.name + indicator);
    styleTab(tab, index == selectedIndex, error);
  }

  private void updatePurchaseVerdict() {
    int completed = 0;
    int alerts = 0;
    for (int index = 0; index < resultComplete.length; index++) {
      if (!enabled[index]) continue;
      if (!resultComplete[index]) continue;
      completed++;
      if (sourceAlert[index]) alerts++;
    }
    if (completed < selectedCount) {
      purchaseVerdict.setText(
          "INFORME EN CURSO · "
              + completed
              + "/"
              + selectedCount
              + " fuentes"
              + (alerts > 0 ? " · " + alerts + " alerta(s)" : "")
              + "\nEspera los resultados pendientes antes de decidir.");
      purchaseVerdict.setTextColor(Color.parseColor("#002B5C"));
    } else if (alerts > 0) {
      purchaseVerdict.setText(
          "REVISAR ANTES DE COMPRAR · "
              + alerts
              + (alerts == 1 ? " alerta detectada" : " alertas detectadas")
              + "\nSolicita al vendedor aclarar o regularizar los puntos señalados.");
      purchaseVerdict.setTextColor(Color.parseColor("#A50E25"));
    } else {
      purchaseVerdict.setText(
          "SIN ALERTAS EN LAS FUENTES CONSULTADAS\n"
              + "Confirma titularidad, cargas registrales y condición mecánica antes de comprar.");
      purchaseVerdict.setTextColor(Color.parseColor("#137333"));
    }
  }

  private class ResultBridge {
    private final int index;

    ResultBridge(int index) {
      this.index = index;
    }

    @JavascriptInterface
    public void report(String text) {
      runOnUiThread(() -> interpretResult(index, text == null ? "" : text));
    }

    @JavascriptInterface
    public void reportApi(String payload) {
      runOnUiThread(() -> interpretApiResult(index, payload == null ? "" : payload));
    }

    @JavascriptInterface
    public void reportStructured(String payload) {
      if (index != 3) return;
      runOnUiThread(
          () -> {
            try {
              JSONObject data = new JSONObject(payload);
              if (!PlateUtils.normalize(data.optString("plate")).equals(plate)) return;
              String state = data.optString("state").trim();
              String inspection = data.optString("result").trim();
              String end = data.optString("end").trim();
              if (state.isEmpty() || end.isEmpty()) return;
              reportDetails[index] =
                  "Placa: "
                      + data.optString("plate")
                      + "\nDesde: "
                      + data.optString("start")
                      + "\nHasta: "
                      + end
                      + "\nResultado: "
                      + inspection
                      + "\nEstado: "
                      + state;
              resultComplete[index] = true;
              validationPending[index] = false;
              sourceAlert[index] =
                  hasRiskWords(index, (state + " " + inspection).toLowerCase(Locale.ROOT));
              setResult(index, state + " · " + inspection + " · hasta " + end, false);
              updatePurchaseVerdict();
              showCaptchaPanelIfReady();
            } catch (Exception ignored) {
            }
          });
    }

    @JavascriptInterface
    public void reportPortalMessage(String message) {
      runOnUiThread(
          () -> {
            String safeMessage = message == null ? "" : message.trim();
            String lower = safeMessage.toLowerCase(Locale.ROOT);
            boolean invalidCaptcha =
                (lower.contains("código") || lower.contains("codigo") || lower.contains("captcha"))
                    && (lower.contains("incorrecto")
                        || lower.contains("no es valido")
                        || lower.contains("no es válido"));
            if (invalidCaptcha && index >= 1 && index <= 3) {
              handleInvalidCaptcha(index);
            } else if (!safeMessage.isEmpty()) {
              interpretResult(index, safeMessage);
            }
          });
    }

    @JavascriptInterface
    public void reportCaptcha(String dataUrl) {
      if (dataUrl == null || index < 1 || index > 3 || resultComplete[index]) return;
      int comma = dataUrl.indexOf(',');
      if (comma < 0) return;
      try {
        byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) return;
        runOnUiThread(
            () -> {
              ImageView target =
                  findViewById(
                      index == 1
                          ? R.id.captcha_soat_image
                          : index == 2 ? R.id.captcha_sutran_image : R.id.captcha_mtc_image);
              Bitmap previous = captchaBitmaps[index];
              captchaBitmaps[index] = bitmap;
              target.setImageBitmap(bitmap);
              if (previous != null && previous != bitmap && !previous.isRecycled())
                previous.recycle();
              captchaReady[index] = true;
              showCaptchaPanelIfReady();
            });
      } catch (IllegalArgumentException ignored) {
        // The official page will remain available in its tab as a fallback.
      }
    }

    @JavascriptInterface
    public void reportImage(String dataUrl) {
      if (dataUrl == null || imageProcessed[index]) return;
      imageProcessed[index] = true;
      int comma = dataUrl.indexOf(',');
      if (comma < 0) {
        imageProcessed[index] = false;
        return;
      }
      try {
        byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
          imageProcessed[index] = false;
          return;
        }
        if (index == 0)
          runOnUiThread(
              () -> {
                if (sunarpOfficialImage != null && !sunarpOfficialImage.isRecycled())
                  sunarpOfficialImage.recycle();
                sunarpOfficialImage = bitmap;
                reportDetails[0] =
                    "Vehículo encontrado en SUNARP.\n"
                        + "La consulta oficial completa se incluye como anexo en el PDF.\n"
                        + "Los campos no legibles no se consideran datos confirmados.";
              });
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener(
                text ->
                    runOnUiThread(
                        () -> {
                          String recognized = text.getText();
                          String lower = recognized.toLowerCase(Locale.ROOT);
                          boolean isVehicleReport =
                              lower.contains("marca")
                                  && lower.contains("modelo")
                                  && (lower.contains("placa") || lower.contains("motor"));
                          if (isVehicleReport) interpretResult(index, recognized);
                          else imageProcessed[index] = false;
                        }))
            .addOnFailureListener(error -> imageProcessed[index] = false);
      } catch (IllegalArgumentException error) {
        imageProcessed[index] = false;
      }
    }
  }

  private void interpretApiResult(int index, String payload) {
    int separator = payload.indexOf('\n');
    String json = separator >= 0 ? payload.substring(separator + 1) : payload;
    try {
      Object parsed = new JSONTokener(json).nextValue();
      String message = findJsonValue(parsed, "mensaje", "message", "error");
      if ("SUNARP".equals(PORTALS[index].name)) {
        int code = parsed instanceof JSONObject ? ((JSONObject) parsed).optInt("cod", 0) : 0;
        if (code == 1) {
          String alert = findJsonValue(parsed, "msgalertarobo");
          resultComplete[index] = true;
          sourceAlert[index] =
              !alert.isEmpty() && !alert.toLowerCase(Locale.ROOT).contains("sin alerta");
          setResult(
              index,
              alert.isEmpty() ? "Vehículo encontrado; informe oficial recibido" : alert,
              false);
          updatePurchaseVerdict();
        } else if (!message.isEmpty()) {
          setResult(index, message, false);
        }
        return;
      }

      String status = findJsonValue(parsed, "estado", "status", "situacion", "situación");
      String company =
          findJsonValue(
              parsed, "nombrecompania", "compania", "compañia", "compañía", "aseguradora");
      String endDate = findJsonValue(parsed, "fechafin", "fecha_fin", "vigenciafin", "finvigencia");
      List<String> parts = new ArrayList<>();
      if (!status.isEmpty()) parts.add(status);
      if (!company.isEmpty()) parts.add(company);
      if (!endDate.isEmpty()) parts.add("Hasta " + endDate);
      if (!parts.isEmpty()) {
        String startDate = findJsonValue(parsed, "fechainicio", "vigenciainicio", "inicioVigencia");
        String certificate =
            findJsonValue(parsed, "numeropoliza", "nropoliza", "numerocertificado");
        reportDetails[index] =
            "Estado: "
                + (status.isEmpty() ? "Por confirmar" : status)
                + "\nAseguradora: "
                + company
                + "\nInicio: "
                + startDate
                + "\nFin: "
                + endDate
                + (certificate.isEmpty() ? "" : "\nPóliza/certificado: " + certificate);
        resultComplete[index] = true;
        validationPending[index] = false;
        sourceAlert[index] =
            (index == 1 && status.isEmpty())
                || hasRiskWords(index, (status + " " + company).toLowerCase(Locale.ROOT))
                || company.toLowerCase(Locale.ROOT).contains("no encontrada");
        setResult(
            index,
            (index == 1 && status.isEmpty() ? "Vigencia por confirmar · " : "")
                + String.join(" · ", parts),
            false);
        updatePurchaseVerdict();
        showCaptchaPanelIfReady();
      } else if (!message.isEmpty()) setResult(index, message, false);
      else setResult(index, "Respuesta oficial recibida", false);
    } catch (Exception ignored) {
      if (!json.trim().isEmpty()) interpretResult(index, json);
    }
  }

  private String findJsonValue(Object value, String... keys) {
    if (value instanceof JSONObject) {
      JSONObject object = (JSONObject) value;
      Iterator<String> names = object.keys();
      while (names.hasNext()) {
        String name = names.next();
        for (String key : keys) {
          if (name.replace("_", "").equalsIgnoreCase(key.replace("_", ""))) {
            Object found = object.opt(name);
            if (found != null
                && found != JSONObject.NULL
                && !(found instanceof JSONObject)
                && !(found instanceof JSONArray)) {
              String text = String.valueOf(found).trim();
              if (!text.isEmpty()) return text;
            }
          }
        }
        String nested = findJsonValue(object.opt(name), keys);
        if (!nested.isEmpty()) return nested;
      }
    } else if (value instanceof JSONArray) {
      JSONArray array = (JSONArray) value;
      for (int i = 0; i < array.length(); i++) {
        String nested = findJsonValue(array.opt(i), keys);
        if (!nested.isEmpty()) return nested;
      }
    }
    return "";
  }

  private void styleTab(TextView tab, boolean selected, boolean error) {
    GradientDrawable background = new GradientDrawable();
    background.setCornerRadius(dp(16));
    int color =
        error ? Color.parseColor("#FCE8E8") : selected ? Color.parseColor("#002B5C") : Color.WHITE;
    background.setColor(color);
    background.setStroke(dp(1), error ? Color.parseColor("#C8102E") : Color.parseColor("#E7EAF0"));
    tab.setBackground(background);
    tab.setTextColor(
        error ? Color.parseColor("#A50E25") : selected ? Color.WHITE : Color.parseColor("#002B5C"));
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  public void dispose() {
    disposed = true;
    handler.removeCallbacksAndMessages(null);
    for (WebView webView : webViews) {
      webView.stopLoading();
      webView.destroy();
    }
    for (Bitmap bitmap : captchaBitmaps) {
      if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
    }
    webViews.clear();
    if (sunarpOfficialImage != null && !sunarpOfficialImage.isRecycled())
      sunarpOfficialImage.recycle();
  }

  private static class Portal {
    final String name;
    final String url;
    final int tabId;
    final int resultId;
    final String inputSelector;

    Portal(String name, String url, int tabId, int resultId, String inputSelector) {
      this.name = name;
      this.url = url;
      this.tabId = tabId;
      this.resultId = resultId;
      this.inputSelector = inputSelector;
    }
  }
}
