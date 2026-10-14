package com.example.app_autocar;

import java.util.Locale;

public final class PlateUtils {
  private PlateUtils() {}

  public static String normalize(String value) {
    if (value == null) return "";
    return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
  }

  public static boolean isValidPeruvianPlate(String value) {
    String plate = normalize(value);
    return plate.matches("[A-Z0-9]{6}");
  }

  public static String display(String value) {
    String plate = normalize(value);
    if (plate.length() != 6) return plate;
    return plate.substring(0, 3) + "-" + plate.substring(3);
  }
}
