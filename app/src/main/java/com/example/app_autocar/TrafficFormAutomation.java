package com.example.app_autocar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.WebView;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import org.json.JSONObject;

/**
 * Rellena los formularios asistidos de tránsito (Cinemómetro de SUTRAN, Captura y Depósito de SAT
 * Lima) y resuelve su CAPTCHA de imagen con el reconocimiento local de ddddocr.
 *
 * <p>Solo se completan campos y se pulsa el botón propio del formulario oficial; nunca se alteran
 * verificaciones interactivas (reCAPTCHA, hCaptcha, Turnstile) ni se fabrican tokens. Cuando el
 * código no se puede leer o el portal lo rechaza, se pide una imagen nueva y, agotados los intentos,
 * el formulario queda disponible para que el usuario escriba el código a mano.
 */
final class TrafficFormAutomation {
  private TrafficFormAutomation() {}

  private static final String SUTRAN_HOST = "webexterno.sutran.gob.pe";
  private static final String SUTRAN_PATH = "/WebExterno/Pages/frmPapeletasCinemometro.aspx";
  private static final String SAT_HOST = "www.sat.gob.pe";
  private static final String SAT_CAPTURAS_PATH = "/VirtualSAT/modulos/Capturas.aspx";
  private static final String SAT_DEPOSITO_PATH = "/VirtualSAT/modulos/ConsultaDeposito.aspx";
  private static final String SAT_TRIBUTOS_PATH = "/VirtualSAT/modulos/BusquedaTributario.aspx";
  private static final String SAT_PRINCIPAL_PATH = "/VirtualSAT/principal.aspx";

  /** Envíos automáticos permitidos antes de dejar el formulario al usuario. */
  static final int MAX_SOLVES = 3;
  /** Imágenes nuevas que se piden cuando el reconocimiento no es fiable. */
  static final int MAX_UNREADABLE = 3;
  private static final int MAX_MODE_SWITCHES = 2;
  private static final int CAPTURE_RETRIES = 8;
  private static final long CAPTURE_DELAY_MS = 800;
  private static final float MIN_CONFIDENCE = 0.5f;
  private static final int MIN_TEXT_LENGTH = 3;

  private static final Handler HANDLER = new Handler(Looper.getMainLooper());
  private static final Map<WebView, State> STATES = new WeakHashMap<>();

  private static final class State {
    final String kind;
    final String path;
    String plate = "";
    String dni = "";
    String digit = "";
    boolean solve;
    int solves;
    int unreadable;
    int captures;
    int modeSwitches;
    String solvedSrc;
    String pendingSrc;
    boolean stopped;

    State(String kind, String path) {
      this.kind = kind;
      this.path = path;
    }
  }

  /** Punto de entrada compartido: reconoce el CAPTCHA y envía el formulario. */
  static void fill(WebView webView, String url, String plate) {
    prepare(webView, url, plate, true);
  }

  /**
   * Rellena los campos del formulario y prepara la imagen del CAPTCHA. Con {@code solve} en falso
   * solo se deja listo para enviar el código; lo usan las pruebas para no depender del OCR.
   */
  static void prepare(WebView webView, String url, String plate, boolean solve) {
    if (webView == null || url == null) return;
    Uri page = Uri.parse(url);
    if (!"https".equals(page.getScheme())) return;
    int port = page.getPort();
    if (port != -1 && port != 443) return;
    String host = page.getHost();
    String path = page.getPath();
    String kind = null;
    if (SUTRAN_HOST.equals(host) && SUTRAN_PATH.equals(path)) kind = "sutran";
    else if (SAT_HOST.equals(host)
        && (SAT_CAPTURAS_PATH.equals(path) || SAT_DEPOSITO_PATH.equals(path))) kind = "sat";
    else if (SAT_HOST.equals(host) && SAT_TRIBUTOS_PATH.equals(path)) kind = "tributos";
    else if (SAT_HOST.equals(host) && SAT_PRINCIPAL_PATH.equals(path)) kind = "bootstrap";
    if (kind == null) return;

    State state = STATES.get(webView);
    if (state == null || !state.path.equals(path) || !kind.equals(state.kind)) {
      state = new State(kind, path);
      STATES.put(webView, state);
    }
    if (state.stopped) return;

    String normalized = PlateUtils.normalize(plate);
    state.plate = PlateUtils.isValidPeruvianPlate(normalized) ? normalized : "";
    if (state.kind.equals("sutran") || state.kind.equals("tributos")) {
      android.content.SharedPreferences profile =
          webView.getContext().getSharedPreferences("traffic_identity", Context.MODE_PRIVATE);
      state.dni = profile.getString("dni", "").trim();
      state.digit = profile.getString("digit", "").trim();
    }
    state.solve = solve;
    state.captures = 0;
    if (state.kind.equals("bootstrap")) {
      // La página es un frameset con una sesión; se navega directamente al formulario de tributos.
      navigateToTributos(webView, state);
      return;
    }
    boolean hasData;
    if (state.kind.equals("sutran")) {
      hasData = !state.dni.isEmpty() || !state.plate.isEmpty();
    } else if (state.kind.equals("tributos")) {
      hasData = !state.dni.isEmpty();
    } else {
      hasData = !state.plate.isEmpty();
    }
    if (!hasData) {
      state.stopped = true;
      return;
    }
    capture(webView, state);
  }

