package com.example.app_autocar;

import java.text.Normalizer;
import java.util.Locale;

/** Interpret only the official response area after a human-submitted query. */
public final class SutranResponse {
  private SutranResponse() {}

  public static String classify(String response, boolean querySent) {
    if (!querySent || response == null || response.trim().isEmpty()) return "pending";
    String text =
        Normalizer.normalize(response, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    if (text.matches(
        "(?s).*(codigo|captcha).*(incorrect|invalido|no es valido|errado|no coincide).*"))
      return "captcha";
    if (text.matches(
        "(?s).*(error|no se pudo|intente mas tarde|ingrese el codigo|ingrese los datos).*"))
      return "pending";
    if (text.matches(
        "(?s).*(no (se )?(tiene|registra|cuenta"
            + " con|presenta)(.{0,50})(papeleta|infraccion|deuda)|no se"
            + " (encontr(ar|o)|encuentra)(.{0,40})(resultado|informacion|registro|papeleta|infraccion|deuda)|sin"
            + " (papeletas|infracciones|deudas|resultados|registros)).*")) return "empty";
    return "pending";
  }
}
