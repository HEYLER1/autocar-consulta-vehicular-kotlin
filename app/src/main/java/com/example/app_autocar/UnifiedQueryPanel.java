package com.example.app_autocar;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;

/** One plate, independent sessions. Captchas remain in the original official forms. */
public class UnifiedQueryPanel extends android.view.ContextThemeWrapper {
    private final AppCompatActivity host;
    private final android.content.Intent request;
    private final Runnable onClose;
    private View rootView;
    private boolean disposed;
    private Runnable completionListener=()->{};
    private boolean completionReported;
    private boolean externalResultsLink;
    public void useExternalResultsLink(){externalResultsLink=true;}
    public void openResults(){showSection(selectedSection==0?-1:0);}
    public void setCompletionListener(Runnable listener){completionListener=listener;}
    public UnifiedQueryPanel(AppCompatActivity host,android.content.Intent request,Runnable onClose){super(host,host.getTheme());this.host=host;this.request=request;this.onClose=onClose;}
    public View view(){return rootView;}
    private android.content.Intent getIntent(){return request;}
    private android.view.Window getWindow(){return host.getWindow();}
    private boolean isFinishing(){return disposed||host.isFinishing();}
    private boolean isDestroyed(){return disposed||host.isDestroyed();}
    private void runOnUiThread(Runnable action){host.runOnUiThread(action);}
    private void finish(){onClose.run();}

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<WebView> portals = new ArrayList<>();
    private final java.util.Set<WebView> submitted = new java.util.HashSet<>();
    private final java.util.Map<WebView, LinearLayout> verificationPanels = new java.util.HashMap<>();
    private final java.util.Map<WebView, Integer> attempts = new java.util.HashMap<>();
    private final java.util.Map<WebView, Integer> generations = new java.util.HashMap<>();
    private final java.util.Map<WebView, Integer> resultAttempts = new java.util.HashMap<>();
    private final java.util.Set<WebView> failed = new java.util.HashSet<>();
    private android.widget.FrameLayout engineHost;
    private final java.util.Map<WebView, TextView> summaries = new java.util.HashMap<>();
    private final java.util.Map<WebView, String> rawDetails = new java.util.HashMap<>();
    private final java.util.Map<WebView,android.graphics.Bitmap> officialImages=new java.util.HashMap<>();
    private final java.util.Set<WebView> readingImages=new java.util.HashSet<>();
    private PriorityQueryEngine priorityEngine;
    private final java.util.Map<WebView,Integer> priorityIndices=new java.util.HashMap<>();
    private final java.util.Map<WebView,android.graphics.Bitmap> shownPriorityCaptchas=new java.util.HashMap<>();
    private final java.util.Map<WebView,Integer> priorityCaptchaHashes=new java.util.HashMap<>();
    private final java.util.Map<WebView,TextView> fullDetails=new java.util.HashMap<>();
    private final java.util.Map<WebView,Button> detailButtons=new java.util.HashMap<>();
    private android.widget.ProgressBar queryProgress;
    private TextView queryProgressText;
    private TextView resultsAction;
    private final java.util.Map<WebView, LinearLayout> sourceCards = new java.util.HashMap<>();
    private final java.util.Map<WebView, LinearLayout> verificationCards = new java.util.HashMap<>();
    private final java.util.Map<WebView, android.widget.EditText> codes = new java.util.HashMap<>();
    private final java.util.Map<WebView, Button> codeButtons = new java.util.HashMap<>();
    private final java.util.Map<WebView, TextView> externalLinks = new java.util.HashMap<>();
    private LinearLayout resultsList, verificationList, sourcesList;
    private ScrollView resultsScroll, verificationScroll, sourcesScroll;
    private TextView resultEmpty;
    private TextView generalSummary,generalDetail;
    private TextView verificationHint;
    private View queryHeader,queryTabs,reportFooter;
    private TextView resultTab, verificationTab, sourceTab;
    private int selectedSection;
    private boolean verificationFocused;
    private final ArrayList<TextView> reportStatuses=new ArrayList<>();
    private final ArrayList<TextView> reportDetails=new ArrayList<>();
    private String[] sourceNames,sourceUrls;
    private String queryPlate;
    private TextView reportState;
    private java.io.File latestPdf;
    private boolean latestPdfShared;
    private int pdfSignature;
    private boolean hasPdfSignature;
    private final Runnable updatePdf=() -> savePdf(false);