  /** Envía el código ya reconocido a un formulario preparado por {@link #prepare}. */
  static void submit(WebView webView, String code) {
    State state = STATES.get(webView);
    if (state == null || state.stopped) return;
    submit(webView, state, code);
  }

  /** Olvida el estado de un WebView que se va a destruir. */
  static void clear(WebView webView) {
    if (webView == null) return;
    State state = STATES.remove(webView);
    if (state != null) state.stopped = true;
  }

  private static void capture(WebView webView, State state) {
    if (state.stopped) return;
    webView.evaluateJavascript(
        prepareScript(state),
        encoded -> {
          if (STATES.get(webView) != state || state.stopped) return;
          JSONObject response = decodeObject(encoded);
          if (response == null) return;
          String result = response.optString("state");
          if ("switching".equals(result)) {
            state.modeSwitches++;
            return;
          }
          if ("done".equals(result)) {
            state.stopped = true;
            return;
          }
          if ("captcha".equals(result)) {
            if (state.solves == 0 && state.unreadable < MAX_UNREADABLE) {
              state.unreadable++;
              state.modeSwitches = 0;
              if (state.kind.equals("sat") || state.kind.equals("tributos")) {
                webView.evaluateJavascript(
                    "(function(){try{var i=document.querySelector('#ctl00_cplPrincipal_txtCaptcha');"
                        + "if(i)i.value='';var r=document.querySelector('#ctl00_cplPrincipal_ibtnRefresh');"
                        + "if(r)r.click();}catch(e){}})();",
                    r -> {});
                HANDLER.postDelayed(() -> capture(webView, state), CAPTURE_DELAY_MS * 2);
              } else {
                webView.reload();
              }
            } else {
              state.stopped = true;
            }
            return;
          }
          if ("waiting".equals(result)) {
            if (++state.captures < CAPTURE_RETRIES) {
              HANDLER.postDelayed(() -> capture(webView, state), CAPTURE_DELAY_MS);
            } else {
              state.stopped = true;
            }
            return;
          }
          if (!"ready".equals(result)) return;
          String src = response.optString("src");
          if (src.equals(state.solvedSrc)) return;
          if (state.solves >= MAX_SOLVES || !state.solve) return;
          Bitmap bitmap = decodeImage(response.optString("image"));
          if (bitmap == null) return;
          state.pendingSrc = src;
          DdddOcrEngine.recognizeAsync(
              webView.getContext(),
              bitmap,
              recognition -> {
                if (STATES.get(webView) != state || state.stopped) return;
                String code = recognition == null ? "" : normalize(recognition.text);
                if (recognition == null
                    || code.length() < MIN_TEXT_LENGTH
                    || recognition.confidence < MIN_CONFIDENCE) {
                  onUnreadable(webView, state);
                  return;
                }
                HANDLER.post(() -> submit(webView, state, code));
              });
        });
  }

  private static void submit(WebView webView, State state, String code) {
    if (state.stopped || state.solves >= MAX_SOLVES) return;
    if (STATES.get(webView) != state) return;
    webView.evaluateJavascript(
        submitScript(state, code),
        encoded -> {
          if (STATES.get(webView) != state || state.stopped) return;
          JSONObject response = decodeObject(encoded);
          String result = response == null ? "" : response.optString("state");
          if ("sent".equals(result)) {
            state.solves++;
            state.solvedSrc = state.pendingSrc;
          } else if ("stale".equals(result) || "nobutton".equals(result)) {
            if (++state.captures < CAPTURE_RETRIES) {
              HANDLER.postDelayed(() -> capture(webView, state), CAPTURE_DELAY_MS);
            }
          }
        });
  }

