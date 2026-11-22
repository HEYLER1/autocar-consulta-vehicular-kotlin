package com.example.app_autocar;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Reconocimiento local de captchas basado en ddddocr (MIT,
 * https://github.com/sml2h3/ddddocr).
 *
 * <p>Se ejecuta el modelo original {@code common_old.onnx} con ONNX Runtime para Android. No hay
 * llamadas a servicios externos ni se envía la imagen a ningún servidor. El resultado se limita a
 * letras A-Z, a-z y dígitos, que son los caracteres usados por los portales consultados.
 *
 * <p>El código reconocido es una sugerencia: el usuario puede corregirlo antes de enviarlo. Si el
 * motor no está disponible (por ejemplo, una ABI sin librería nativa), la aplicación continúa con
 * el ingreso manual.
 */
public final class DdddOcrEngine {
  private static final String TAG = "AutocarDdddOcr";
  private static final String MODEL_PATH = "ddddocr/common_old.onnx";
  private static final String CHARSET_PATH = "ddddocr/ascii_charset.json";
  private static final int INPUT_HEIGHT = 64;
  private static final int MAX_INPUT_WIDTH = 1024;
  private static final int MIN_INPUT_WIDTH = 8;

  private static volatile DdddOcrEngine instance;
  private static final ExecutorService EXECUTOR =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "autocar-ddddocr");
            thread.setDaemon(true);
            return thread;
          });

  /** Resultado del reconocimiento. */
  public static final class Result {
    public final String text;
    public final float confidence;

    Result(String text, float confidence) {
      this.text = text;
      this.confidence = confidence;
    }
  }

  public interface Callback {
    /** {@code result} es nulo cuando el reconocimiento no está disponible o falla. */
    void onResult(Result result);
  }

  private final Context appContext;
  private final Map<Integer, Character> charset;
  private OrtEnvironment environment;
  private OrtSession session;
  private String inputName;
  private boolean failed;
  private boolean failureLogged;

  private DdddOcrEngine(Context context) {
    appContext = context;
    Map<Integer, Character> parsed = new HashMap<>();
    try {
      String json = readAsset(context, CHARSET_PATH);
      JSONObject object = new JSONObject(json);
      Iterator<String> keys = object.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        String value = object.getString(key);
        if (!value.isEmpty()) parsed.put(Integer.parseInt(key), value.charAt(0));
      }
    } catch (Throwable error) {
      Log.w(TAG, "No se pudo cargar el alfabeto de ddddocr", error);
    }
    charset = Collections.unmodifiableMap(parsed);
  }

  /** Devuelve la instancia compartida; nunca lanza si el motor no puede inicializarse. */
  public static DdddOcrEngine get(Context context) {
    DdddOcrEngine current = instance;
    if (current == null) {
      synchronized (DdddOcrEngine.class) {
        current = instance;
        if (current == null) {
          Context app = context.getApplicationContext();
          instance = current = new DdddOcrEngine(app == null ? context : app);
        }
      }
    }
    return current;
  }

  /**
   * Reconoce el captcha en segundo plano. La imagen se copia antes de salir del hilo llamador, por
   * lo que el llamador puede reciclarla sin afectar al reconocimiento.
   */
  public static void recognizeAsync(Context context, Bitmap bitmap, Callback callback) {
    if (bitmap == null || bitmap.isRecycled()) {
      callback.onResult(null);
      return;
    }
    Bitmap copy;
    try {
      copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
    } catch (Throwable error) {
      callback.onResult(null);
      return;
    }
    if (copy == null) {
      callback.onResult(null);
      return;
    }
    Context app = context.getApplicationContext();
    Context safeContext = app == null ? context : app;
    EXECUTOR.execute(
        () -> {
          Result result = null;
          try {
            result = get(safeContext).recognize(copy);
          } catch (Throwable error) {
            Log.w(TAG, "Reconocimiento de captcha no disponible", error);
          } finally {
            if (!copy.isRecycled()) copy.recycle();
          }
          callback.onResult(result);
        });
  }

  /** Reconocimiento sincrónico. Debe llamarse fuera del hilo principal. */
  public synchronized Result recognize(Bitmap bitmap) throws Exception {
    if (bitmap == null || bitmap.isRecycled()) return null;
    ensureSession();
    int sourceWidth = bitmap.getWidth();
    int sourceHeight = bitmap.getHeight();
    if (sourceWidth <= 0 || sourceHeight <= 0) return null;

    int targetWidth = (int) (sourceWidth * (INPUT_HEIGHT / (float) sourceHeight));
    targetWidth = Math.max(MIN_INPUT_WIDTH, Math.min(MAX_INPUT_WIDTH, targetWidth));
    Bitmap scaled =
        targetWidth == sourceWidth && sourceHeight == INPUT_HEIGHT
            ? bitmap
            : Bitmap.createScaledBitmap(bitmap, targetWidth, INPUT_HEIGHT, true);

    float[] input;
    try {
      int[] pixels = new int[targetWidth * INPUT_HEIGHT];
      scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, INPUT_HEIGHT);
      input = new float[pixels.length];
      for (int i = 0; i < pixels.length; i++) {
        int pixel = pixels[i];
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = pixel & 0xff;
        // Conversión a gris equivalente a PIL "L": 299/587/114 sobre 1000.
        input[i] = (red * 299 + green * 587 + blue * 114) / 255000f;
      }
    } finally {
      if (scaled != bitmap && !scaled.isRecycled()) scaled.recycle();
    }

    try (OnnxTensor tensor =
            OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(input),
                new long[] {1, 1, INPUT_HEIGHT, targetWidth});
        OrtSession.Result outputs =
            session.run(Collections.singletonMap(inputName, tensor))) {
      OnnxTensor output = (OnnxTensor) outputs.get(0);
      long[] shape = output.getInfo().getShape();
      if (shape.length != 3) return null;
      int steps = (int) shape[0];
      int classes = (int) shape[2];
      if (steps <= 0 || classes <= 0) return null;
      float[] logits = new float[steps * classes];
      FloatBuffer buffer = output.getFloatBuffer();
      buffer.get(logits);
      return decodeText(logits, steps, classes, charset);
    }
  }

  private synchronized void ensureSession() throws Exception {
    if (session != null) return;
    if (failed) throw new IllegalStateException("ddddocr no disponible");
    try {
      byte[] model = readAssetBytes(appContext, MODEL_PATH);
      if (model.length == 0) throw new IllegalStateException("modelo vacío");
      environment = OrtEnvironment.getEnvironment();
      try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
        try {
          options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
          options.setIntraOpNumThreads(
              Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())));
        } catch (Throwable ignored) {
          // Las opciones son una mejora; el modelo funciona con los valores por defecto.
        }
        session = environment.createSession(model, options);
      }
      inputName = session.getInputNames().iterator().next();
    } catch (Throwable error) {
      failed = true;
      if (!failureLogged) {
        failureLogged = true;
        Log.w(TAG, "No se pudo iniciar ONNX Runtime; se usará el ingreso manual", error);
      }
      if (error instanceof Exception) throw (Exception) error;
      throw new IllegalStateException("ONNX Runtime no disponible", error);
    }
  }

  /**
   * Decodifica la salida del modelo con CTC. Es estático y sin dependencias de Android para poder
   * verificarlo en pruebas unitarias.
   */
  static Result decodeText(
      float[] logits, int steps, int classes, Map<Integer, Character> charset) {
    StringBuilder text = new StringBuilder();
    int previous = -1;
    double confidence = 0;
    for (int step = 0; step < steps; step++) {
      int base = step * classes;
      int best = 0;
      float bestValue = logits[base];
      for (int index = 1; index < classes; index++) {
        float value = logits[base + index];
        if (value > bestValue) {
          bestValue = value;
          best = index;
        }
      }
      double sum = 0;
      for (int index = 0; index < classes; index++) {
        sum += StrictMath.exp(logits[base + index] - bestValue);
      }
      confidence += 1.0 / sum;
      if (best != previous && best != 0) {
        Character character = charset.get(best);
        if (character != null) text.append(character.charValue());
      }
      previous = best;
    }
    float average = steps == 0 ? 0f : (float) (confidence / steps);
    return new Result(text.toString().toUpperCase(Locale.ROOT), average);
  }

  private static String readAsset(Context context, String path) throws Exception {
    return new String(readAssetBytes(context, path), "UTF-8");
  }

  private static byte[] readAssetBytes(Context context, String path) throws Exception {
    try (InputStream input = context.getAssets().open(path);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[64 * 1024];
      int read;
      while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
      return output.toByteArray();
    }
  }
}
