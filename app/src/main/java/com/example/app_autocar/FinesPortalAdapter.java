package com.example.app_autocar;

import java.net.URI;
import java.util.Locale;

/** Selectors checked against public forms on 13/10/2026; no captcha solving. */
public final class FinesPortalAdapter {
  final String plate, button, code, image, results, prepare;

  private FinesPortalAdapter(
      String plate, String button, String code, String image, String results, String prepare) {
    this.plate = plate;
    this.button = button;
    this.code = code;
    this.image = image;
    this.results = results;
    this.prepare = prepare;
  }

  static String host(String url) {
    try {
      String h = URI.create(url).getHost();
      return h == null ? "" : h.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
    } catch (Exception e) {
      return "";
    }
  }

  static boolean sameSource(String expected, String actual) {
    String a = host(expected), b = host(actual);
    if ((a.equals("portal.mtc.gob.pe") && b.equals("rec.mtc.gob.pe"))
        || (a.equals("rec.mtc.gob.pe") && b.equals("portal.mtc.gob.pe"))) return true;
    return !a.isEmpty()
        && (a.equals(b)
            || ((a.equals("pagopapeletascallao.pe") || a.equals("pagonube.municallao.gob.pe"))
                && (b.equals("pagopapeletascallao.pe") || b.equals("pagonube.municallao.gob.pe")))
            || ((a.equals("munipiura.gob.pe") || a.equals("www2.munipiura.gob.pe"))
                && (b.equals("munipiura.gob.pe") || b.equals("www2.munipiura.gob.pe"))));
  }