  /**
   * El reconocimiento no fue fiable: se pide una imagen nueva mientras la página no se haya enviado
   * todavía. Una vez enviada, recargar repetiría la consulta, así que se deja el código a mano.
   */
  private static void onUnreadable(WebView webView, State state) {
    state.unreadable++;
    if (state.solves == 0 && state.unreadable < MAX_UNREADABLE) {
      state.modeSwitches = 0;
      if (state.kind.equals("sat") || state.kind.equals("tributos")) {
        webView.evaluateJavascript(
            "(function(){try{var i=document.querySelector('#ctl00_cplPrincipal_txtCaptcha');"
                + "if(i)i.value='';var r=document.querySelector('#ctl00_cplPrincipal_ibtnRefresh');"
                + "if(r)r.click();}catch(e){}})();",
            result -> {});
        HANDLER.postDelayed(() -> capture(webView, state), CAPTURE_DELAY_MS * 2);
      } else {
        webView.reload();
      }
      return;
    }
    state.stopped = true;
    try {
      android.widget.Toast.makeText(
          webView.getContext(),
          "CAPTCHA no resuelto automáticamente: complétalo a mano",
          android.widget.Toast.LENGTH_SHORT)
          .show();
    } catch (Exception ignored) {
    }
  }

  private static void navigateToTributos(WebView webView, State state) {
    if (state.stopped) return;
    webView.evaluateJavascript(
        "(function(){try{var m=new URLSearchParams(location.search).get('mysession');"
            + "if(m){location.href='/VirtualSAT/modulos/BusquedaTributario.aspx?mysession='"
            + "+encodeURIComponent(m)+'&tri=T';return;} "
            + "var f=document.querySelector('frame');if(f&&f.src){var u=new URL(f.src,location.href);"
            + "var mm=u.searchParams.get('mysession');if(mm)location.href='/VirtualSAT/modulos/BusquedaTributario.aspx?mysession='"
            + "+encodeURIComponent(mm)+'&tri=T';} }catch(e){} })();",
        result -> {});
    state.stopped = true;
  }

  private static String prepareScript(State state) {
    StringBuilder js = new StringBuilder();
    js.append("(function(){try{");
    js.append("if(location.origin!==")
        .append(JSONObject.quote("https://" + (state.kind.equals("sutran") ? SUTRAN_HOST : SAT_HOST)))
        .append("||location.pathname!==")
        .append(JSONObject.quote(state.path))
        .append(")return JSON.stringify({state:'other'});");
    js.append(
        "function set(id,value){var e=document.getElementById(id);"
            + "if(!e||value===undefined||value===null||value==='')return;"
            + "if(e.tagName==='SELECT'){if(e.value!==value){e.value=value;"
            + "e.dispatchEvent(new Event('change',{bubbles:true}));}return;}"
            + "if(e.type!=='text'&&e.tagName!=='TEXTAREA')return;if(e.value===value)return;"
            + "var d=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');"
            + "if(d&&d.set)d.set.call(e,value);else e.value=value;"
            + "e.dispatchEvent(new Event('input',{bubbles:true}));"
            + "e.dispatchEvent(new Event('change',{bubbles:true}));}");
    if (state.kind.equals("sutran")) {
      if (state.dni.isEmpty()) {
        // Sin documento guardado se consulta por placa, como el flujo anterior de la app.
        if (state.modeSwitches < MAX_MODE_SWITCHES) {
          js.append(
              "var modeP=document.querySelector('input[type=radio][name=rbtListMovimiento][value=P]');");
          js.append("if(modeP&&!modeP.checked){modeP.click();return JSON.stringify({state:'switching'});}");
        }
        js.append("set('txtPlaca',").append(JSONObject.quote(state.plate)).append(");");
      } else {
        js.append("var type=document.getElementById('ddlTipoBusqueda');if(type)type.value='2';");
        js.append("set('TxtBuscar',").append(JSONObject.quote(state.dni)).append(");");
        js.append("set('txtCodigoVerificador',").append(JSONObject.quote(state.digit)).append(");");
      }
    } else if (state.kind.equals("tributos")) {
      js.append("var type=document.getElementById('ctl00_cplPrincipal_ddlTipoDocu');if(type)type.value='2';");
      js.append("set('ctl00_cplPrincipal_txtDocumento',").append(JSONObject.quote(state.dni)).append(");");
    } else {
      js.append("set('ctl00_cplPrincipal_txtPlaca',")
          .append(JSONObject.quote(state.plate))
          .append(");");
    }
    if (state.kind.equals("sutran")) {
      js.append("var msg=(document.querySelector('#LblMensaje')||{}).innerText||'';");
      js.append(
          "if(document.querySelector('#gvDeudas')||/no (se"
              + " )?(registra|tiene|cuenta con|presenta)|no se"
              + " (encontr|encuentra)|sin (papeletas|infracciones|resultados)/i.test(msg))"
              + "return JSON.stringify({state:'done'});");
    } else if (state.kind.equals("tributos")) {
      js.append(
          "var msg=(document.querySelector('#ctl00_cplPrincipal_lblMensaje')||{}).innerText||'';");
      js.append(
          "var cant=(document.querySelector('#ctl00_cplPrincipal_lblMensajeCantidad')||{}).innerText||'';");
      js.append(
          "if(cant.trim()||/no (se registra|se encontro|cuenta con|resultad)/i.test(msg))"
              + "return JSON.stringify({state:'done'});");
    } else {
      js.append(
          "var vacio=(document.querySelector('#ctl00_cplPrincipal_lblMensajeVacio')||{}).innerText||'';");
      js.append(
          "var msg=(document.querySelector('#ctl00_cplPrincipal_lblMensaje')||{}).innerText||'';");
      js.append(
          "var capchaErr=(document.querySelector('#ctl00_cplPrincipal_lblMensajeCapcha')||{}).innerText||'';");
      js.append(
          "var captures=document.querySelector('#ctl00_cplPrincipal_divCapturas');"
              + "if(capchaErr.trim())return JSON.stringify({state:'captcha'});"
              + "if(vacio.trim()||msg.trim()||(captures&&captures.innerText.trim()))"
              + "return JSON.stringify({state:'done'});");
    }
    boolean isSutran = state.kind.equals("sutran");
    js.append("var img=")
        .append(
            isSutran
                ? "document.querySelector('#iimage')"
                : "document.querySelector('img.captcha_class,img[alt=\"Visual verification\"]')")
        .append(";");
    js.append(
        "if(img&&img.tagName==='IFRAME'){try{img=img.contentDocument.querySelector('img');}catch(e){img=null;}}");
    js.append("var input=document.querySelector(")
        .append(
            JSONObject.quote(
                isSutran ? "#TxtCodImagen" : "#ctl00_cplPrincipal_txtCaptcha"))
        .append(");");
    js.append("if(!img||!input)return JSON.stringify({state:'waiting'});");
    js.append("if(!img.complete||!img.naturalWidth)return JSON.stringify({state:'waiting'});");
    js.append("var src=img.currentSrc||img.src||'';");
    js.append(
        "var c=document.createElement('canvas');c.width=img.naturalWidth;c.height=img.naturalHeight;");
    js.append("c.getContext('2d').drawImage(img,0,0);");
    js.append("var data=c.toDataURL('image/png');");
    js.append("window.__autocarTrafficInput=input;");
    js.append("return JSON.stringify({state:'ready',image:data,src:src});");
    js.append("}catch(e){return JSON.stringify({state:'error'});}})();");
    return js.toString();
  }

