package com.example.app_autocar;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

  public static final String EXTRA_TITLE = "title";
  public static final String EXTRA_URL = "url";
  public static final String EXTRA_PLATE = "plate";
  public static final String EXTRA_TRAFFIC_AUTOFILL = "traffic_autofill";

  private WebView webView;
  private ProgressBar progressBar;
  private String plate;
  private String initialUrl;
  private View errorView;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_web_view);
    int[] actions = {R.id.web_back, R.id.web_refresh, R.id.web_external};
    int[] icons = {R.drawable.ic_back, R.drawable.ic_refresh, R.drawable.ic_external};
    String[] labels = {"Volver", "Actualizar página", "Abrir en navegador"};
    for (int i = 0; i < actions.length; i++) {
      TextView action = findViewById(actions[i]);
      IconActions.apply(action, icons[i], labels[i]);
      action.getCompoundDrawables()[0].setTint(Color.WHITE);
      action.setPadding(12, 0, 0, 0);
    }

    String title = getIntent().getStringExtra(EXTRA_TITLE);
    initialUrl = getIntent().getStringExtra(EXTRA_URL);
    plate = PlateUtils.normalize(getIntent().getStringExtra(EXTRA_PLATE));
    if (plate == null) plate = "";

    ((TextView) findViewById(R.id.web_title))
        .setText(title == null ? getString(R.string.app_name) : title);
    findViewById(R.id.web_back).setOnClickListener(view -> goBackOrClose());
    findViewById(R.id.web_refresh).setOnClickListener(view -> webView.reload());
    findViewById(R.id.web_external).setOnClickListener(view -> openExternal());
    findViewById(R.id.web_retry)
        .setOnClickListener(
            view -> {
              errorView.setVisibility(View.GONE);
              webView.setVisibility(View.VISIBLE);
              webView.reload();
            });
    progressBar = findViewById(R.id.web_progress);
    webView = findViewById(R.id.web_view);
    errorView = findViewById(R.id.web_error);

    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);

    webView.setWebChromeClient(
        new WebChromeClient() {
          @Override
          public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
          }
        });
    webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            errorView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            if (AutoLoginAutomation.tryAutoLogin(view, url)) {
              // Se envió el formulario de login; se espera la siguiente carga de página.
              return;
            }
            if (getIntent().getBooleanExtra(EXTRA_TRAFFIC_AUTOFILL, false)) {
              fillTrafficForm(view, url, plate);
              return;
            }
            if (initialUrl != null && initialUrl.contains("sat.gob.pe/VirtualSAT/principal.aspx")) {
              routeSatToFines();
            }
            injectPlateIfAvailable();
            injectSunarpIdentity(url);
            injectSigueloIdentity(url);
          }

          @Override
          public void onReceivedError(
              WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
              errorView.setVisibility(View.VISIBLE);
              webView.setVisibility(View.GONE);
            }
          }

          @Override
          public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;
            try {
              startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException ignored) {
              Toast.makeText(
                      WebViewActivity.this, "No hay una aplicación compatible.", Toast.LENGTH_SHORT)
                  .show();
            }
            return true;
          }
        });

    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                goBackOrClose();
              }
            });

    if (initialUrl == null || initialUrl.isEmpty()) {
      finish();
    } else {
      webView.loadUrl(initialUrl);
    }
  }

  static void fillTrafficForm(WebView webView, String url, String plate) {
    TrafficFormAutomation.fill(webView, url, plate);
  }

  private void injectPlateIfAvailable() {
    if (plate.isEmpty()) return;
    String safePlate = plate.replace("\\", "\\\\").replace("'", "\\'");
    String script =
        "(function(){var inputs=document.querySelectorAll('input');for(var"
            + " i=0;i<inputs.length;i++){var e=inputs[i], k=((e.id||'')+' '+(e.name||'')+'"
            + " '+(e.placeholder||'')).toLowerCase();"
            + "if(k.indexOf('placa')>=0||k.indexOf('patente')>=0){e.value='"
            + safePlate
            + "';"
            + "e.dispatchEvent(new Event('input',{bubbles:true}));"
            + "e.dispatchEvent(new Event('change',{bubbles:true}));break;}}})();";
    webView.evaluateJavascript(script, null);
  }

  private void routeSatToFines() {
    String script =
        "(function(){if(window.location.pathname.toLowerCase().endsWith('/papeletas.aspx')||window.__autocarSatRouting)return;window.__autocarSatRouting=true;var"
            + " tries=0,t=setInterval(function(){tries++;var"
            + " docs=[document],frames=document.querySelectorAll('frame,iframe');for(var"
            + " i=0;i<frames.length;i++){try{if(frames[i].contentDocument)docs.push(frames[i].contentDocument);}catch(e){}}for(var"
            + " d=0;d<docs.length;d++){var links=docs[d].querySelectorAll('a[href]');for(var"
            + " k=0;k<links.length;k++){var"
            + " a=links[k];if(a.href.indexOf('/VirtualSAT/modulos/papeletas.aspx?')>=0){"
            + "clearInterval(t);window.location.href=a.href;return;}}}if(tries>=40)clearInterval(t);},500);})();";
    webView.evaluateJavascript(script, null);
  }

  private void injectSunarpIdentity(String url) {
    if (url == null || !"conoce-aqui.sunarp.gob.pe".equals(Uri.parse(url).getHost())) return;
    android.content.SharedPreferences identity =
        getSharedPreferences("sunarp_identity", MODE_PRIVATE);
    if (!identity.contains("dni")) return;
    String dni = org.json.JSONObject.quote(identity.getString("dni", ""));
    String date = org.json.JSONObject.quote(identity.getString("date", ""));
    String digit = org.json.JSONObject.quote(identity.getString("digit", ""));
    String email = org.json.JSONObject.quote(identity.getString("email", ""));
    webView.evaluateJavascript(
        "(function(){if(window.__autocarIdentity)return;window.__autocarIdentity=true;var"
            + " tries=0,t=setInterval(function(){"
            + "tries++;"
            + "var sel=document.querySelector('#docType,select[name=docType]');if(sel){var opt=Array.from(sel.options).find(function(o){return /dni/i.test(o.text||o.label||'');});if(opt){sel.value=opt.value;sel.dispatchEvent(new Event('change',{bubbles:true}));}}"
            + "document.querySelectorAll('input').forEach(function(e){if(e.type==='hidden'||e.type==='checkbox'||e.type==='radio')return;var"
            + " labels=Array.from(e.labels||[]).map(function(l){return l.innerText}).join(' ');var"
            + " k=((e.id||'')+' '+(e.name||'')+' '+(e.placeholder||'')+'"
            + " '+(e.getAttribute('formcontrolname')||'')+'"
            + " '+labels).toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');var"
            + " v=null;if(/digito|verificador/.test(k))v="
            + digit
            + ";else if(/emision/.test(k)){v="
            + date
            + ";if(e.type==='date'){var"
            + " p=v.split('/');v=p[2]+'-'+p[1].padStart(2,'0')+'-'+p[0].padStart(2,'0');}}else"
            + " if(/dni|numerodocumento|numero.*documento|nrodocumento|documenttovalidate/.test(k))v="
            + dni
            + ";else if(/correo|email/.test(k))v="
            + email
            + ";if(v!==null&&!e.value){var"
            + " s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;s.call(e,v);e.dispatchEvent(new"
            + " Event('input',{bubbles:true}));e.dispatchEvent(new"
            + " Event('change',{bubbles:true}));}});"
            + "var sendBtn=document.querySelector('#sendCodeButton,button[onclick*=sendEmail]');"
            + "if(sendBtn&&!sendBtn.disabled&&!window.__autocarCodeSent){window.__autocarCodeSent=true;sendBtn.click();}"
            + "var otp=document.querySelector('input[maxlength=\"6\"]');"
            + "if(otp&&otp.value.length>=4&&!window.__autocarOtpSubmitted){var verifyBtn=document.querySelector('button[type=submit]:not(#sendCodeButton),#verifyButton,#kc-login');if(verifyBtn&&!verifyBtn.disabled){window.__autocarOtpSubmitted=true;verifyBtn.click();}}"
            + "if(tries>=40)clearInterval(t);},700);})();",
        null);
  }

  private void injectSigueloIdentity(String url) {
    if (url == null || !"sigueloplus.sunarp.gob.pe".equals(Uri.parse(url).getHost())) return;
    android.content.SharedPreferences identity =
        getSharedPreferences("siguelo_identity", MODE_PRIVATE);
    if (!identity.contains("numero")) return;
    String oficina = org.json.JSONObject.quote(identity.getString("oficina", ""));
    String anio = org.json.JSONObject.quote(identity.getString("anio", ""));
    String numero = org.json.JSONObject.quote(identity.getString("numero", ""));
    webView.evaluateJavascript(
        "(function(){if(window.__autocarSiguelo)return;window.__autocarSiguelo=true;var"
            + " tries=0,t=setInterval(function(){"
            + "tries++;"
            // Oficina registral select
            + "var off=document.querySelector('select[ngModel],select');if(off&&!off.value){var opt=Array.from(off.options).find(function(o){return (o.text||'').toUpperCase().includes("
            + oficina
            + ".toUpperCase());});if(opt){off.value=opt.value;off.dispatchEvent(new Event('change',{bubbles:true}));}}"
            // Año select
            + "var an=document.querySelectorAll('select');Array.from(an).forEach(function(s){if(!s.value&&Array.from(s.options).some(function(o){return (o.text||'').includes("
            + anio
            + ");})){var o=Array.from(s.options).find(function(o){return (o.text||'').includes("
            + anio
            + ");});if(o){s.value=o.value;s.dispatchEvent(new Event('change',{bubbles:true}));}}});"
            // Número de título input
            + "document.querySelectorAll('input').forEach(function(e){if(e.type==='hidden')return;var"
            + " k=((e.id||'')+' '+(e.name||'')+' '+(e.placeholder||'')).toLowerCase();if(/numero|titulo/.test(k)&&!e.value){var"
            + " s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;s.call(e,"
            + numero
            + ");e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));}});"
            // Auto-enviar cuando Turnstile esté resuelto
            + "var token=document.querySelector('[name=\"cf-turnstile-response\"]');"
            + "if(token&&token.value&&token.value.length>10&&!window.__autocarSigueloSubmitted){var btn=Array.from(document.querySelectorAll('button')).find(function(b){return /buscar|consultar|verificar/i.test(b.innerText||b.value||'');});if(btn&&!btn.disabled){window.__autocarSigueloSubmitted=true;btn.click();}}"
            + "if(tries>=60)clearInterval(t);},700);})();",
        null);
  }

  private void goBackOrClose() {
    if (webView != null && webView.canGoBack()) webView.goBack();
    else finish();
  }

  private void openExternal() {
    String url = webView.getUrl();
    if (url == null || url.isEmpty()) url = initialUrl;
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (ActivityNotFoundException error) {
      Toast.makeText(this, "No hay un navegador disponible.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onDestroy() {
    if (webView != null) {
      TrafficFormAutomation.clear(webView);
      webView.stopLoading();
      webView.destroy();
    }
    super.onDestroy();
  }
}