  static FinesPortalAdapter forUrl(String url) {
    switch (host(url)) {
      case "consultavehicular.sunarp.gob.pe":
        return new FinesPortalAdapter(
            "input[placeholder*=placa i],input[id*=placa i]", "", "", "", "", "");
      case "servicios.sbs.gob.pe":
        return new FinesPortalAdapter(
            "#ctl00_MainBodyContent_txtPlaca",
            "#ctl00_MainBodyContent_btnIngresarPla",
            "",
            "",
            "",
            "");
      case "portal.mtc.gob.pe":
      case "rec.mtc.gob.pe":
        return new FinesPortalAdapter(
            "#texFiltro",
            "#btnBuscar",
            "#texCaptcha",
            "#imgCaptcha",
            "#Panel1,#Panel2,#Panel3",
            select("#selBUS_Filtro", "1"));
      case "webexterno.sutran.gob.pe":
        return new FinesPortalAdapter(
            "#txtPlaca", "#BtnBuscar", "#TxtCodImagen", "#iimage", "#gvDeudas", "");
      case "pagopapeletascallao.pe":
      case "pagonube.municallao.gob.pe":
        return new FinesPortalAdapter(
            "#valor_busqueda",
            "#idBuscar",
            "#captcha",
            "img[alt=\"captcha\"]",
            "table.table,.alert",
            select("#tipo_busqueda", "1"));
      case "munitacna.gob.pe":
        return new FinesPortalAdapter(
            "#busca",
            "form.form-mpt button.btn-danger",
            "#codigo",
            "img.img-captcha",
            "[ref=resultado],.list-message",
            select("#opcion", "placa"));
      case "sat-t.gob.pe":
        return new FinesPortalAdapter(
            "#placa_vehiculo",
            "#btn_buscar_papeleta",
            "#gd_captcha_code",
            "#gd_captcha_img",
            "#mostrartabla",
            "");
      case "cusco.gob.pe":
        return new FinesPortalAdapter(
            "#tx_numero",
            "#bt_consultar",
            "",
            "",
            "#ct_tabla",
            select("#sl_tipo", "P")
                + "var r=document.querySelector('input[name=rb_tipo][value=P]');if(r&&!r.checked)r.click();");
      case "munihuanuco.gob.pe":
        return new FinesPortalAdapter(
            "#consulta_placa #placa",
            "#consulta_placa button[type=submit]",
            "",
            "",
            "#table_consulta_pit",
            "");
      case "app.munichachapoyas.gob.pe":
        return new FinesPortalAdapter("#placa_cnt", "#btnConsulta", "", "", "#resultado", "");
      case "consultas.municportillo.gob.pe":
        return new FinesPortalAdapter(
            "form[name=f1] #placa", "form[name=f1] button[type=submit]", "", "", "table.table", "");
      case "munipiura.gob.pe":
      case "www2.munipiura.gob.pe":
        return new FinesPortalAdapter(
            "#buscar-por-placa input[name=PlaMot]",
            "#buscar-por-placa input[type=submit]",
            "",
            "",
            "table",
            "if(!window.__autocarPiura){var"
                + " a=document.querySelector('#tab-buscar-por-placa');if(a){a.click();window.__autocarPiura=true;}}var"
                + " pf=document.querySelector('#buscar-por-placa form');if(pf)pf.target='_self';");
      case "virtualsatch.satch.gob.pe":
        return new FinesPortalAdapter(
            "form#frm input[name=search]",
            "form#frm button[type=submit]",
            "",
            "",
            "section.content table,section.content .alert,section.content .card .body",
            "");
      case "m.satica.gob.pe":
        return new FinesPortalAdapter(
            "form[name=buscar] input[name=placa]",
            "form[name=buscar] button[type=submit]",
            "",
            "",
            "[data-role=content]",
            "");
      case "muniarequipa.gob.pe":
        return new FinesPortalAdapter(
            "#placa", "#btnConsultar", "", "", "#resultado,table.table", "");
      case "satcajamarca.gob.pe":
        return new FinesPortalAdapter(
            "#menu1 input.form-control",
            "#field_1 button.next",
            "",
            "",
            "#field_2,#field_3,table.table",
            "if(!window.__autocarTransit){var"
                + " t=document.querySelector('a[href=\"#menu1\"]');if(t){t.click();window.__autocarTransit=true;}}"
                + select("#opcion_busqueda_record", "2"));
      case "sat.gob.pe":
        return new FinesPortalAdapter(
            "#ctl00_cplPrincipal_txtPlaca",
            "#ctl00_cplPrincipal_CaptchaContinue",
            "",
            "",
            "table[id*=grd],table[id*=Grid],.alert",
            "if(!location.pathname.toLowerCase().endsWith('/papeletas.aspx')){var"
                + " ds=[document];document.querySelectorAll('frame,iframe').forEach(function(f){try{if(f.contentDocument)ds.push(f.contentDocument);}catch(e){}});for(var"
                + " d of ds){var"
                + " a=Array.from(d.querySelectorAll('a[href]')).find(function(a){return"
                + " a.href.includes('/VirtualSAT/modulos/papeletas.aspx?')});if(a){location.href=a.href;return"
                + " 'waiting';}}}var s=document.querySelector('#tipoBusquedaPapeletas');if(s){var"
                + " o=Array.from(s.options).find(function(o){return /por"
                + " placa/i.test(o.text)});if(o&&s.value!==o.value){s.value=o.value;s.dispatchEvent(new"
                + " Event('change',{bubbles:true}));return 'waiting';}}");
      case "pasarela.atu.gob.pe":
        return new FinesPortalAdapter(
            "#PlacaBusquedainputElemento",
            "#formBusqueda button[type=submit]",
            "",
            "",
            "#tablePrincipal table",
            "if(!document.querySelector('#TipoBusquedaselectElemento')){var"
                + " a=Array.from(document.querySelectorAll('#liMenusMain"
                + " a')).find(function(a){return /Consulta y Pago de"
                + " Infracciones/i.test(a.innerText)});if(a&&!window.__autocarOpenAtu){window.__autocarOpenAtu=true;a.click();}}"
                + select("#TipoBusquedaselectElemento", "2"));
      default:
        return new FinesPortalAdapter("", "", "", "", "", "");
    }
  }

  private static String select(String selector, String value) {
    return "var s=document.querySelector('"
        + selector
        + "');if(s&&s.value!=='"
        + value
        + "'){s.value='"
        + value
        + "';s.dispatchEvent(new Event('change',{bubbles:true}));return 'waiting';}";
  }

  static String quote(String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
  }