  private static String submitScript(State state, String code) {
    StringBuilder js = new StringBuilder();
    js.append("(function(){try{");
    js.append("var input=window.__autocarTrafficInput;");
    js.append("if(!input||!input.isConnected)return JSON.stringify({state:'stale'});");
    js.append("var d=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');");
    js.append("if(d&&d.set)d.set.call(input,")
        .append(JSONObject.quote(code))
        .append(");else input.value=")
        .append(JSONObject.quote(code))
        .append(";");
    js.append("input.dispatchEvent(new Event('input',{bubbles:true}));");
    js.append("input.dispatchEvent(new Event('change',{bubbles:true}));");
    js.append("var b=document.querySelector(")
        .append(
            JSONObject.quote(state.kind.equals("sutran") ? "#BtnBuscar" : "#ctl00_cplPrincipal_CaptchaContinue"))
        .append(");");
    js.append("if(!b||b.disabled)return JSON.stringify({state:'nobutton'});");
    if (state.kind.equals("sutran")) {
      // validarBuscar() compara contra un código fijo del cliente; se deja que valide el servidor.
      js.append("b.onclick=null;");
    }
    js.append("b.click();return JSON.stringify({state:'sent'});");
    js.append("}catch(e){return JSON.stringify({state:'error'});}})();");
    return js.toString();
  }

  private static JSONObject decodeObject(String encoded) {
    if (encoded == null) return null;
    try {
      Object value = new org.json.JSONTokener(encoded).nextValue();
      if (!(value instanceof String)) return null;
      return new JSONObject((String) value);
    } catch (Exception error) {
      return null;
    }
  }

  private static Bitmap decodeImage(String data) {
    if (data == null || !data.startsWith("data:image")) return null;
    int comma = data.indexOf(',');
    if (comma < 0) return null;
    try {
      byte[] bytes = Base64.decode(data.substring(comma + 1), Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    } catch (Exception error) {
      return null;
    }
  }

  private static String normalize(String text) {
    if (text == null) return "";
    return text.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
  }
}
