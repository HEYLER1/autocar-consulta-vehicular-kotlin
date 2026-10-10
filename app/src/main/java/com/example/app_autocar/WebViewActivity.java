package com.example.app_autocar;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        initialUrl = getIntent().getStringExtra(EXTRA_URL);
        plate = getIntent().getStringExtra(EXTRA_PLATE);
        if (plate == null) plate = "";

        ((TextView) findViewById(R.id.web_title)).setText(title == null ? getString(R.string.app_name) : title);
        findViewById(R.id.web_back).setOnClickListener(view -> goBackOrClose());
        findViewById(R.id.web_refresh).setOnClickListener(view -> webView.reload());
        findViewById(R.id.web_external).setOnClickListener(view -> openExternal());
        findViewById(R.id.web_retry).setOnClickListener(view -> {
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

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }

            @Override public void onPageFinished(WebView view, String url) {
                if (initialUrl != null && initialUrl.contains("sat.gob.pe/VirtualSAT/principal.aspx")) {
                    routeSatToFines();
                }
                injectPlateIfAvailable();
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    errorView.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                    Toast.makeText(WebViewActivity.this, "No hay una aplicación compatible.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                goBackOrClose();
            }
        });

        if (initialUrl == null || initialUrl.isEmpty()) {
            finish();
        } else {
            webView.loadUrl(initialUrl);
        }
    }

    private void injectPlateIfAvailable() {
        if (plate.isEmpty()) return;
        String safePlate = plate.replace("\\", "\\\\").replace("'", "\\'");
        String script = "(function(){" +
                "var inputs=document.querySelectorAll('input');" +
                "for(var i=0;i<inputs.length;i++){" +
                "var e=inputs[i], k=((e.id||'')+' '+(e.name||'')+' '+(e.placeholder||'')).toLowerCase();" +
                "if(k.indexOf('placa')>=0||k.indexOf('patente')>=0){" +
                "e.value='" + safePlate + "';" +
                "e.dispatchEvent(new Event('input',{bubbles:true}));" +
                "e.dispatchEvent(new Event('change',{bubbles:true}));break;}}})();";
        webView.evaluateJavascript(script, null);
    }

    private void routeSatToFines() {
        String script = "(function(){if(window.location.pathname.toLowerCase().endsWith('/papeletas.aspx')||window.__autocarSatRouting)return;window.__autocarSatRouting=true;"
                + "var tries=0,t=setInterval(function(){tries++;var docs=[document],frames=document.querySelectorAll('frame,iframe');"
                + "for(var i=0;i<frames.length;i++){try{if(frames[i].contentDocument)docs.push(frames[i].contentDocument);}catch(e){}}"
                + "for(var d=0;d<docs.length;d++){var links=docs[d].querySelectorAll('a[href]');"
                + "for(var k=0;k<links.length;k++){var a=links[k];if(a.href.indexOf('/VirtualSAT/modulos/papeletas.aspx?')>=0){"
                + "clearInterval(t);window.location.href=a.href;return;}}}if(tries>=40)clearInterval(t);},500);})();";
        webView.evaluateJavascript(script, null);
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
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
