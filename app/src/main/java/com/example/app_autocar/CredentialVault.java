package com.example.app_autocar;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/**
 * Almacén cifrado de credenciales por portal (host). La clave AES-256 se guarda en el
 * AndroidKeyStore del dispositivo y nunca sale de él; la contraseña se cifra con AES/GCM y el
 * resultado se persiste en SharedPreferences. El usuario debe autorizar explícitamente el guardado.
 */
final class CredentialVault {
  private static final String TAG = "AutocarCreds";
  private static final String KEYSTORE = "AndroidKeyStore";
  private static final String KEY_ALIAS = "autocar_cred_vault";
  private static final String PREFS = "portal_credentials";
  private static final String FIELD_USER = "u";
  private static final String FIELD_CT = "c";
  private static final String FIELD_IV = "i";

  private CredentialVault() {}

  private static SharedPreferences prefs(Context context) {
    return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  private static SecretKey getOrCreateKey() throws Exception {
    KeyStore store = KeyStore.getInstance(KEYSTORE);
    store.load(null);
    if (store.containsAlias(KEY_ALIAS)) {
      KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null);
      if (entry != null) return entry.getSecretKey();
    }
    KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
    generator.init(
        new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build());
    return generator.generateKey();
  }

  /** Guarda (o sobrescribe) las credenciales del host. Devuelve true si se pudo cifrar. */
  static boolean save(Context context, String host, String username, String password) {
    if (host == null || host.trim().isEmpty() || username == null || password == null) return false;
    String normalized = host.trim().toLowerCase(java.util.Locale.ROOT);
    try {
      SecretKey key = getOrCreateKey();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] iv = cipher.getIV();
      byte[] ct = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
      JSONObject object = new JSONObject();
      object.put(FIELD_USER, username);
      object.put(FIELD_CT, Base64.encodeToString(ct, Base64.NO_WRAP));
      object.put(FIELD_IV, Base64.encodeToString(iv, Base64.NO_WRAP));
      prefs(context).edit().putString(normalized, object.toString()).apply();
      return true;
    } catch (Exception error) {
      Log.w(TAG, "No se pudieron guardar las credenciales", error);
      return false;
    }
  }

  /** Devuelve {username, password} o null si no hay credenciales guardadas para el host. */
  static String[] load(Context context, String host) {
    if (host == null) return null;
    String normalized = host.trim().toLowerCase(java.util.Locale.ROOT);
    String stored = prefs(context).getString(normalized, null);
    if (stored == null) return null;
    try {
      JSONObject object = new JSONObject(stored);
      String username = object.optString(FIELD_USER, "");
      String ctB64 = object.optString(FIELD_CT, "");
      String ivB64 = object.optString(FIELD_IV, "");
      if (username.isEmpty() || ctB64.isEmpty() || ivB64.isEmpty()) return null;
      SecretKey key = getOrCreateKey();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key,
          new GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)));
      byte[] plain = cipher.doFinal(Base64.decode(ctB64, Base64.NO_WRAP));
      String password = new String(plain, StandardCharsets.UTF_8);
      return new String[] {username, password};
    } catch (Exception error) {
      Log.w(TAG, "No se pudieron leer las credenciales", error);
      return null;
    }
  }

  static boolean has(Context context, String host) {
    return load(context, host) != null;
  }

  static void clear(Context context, String host) {
    if (host == null) return;
    prefs(context).edit().remove(host.trim().toLowerCase(java.util.Locale.ROOT)).apply();
  }

  static void clearAll(Context context) {
    prefs(context).edit().clear().apply();
  }

  /** Lista de hosts con credenciales guardadas. */
  static java.util.List<String> hosts(Context context) {
    java.util.List<String> result = new java.util.ArrayList<>();
    for (String key : prefs(context).getAll().keySet()) result.add(key);
    java.util.Collections.sort(result);
    return result;
  }
}
