package com.example.app_autocar;

import android.webkit.WebView;
import org.json.JSONObject;

/**
 * Inicio de sesión automático en portales que exigen cuenta. Detecta formularios de login de forma
 * genérica (campo de contraseña + usuario + botón de envío) y, si existen credenciales guardadas para
 * el host, las rellena y envía. Requiere consentimiento explícito del usuario (CredentialVault).
 *
 * <p>No bypasea verificaciones interactivas: si el portal muestra reCAPTCHA/hCaptcha/Turnstile, se
 * deja para que el usuario lo resuelva; el envío se dispara automáticamente al detectar el token.
 */
final class AutoLoginAutomation {
  private AutoLoginAutomation() {}

  /**
   * Intenta iniciar sesión si la página actual es un formulario de login y hay credenciales guardadas.
   * Devuelve true si se envió el formulario.
   */
  static boolean tryAutoLogin(WebView webView, String url) {
    if (webView == null || url == null) return false;
    String[] creds = CredentialVault.load(webView.getContext(), hostOf(url));
    if (creds == null) return false;
    String user = creds[0];
    String pass = creds[1];
    String script =
        "(function(){try{"
            + "var pw=document.querySelector('input[type=\"password\"]');"
            + "if(!pw||pw.value)return false;"
            + "var form=pw.form||pw.closest('form');"
            + "var userField=null;"
            + "if(form){var candidates=form.querySelectorAll('input');for(var i=0;i<candidates.length;i++){var t=candidates[i].type||'';if((t==='text'||t==='email'||t==='tel')&&!candidates[i].disabled){userField=candidates[i];break;}}}"
            + "if(!userField){var all=document.querySelectorAll('input');for(var i=0;i<all.length;i++){var t=all[i].type||'';if((t==='text'||t==='email')&&!all[i].disabled){userField=all[i];break;}}}"
            + "if(!userField)return false;"
            + "var set=function(e,v){var d=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');if(d&&d.set)d.set.call(e,v);else e.value=v;e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));};"
            + "set(userField,"
            + JSONObject.quote(user)
            + ");set(pw,"
            + JSONObject.quote(pass)
            + ");"
            + "var btn=null;if(form)btn=form.querySelector('button[type=\"submit\"],input[type=\"submit\"],button:not([type])');"
            + "if(!btn){var bs=document.querySelectorAll('button,input[type=\"submit\"]');if(bs.length)btn=bs[bs.length-1];}"
            + "if(btn&&!btn.disabled){btn.click();return true;}"
            + "if(form){form.submit();return true;}"
            + "return false;"
            + "}catch(e){return false;}})();";
    final boolean[] sent = {false};
    webView.evaluateJavascript(
        script,
        result -> {
          if ("true".equals(result)) sent[0] = true;
        });
    return sent[0];
  }

  /** Detecta si la página actual sigue siendo un formulario de login (hay campo de contraseña vacío). */
  static boolean isLoginPage(WebView webView) {
    if (webView == null) return false;
    final boolean[] is = {false};
    webView.evaluateJavascript(
        "(function(){try{var pw=document.querySelector('input[type=\"password\"]');return !!(pw&&!pw.value);}catch(e){return false;}})();",
        result -> {
          if ("true".equals(result)) is[0] = true;
        });
    return is[0];
  }

  private static String hostOf(String url) {
    try {
      return new java.net.URI(url).getHost();
    } catch (Exception e) {
      return null;
    }
  }
}