  String fillScript(String rawPlate) {
    String value = quote(PlateUtils.normalize(rawPlate));
    String tracker = "";
    if (plate.equals("#PlacaBusquedainputElemento")) {
      // Observe the page's own query only. Never manufacture requests or captcha tokens.
      tracker =
          "if(!window.__autocarResponseTracker){window.__autocarResponseTracker=true;var acPlate="
              + value
              + ";function belongs(u,body){try{var url=new URL(u,location.href);return"
              + " (url.hostname==='atu.gob.pe'||url.hostname.endsWith('.atu.gob.pe'))&&(String(u)+'"
              + " '+String(body||'')).toUpperCase().includes(acPlate);}catch(e){return"
              + " false;}}function"
              + " confirmed(data){if(data&&(data.success===false||data.Success===false||data.status===false||data.estado===false||data.IsSuccess===false||data.error===true)){window.__autocarServerRejected=true;}else"
              + " if((Array.isArray(data)&&(data.length===0||JSON.stringify(data).toUpperCase().includes(acPlate)))||(data&&(data.success===true||data.Success===true||data.estado===true||data.IsSuccess===true)))window.__autocarServerResponse=true;}var"
              + " op=XMLHttpRequest.prototype.open,send=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.open=function(method,url){this.__acUrl=url;return"
              + " op.apply(this,arguments);};XMLHttpRequest.prototype.send=function(body){if(belongs(this.__acUrl,body)){this.addEventListener('load',function(){if(this.status>=200&&this.status<300){try{confirmed(this.responseType==='json'?this.response:JSON.parse(this.responseText));}catch(e){}}});}return"
              + " send.apply(this,arguments);};var"
              + " fetch=window.fetch;if(fetch)window.fetch=function(input,options){var"
              + " matched=belongs(input&&input.url||input,options&&options.body);return"
              + " fetch.apply(this,arguments).then(function(r){if(matched&&r.ok)r.clone().json().then(confirmed).catch(function(){});return"
              + " r;});};}";
    }
    return "(function(){"
        + tracker
        + "if(/blocked by a Web Application Firewall|access denied|temporarily"
        + " unavailable/i.test((document.body&&document.body.innerText)||''))return 'blocked';"
        + prepare
        + "var e="
        + (plate.isEmpty() ? "null" : "document.querySelector(" + quote(plate) + ")")
        + ";var b="
        + (button.isEmpty() ? "null" : "document.querySelector(" + quote(button) + ")")
        + ";if(!e)e=Array.from(document.querySelectorAll('input')).find(function(e){return"
        + " !['hidden','submit','button'].includes(e.type)&&/placa|patente/i.test((e.id||'')+'"
        + " '+(e.name||'')+' '+(e.placeholder||'')+'"
        + " '+Array.from(e.labels||[]).map(function(l){return l.innerText}).join('"
        + " '));});if(!e)return 'unsupported';if(!window.__autocarSubmitted&&e.value!=="
        + value
        + "){Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(e,"
        + value
        + ");e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new"
        + " Event('change',{bubbles:true}));e.dispatchEvent(new Event('blur',{bubbles:true}));}"
        + (plate.equals("#PlacaBusquedainputElemento")
            ? "if(window.__autocarServerResponse||window.__autocarServerRejected)return 'response';"
            : "")
        + "var f=e.form||document;var code="
        + (code.isEmpty() ? "null" : "document.querySelector(" + quote(code) + ")")
        + ";if(code)return 'captcha';"
        + (plate.equals("#ctl00_MainBodyContent_txtPlaca")
            ? "var token=document.querySelector('#ctl00_MainBodyContent_hdnReCaptchaV3');if(!token||!token.value)return"
                  + " 'waiting';if(window.__autocarSubmitted)return 'sent';if(!b||b.disabled)return"
                  + " 'waiting';window.__autocarSubmitted=true;b.click();return 'sent';"
            : "")
        + "var solved=f.querySelector('[name=cf-turnstile-response]');var"
        + " interactive=f.querySelector('iframe[src*=recaptcha],iframe[src*=turnstile],.g-recaptcha,.h-captcha,[name*=turnstile"
        + " i]')"
        + (plate.equals("#PlacaBusquedainputElemento")
            ? "||document.querySelector('iframe[src*=recaptcha],.g-recaptcha')"
            : "")
        + ";if(interactive&&!(solved&&solved.value)){"
        + (plate.equals("#PlacaBusquedainputElemento")
            ? "if(b&&!b.disabled&&!window.__autocarVerificationRequested){window.__autocarVerificationRequested=true;b.click();}"
            : "")
        + "return"
        + " 'interactive';}if(f.querySelector('#TxtCodImagen,#gd_captcha_code,#captcha,iframe[src*=captcha],input[name*=captcha"
        + " i],input[id*=captcha i]'))return 'captcha';if(window.__autocarSubmitted)return"
        + " 'sent';var"
        + " bs=b?[b]:Array.from(f.querySelectorAll('button,input[type=submit],input[type=button]')).filter(function(b){return"
        + " /^(buscar(?: papeletas)?|consultar(?: placa| papeletas)?|consulta|ver"
        + " resultado|ingresar)$/i.test((b.innerText||b.value||'').trim())&&!b.disabled&&b.getClientRects().length;});"
        + (plate.startsWith("input[placeholder")
            ? "if(bs.length===0)bs=Array.from(f.querySelectorAll('button,input[type=submit]')).filter(function(b){return"
                  + " /consultar|buscar/i.test(b.innerText||b.value||'')&&!b.disabled&&b.getClientRects().length;});"
            : "")
        + "if(bs.length!==1||bs[0].disabled)return"
        + " 'manual';if(e.form&&!e.form.checkValidity())return"
        + " 'manual';window.__autocarSubmitted=true;window.__autocarQueryAt=Date.now();bs[0].click();return"
        + " 'sent';})();";
  }
}