    @SuppressLint("SetJavaScriptEnabled")
    public void start() {
        androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        String plate = PlateUtils.normalize(getIntent().getStringExtra("plate"));
        String[] names = getIntent().getStringArrayExtra("names");
        String[] urls = getIntent().getStringArrayExtra("urls");
        if (names == null || urls == null || names.length != urls.length || !PlateUtils.isValidPeruvianPlate(plate)) { finish(); return; }
        sourceNames=names;sourceUrls=urls;queryPlate=plate;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(244,247,249));
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(22),dp(12),dp(22),dp(24));hero.setBackgroundColor(Color.rgb(0,43,92));root.addView(hero);
        queryHeader=hero;
        TextView back = new TextView(this); back.setText("‹  VOLVER");back.setTextColor(Color.WHITE);back.setTextSize(12);back.setPadding(0,dp(8),0,dp(16));back.setOnClickListener(v -> finish());hero.addView(back);
        TextView title=new TextView(this);title.setText("Tu consulta vehicular");title.setTextColor(Color.WHITE);title.setTextSize(22);title.setTypeface(null,android.graphics.Typeface.BOLD);hero.addView(title);
        TextView plateLabel=new TextView(this);plateLabel.setText(PlateUtils.display(plate)+"  ·  "+names.length+" fuentes");plateLabel.setTextColor(Color.rgb(255,214,0));plateLabel.setTextSize(18);plateLabel.setPadding(0,dp(8),0,0);hero.addView(plateLabel);
        LinearLayout progressStrip=new LinearLayout(this);progressStrip.setOrientation(LinearLayout.VERTICAL);progressStrip.setPadding(dp(16),dp(8),dp(16),dp(4));root.addView(progressStrip);
        queryProgressText=new TextView(this);queryProgressText.setText("0% · Preparando consulta");queryProgressText.setTextSize(13);queryProgressText.setTextColor(Color.rgb(0,43,92));progressStrip.addView(queryProgressText);
        queryProgress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);queryProgress.setMax(100);progressStrip.addView(queryProgress,new LinearLayout.LayoutParams(-1,dp(8)));
        resultsAction=new TextView(this);resultsAction.setText("Ver resultados →");resultsAction.setTextColor(Color.rgb(0,43,92));resultsAction.setTextSize(15);resultsAction.setTypeface(null,android.graphics.Typeface.BOLD);resultsAction.setGravity(android.view.Gravity.CENTER_VERTICAL);resultsAction.setMinHeight(dp(48));progressStrip.addView(resultsAction);resultsAction.setOnClickListener(v->openResultsPage());
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(4),0,dp(4),0);root.addView(tabs);
        queryTabs=tabs;
        resultTab=sectionButton("Resultados",0,tabs);verificationTab=sectionButton("Verificación",1,tabs);sourceTab=sectionButton("Fuentes",2,tabs);
        verificationTab.setVisibility(View.GONE);sourceTab.setTextSize(11);sourceTab.setLayoutParams(new LinearLayout.LayoutParams(dp(76),dp(44)));
        LinearLayout sections=new LinearLayout(this);sections.setOrientation(LinearLayout.VERTICAL);root.addView(sections,new LinearLayout.LayoutParams(-1,-2));
        resultsList=new LinearLayout(this);verificationList=new LinearLayout(this);sourcesList=new LinearLayout(this);
        verificationScroll=sectionScroll(verificationList,sections);resultsScroll=sectionScroll(resultsList,sections);sourcesScroll=sectionScroll(sourcesList,sections);
        resultEmpty=new TextView(this);resultEmpty.setText("Consultando las fuentes…\nLos resultados aparecerán aquí. Completa las verificaciones pendientes para continuar.");resultEmpty.setTextSize(15);resultEmpty.setTextColor(Color.rgb(107,114,128));resultEmpty.setPadding(dp(10),dp(24),dp(10),dp(24));resultsList.addView(resultEmpty);
        generalSummary=new TextView(this);generalSummary.setTextSize(15);generalSummary.setTextColor(Color.rgb(26,26,46));generalSummary.setLineSpacing(dp(5),1);generalSummary.setTextIsSelectable(true);resultsList.addView(generalSummary);
        TextView overallToggle=new TextView(this);overallToggle.setText("Ver información completa ▾");overallToggle.setTextSize(13);overallToggle.setTextColor(Color.rgb(85,99,116));overallToggle.setMinHeight(dp(48));overallToggle.setGravity(android.view.Gravity.CENTER_VERTICAL);resultsList.addView(overallToggle);
        generalDetail=new TextView(this);generalDetail.setTextSize(14);generalDetail.setTextIsSelectable(true);generalDetail.setLineSpacing(dp(4),1);generalDetail.setVisibility(View.GONE);resultsList.addView(generalDetail);overallToggle.setOnClickListener(v->{boolean open=generalDetail.getVisibility()!=View.VISIBLE;generalDetail.setVisibility(open?View.VISIBLE:View.GONE);overallToggle.setText(open?"Ocultar información completa ▴":"Ver información completa ▾");});
        LinearLayout list=sourcesList;
        TextView verificationBack=new TextView(this);verificationBack.setText("Verificación");verificationBack.setTextColor(Color.rgb(0,43,92));verificationBack.setTextSize(14);verificationBack.setGravity(android.view.Gravity.CENTER_VERTICAL);verificationBack.setMinHeight(dp(48));verificationList.addView(verificationBack);
        verificationHint=new TextView(this);verificationHint.setVisibility(View.GONE);verificationList.addView(verificationHint);
        com.google.android.material.button.MaterialButton continueAll=new com.google.android.material.button.MaterialButton(this);continueAll.setText("Continuar");continueAll.setAllCaps(false);continueAll.setCornerRadius(dp(10));continueAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0,43,92)));continueAll.setOnClickListener(v -> {
            boolean any=false;for(WebView web:portals){android.widget.EditText code=codes.get(web);Button button=codeButtons.get(web);if(code!=null&&button!=null&&verificationCards.get(web).getVisibility()==View.VISIBLE&&!code.getText().toString().trim().isEmpty()&&button.isEnabled()){button.performClick();any=true;}else if(code==null&&verificationCards.get(web).getVisibility()==View.VISIBLE){resumeInteractive(web);any=true;}}
            if(!any)android.widget.Toast.makeText(this,"Introduce un código pendiente o completa la verificación oficial",android.widget.Toast.LENGTH_SHORT).show();
        });verificationList.addView(continueAll);
        showSection(-1);
        LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.VERTICAL);footer.setPadding(dp(16),dp(8),dp(16),dp(12));footer.setBackgroundColor(Color.WHITE);root.addView(footer);
        reportFooter=footer;queryHeader.setVisibility(View.GONE);footer.setVisibility(View.GONE);
        reportState=new TextView(this);reportState.setText("Preparando informe PDF…");reportState.setTextSize(12);reportState.setTextColor(Color.rgb(107,114,128));footer.addView(reportState);
        com.google.android.material.button.MaterialButton pdf=new com.google.android.material.button.MaterialButton(this);pdf.setText("Guardar / compartir informe PDF");pdf.setCornerRadius(dp(16));pdf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0,43,92)));pdf.setTextColor(Color.WHITE);pdf.setOnClickListener(v -> savePdf(true));footer.addView(pdf,new LinearLayout.LayoutParams(-1,dp(56)));
        android.widget.FrameLayout layers=new InlineQueryFrame(this);engineHost=new android.widget.FrameLayout(this);layers.addView(engineHost,new android.widget.FrameLayout.LayoutParams(-1,-1));layers.addView(root,new android.widget.FrameLayout.LayoutParams(-1,-1));rootView=layers;
        java.util.ArrayList<String> priorityNames=new java.util.ArrayList<>();for(String url:urls){int index=priorityIndex(url);if(index>=0)priorityNames.add(new String[]{"SUNARP","SOAT","SUTRAN","MTC"}[index]);}
        if(!priorityNames.isEmpty()){
            android.content.Intent engineIntent=new android.content.Intent().putExtra("plate",plate).putExtra("selected_portals",priorityNames.toArray(new String[0]));priorityEngine=new PriorityQueryEngine(host,engineIntent);priorityEngine.start();engineHost.addView(priorityEngine.view(),new android.widget.FrameLayout.LayoutParams(-1,-1));priorityEngine.view().setVisibility(View.INVISIBLE);
        }
        for (int i=0;i<urls.length;i++) {
            final String url=urls[i];
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18),dp(16),dp(18),dp(14));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(20));bg.setStroke(dp(1),Color.rgb(231,234,240));card.setBackground(bg);LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);cardParams.bottomMargin=dp(12);list.addView(card,cardParams);
            TextView sourceTitle=new TextView(this);sourceTitle.setText(names[i]);sourceTitle.setTextColor(Color.rgb(0,43,92));sourceTitle.setTextSize(17);sourceTitle.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(sourceTitle);
            TextView status=new TextView(this); status.setText("En cola"); status.setTextColor(Color.rgb(107,114,128)); status.setTextSize(13);status.setPadding(0,dp(6),0,dp(6)); card.addView(status);
            TextView summary=new TextView(this);summary.setTextSize(14);summary.setTextColor(Color.rgb(26,26,46));summary.setLineSpacing(dp(4),1);summary.setPadding(0,dp(12),0,dp(12));summary.setMaxLines(6);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);summary.setVisibility(View.GONE);card.addView(summary);
            reportStatuses.add(status);reportDetails.add(summary);
            android.text.TextWatcher watcher=new android.text.TextWatcher(){
                @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                @Override public void onTextChanged(CharSequence s,int start,int before,int count){String label=status.getText().toString();status.setTextColor(label.startsWith("Información recibida")?Color.rgb(19,115,51):label.startsWith("No ")||label.startsWith("Error")||label.startsWith("Acceso")?Color.rgb(179,38,30):label.contains("Verificación")||label.contains("Código")||label.contains("Mensaje")?Color.rgb(138,86,0):Color.rgb(52,94,134));handler.removeCallbacks(updatePdf);handler.postDelayed(updatePdf,1500);updateCounts();}
                @Override public void afterTextChanged(android.text.Editable s){}
            };status.addTextChangedListener(watcher);summary.addTextChangedListener(watcher);
            TextView toggle=new TextView(this);toggle.setText("Fuente oficial ↗");toggle.setTextSize(12);toggle.setTextColor(Color.rgb(85,99,116));toggle.setMinHeight(dp(48));toggle.setGravity(android.view.Gravity.CENTER_VERTICAL);card.addView(toggle);
            LinearLayout verification = new LinearLayout(this); verification.setOrientation(LinearLayout.VERTICAL);
            LinearLayout verificationCard=new LinearLayout(this);verificationCard.setOrientation(LinearLayout.VERTICAL);verificationCard.setPadding(dp(4),dp(4),dp(4),dp(4));android.graphics.drawable.GradientDrawable verifyBg=new android.graphics.drawable.GradientDrawable();verifyBg.setColor(Color.WHITE);verifyBg.setCornerRadius(0);verificationCard.setBackground(verifyBg);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.bottomMargin=dp(4);verificationList.addView(verificationCard,verificationList.getChildCount()-1,vp);TextView verifyTitle=new TextView(this);verifyTitle.setText(names[i]);verifyTitle.setTextSize(13);verifyTitle.setTypeface(null,android.graphics.Typeface.BOLD);verifyTitle.setTextColor(Color.rgb(0,43,92));verificationCard.addView(verifyTitle);verificationCard.addView(verification);verificationCard.setVisibility(View.GONE);
            int priority=priorityIndex(url);WebView web=priority>=0?priorityEngine.portal(priority):new WebView(this);if(web.getParent()!=null)((android.view.ViewGroup)web.getParent()).removeView(web);portals.add(web); web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true); web.getSettings().setUseWideViewPort(true); web.getSettings().setLoadWithOverviewMode(true);
            engineHost.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1)); web.setVisibility(View.INVISIBLE);
            summaries.put(web,summary);
            TextView completeDetail=new TextView(this);completeDetail.setTextSize(14);completeDetail.setTextIsSelectable(true);completeDetail.setLineSpacing(dp(4),1);completeDetail.setVisibility(View.GONE);card.addView(completeDetail);fullDetails.put(web,completeDetail);
            com.google.android.material.button.MaterialButton detailButton=new com.google.android.material.button.MaterialButton(this);detailButton.setText("Ver detalle completo");detailButton.setAllCaps(false);detailButton.setTextSize(12);detailButton.setVisibility(View.GONE);card.addView(detailButton);detailButtons.put(web,detailButton);
            detailButton.setOnClickListener(v->{boolean expand=completeDetail.getVisibility()!=View.VISIBLE;completeDetail.setVisibility(expand?View.VISIBLE:View.GONE);summary.setVisibility(expand?View.GONE:View.VISIBLE);detailButton.setText(expand?"Ocultar detalle":"Ver detalle completo");});
            sourceCards.put(web,card);verificationCards.put(web,verificationCard);
            TextView external=new TextView(this);external.setText("Abrir enlace oficial en el navegador ↗");external.setTextColor(Color.rgb(0,43,92));external.setTextSize(13);external.setPadding(0,dp(12),0,dp(8));external.setVisibility(View.GONE);external.setOnClickListener(v -> {try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(android.content.ActivityNotFoundException error){android.widget.Toast.makeText(this,"No hay navegador disponible",android.widget.Toast.LENGTH_SHORT).show();}});card.addView(external);externalLinks.put(web,external);
            verificationPanels.put(web, verification);
            toggle.setOnClickListener(v -> { boolean open=web.getParent()!=card; ((android.view.ViewGroup)web.getParent()).removeView(web);if(open){card.addView(web,new LinearLayout.LayoutParams(-1,dp(460)));web.setVisibility(View.VISIBLE);if(web.getUrl()==null)web.loadUrl(url);}else{engineHost.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1));web.setVisibility(View.INVISIBLE);}toggle.setText(open?"Ocultar fuente oficial":"Ver fuente oficial ›"); });
            final String name=names[i];
            if(priority>=0){priorityIndices.put(web,priority);web.setVisibility(View.VISIBLE);handler.postDelayed(()->pollPriority(web,status,name),500);continue;}
            web.setWebViewClient(new WebViewClient(){
                @Override public void onPageStarted(WebView view,String loadedUrl,android.graphics.Bitmap icon){
                    if(!sameHost(url,loadedUrl)||failed.contains(view)||submitted.contains(view))return;
                    int generation=generations.getOrDefault(view,0)+1;generations.put(view,generation);attempts.put(view,0);
                    handler.postDelayed(() -> pollForm(view,url,plate,status,name,generation),3500);
                }
                @Override public void onPageFinished(WebView view,String loadedUrl){
                    if (!sameHost(url,loadedUrl)) return;
                    if(failed.contains(view))return;
                    if(submitted.contains(view)){handler.postDelayed(() -> readResult(view,status,name),1800);return;}
                    if (url.contains("sat.gob.pe/VirtualSAT")) view.evaluateJavascript("(function(){if(location.pathname.toLowerCase().endsWith('/papeletas.aspx'))return;var ds=[document];document.querySelectorAll('frame,iframe').forEach(function(f){try{if(f.contentDocument)ds.push(f.contentDocument);}catch(e){}});for(var d of ds){var a=Array.from(d.querySelectorAll('a[href]')).find(function(a){return a.href.includes('/VirtualSAT/modulos/papeletas.aspx?')});if(a){location.href=a.href;return;}}})();",null);
                    attempts.put(view, 0);
                    int generation=generations.getOrDefault(view,0)+1;generations.put(view,generation);
                    handler.postDelayed(() -> pollForm(view,url,plate,status,name,generation),2500);
                }
                @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){failed.add(v);status.setText("No disponible · error de conexión");}}
                @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,android.webkit.WebResourceResponse response){if(r.isForMainFrame()&&response.getStatusCode()>=400){failed.add(v);status.setText("No disponible · HTTP "+response.getStatusCode());}}
                @Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler h,android.net.http.SslError e){h.cancel();failed.add(v);status.setText("No disponible · conexión segura rechazada");}
                @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String s=r.getUrl().getScheme();return !"https".equals(s)&&!"http".equals(s);}
            });
            web.setWebChromeClient(new android.webkit.WebChromeClient(){
                @Override public boolean onJsAlert(WebView view,String pageUrl,String message,android.webkit.JsResult result){
                    if(!sameHost(url,pageUrl))return false;
                    if(message.toLowerCase(java.util.Locale.ROOT).matches("(?s).*(captcha|c[oó]digo).*(incorrect|inv[aá]lid|no es v[aá]lido|error).*")){
                        submitted.remove(view);web.evaluateJavascript("window.__autocarSubmitted=false",null);
                        LinearLayout p=verificationPanels.get(view);p.removeAllViews();p.setVisibility(View.GONE);status.setText("Código rechazado · revisa la nueva imagen");handler.postDelayed(() -> captureVerification(view,status,name),1000);
                    }else {failed.add(view);status.setText("Mensaje del portal · "+message);}
                    result.confirm();return true;
                }
            });
            if(url.contains("www.gob.pe/53428")){failed.add(web);status.setText("No automatizado · Trujillo requiere cuenta o datos adicionales");}
            else handler.postDelayed(() -> {status.setText("Cargando…");web.loadUrl(url);handler.postDelayed(() -> {if(!isFinishing()&&!isDestroyed()&&!submitted.contains(web)&&!failed.contains(web)&&(web.getUrl()==null||web.getUrl().equals("about:blank"))){failed.add(web);web.stopLoading();status.setText("No disponible · el portal no terminó de cargar");}},60000);},i*1800L);
        }
        handler.postDelayed(updatePdf,1500);
    }

    private boolean sameHost(String expected,String actual){return actual!=null && FinesPortalAdapter.sameSource(expected,actual);}

    private void pollForm(WebView web,String url,String plate,TextView status,String name,int generation) {
        if(isFinishing()||isDestroyed()||!sameHost(url,web.getUrl())||submitted.contains(web)||failed.contains(web)||generations.getOrDefault(web,0)!=generation)return;
        int count=attempts.getOrDefault(web,0)+1; attempts.put(web,count);
        fillAndConsult(web,plate,status,name);
        if(count<30)handler.postDelayed(() -> pollForm(web,url,plate,status,name,generation),2000);
    }

    private void fillAndConsult(WebView web,String plate,TextView status,String name){
        if (submitted.contains(web)) return;
        FinesPortalAdapter adapter=FinesPortalAdapter.forUrl(web.getUrl()==null?"":web.getUrl());
        String script=adapter.fillScript(plate);
        web.evaluateJavascript(script, result -> {
            if(isFinishing()||isDestroyed())return;
            if("\"captcha\"".equals(result)){status.setText(name+" · Verificación pendiente");captureVerification(web,status,name);}
            else if("\"interactive\"".equals(result)){status.setText("Verificación interactiva oficial");showInteractiveVerification(web,name);}
            else if("\"response\"".equals(result)){submitted.add(web);readResult(web,status,name);}
            else if("\"waiting\"".equals(result))status.setText("Preparando búsqueda por placa…");
            else if("\"blocked\"".equals(result)){failed.add(web);status.setText("Acceso restringido por el portal · consulta no completada");}
            else if("\"sent\"".equals(result)){submitted.add(web);status.setText("Consultando…");handler.postDelayed(() -> readResult(web,status,name),5000);}
            else if("\"manual\"".equals(result))status.setText(name+" · Placa rellenada; completa el formulario");
            else {if(attempts.getOrDefault(web,0)>=30)failed.add(web);status.setText(attempts.getOrDefault(web,0)>=30?"No se pudo preparar la consulta · revisar fuente":"Buscando formulario de placa…");}
        });
    }

    private void captureVerification(WebView web,TextView status,String name) {
        LinearLayout panel=verificationPanels.get(web);
        if(panel==null||panel.getChildCount()>0)return;
        FinesPortalAdapter adapter=FinesPortalAdapter.forUrl(web.getUrl()==null?"":web.getUrl());
        // Only display the existing image. No OCR, solving, token modification or new challenge request.
        String script="(function(){try{var ds=[document];document.querySelectorAll('iframe').forEach(function(f){try{if(f.contentDocument)ds.push(f.contentDocument);}catch(e){}});"
                +"var img="+(adapter.image.isEmpty()?"null":"document.querySelector("+FinesPortalAdapter.quote(adapter.image)+")")+";if(img&&img.tagName==='IFRAME'){try{img=img.contentDocument.querySelector('img');}catch(e){img=null;}}var input=null;for(var d of ds){if(img)break;img=d.querySelector('#imgCaptcha,img.captcha-img,img[alt*=captcha i],img[src*=captcha i]');"
                +"if(!img&&d!==document&&d.querySelectorAll('img').length===1)img=d.querySelector('img');if(img)break;}"
                +"input="+(adapter.code.isEmpty()?"null":"document.querySelector("+FinesPortalAdapter.quote(adapter.code)+")")+";if(!input)input=Array.from(document.querySelectorAll('input')).find(function(e){return !['hidden','checkbox','submit','button'].includes(e.type)&&/captcha|cod.*seguridad|txt.*codigo|txt.*cod/i.test((e.id||'')+' '+(e.name||'')+' '+(e.placeholder||''));});"
                +"if(!img||!input||!img.complete||!img.naturalWidth)return '';var c=document.createElement('canvas');c.width=img.naturalWidth;c.height=img.naturalHeight;if(c.width*c.height>1000000)return '';c.getContext('2d').drawImage(img,0,0);"
                +"window.__autocarCaptchaInput=input;window.__autocarCaptchaImage=img.src;return c.toDataURL('image/png');}catch(e){return '';}})();";
        web.evaluateJavascript(script,encoded -> {
            if(isFinishing()||isDestroyed()||panel.getChildCount()>0)return;
            try {
                Object decoded=new org.json.JSONTokener(encoded).nextValue();
                if(!(decoded instanceof String)||!((String)decoded).startsWith("data:image/png;base64,")) {
                    status.setText("Preparando imagen de verificación…");if(attempts.getOrDefault(web,0)>=25)showInteractiveVerification(web,name);return;
                }
                String data=(String)decoded; if(data.length()>2000000)return;
                byte[] bytes=android.util.Base64.decode(data.substring(data.indexOf(',')+1),android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length); if(bitmap==null)return;
                android.widget.ImageView image=new android.widget.ImageView(this); image.setImageBitmap(bitmap); image.setAdjustViewBounds(true); image.setContentDescription("Código de verificación oficial de "+name); LinearLayout captchaRow=new LinearLayout(this);captchaRow.setGravity(android.view.Gravity.CENTER_VERTICAL);panel.addView(captchaRow);captchaRow.addView(image,new LinearLayout.LayoutParams(dp(120),dp(48)));
                android.widget.EditText code=new android.widget.EditText(this);code.setHint("Código");code.setSingleLine(true);code.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);captchaRow.addView(code,new LinearLayout.LayoutParams(0,dp(48),1));codes.put(web,code);
                com.google.android.material.button.MaterialButton send=new com.google.android.material.button.MaterialButton(this);send.setText("Continuar consulta");send.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0,43,92)));send.setTextColor(Color.WHITE);send.setCornerRadius(dp(14));panel.addView(send);send.setVisibility(View.GONE);codeButtons.put(web,send);panel.setVisibility(View.VISIBLE);verificationCards.get(web).setVisibility(View.VISIBLE);focusVerification();updateCounts();
                send.setOnClickListener(v -> {
                    String answer=code.getText().toString().trim();if(answer.isEmpty()){code.setError("Introduce el código");return;}
                    send.setEnabled(false);
                    String submit="(function(){var e=window.__autocarCaptchaInput;if(!e||!e.isConnected)return 'stale';"
                            +"Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(e,"+org.json.JSONObject.quote(answer)+");e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));"
                            +"var f=e.form||document;var known="+(adapter.button.isEmpty()?"null":"document.querySelector("+FinesPortalAdapter.quote(adapter.button)+")")+";var bs=known?[known]:Array.from(f.querySelectorAll('button,input[type=submit],input[type=button]')).filter(function(b){return /^(buscar(?: papeletas)?|consultar|consulta|ver resultado|ingresar)$/i.test((b.innerText||b.value||'').trim())&&!b.disabled&&b.getClientRects().length;});if(bs.length!==1||bs[0].disabled)return 'manual';window.__autocarSubmitted=true;window.__autocarQueryAt=Date.now();bs[0].click();return 'sent';})();";
                    web.evaluateJavascript(submit,result -> {
                        if(isFinishing()||isDestroyed())return;
                        if("\"sent\"".equals(result)&&!status.getText().toString().startsWith("Código rechazado")){submitted.add(web);panel.setVisibility(View.GONE);verificationCards.get(web).setVisibility(View.GONE);updateCounts();status.setText("Validando código…");handler.postDelayed(() -> readResult(web,status,name),5000);}
                        else if("\"stale\"".equals(result)){panel.removeAllViews();codeButtons.remove(web);codes.remove(web);captureVerification(web,status,name);}
                        else {send.setEnabled(true);status.setText("Revisa los campos de la verificación oficial");}
                    });
                });
            } catch(Exception ignored){status.setText(name+" · Verificación disponible en el formulario oficial");}
        });
    }

    private void readResult(WebView web,TextView status,String name) {
        if(isFinishing()||isDestroyed())return;
        if(priorityIndices.containsKey(web)){pollPriority(web,status,name);return;}
        if(FinesPortalAdapter.host(web.getUrl()).equals("consultavehicular.sunarp.gob.pe")){readSunarp(web,status,name);return;}
        FinesPortalAdapter adapter=FinesPortalAdapter.forUrl(web.getUrl()==null?"":web.getUrl());
        String script="(function(){var t=(document.body&&document.body.innerText)||'';if(/(?:captcha|c[oó]digo).{0,70}(?:incorrecto|inv[aá]lido|no es v[aá]lido)/i.test(t))return JSON.stringify({error:true});"
                +"var resultPage=/resultadopapeleta_web\\.php|record_infracciones\\/buscar_placa$/.test(location.pathname);var nodes=document.querySelectorAll("+FinesPortalAdapter.quote(adapter.results.isEmpty()?"#gvDeudas,table.table,table.dataTable":adapter.results)+");"
                +"nodes=Array.from(nodes).filter(function(n){return n.getClientRects().length&&!n.querySelector('input:not([type=hidden])')&&(n.tagName!=='TABLE'||/LIBRE DE INFRACCIONES|sin registros|no.*papeletas/i.test(n.innerText)||Array.from(n.querySelectorAll('tbody tr, tr')).some(function(r){return r.querySelector('td')&&!r.closest('thead');}));});"
                +"var text=Array.from(nodes).map(function(n){return n.innerText}).join('\\n');if(!text&&resultPage){var area=document.querySelector('section.content,[data-role=content],main');text=area?area.innerText:t;}"
                +(adapter.plate.equals("#ctl00_MainBodyContent_txtPlaca")?"if(/Resultado de la b[uú]squeda|Listado de p[oó]lizas SOAT/i.test(t)&&t.replace(/[^A-Z0-9]/gi,'').toUpperCase().includes("+FinesPortalAdapter.quote(queryPlate)+")){var start=t.search(/Resultado de la b[uú]squeda/i);text=t.slice(start>=0?start:0);var end=text.search(/Coberturas del SOAT|Versi[oó]n Impresa/i);if(end>0)text=text.slice(0,end);text+='\\nSBS publica información con desfase; no confirma por sí sola cobertura actual.';}else text='';":"")
                +(adapter.plate.equals("#texFiltro")?"if(!text.replace(/[^A-Z0-9]/gi,'').toUpperCase().includes("+FinesPortalAdapter.quote(queryPlate)+"))text='';":"")
                +(adapter.plate.equals("#PlacaBusquedainputElemento")?"if(window.__autocarServerRejected)return JSON.stringify({failure:'La fuente no confirmó la consulta'});if(!window.__autocarServerResponse&&!text.replace(/[^A-Z0-9]/gi,'').toUpperCase().includes("+FinesPortalAdapter.quote(queryPlate)+"))text='';":"")
                +"var records=[];for(var table of nodes){var trs=Array.from(table.querySelectorAll('tr'));if(!trs.length)continue;var headers=Array.from(trs[0].querySelectorAll('th,td')).map(function(c){return c.innerText.trim()});if(!headers.some(function(h){return /papeleta|placa|infracci[oó]n|monto|importe|fecha/i.test(h)}))continue;for(var tr of trs.slice(1,7)){var cells=Array.from(tr.querySelectorAll('td'));if(cells.length!==headers.length)continue;var fields=cells.map(function(c,i){return {label:headers[i],value:c.innerText.trim()}}).filter(function(f){return f.value});if(fields.length)records.push(fields);}}"
                +"if(/^(cargando|consultando|procesando|espere)/i.test(text.trim()))text='';if(/no se pudo|error al procesar|ocurri[oó] un error|acceso denegado/i.test(text))return JSON.stringify({failure:text.slice(0,350)});return JSON.stringify({text:text.slice(0,100000),truncated:text.length>100000,records:records});})();";
        web.evaluateJavascript(script,value -> {
            if(isFinishing()||isDestroyed())return;
            try {
                Object raw=new org.json.JSONTokener(value).nextValue(); if(!(raw instanceof String))return;
                org.json.JSONObject data=new org.json.JSONObject((String)raw);
                if(data.has("failure")){failed.add(web);status.setText("Error del portal · "+data.optString("failure"));return;}
                if(data.optBoolean("error")) {
                    submitted.remove(web);LinearLayout panel=verificationPanels.get(web);panel.removeAllViews();panel.setVisibility(View.GONE);codes.remove(web);codeButtons.remove(web);
                    web.evaluateJavascript("window.__autocarSubmitted=false",null);
                    status.setText("Código rechazado · vuelve a verificar");
                    fillAndConsult(web,PlateUtils.normalize(getIntent().getStringExtra("plate")),status,name);return;
                }
                String text=data.optString("text").trim();TextView summary=summaries.get(web);
                if(!text.isEmpty()){
                    rawDetails.put(web,text.replace("\t"," | ")+(data.optBoolean("truncated")?"\nDetalle truncado por tamaño: revisa la fuente oficial para el contenido completo.":""));
                    org.json.JSONArray records=data.optJSONArray("records");StringBuilder formatted=new StringBuilder();
                    if(records!=null)for(int i=0;i<Math.min(records.length(),6);i++){
                        if(i>0)formatted.append("\n\n");formatted.append("REGISTRO ").append(i+1).append("\n");
                        org.json.JSONArray fields=records.getJSONArray(i);for(int j=0;j<fields.length();j++){
                            org.json.JSONObject field=fields.getJSONObject(j);String label=field.optString("label");
                            formatted.append(label.isEmpty()?"Dato":label).append(": ").append(field.optString("value")).append("\n");
                        }
                    }
                    summary.setText(formatted.length()>0?formatted.toString().trim():text);summary.setVisibility(View.VISIBLE);LinearLayout card=sourceCards.get(web);if(card.getParent()!=sourcesList){((android.view.ViewGroup)card.getParent()).removeView(card);sourcesList.addView(card);}resultEmpty.setVisibility(View.GONE);verificationCards.get(web).setVisibility(View.GONE);status.setText("Información recibida · verifica el detalle completo");updateCounts();
                }
                else if(resultAttempts.getOrDefault(web,0)<20&&submitted.contains(web)&&!failed.contains(web)){resultAttempts.put(web,resultAttempts.getOrDefault(web,0)+1);status.setText("Esperando respuesta del portal…");handler.postDelayed(() -> readResult(web,status,name),2000);}
                else {failed.add(web);status.setText("No disponible · el portal no entregó una respuesta verificable");}
            }catch(Exception ignored){status.setText("Resultado pendiente de verificar");}
        });
    }

    private static int priorityIndex(String url){String host=FinesPortalAdapter.host(url);return host.equals("consultavehicular.sunarp.gob.pe")?0:host.equals("servicios.sbs.gob.pe")?1:host.equals("webexterno.sutran.gob.pe")?2:host.equals("portal.mtc.gob.pe")||host.equals("rec.mtc.gob.pe")?3:-1;}
    private void pollPriority(WebView web,TextView status,String name){
        if(isFinishing()||isDestroyed()||priorityEngine==null)return;int index=priorityIndices.get(web);
        String message=priorityEngine.summary(index);if(!priorityEngine.complete(index)&&message!=null&&!message.equals(status.getText().toString()))status.setText(message);
        if(priorityEngine.error(index))failed.add(web);else failed.remove(web);
        if(priorityEngine.complete(index)){
            String detail=priorityEngine.detail(index);String summary=priorityEngine.summary(index);String content=detail!=null&&!detail.trim().isEmpty()?detail:summary;
            if(content!=null&&!content.trim().isEmpty()){rawDetails.put(web,content);TextView target=summaries.get(web);String preview=summary==null||summary.trim().isEmpty()?content:summary;if(!preview.equals(target.getText().toString()))target.setText(preview);target.setVisibility(fullDetails.get(web).getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);LinearLayout card=sourceCards.get(web);if(card.getParent()!=sourcesList){((android.view.ViewGroup)card.getParent()).removeView(card);sourcesList.addView(card);}resultEmpty.setVisibility(View.GONE);if(index==0&&priorityEngine.officialImage()!=null&&officialImages.get(web)!=priorityEngine.officialImage()){officialImages.put(web,priorityEngine.officialImage());hasPdfSignature=false;handler.removeCallbacks(updatePdf);handler.postDelayed(updatePdf,1500);}String label="Información recibida · "+(summary==null?name:summary);if(!label.equals(status.getText().toString()))status.setText(label);}
            verificationCards.get(web).setVisibility(View.GONE);updateCounts();handler.postDelayed(()->pollPriority(web,status,name),2500);return;
        }
        android.graphics.Bitmap captcha=priorityEngine.captcha(index);
        int hash=0;if(captcha!=null&&!captcha.isRecycled()){int[] pixels=new int[captcha.getWidth()*captcha.getHeight()];captcha.getPixels(pixels,0,captcha.getWidth(),0,0,captcha.getWidth(),captcha.getHeight());hash=java.util.Arrays.hashCode(pixels);}
        if(captcha!=null&&!captcha.isRecycled()&&!java.util.Objects.equals(priorityCaptchaHashes.get(web),hash)){
            priorityCaptchaHashes.put(web,hash);LinearLayout panel=verificationPanels.get(web);panel.removeAllViews();android.graphics.Bitmap display=captcha.copy(android.graphics.Bitmap.Config.ARGB_8888,false);android.graphics.Bitmap previous=shownPriorityCaptchas.put(web,display);
            android.widget.ImageView image=new android.widget.ImageView(this);image.setImageBitmap(display);if(previous!=null&&!previous.isRecycled())previous.recycle();image.setAdjustViewBounds(true);image.setContentDescription("Código de verificación oficial de "+name);panel.addView(image,new LinearLayout.LayoutParams(-1,dp(90)));
            android.widget.EditText code=new android.widget.EditText(this);code.setHint("Código");code.setSingleLine(true);panel.addView(code);codes.put(web,code);
            Button send=new Button(this);send.setVisibility(View.GONE);panel.addView(send);codeButtons.put(web,send);
            send.setOnClickListener(v->{String answer=code.getText().toString().trim();if(answer.isEmpty())return;priorityEngine.submit(index,answer);priorityCaptchaHashes.remove(web);send.setEnabled(false);verificationCards.get(web).setVisibility(View.GONE);panel.setVisibility(View.GONE);status.setText("Validando código…");updateCounts();});
            panel.setVisibility(View.VISIBLE);verificationCards.get(web).setVisibility(View.VISIBLE);focusVerification();updateCounts();
        }
        handler.postDelayed(()->pollPriority(web,status,name),750);
    }

    private void readSunarp(WebView web,TextView status,String name){
        if(readingImages.contains(web)||officialImages.containsKey(web))return;
        web.evaluateJavascript("(function(){var images=Array.from(document.querySelectorAll('img[src^=\"data:image\"]')).filter(function(i){return i.complete&&i.naturalWidth>400&&i.naturalHeight>200&&i.src.length>50000&&!/captcha/i.test(i.id+' '+i.alt)});images.sort(function(a,b){return b.src.length-a.src.length});return images.length?images[0].src:'';})();",value->{
            if(isFinishing()||isDestroyed())return;
            try{Object decoded=new org.json.JSONTokener(value).nextValue();String data=decoded instanceof String?(String)decoded:"";
                if(data.isEmpty()){int tries=resultAttempts.getOrDefault(web,0);if(tries<30){resultAttempts.put(web,tries+1);status.setText("Esperando informe SUNARP…");handler.postDelayed(()->readSunarp(web,status,name),2000);}else {failed.add(web);status.setText("No disponible · SUNARP no entregó un informe verificable");}return;}
                if(data.length()>8000000)throw new IllegalArgumentException();byte[] bytes=android.util.Base64.decode(data.substring(data.indexOf(',')+1),android.util.Base64.DEFAULT);android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap==null)throw new IllegalArgumentException();
                readingImages.add(web);com.google.mlkit.vision.text.TextRecognizer recognizer=com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(text->{
                    if(isFinishing()||isDestroyed()){bitmap.recycle();return;}String normalized=text.getText().replaceAll("[^A-Za-z0-9]","").toUpperCase(java.util.Locale.ROOT);
                    if(!normalized.contains(queryPlate)){bitmap.recycle();failed.add(web);status.setText("No se pudo verificar la placa del informe SUNARP");return;}
                    officialImages.put(web,bitmap);String detail="Informe SUNARP recibido para "+PlateUtils.display(queryPlate)+".\nTexto extraído de la imagen; verifica los campos en el documento original:\n"+text.getText();rawDetails.put(web,detail);TextView summary=summaries.get(web);summary.setText(detail);summary.setVisibility(View.VISIBLE);
                    LinearLayout card=sourceCards.get(web);if(card.getParent()!=sourcesList){((android.view.ViewGroup)card.getParent()).removeView(card);sourcesList.addView(card);}android.widget.ImageView original=new android.widget.ImageView(this);original.setImageBitmap(bitmap);original.setAdjustViewBounds(true);original.setContentDescription("Informe oficial SUNARP");card.addView(original,new LinearLayout.LayoutParams(-1,-2));verificationCards.get(web).setVisibility(View.GONE);resultEmpty.setVisibility(View.GONE);status.setText("Información recibida · informe SUNARP");updateCounts();
                }).addOnFailureListener(error->{if(!isDestroyed()){failed.add(web);status.setText("No se pudo leer el informe SUNARP");}bitmap.recycle();}).addOnCompleteListener(task->{readingImages.remove(web);recognizer.close();});
            }catch(Exception e){failed.add(web);status.setText("No se pudo leer el informe SUNARP");}
        });
    }

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView sectionButton(String text,int section,LinearLayout tabs){
        TextView link=new TextView(this);link.setText(text);link.setTextSize(12);link.setTextColor(Color.rgb(85,99,116));link.setGravity(android.view.Gravity.CENTER_VERTICAL);link.setPadding(dp(4),0,dp(4),0);tabs.addView(link,new LinearLayout.LayoutParams(0,dp(48),1));link.setOnClickListener(v -> showSection(selectedSection==section?-1:section));return link;
    }
    private ScrollView sectionScroll(LinearLayout list,android.view.ViewGroup sections){list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(4),0,dp(4),dp(4));ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(list);sections.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return scroll;}
    private void showSection(int section){
        selectedSection=section;
        resultsScroll.setVisibility(section==0?View.VISIBLE:View.GONE);
        verificationScroll.setVisibility(section==1?View.VISIBLE:View.GONE);
        sourcesScroll.setVisibility(section==2?View.VISIBLE:View.GONE);
        if(queryHeader!=null)queryHeader.setVisibility(View.GONE);
        if(queryTabs!=null)queryTabs.setVisibility(View.VISIBLE);
        if(reportFooter!=null)reportFooter.setVisibility(section==0?View.VISIBLE:View.GONE);
        resultTab.setVisibility(View.GONE);
        resultTab.setTextColor(Color.rgb(85,99,116));verificationTab.setTextColor(Color.rgb(85,99,116));
    }
    private void focusVerification(){verificationFocused=true;}
    private void updateCounts(){
        if(resultTab==null)return;int results=0,pending=0,errors=0,finished=0;
        for(WebView web:portals){
            TextView detail=summaries.get(web);boolean received=detail!=null&&!detail.getText().toString().trim().isEmpty();
            boolean error=failed.contains(web);if(received)results++;if(error)errors++;if(received||error)finished++;
            if(error){LinearLayout result=sourceCards.get(web);if(result!=null&&result.getParent()!=sourcesList){((android.view.ViewGroup)result.getParent()).removeView(result);sourcesList.addView(result);}}
            LinearLayout card=verificationCards.get(web);if(card!=null&&card.getVisibility()==View.VISIBLE)pending++;
            TextView external=externalLinks.get(web);if(external!=null)external.setVisibility(error?View.VISIBLE:View.GONE);
            TextView full=fullDetails.get(web);Button button=detailButtons.get(web);
            if(full!=null&&received){String content=rawDetails.getOrDefault(web,detail.getText().toString());if(!content.equals(full.getText().toString()))full.setText(content);if(button!=null)button.setVisibility(View.VISIBLE);}
        }
        int total=sourceNames.length;int percent=total==0?0:Math.min(100,finished*100/total);
        if(queryProgress!=null)queryProgress.setProgress(percent,true);
        if(queryProgressText!=null)queryProgressText.setText(percent+"% · "+results+" con datos"+(errors>0?" · "+errors+" sin completar":"")+(pending>0?" · "+pending+" por verificar":"")+(finished==total?" · Finalizado":""));
        resultTab.setText("Ver resultados");verificationTab.setText("Verificar ("+pending+")");verificationTab.setVisibility(pending>0?View.VISIBLE:View.GONE);sourceTab.setText("Fuentes");
        java.util.ArrayList<GeneralVehicleReport.Entry> entries=new java.util.ArrayList<>();
        for(int i=0;i<portals.size();i++){WebView web=portals.get(i);String brief=summaries.get(web)==null?"":summaries.get(web).getText().toString();entries.add(new GeneralVehicleReport.Entry(sourceNames[i],sourceUrls[i],reportStatuses.get(i).getText().toString(),brief,rawDetails.getOrDefault(web,brief)));}
        if(generalSummary!=null){String text=GeneralVehicleReport.summary(queryPlate,entries);if(!text.equals(generalSummary.getText().toString()))generalSummary.setText(text);}
        if(generalDetail!=null){String text=GeneralVehicleReport.detail(queryPlate,entries);if(!text.equals(generalDetail.getText().toString()))generalDetail.setText(text);}
        if(resultEmpty!=null&&results==0){resultEmpty.setVisibility(View.VISIBLE);resultEmpty.setText(finished==total?"No se obtuvo información verificable. Revisa los errores en Fuentes.":pending>0?"Completa los códigos para continuar. Las demás fuentes siguen consultándose.":"Consultando… Los resultados aparecerán automáticamente.");}
        verificationScroll.setVisibility(pending>0&&selectedSection==1?View.VISIBLE:View.GONE);
        if(pending==0&&selectedSection==1)showSection(-1);
        if(total>0&&portals.size()==total&&finished==total&&pending==0&&!completionReported){completionReported=true;completionListener.run();}
    }
    private void showInteractiveVerification(WebView web,String name){
        LinearLayout panel=verificationPanels.get(web);if(panel==null||panel.getChildCount()>0)return;
        TextView hint=new TextView(this);hint.setText("Esta fuente usa una verificación interactiva. Complétala en el recuadro oficial; no se resuelve automáticamente.");hint.setTextSize(13);hint.setTextColor(Color.rgb(107,114,128));hint.setPadding(0,dp(8),0,dp(10));panel.addView(hint);
        ((android.view.ViewGroup)web.getParent()).removeView(web);web.getSettings().setUseWideViewPort(false);web.getSettings().setLoadWithOverviewMode(false);panel.addView(web,new LinearLayout.LayoutParams(-1,dp(520)));web.setVisibility(View.VISIBLE);panel.setVisibility(View.VISIBLE);verificationCards.get(web).setVisibility(View.VISIBLE);focusVerification();updateCounts();
        web.evaluateJavascript("(function(){var e=document.querySelector('.g-recaptcha,.h-captcha,iframe[src*=recaptcha],iframe[src*=turnstile]');if(e)e.scrollIntoView({block:'center'});})();",null);
    }
    private void resumeInteractive(WebView web){
        int index=portals.indexOf(web);if(index<0)return;TextView status=reportStatuses.get(index);String name=sourceNames[index];
        FinesPortalAdapter adapter=FinesPortalAdapter.forUrl(web.getUrl()==null?"":web.getUrl());
        if(adapter.button.isEmpty())return;
        String script="(function(){var response=document.querySelector('[name=\"g-recaptcha-response\"],[name=\"h-captcha-response\"],[name=\"cf-turnstile-response\"]');if(!response||!response.value)return 'pending';var b=document.querySelector("+FinesPortalAdapter.quote(adapter.button)+");if(!b||b.disabled)return 'pending';window.__autocarSubmitted=true;b.click();return 'sent';})();";
        web.evaluateJavascript(script,value -> {if(isFinishing()||isDestroyed())return;if("\"sent\"".equals(value)){submitted.add(web);verificationCards.get(web).setVisibility(View.GONE);status.setText("Consultando…");updateCounts();handler.postDelayed(() -> readResult(web,status,name),5000);}else status.setText("Completa la verificación en el recuadro oficial");});
    }
    private void savePdf(boolean share) {
        if(isFinishing()||isDestroyed())return;
        ArrayList<FinesPdfReport.Source> snapshot=new ArrayList<>();int count=0,signature=1;
        for(int i=0;i<reportStatuses.size();i++) {
            String detail=rawDetails.getOrDefault(portals.get(i),reportDetails.get(i).getText().toString());if(!detail.trim().isEmpty())count++;
            snapshot.add(new FinesPdfReport.Source(sourceNames[i],sourceUrls[i],reportStatuses.get(i).getText().toString(),detail,officialImages.get(portals.get(i))));
            signature=31*signature+java.util.Objects.hash(sourceNames[i],reportStatuses.get(i).getText().toString(),detail);
        }
        if(!share&&hasPdfSignature&&signature==pdfSignature&&latestPdf!=null&&latestPdf.isFile())return;
        try {
            java.io.File previous=latestPdf;boolean preserve=latestPdfShared;latestPdf=FinesPdfReport.write(this,queryPlate,snapshot);
            pdfSignature=signature;hasPdfSignature=true;
            if(previous!=null&&!preserve)previous.delete();latestPdfShared=share;
            reportState.setText("PDF actualizado · "+count+" / "+sourceNames.length+" fuentes con datos"+(count<sourceNames.length?" · Parcial":""));
            if(share){android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".reports",latestPdf);
                android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/pdf").putExtra(android.content.Intent.EXTRA_STREAM,uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(android.content.ClipData.newRawUri("Informe AUTOCAR",uri));startActivity(android.content.Intent.createChooser(intent,"Guardar o compartir informe"));}
        }catch(java.io.IOException|IllegalArgumentException error){reportState.setText("No se pudo generar el PDF. Vuelve a intentarlo.");}
    }

    private void openResultsPage(){
        if(isDestroyed())return;updateCounts();savePdf(false);
        try{
            java.io.File folder=new java.io.File(getCacheDir(),"reports");if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException();
            java.io.File snapshot=new java.io.File(folder,"general_"+java.util.UUID.randomUUID()+".json");
            org.json.JSONObject data=new org.json.JSONObject().put("plate",queryPlate).put("summary",generalSummary.getText().toString()).put("detail",generalDetail.getText().toString()).put("progress",queryProgressText.getText().toString()).put("pdf",latestPdf==null?"":latestPdf.getName()).put("created",System.currentTimeMillis());
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(snapshot)){out.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            latestPdfShared=true;
            host.startActivity(new android.content.Intent(host,GeneralReportActivity.class).putExtra("snapshot",snapshot.getName()));
        }catch(Exception e){android.widget.Toast.makeText(host,"No se pudo abrir el informe. Reintenta.",android.widget.Toast.LENGTH_SHORT).show();}
    }
    /** Hidden WebViews need a viewport, but must never determine Home's visible height. */
    private static final class InlineQueryFrame extends android.widget.FrameLayout {
        InlineQueryFrame(android.content.Context context){super(context);setClipChildren(true);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            if(getChildCount()<2){super.onMeasure(widthSpec,heightSpec);return;}
            View content=getChildAt(1);
            content.measure(widthSpec,View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            int width=View.resolveSize(content.getMeasuredWidth(),widthSpec);
            int height=View.resolveSize(content.getMeasuredHeight(),heightSpec);
            setMeasuredDimension(width,height);
            int engineHeight=Math.max(height,Math.round(480*getResources().getDisplayMetrics().density));
            getChildAt(0).measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(engineHeight,View.MeasureSpec.EXACTLY));
        }
    }
    public void dispose(){disposed=true;handler.removeCallbacksAndMessages(null);for(WebView w:portals)if(!priorityIndices.containsKey(w)){w.stopLoading();w.destroy();}if(priorityEngine!=null)priorityEngine.dispose();}
}
