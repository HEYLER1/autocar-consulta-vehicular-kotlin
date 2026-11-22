package com.example.app_autocar;

import android.os.SystemClock;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** Anonymous AI Horde text generation. No reports or vehicle data are attached. */
final class VehicleAssistant {
  private static final Pattern QWEN_35 = Pattern.compile("qwen[-_/ ]?3[._-]?5(?![0-9])", Pattern.CASE_INSENSITIVE);
  private VehicleAssistant() {}

  static String errorMessage(Exception error) {
    if (error instanceof java.net.UnknownHostException)
      return "No se encontró AI Horde en la red. Revisa tu conexión o DNS.";
    if (error instanceof java.net.SocketTimeoutException)
      return "AI Horde tardó demasiado en responder. Intenta más tarde.";
    if (error instanceof org.json.JSONException || error instanceof IllegalArgumentException)
      return "AI Horde devolvió una respuesta inesperada.";
    if (error instanceof ServiceException) return error.getMessage();
    return "No se pudo completar la conexión con AI Horde. Puedes seguir consultando por placa.";
  }

  private static final class ServiceException extends IOException {
    ServiceException(String message) { super(message); }
  }

  static String ask(String question) throws Exception {
    String model = chooseModel(new JSONArray(request("status/models?type=text", "GET", null)));
    if (model.isEmpty()) return "No hay un Qwen 3.5 disponible en AI Horde. Intenta más tarde.";
    String prompt = "<|im_start|>system\nResponde en español y brevemente. Da orientación vehicular "
        + "general en Perú. No tienes acceso a registros: no inventes resultados ni expliques cómo "
        + "eludir controles de acceso.<|im_end|>\n<|im_start|>user\n" + question
        + "\n/no_think<|im_end|>\n<|im_start|>assistant\n<think>\n</think>\n";
    JSONObject payload = new JSONObject().put("prompt", prompt)
        .put("models", new JSONArray().put(model)).put("trusted_workers", true)
        .put("slow_workers", false).put("context_fit", "reject")
        .put("params", new JSONObject().put("n", 1).put("max_length", 180)
            .put("max_context_length", 2048).put("temperature", 0.2)
            .put("stop_sequence", new JSONArray().put("<|im_end|>").put("<|im_start|>")));
    String id = new JSONObject(request("generate/text/async", "POST", payload)).getString("id");
    if (!UUID.fromString(id).toString().equalsIgnoreCase(id)) throw new IOException("Identificador inválido.");
    String path = "generate/text/status/" + id;
    boolean completed = false;
    try {
      long started = SystemClock.elapsedRealtime();
      while (SystemClock.elapsedRealtime() - started < 65_000) {
        JSONObject status = new JSONObject(request(path, "GET", null));
        if (status.optBoolean("faulted") || !status.optBoolean("is_possible", true))
          return "AI Horde no pudo procesar la pregunta con Qwen. Intenta más tarde.";
        if (status.optBoolean("done")) {
          completed = true;
          JSONArray generations = status.optJSONArray("generations");
          JSONObject result = generations == null ? null : generations.optJSONObject(0);
          if (result == null || !model.equals(result.optString("model")))
            return "AI Horde no devolvió una respuesta del Qwen solicitado.";
          String answer = result.optString("text").replaceAll("(?s)<think>.*?</think>", "").trim();
          return answer.isEmpty() || answer.startsWith("<think>")
              ? "Qwen no devolvió una respuesta utilizable." : answer;
        }
        if (status.optInt("processing") == 0 && SystemClock.elapsedRealtime() - started > 15_000) break;
        Thread.sleep(2500);
      }
      return "AI Horde está ocupado. Intenta más tarde; la consulta por placa sigue disponible.";
    } finally {
      if (!completed) {
        try { request(path, "DELETE", null); } catch (Exception ignored) {}
      }
    }
  }

  static String chooseModel(JSONArray models) {
    String chosen = "";
    double best = Double.POSITIVE_INFINITY;
    for (int i = 0; i < models.length(); i++) {
      JSONObject model = models.optJSONObject(i);
      if (model == null) continue;
      String name = model.optString("name");
      int workers = model.optInt("count");
      double speed = model.optDouble("performance");
      double queued = model.optDouble("queued", 0);
      if (!QWEN_35.matcher(name).find() || workers <= 0 || !Double.isFinite(speed)
          || speed <= 0 || !Double.isFinite(queued) || queued < 0) continue;
      double estimate = (queued + 180) / (speed * workers);
      if (estimate < best) {
        best = estimate;
        chosen = name;
      }
    }
    return chosen;
  }

  private static String request(String path, String method, JSONObject payload) throws Exception {
    HttpURLConnection connection = (HttpURLConnection)
        new URL("https://aihorde.net/api/v2/" + path).openConnection();
    try {
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(6000);
      connection.setReadTimeout(8000);
      connection.setRequestMethod(method);
      connection.setRequestProperty("Client-Agent", "AUTOCAR:1.19:https://github.com/HEYLER1/autocar-consulta-vehicular-kotlin");
      if (!method.equals("GET")) connection.setRequestProperty("apikey", "0000000000");
      if (payload != null) {
        byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        connection.setRequestProperty("Content-Type", "application/json");
        try (java.io.OutputStream out = connection.getOutputStream()) { out.write(body); }
      }
      int status = connection.getResponseCode();
      if (status == 429) throw new ServiceException("AI Horde limitó el acceso anónimo. Intenta más tarde.");
      if (status == 401 || status == 403) throw new ServiceException("AI Horde no aceptó acceso anónimo.");
      if (status < 200 || status >= 300)
        throw new ServiceException("AI Horde rechazó la solicitud (HTTP " + status + ").");
      try (java.io.InputStream in = connection.getInputStream()) {
        byte[] bytes = in.readNBytes(1_000_001);
        if (bytes.length > 1_000_000) throw new IOException("Respuesta de AI Horde demasiado grande.");
        return new String(bytes, StandardCharsets.UTF_8);
      }
    } finally {
      connection.disconnect();
    }
  }
}
